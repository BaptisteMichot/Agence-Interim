package be.agence_interim.dto;

import be.agence_interim.model.Experience;
import java.time.LocalDate;

public record ExperienceResponse(
        int id,
        String companyName,
        String position,
        LocalDate startDate,
        LocalDate endDate) {

    public static ExperienceResponse fromEntity(Experience experience) {
        return new ExperienceResponse(
                experience.getId(),
                experience.getCompanyName(),
                experience.getPosition(),
                experience.getStartDate(),
                experience.getEndDate());
    }
}
