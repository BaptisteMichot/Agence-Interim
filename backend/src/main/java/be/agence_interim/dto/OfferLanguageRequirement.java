package be.agence_interim.dto;

import be.agence_interim.model.LanguageLevel;
import jakarta.validation.constraints.NotNull;

/** Langue requise par une offre (choisie dans la liste fixe). */
public record OfferLanguageRequirement(
        @NotNull(message = "La langue est obligatoire.") Integer languageId,
        @NotNull(message = "Le caractère obligatoire de la langue doit être précisé.") Boolean isMandatory,
        @NotNull(message = "Le niveau requis de la langue est obligatoire.") LanguageLevel requiredLevel) {
}
