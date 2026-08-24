package be.agence_interim.dto;

import static be.agence_interim.model.User.ADDRESS_MAX_LENGTH;
import static be.agence_interim.model.User.COMPANY_NAME_MAX_LENGTH;
import static be.agence_interim.model.User.COMPANY_NUMBER_MAX_LENGTH;
import static be.agence_interim.model.User.JOINT_COMMITTEE_MAX_LENGTH;
import static be.agence_interim.model.User.EMAIL_MAX_LENGTH;
import static be.agence_interim.model.User.FIRST_NAME_MAX_LENGTH;
import static be.agence_interim.model.User.LAST_NAME_MAX_LENGTH;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inscription employeur : compte + informations sur l'entreprise. Flux distinct de
 * l'inscription intérimaire, même si plusieurs champs sont communs.
 */
public record EmployerRegisterRequest(
        @NotBlank(message = "Le nom est obligatoire.") @Size(max = LAST_NAME_MAX_LENGTH, message = "Le nom ne peut pas depasser {max} caracteres.") String lastName,
        @NotBlank(message = "Le prenom est obligatoire.") @Size(max = FIRST_NAME_MAX_LENGTH, message = "Le prenom ne peut pas depasser {max} caracteres.") String firstName,
        @NotBlank(message = "L'email est obligatoire.") @Email(message = "L'email doit etre une adresse valide, par exemple nom@domaine.com.") @Size(max = EMAIL_MAX_LENGTH, message = "L'email ne peut pas depasser {max} caracteres.") String email,
        @StrongPassword String password,
        @NotBlank(message = "Le nom de l'entreprise est obligatoire.") @Size(max = COMPANY_NAME_MAX_LENGTH, message = "Le nom de l'entreprise ne peut pas depasser {max} caracteres.") String companyName,
        @NotBlank(message = "L'adresse du siege est obligatoire.") @Size(max = ADDRESS_MAX_LENGTH, message = "L'adresse ne peut pas depasser {max} caracteres.") String address,
        @NotBlank(message = "Le numero d'entreprise est obligatoire.") @Size(max = COMPANY_NUMBER_MAX_LENGTH, message = "Le numero d'entreprise ne peut pas depasser {max} caracteres.") String companyNumber,
        @NotBlank(message = "La commission paritaire est obligatoire.") @Size(max = JOINT_COMMITTEE_MAX_LENGTH, message = "La commission paritaire ne peut pas depasser {max} caracteres.") String jointCommittee) {
}
