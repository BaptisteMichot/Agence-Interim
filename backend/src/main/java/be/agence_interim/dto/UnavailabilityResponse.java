package be.agence_interim.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import be.agence_interim.model.Unavailability;

/** Indisponibilité de l'intérimaire, avec l'information « modifiable ou non » (délai J+8). */
public record UnavailabilityResponse(
        int id,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String reason,
        boolean editable) {

    public static UnavailabilityResponse of(Unavailability unavailability, LocalDate editableFrom) {
        return new UnavailabilityResponse(
                unavailability.getId(),
                unavailability.getDate(),
                unavailability.getStartTime(),
                unavailability.getEndTime(),
                unavailability.getReason(),
                !unavailability.getDate().isBefore(editableFrom));
    }
}
