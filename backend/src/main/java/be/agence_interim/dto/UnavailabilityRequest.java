package be.agence_interim.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Indisponibilité déclarée par l'intérimaire (congé, plage non travaillée). */
public record UnavailabilityRequest(
        @NotNull(message = "La date est obligatoire.") LocalDate date,
        @NotNull(message = "L'heure de début est obligatoire.") LocalTime startTime,
        @NotNull(message = "L'heure de fin est obligatoire.") LocalTime endTime,
        @Size(max = 150, message = "Le motif ne doit pas dépasser 150 caractères.") String reason) {
}
