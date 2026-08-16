package be.agence_interim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Code à usage unique reçu par email, confirmant la signature du contrat. */
public record SignContractRequest(
        @NotBlank(message = "Le code de signature est obligatoire.")
        @Pattern(regexp = "\\s*\\d{6}\\s*", message = "Le code de signature compte 6 chiffres.")
        String code) {
}
