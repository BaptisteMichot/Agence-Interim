package be.agence_interim.dto;

import be.agence_interim.model.SkillLevel;
import jakarta.validation.constraints.NotNull;

public record UpdateSkillLevelRequest(
        @NotNull(message = "Le niveau est obligatoire.") SkillLevel level) {
}
