package be.agence_interim.dto;

import be.agence_interim.model.Role;
import be.agence_interim.model.User;

public record AuthResponse(
        int userId,
        String lastName,
        String firstName,
        String email,
        Role role,
        String token,
        String message) {

    public static AuthResponse fromUser(User user, String token, String message) {
        return new AuthResponse(
                user.getId(),
                user.getLastName(),
                user.getFirstName(),
                user.getEmail(),
                user.getRole(),
                token,
                message);
    }
}
