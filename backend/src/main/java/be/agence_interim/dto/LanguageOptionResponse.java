package be.agence_interim.dto;

import be.agence_interim.model.Language;

/** Langue proposée dans la liste fixe. */
public record LanguageOptionResponse(int id, String name) {

    public static LanguageOptionResponse fromEntity(Language language) {
        return new LanguageOptionResponse(language.getId(), language.getName());
    }
}
