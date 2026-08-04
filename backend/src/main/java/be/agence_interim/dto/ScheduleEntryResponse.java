package be.agence_interim.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import be.agence_interim.model.DailySchedule;
import be.agence_interim.model.Mission;
import be.agence_interim.model.User;

/** Journée de mission affichée dans le planning de l'intérimaire. */
public record ScheduleEntryResponse(
        int missionId,
        String position,
        String company,
        String workplace,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime) {

    public static ScheduleEntryResponse fromEntity(DailySchedule schedule) {
        Mission mission = schedule.getMission();
        User employer = mission.getApplication().getJobOffer().getEmployer();
        String company = employer.getCompanyName() != null
                ? employer.getCompanyName()
                : employer.getFirstName() + " " + employer.getLastName();
        return new ScheduleEntryResponse(
                mission.getId(),
                mission.getPosition(),
                company,
                mission.getWorkplace(),
                schedule.getDate(),
                schedule.getStartTime(),
                schedule.getEndTime());
    }
}
