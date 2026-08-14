package be.agence_interim.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

/**
 * Journée travaillée saisie par l'employeur lors de la création d'une mission.
 * La pause, fixée par l'employeur, est facultative et n'est pas rémunérée.
 */
public record DailySlotRequest(
        @NotNull(message = "La date de la journée est obligatoire.") LocalDate date,
        @NotNull(message = "L'heure de début est obligatoire.") LocalTime startTime,
        @NotNull(message = "L'heure de fin est obligatoire.") LocalTime endTime,
        LocalTime breakStart,
        LocalTime breakEnd) {
}
