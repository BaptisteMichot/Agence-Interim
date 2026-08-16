package be.agence_interim.service;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import be.agence_interim.dto.UpdateProfileRequest;
import be.agence_interim.model.User;
import be.agence_interim.repository.UserRepository;

/** Gestion des champs de base du profil de l'utilisateur courant. */
@Service
public class ProfileService {

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUser(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable."));
    }

    /** Met à jour les champs de base du profil, adresse et registre national compris. */
    public User updateBase(int userId, UpdateProfileRequest request) {
        User user = getUser(userId);
        user.setLastName(request.lastName());
        user.setFirstName(request.firstName());
        user.setBirthdate(request.birthdate());
        user.setHasVehicle(request.hasVehicle());
        user.setAddress(blankToNull(request.address()));
        user.setNationalNumber(nationalNumber(request.nationalNumber()));
        return userRepository.save(user);
    }

    /** Normalise le numéro de registre national et refuse une clé de contrôle incorrecte. */
    private String nationalNumber(String value) {
        String cleaned = blankToNull(value);
        if (cleaned == null) {
            return null;
        }
        if (!BelgianIdentifiers.isValidNationalNumber(cleaned)) {
            throw new IllegalArgumentException(
                    "Le numéro de registre national est invalide : il compte 11 chiffres "
                            + "et sa clé de contrôle doit correspondre.");
        }
        return BelgianIdentifiers.formatNationalNumber(cleaned);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
