package be.agence_interim.dto;

import static be.agence_interim.model.DegreeUser.INSTITUTION_MAX_LENGTH;

import jakarta.validation.constraints.Size;

/** Mise à jour des informations propres au diplôme de l'utilisateur (établissement, année). */
public record UpdateDegreeRequest(
        @Size(max = INSTITUTION_MAX_LENGTH, message = "L'etablissement ne peut pas depasser {max} caracteres.") String institution,
        Integer graduationYear) {
}
