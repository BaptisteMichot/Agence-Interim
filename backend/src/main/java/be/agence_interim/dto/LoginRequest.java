package be.agence_interim.dto;

import static be.agence_interim.model.User.EMAIL_MAX_LENGTH;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
        @Size(max = EMAIL_MAX_LENGTH)
        String email,
        @NotBlank
        String password) {
}
