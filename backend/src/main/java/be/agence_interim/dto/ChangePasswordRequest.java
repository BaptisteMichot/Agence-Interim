package be.agence_interim.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Changement de mot de passe par un utilisateur connecté.
 *
 * <p>Le mot de passe actuel est redemandé : sans lui, un poste laissé déverrouillé —
 * ou une session détournée — suffirait à s'approprier définitivement le compte.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Le mot de passe actuel est obligatoire.") String currentPassword,
        @StrongPassword String newPassword) {
}
