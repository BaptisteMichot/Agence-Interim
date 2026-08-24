package be.agence_interim.dto;

import be.agence_interim.model.EmployerAccessStatus;
import be.agence_interim.model.Role;
import be.agence_interim.model.User;

/**
 * Identité de l'utilisateur connecté. Le jeton n'y figure pas : il voyage dans un
 * cookie HttpOnly, hors de portée du JavaScript de la page.
 */
public record AuthResponse(
        int userId,
        String lastName,
        String firstName,
        String email,
        Role role,
        EmployerAccessStatus employerRequestStatus,
        String message) {

    public static AuthResponse of(
            User user, EmployerAccessStatus employerRequestStatus, String message) {
        return new AuthResponse(
                user.getId(),
                user.getLastName(),
                user.getFirstName(),
                user.getEmail(),
                user.getRole(),
                employerRequestStatus,
                message);
    }
}
