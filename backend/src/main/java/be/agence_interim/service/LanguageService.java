package be.agence_interim.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.model.Language;
import be.agence_interim.model.LanguageLevel;
import be.agence_interim.model.LanguageUser;
import be.agence_interim.repository.LanguageRepository;
import be.agence_interim.repository.LanguageUserRepository;
import be.agence_interim.repository.UserRepository;

/** Liste fixe des langues et langues de l'utilisateur courant (pas d'ajout perso). */
@Service
public class LanguageService {

    private final LanguageRepository languageRepository;
    private final LanguageUserRepository languageUserRepository;
    private final UserRepository userRepository;

    public LanguageService(
            LanguageRepository languageRepository,
            LanguageUserRepository languageUserRepository,
            UserRepository userRepository) {
        this.languageRepository = languageRepository;
        this.languageUserRepository = languageUserRepository;
        this.userRepository = userRepository;
    }

    public List<Language> available() {
        return languageRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<LanguageUser> userLanguages(int userId) {
        return languageUserRepository.findByUserIdFetchLanguage(userId);
    }

    @Transactional
    public LanguageUser add(int userId, int languageId, LanguageLevel level) {
        Language language = languageRepository.findById(languageId)
                .orElseThrow(() -> new NoSuchElementException("Langue introuvable."));
        if (languageUserRepository.existsByUserIdAndLanguageId(userId, languageId)) {
            throw new IllegalArgumentException("Cette langue est déjà dans votre profil.");
        }
        LanguageUser languageUser = new LanguageUser();
        languageUser.setLanguage(language);
        languageUser.setUser(userRepository.getReferenceById(userId));
        languageUser.setLevel(level);
        return languageUserRepository.save(languageUser);
    }

    @Transactional
    public LanguageUser updateLevel(int userId, int languageId, LanguageLevel level) {
        LanguageUser languageUser = languageUserRepository.findByUserIdAndLanguageId(userId, languageId)
                .orElseThrow(() -> new NoSuchElementException("Langue introuvable dans votre profil."));
        languageUser.setLevel(level);
        return languageUserRepository.save(languageUser);
    }

    @Transactional
    public void remove(int userId, int languageId) {
        LanguageUser languageUser = languageUserRepository.findByUserIdAndLanguageId(userId, languageId)
                .orElseThrow(() -> new NoSuchElementException("Langue introuvable dans votre profil."));
        languageUserRepository.delete(languageUser);
    }
}
