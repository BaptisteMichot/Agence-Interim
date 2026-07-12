package be.agence_interim.dto;

import be.agence_interim.model.Language;
import be.agence_interim.model.LanguageLevel;
import be.agence_interim.model.LanguageUser;

/** Langue du profil de l'utilisateur, avec son niveau. */
public record UserLanguageResponse(int languageId, String name, LanguageLevel level) {

    public static UserLanguageResponse fromEntity(LanguageUser languageUser) {
        Language language = languageUser.getLanguage();
        return new UserLanguageResponse(language.getId(), language.getName(), languageUser.getLevel());
    }
}
