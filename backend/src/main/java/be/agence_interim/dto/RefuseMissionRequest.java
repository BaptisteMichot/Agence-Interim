package be.agence_interim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motif du refus d'une mission par l'agence, transmis à l'employeur. */
public record RefuseMissionRequest(
        @NotBlank(message = "Le motif du refus est obligatoire.")
        @Size(max = 500, message = "Le motif ne doit pas dépasser 500 caractères.")
        String reason) {
}
