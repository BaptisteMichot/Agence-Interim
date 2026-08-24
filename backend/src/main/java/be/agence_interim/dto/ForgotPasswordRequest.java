package be.agence_interim.dto;

import static be.agence_interim.model.User.EMAIL_MAX_LENGTH;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Demande d'un code de réinitialisation, envoyé sur l'adresse du compte. */
public record ForgotPasswordRequest(
        @NotBlank(message = "L'email est obligatoire.")
        @Size(max = EMAIL_MAX_LENGTH)
        String email) {
}
