package be.agence_interim.dto;

import static be.agence_interim.model.User.PASSWORD_MIN_LENGTH;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Politique de mot de passe de l'application, écrite une seule fois.
 *
 * <p>Contrainte composée : elle n'a pas de validateur propre
 * ({@code validatedBy = {}}), elle agrège celles qu'elle porte. Les quatre formulaires
 * qui acceptent un mot de passe — inscription intérimaire, inscription employeur,
 * changement et réinitialisation — appliquent ainsi exactement la même règle. Avant, la
 * liste des six annotations était recopiée sur chaque champ : une politique dupliquée
 * finit toujours par diverger, et c'est en général l'exemplaire oublié qui est le plus
 * permissif.
 *
 * <p>Un plafond de longueur accompagne le plancher. BCrypt ne considère que les
 * 72 premiers octets ; au-delà, la fin du mot de passe ne protège plus rien, et rien ne
 * justifie de faire hacher un mégaoctet au serveur.
 */
@Documented
@NotBlank(message = "Le mot de passe est obligatoire.")
@Size(min = PASSWORD_MIN_LENGTH, max = StrongPassword.MAX_LENGTH,
        message = "Le mot de passe doit contenir au moins {min} caracteres.")
@Pattern(regexp = ".*[a-z].*", message = "Le mot de passe doit contenir au moins une minuscule.")
@Pattern(regexp = ".*[A-Z].*", message = "Le mot de passe doit contenir au moins une majuscule.")
@Pattern(regexp = ".*\\d.*", message = "Le mot de passe doit contenir au moins un chiffre.")
@Pattern(regexp = ".*[^A-Za-z0-9].*", message = "Le mot de passe doit contenir au moins un caractere special.")
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD,
        ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    /** Au-delà, BCrypt ignore le surplus : le refuser est plus honnête que le tronquer. */
    int MAX_LENGTH = 72;

    String message() default "Le mot de passe ne respecte pas la politique de securite.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
