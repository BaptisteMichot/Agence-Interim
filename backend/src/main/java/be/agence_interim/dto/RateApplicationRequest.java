package be.agence_interim.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Note attribuée par l'employeur à une candidature. */
public record RateApplicationRequest(
        @NotNull(message = "La note est obligatoire.")
        @Min(value = 1, message = "La note doit etre comprise entre 1 et 5.")
        @Max(value = 5, message = "La note doit etre comprise entre 1 et 5.")
        Integer rating) {
}
