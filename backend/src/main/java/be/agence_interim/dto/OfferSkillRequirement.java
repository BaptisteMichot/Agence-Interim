package be.agence_interim.dto;

import static be.agence_interim.model.Skill.NAME_MAX_LENGTH;

import be.agence_interim.model.SkillLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Compétence requise par une offre (résolue par nom : globale, perso existante
 * ou créée).
 */
public record OfferSkillRequirement(
        @NotBlank(message = "Le nom de la compétence est obligatoire.") @Size(max = NAME_MAX_LENGTH, message = "La compétence ne peut pas depasser {max} caracteres.") String name,
        @NotNull(message = "Le caractère obligatoire de la compétence doit être précisé.") Boolean isMandatory,
        @NotNull(message = "Le niveau requis de la compétence est obligatoire.") SkillLevel requiredLevel) {
}
