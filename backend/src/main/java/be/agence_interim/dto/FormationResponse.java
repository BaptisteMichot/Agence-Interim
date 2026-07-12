package be.agence_interim.dto;

import be.agence_interim.model.Formation;
import be.agence_interim.model.FormationStatus;
import java.time.LocalDate;

public record FormationResponse(
        int id,
        String title,
        String institution,
        LocalDate startDate,
        LocalDate endDate,
        FormationStatus status) {

    public static FormationResponse fromEntity(Formation formation) {
        return new FormationResponse(
                formation.getId(),
                formation.getTitle(),
                formation.getInstitution(),
                formation.getStartDate(),
                formation.getEndDate(),
                formation.getStatus());
    }
}
