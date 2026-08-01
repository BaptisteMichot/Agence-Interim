package be.agence_interim.dto;

import java.time.LocalDateTime;

import be.agence_interim.model.Application;
import be.agence_interim.model.User;

/** Candidature reçue sur une offre, vue par l'employeur (candidat + note). */
public record OfferApplicationResponse(
        int id,
        LocalDateTime applicationTime,
        Integer rating,
        int candidateId,
        String firstName,
        String lastName,
        String email) {

    public static OfferApplicationResponse fromEntity(Application application) {
        User candidate = application.getJobSeeker();
        return new OfferApplicationResponse(
                application.getId(),
                application.getApplicationTime(),
                application.getRating(),
                candidate.getId(),
                candidate.getFirstName(),
                candidate.getLastName(),
                candidate.getEmail());
    }
}
