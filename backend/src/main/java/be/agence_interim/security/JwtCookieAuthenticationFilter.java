package be.agence_interim.security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authentifie la requête à partir du JWT porté par le cookie de session.
 *
 * <p><strong>Pourquoi ce filtre plutôt que {@code oauth2ResourceServer} ?</strong> Le
 * support « serveur de ressources » de Spring dispense automatiquement du CSRF toute
 * requête porteuse d'un jeton, et il reconnaît ces requêtes en interrogeant le
 * {@code BearerTokenResolver} configuré. C'est un raccourci correct tant que le jeton
 * vient d'un en-tête, qu'un navigateur n'envoie jamais de lui-même. Brancher ce
 * résolveur sur le cookie faisait basculer <em>toutes</em> les requêtes authentifiées
 * dans cette exemption : la protection CSRF restait configurée mais ne rejetait plus
 * rien, sans le moindre avertissement. L'authentification est donc explicite ici.
 *
 * <p>Un jeton absent ou invalide n'est pas une erreur : la requête poursuit son chemin
 * sans authentification, et ce sont les règles d'autorisation qui décident.
 *
 * <p>Sa place dans la chaîne compte : il doit s'exécuter <em>avant</em> le filtre
 * anonyme, qui pose sans condition une authentification « anonymousUser ». Placé après,
 * il trouverait le contexte déjà occupé et ne ferait jamais rien.
 *
 * <p>Volontairement pas un {@code @Component} : Spring Boot enregistre tout bean de type
 * {@code Filter} dans la chaîne de servlets, où il s'exécuterait une seconde fois, hors
 * de la chaîne de sécurité. Il est construit par {@code SecurityConfig}, qui seul décide
 * de sa place.
 */
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;
    private final Converter<Jwt, ? extends AbstractAuthenticationToken> authenticationConverter;
    private final UserRepository userRepository;

    public JwtCookieAuthenticationFilter(
            JwtDecoder jwtDecoder,
            Converter<Jwt, ? extends AbstractAuthenticationToken> authenticationConverter,
            UserRepository userRepository) {
        this.jwtDecoder = jwtDecoder;
        this.authenticationConverter = authenticationConverter;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            AuthCookie.read(request).ifPresent(this::authenticate);
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            if (!stillValid(jwt)) {
                SecurityContextHolder.clearContext();
                return;
            }
            // Un contexte neuf, et non une mutation de celui que porte la requête : ce
            // dernier est chargé paresseusement, et le modifier en place peut passer inaperçu.
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authenticationConverter.convert(jwt));
            SecurityContextHolder.setContext(context);
        } catch (JwtException | IllegalStateException e) {
            // Cookie périmé, forgé ou incomplet : on laisse la requête arriver non authentifiée.
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Confronte la version de session portée par le jeton à celle du compte.
     *
     * <p>Une signature valide dit seulement que le jeton a été émis par l'application ;
     * elle ne dit rien de ce qui s'est passé depuis. Cette lecture — une colonne, par
     * requête authentifiée — est le prix de la révocation : sans elle, une déconnexion,
     * un changement de mot de passe ou un retrait de rôle ne prendraient effet qu'à
     * l'expiration du jeton. Un compte supprimé n'a plus de version du tout, et son
     * jeton cesse d'être accepté au même instant.
     */
    private boolean stillValid(Jwt jwt) {
        Optional<Integer> current = userRepository.findTokenVersionById(CurrentUser.id(jwt));
        Integer presented = jwt.getClaim(JwtService.TOKEN_VERSION_CLAIM) instanceof Number number
                ? number.intValue()
                : null;
        return presented != null && current.isPresent() && current.get().equals(presented);
    }
}
