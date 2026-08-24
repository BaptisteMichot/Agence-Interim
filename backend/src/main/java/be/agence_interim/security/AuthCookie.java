package be.agence_interim.security;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Cookie porteur du JWT.
 *
 * <p>{@code HttpOnly} : le JavaScript de la page ne peut pas le lire, donc une injection
 * XSS ne peut pas voler la session — contrairement à un jeton rangé dans
 * {@code sessionStorage}. En contrepartie, le navigateur l'envoie de lui-même sur toute
 * requête vers l'origine, y compris celles déclenchées par un site tiers : c'est
 * exactement ce qu'exploite le CSRF, traité par le jeton double-envoi de
 * {@link be.agence_interim.config.SecurityConfig}.
 *
 * <p>{@code SameSite=Strict} constitue la première barrière : le cookie n'accompagne
 * aucune requête initiée depuis un autre site. Un lien reçu par email amène bien
 * l'utilisateur sur l'application sans cookie, mais les appels que la page émet ensuite
 * sont de même site et l'emportent avec eux.
 */
@Component
public class AuthCookie {

    /** Nom du cookie de session. */
    public static final String NAME = "auth-token";

    private final boolean secure;
    private final Duration maxAge;

    public AuthCookie(
            @Value("${app.security.cookie-secure}") boolean secure,
            @Value("${security.jwt.expiration-minutes}") long expirationMinutes) {
        this.secure = secure;
        this.maxAge = Duration.ofMinutes(expirationMinutes);
    }

    /** Cookie de connexion, de même durée de vie que le jeton qu'il transporte. */
    public ResponseCookie issue(String token) {
        return base(token).maxAge(maxAge).build();
    }

    /**
     * Cookie de déconnexion : même définition, valeur vide et durée nulle. Le navigateur
     * ne remplace un cookie que si le nom, le chemin et le domaine coïncident.
     */
    public ResponseCookie clear() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                // Faux en développement, où l'application est servie en clair : un cookie
                // « Secure » y serait purement et simplement ignoré par le navigateur.
                .secure(secure)
                .sameSite("Strict")
                .path("/");
    }

    /**
     * Jeton porté par la requête, s'il en porte un.
     *
     * <p>Lambdas plutôt que références de méthode : une référence non liée fait passer
     * le receveur pour un paramètre du descripteur, ce que l'analyse de nullité d'Eclipse
     * signale par « Null type safety: parameter 'this' … needs unchecked conversion ».
     * C'est la convention du reste du projet.
     */
    public static Optional<String> read(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies())
                .stream()
                .flatMap(cookies -> Arrays.stream(cookies))
                .filter(cookie -> NAME.equals(cookie.getName()))
                .map(cookie -> cookie.getValue())
                .filter(value -> !value.isBlank())
                .findFirst();
    }
}
