package be.agence_interim.dto;

import be.agence_interim.model.Degree;
import be.agence_interim.model.DegreeType;
import be.agence_interim.model.DegreeUser;

/** Diplôme du profil de l'utilisateur (référentiel + établissement + année). */
public record UserDegreeResponse(
        int degreeId,
        DegreeType type,
        String section,
        boolean custom,
        String institution,
        Integer graduationYear) {

    public static UserDegreeResponse fromEntity(DegreeUser degreeUser) {
        Degree degree = degreeUser.getDegree();
        return new UserDegreeResponse(
                degree.getId(),
                degree.getType(),
                degree.getSection(),
                !Boolean.TRUE.equals(degree.getIsGlobal()),
                degreeUser.getInstitution(),
                degreeUser.getGraduationYear());
    }
}
