package be.agence_interim.dto;

import static be.agence_interim.model.User.FIRST_NAME_MAX_LENGTH;
import static be.agence_interim.model.User.LAST_NAME_MAX_LENGTH;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Mise à jour des champs de base du profil (intérimaire). */
public record UpdateProfileRequest(
        @NotBlank(message = "Le nom est obligatoire.") @Size(max = LAST_NAME_MAX_LENGTH, message = "Le nom ne peut pas depasser {max} caracteres.") String lastName,
        @NotBlank(message = "Le prenom est obligatoire.") @Size(max = FIRST_NAME_MAX_LENGTH, message = "Le prenom ne peut pas depasser {max} caracteres.") String firstName,
        @Past(message = "La date de naissance doit etre dans le passe.") LocalDate birthdate,
        Boolean hasVehicle) {
}
