package be.agence_interim.security;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Extrait l'identité de l'utilisateur authentifié à partir du JWT.
 * Le claim {@code userId} est ajouté lors de la génération du token (voir JwtService).
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** Identifiant de l'utilisateur porté par le token. */
    public static int id(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        if (claim instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalStateException("Le token ne contient pas d'identifiant utilisateur valide.");
    }
}
