package be.agence_interim.dto;

import be.agence_interim.model.Degree;
import be.agence_interim.model.DegreeType;

/** Diplôme proposé dans le référentiel (global ou perso). */
public record DegreeOptionResponse(int id, DegreeType type, String section, boolean custom) {

    public static DegreeOptionResponse fromEntity(Degree degree) {
        return new DegreeOptionResponse(
                degree.getId(),
                degree.getType(),
                degree.getSection(),
                !Boolean.TRUE.equals(degree.getIsGlobal()));
    }
}
