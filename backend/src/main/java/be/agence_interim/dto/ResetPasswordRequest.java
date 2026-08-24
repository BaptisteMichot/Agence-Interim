package be.agence_interim.dto;

import static be.agence_interim.model.User.EMAIL_MAX_LENGTH;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Nouveau mot de passe, accompagné du code à usage unique reçu par email. */
public record ResetPasswordRequest(
        @NotBlank(message = "L'email est obligatoire.")
        @Size(max = EMAIL_MAX_LENGTH)
        String email,
        @NotBlank(message = "Le code est obligatoire.")
        @Pattern(regexp = "\\s*\\d{6}\\s*", message = "Le code compte 6 chiffres.")
        String code,
        @StrongPassword String newPassword) {
}
