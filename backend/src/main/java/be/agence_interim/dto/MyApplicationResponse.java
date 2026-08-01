package be.agence_interim.dto;

import java.time.LocalDateTime;

import be.agence_interim.model.Application;
import be.agence_interim.model.ApplicationStatus;

/** Candidature de l'intérimaire courant, avec un résumé de l'offre concernée. */
public record MyApplicationResponse(
        int id,
        LocalDateTime applicationTime,
        ApplicationStatus status,
        JobOfferSummaryResponse offer) {

    public static MyApplicationResponse fromEntity(Application application) {
        return new MyApplicationResponse(
                application.getId(),
                application.getApplicationTime(),
                application.getStatus(),
                JobOfferSummaryResponse.fromEntity(application.getJobOffer()));
    }
}
