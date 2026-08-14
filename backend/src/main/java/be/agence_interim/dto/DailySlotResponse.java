package be.agence_interim.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import be.agence_interim.model.DailySchedule;

/** Journée travaillée d'une mission (pause non rémunérée comprise). */
public record DailySlotResponse(
        int id,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        LocalTime breakStart,
        LocalTime breakEnd) {

    public static DailySlotResponse fromEntity(DailySchedule schedule) {
        return new DailySlotResponse(
                schedule.getId(),
                schedule.getDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getBreakStart(),
                schedule.getBreakEnd());
    }
}
