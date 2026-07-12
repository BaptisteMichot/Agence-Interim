package be.agence_interim.dto;

import be.agence_interim.model.LanguageLevel;
import jakarta.validation.constraints.NotNull;

/** Ajout d'une langue au profil (choisie dans la liste fixe) avec son niveau. */
public record UserLanguageRequest(
        @NotNull(message = "La langue est obligatoire.") Integer languageId,
        @NotNull(message = "Le niveau est obligatoire.") LanguageLevel level) {
}
