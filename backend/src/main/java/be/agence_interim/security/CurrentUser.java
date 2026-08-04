package be.agence_interim.security;

import org.springframework.security.oauth2.jwt.Jwt;

import be.agence_interim.model.Role;

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

    /** Vrai si le token est celui d'un administrateur de l'agence. */
    public static boolean isAdmin(Jwt jwt) {
        return Role.ADMIN.name().equals(jwt.getClaimAsString("role"));
    }
}
