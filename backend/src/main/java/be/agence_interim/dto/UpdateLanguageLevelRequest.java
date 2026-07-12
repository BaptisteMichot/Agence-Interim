package be.agence_interim.dto;

import be.agence_interim.model.LanguageLevel;
import jakarta.validation.constraints.NotNull;

public record UpdateLanguageLevelRequest(
        @NotNull(message = "Le niveau est obligatoire.") LanguageLevel level) {
}
