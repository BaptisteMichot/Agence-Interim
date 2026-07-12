package be.agence_interim.dto;

import be.agence_interim.model.Experience;
import be.agence_interim.model.Formation;
import be.agence_interim.model.Role;
import be.agence_interim.model.User;
import java.time.LocalDate;
import java.util.List;

/** Profil complet renvoyé au frontend (champs de base + expériences + formations). */
public record ProfileResponse(
        int userId,
        String lastName,
        String firstName,
        String email,
        Role role,
        LocalDate birthdate,
        Boolean hasVehicle,
        String cvFilePath,
        List<ExperienceResponse> experiences,
        List<FormationResponse> formations) {

    public static ProfileResponse of(User user, List<Experience> experiences, List<Formation> formations) {
        return new ProfileResponse(
                user.getId(),
                user.getLastName(),
                user.getFirstName(),
                user.getEmail(),
                user.getRole(),
                user.getBirthdate(),
                user.getHasVehicle(),
                user.getCvFilePath(),
                experiences.stream().map(ExperienceResponse::fromEntity).toList(),
                formations.stream().map(FormationResponse::fromEntity).toList());
    }
}
