package be.agence_interim.dto;

import static be.agence_interim.model.Skill.NAME_MAX_LENGTH;

import be.agence_interim.model.SkillLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ajout d'une compétence au profil : soit une compétence existante ({@code skillId}),
 * soit une nouvelle compétence perso ({@code name}). Le niveau est obligatoire.
 */
public record UserSkillRequest(
        Integer skillId,
        @Size(max = NAME_MAX_LENGTH, message = "La compétence ne peut pas depasser {max} caracteres.") String name,
        @NotNull(message = "Le niveau est obligatoire.") SkillLevel level) {
}
