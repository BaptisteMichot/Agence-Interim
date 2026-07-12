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

    /** Met à jour le nom, le prénom, la date de naissance et la possession d'un véhicule. */
    public User updateBase(int userId, UpdateProfileRequest request) {
        User user = getUser(userId);
        user.setLastName(request.lastName());
        user.setFirstName(request.firstName());
        user.setBirthdate(request.birthdate());
        user.setHasVehicle(request.hasVehicle());
        return userRepository.save(user);
    }
}
