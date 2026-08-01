package be.agence_interim.dto;

import java.time.LocalDate;
import java.util.List;

/** Profil complet d'un candidat, consulté par l'employeur depuis une candidature. */
public record CandidateProfileResponse(
        int userId,
        String lastName,
        String firstName,
        String email,
        LocalDate birthdate,
        Boolean hasVehicle,
        boolean hasCv,
        List<UserSkillResponse> skills,
        List<UserDegreeResponse> degrees,
        List<UserLanguageResponse> languages,
        List<ExperienceResponse> experiences,
        List<FormationResponse> formations) {
}
