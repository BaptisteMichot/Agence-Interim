package be.agence_interim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Message envoyé dans une conversation. */
public record SendMessageRequest(
        @NotBlank(message = "Le message ne peut pas etre vide.")
        @Size(max = 2000, message = "Le message ne peut pas depasser {max} caracteres.")
        String content) {
}
