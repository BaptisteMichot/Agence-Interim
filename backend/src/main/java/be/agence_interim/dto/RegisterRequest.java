package be.agence_interim.dto;

import static be.agence_interim.model.User.COMPANY_NAME_MAX_LENGTH;
import static be.agence_interim.model.User.EMAIL_MAX_LENGTH;
import static be.agence_interim.model.User.FIRST_NAME_MAX_LENGTH;
import static be.agence_interim.model.User.LAST_NAME_MAX_LENGTH;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Inscription d'un intérimaire.
 *
 * <p>Le champ {@code cvFilePath} a été retiré : il désigne un emplacement de stockage,
 * c'est-à-dire un détail interne, et il était recopié tel quel sur l'entité sans
 * validation. Le CV se dépose par {@code POST /api/profile/cv}, le seul chemin qui
 * vérifie le contenu du fichier, sa taille et son nom.
 */
public record RegisterRequest(
        @NotBlank(message = "Le nom est obligatoire.") @Size(max = LAST_NAME_MAX_LENGTH, message = "Le nom ne peut pas depasser {max} caracteres.") String lastName,
        @NotBlank(message = "Le prenom est obligatoire.") @Size(max = FIRST_NAME_MAX_LENGTH, message = "Le prenom ne peut pas depasser {max} caracteres.") String firstName,
        @NotBlank(message = "L'email est obligatoire.") @Email(message = "L'email doit etre une adresse valide, par exemple nom@domaine.com.") @Size(max = EMAIL_MAX_LENGTH, message = "L'email ne peut pas depasser {max} caracteres.") String email,
        @StrongPassword String password,
        Boolean hasVehicle,
        @Past(message = "La date de naissance doit etre dans le passe.") LocalDate birthdate,
        @Size(max = COMPANY_NAME_MAX_LENGTH, message = "Le nom de l'entreprise ne peut pas depasser {max} caracteres.") String companyName) {
}
