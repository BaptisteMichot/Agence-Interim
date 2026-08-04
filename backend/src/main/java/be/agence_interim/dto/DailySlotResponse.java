package be.agence_interim.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import be.agence_interim.model.DailySchedule;

/** Journée travaillée d'une mission. */
public record DailySlotResponse(int id, LocalDate date, LocalTime startTime, LocalTime endTime) {

    public static DailySlotResponse fromEntity(DailySchedule schedule) {
        return new DailySlotResponse(
                schedule.getId(), schedule.getDate(), schedule.getStartTime(), schedule.getEndTime());
    }
}
