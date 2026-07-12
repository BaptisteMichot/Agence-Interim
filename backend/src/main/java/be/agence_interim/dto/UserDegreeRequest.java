package be.agence_interim.dto;

import static be.agence_interim.model.Degree.SECTION_MAX_LENGTH;
import static be.agence_interim.model.DegreeUser.INSTITUTION_MAX_LENGTH;

import be.agence_interim.model.DegreeType;
import jakarta.validation.constraints.Size;

/**
 * Ajout d'un diplôme au profil : soit un diplôme existant ({@code degreeId}),
 * soit un nouveau (type + section). L'établissement et l'année sont propres à l'utilisateur.
 */
public record UserDegreeRequest(
        Integer degreeId,
        DegreeType type,
        @Size(max = SECTION_MAX_LENGTH, message = "La section ne peut pas depasser {max} caracteres.") String section,
        @Size(max = INSTITUTION_MAX_LENGTH, message = "L'etablissement ne peut pas depasser {max} caracteres.") String institution,
        Integer graduationYear) {
}
