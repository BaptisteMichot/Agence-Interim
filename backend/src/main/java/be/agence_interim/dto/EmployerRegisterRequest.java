package be.agence_interim.dto;

import static be.agence_interim.model.User.COMPANY_NAME_MAX_LENGTH;
import static be.agence_interim.model.User.EMAIL_MAX_LENGTH;
import static be.agence_interim.model.User.FIRST_NAME_MAX_LENGTH;
import static be.agence_interim.model.User.LAST_NAME_MAX_LENGTH;
import static be.agence_interim.model.User.PASSWORD_MIN_LENGTH;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Inscription employeur : compte + informations sur l'entreprise. Flux distinct de
 * l'inscription intérimaire, même si plusieurs champs sont communs.
 */
public record EmployerRegisterRequest(
        @NotBlank(message = "Le nom est obligatoire.") @Size(max = LAST_NAME_MAX_LENGTH, message = "Le nom ne peut pas depasser {max} caracteres.") String lastName,
        @NotBlank(message = "Le prenom est obligatoire.") @Size(max = FIRST_NAME_MAX_LENGTH, message = "Le prenom ne peut pas depasser {max} caracteres.") String firstName,
        @NotBlank(message = "L'email est obligatoire.") @Email(message = "L'email doit etre une adresse valide, par exemple nom@domaine.com.") @Size(max = EMAIL_MAX_LENGTH, message = "L'email ne peut pas depasser {max} caracteres.") String email,
        @NotBlank(message = "Le mot de passe est obligatoire.") @Size(min = PASSWORD_MIN_LENGTH, message = "Le mot de passe doit contenir au moins {min} caracteres.") @Pattern(regexp = ".*[a-z].*", message = "Le mot de passe doit contenir au moins une minuscule.") @Pattern(regexp = ".*[A-Z].*", message = "Le mot de passe doit contenir au moins une majuscule.") @Pattern(regexp = ".*\\d.*", message = "Le mot de passe doit contenir au moins un chiffre.") @Pattern(regexp = ".*[^A-Za-z0-9].*", message = "Le mot de passe doit contenir au moins un caractere special.") String password,
        @NotBlank(message = "Le nom de l'entreprise est obligatoire.") @Size(max = COMPANY_NAME_MAX_LENGTH, message = "Le nom de l'entreprise ne peut pas depasser {max} caracteres.") String companyName) {
}
