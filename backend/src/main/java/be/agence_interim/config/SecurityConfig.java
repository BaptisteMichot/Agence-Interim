package be.agence_interim.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import be.agence_interim.security.JwtCookieAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    /**
     * En-tête de sécurité commun à toutes les réponses de l'API.
     *
     * <p>Volontairement réduit à {@code frame-ancestors} : une réponse d'API ne charge
     * aucune ressource, la politique de contenu qui compte est celle du document HTML,
     * portée par le frontend. Restreindre davantage ici gênerait l'affichage des PDF
     * servis par l'application (CV, contrats) sans rien protéger de plus.
     */
    private static final String API_CONTENT_SECURITY_POLICY = "frame-ancestors 'none'";

    /**
     * Rend l'API sans session côté serveur. Les endpoints d'authentification sont
     * publics ; les autres routes attendent un JWT valide, lu dans le cookie.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        return http
                .csrf(csrf -> csrf
                        // Jeton double-envoi : le serveur dépose XSRF-TOKEN dans un cookie
                        // lisible par le JavaScript, qui doit le renvoyer en en-tête. Un site
                        // tiers peut faire partir la requête avec le cookie de session, mais
                        // la politique de même origine l'empêche de lire ce second cookie.
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfTokenRequestHandler())
                        // Les trois routes publiques d'authentification sont exemptées : il
                        // n'existe pas encore de session à protéger, et exiger un jeton avant
                        // même de pouvoir se connecter obligerait tout client à un aller-retour
                        // préalable. Le risque résiduel est la « CSRF de connexion » (forcer un
                        // visiteur à se connecter sur un compte tiers) ; la déconnexion, elle,
                        // agit sur une session existante et reste protégée.
                        .ignoringRequestMatchers(
                                "/api/auth/login", "/api/auth/register", "/api/auth/register-employer"))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(API_CONTENT_SECURITY_POLICY))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .permissionsPolicyHeader(permissions -> permissions
                                .policy("camera=(), microphone=(), geolocation=(), payment=()")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Redispatch interne de Spring Boot après un sendError. Exiger une
                        // authentification ici écraserait le statut d'origine : un 403 pour
                        // rôle insuffisant repartait en 401, l'authentification n'étant plus
                        // rejouée sur une redispatch d'erreur.
                        .requestMatchers("/error").permitAll()
                        // Déclarés avant la règle générale : ces deux routes supposent une
                        // session, contrairement à l'inscription et à la connexion.
                        .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()
                        // La WebSocket est authentifiée par le JWT passé à la poignée de main.
                        .requestMatchers("/ws/**").permitAll()
                        // Seul l'employeur démarre un chat (FR10) ; les deux participants échangent ensuite.
                        .requestMatchers(HttpMethod.POST, "/api/chat/conversations/application/**")
                        .hasRole("EMPLOYER")
                        .requestMatchers("/api/chat/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/profile/**").hasRole("JOBSEEKER")
                        .requestMatchers("/api/offers/**").hasRole("JOBSEEKER")
                        .requestMatchers("/api/applications/**").hasRole("JOBSEEKER")
                        .requestMatchers("/api/missions/**").hasRole("JOBSEEKER")
                        // Le contrat est accessible aux deux parties et à l'agence : contrôle dans le service.
                        .requestMatchers("/api/contracts/**").authenticated()
                        .requestMatchers("/api/employer/**").hasRole("EMPLOYER")
                        .anyRequest().authenticated())
                // Une requête non authentifiée reçoit 401 : l'application est une API, il
                // n'y a pas de page de connexion vers laquelle rediriger.
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                        (request, response, failure) -> response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED)))
                // Avant le filtre anonyme : celui-ci pose une authentification
                // « anonymousUser » sur toute requête qui n'en a pas encore, ce qui
                // empêcherait la nôtre de s'installer.
                .addFilterBefore(
                        new JwtCookieAuthenticationFilter(jwtDecoder, jwtAuthenticationConverter()),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Le jeton CSRF est chargé à chaque requête plutôt qu'à la demande : sans cela, le
     * cookie XSRF-TOKEN ne serait déposé qu'au premier appel qui en a besoin, et la
     * page n'aurait rien à renvoyer sur sa toute première écriture.
     */
    private CsrfTokenRequestAttributeHandler csrfTokenRequestHandler() {
        CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
        handler.setCsrfRequestAttributeName(null);
        return handler;
    }

    /** Transforme le claim {@code role} du JWT en autorité Spring {@code ROLE_<role>}. */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            return role == null
                    ? List.of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }

    /** Encode les JWT avec la cle symetrique de l'application. */
    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(jwtSecretKey));
    }

    /** Verifie la signature HS256 et l'expiration des tokens recus. */
    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /** Construit la cle HMAC et refuse une configuration trop courte. */
    @Bean
    SecretKey jwtSecretKey(@Value("${security.jwt.secret}") String secret) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("security.jwt.secret doit contenir au moins 32 caracteres.");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
