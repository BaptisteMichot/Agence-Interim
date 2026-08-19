package be.agence_interim.dto;

import static be.agence_interim.model.User.ADDRESS_MAX_LENGTH;
import static be.agence_interim.model.User.FIRST_NAME_MAX_LENGTH;
import static be.agence_interim.model.User.IBAN_MAX_LENGTH;
import static be.agence_interim.model.User.LAST_NAME_MAX_LENGTH;
import static be.agence_interim.model.User.NATIONAL_NUMBER_MAX_LENGTH;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Mise à jour des champs de base du profil (intérimaire). L'adresse, le numéro de
 * registre national et le numéro de compte ne sont pas obligatoires pour utiliser la
 * plateforme, mais ils doivent être renseignés avant qu'une mission puisse être acceptée.
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Le nom est obligatoire.") @Size(max = LAST_NAME_MAX_LENGTH, message = "Le nom ne peut pas depasser {max} caracteres.") String lastName,
        @NotBlank(message = "Le prenom est obligatoire.") @Size(max = FIRST_NAME_MAX_LENGTH, message = "Le prenom ne peut pas depasser {max} caracteres.") String firstName,
        @Past(message = "La date de naissance doit etre dans le passe.") LocalDate birthdate,
        Boolean hasVehicle,
        @Size(max = ADDRESS_MAX_LENGTH, message = "L'adresse ne peut pas depasser {max} caracteres.") String address,
        @Size(max = NATIONAL_NUMBER_MAX_LENGTH, message = "Le numero de registre national ne peut pas depasser {max} caracteres.") String nationalNumber,
        @Size(max = IBAN_MAX_LENGTH, message = "Le numero de compte ne peut pas depasser {max} caracteres.") String iban) {
}
