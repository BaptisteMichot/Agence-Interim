package be.agence_interim.dto;

import static be.agence_interim.model.Degree.SECTION_MAX_LENGTH;

import be.agence_interim.model.DegreeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Diplôme requis par une offre (résolu par type + section : global, perso
 * existant ou créé).
 */
public record OfferDegreeRequirement(
        @NotNull(message = "Le type de diplôme est obligatoire.") DegreeType type,
        @NotBlank(message = "La section du diplôme est obligatoire.") @Size(max = SECTION_MAX_LENGTH, message = "La section ne peut pas depasser {max} caracteres.") String section,
        @NotNull(message = "Le caractère obligatoire du diplôme doit être précisé.") Boolean isMandatory) {
}
