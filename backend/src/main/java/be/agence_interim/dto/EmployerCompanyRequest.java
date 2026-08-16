package be.agence_interim.dto;

import static be.agence_interim.model.User.ADDRESS_MAX_LENGTH;
import static be.agence_interim.model.User.COMPANY_NAME_MAX_LENGTH;
import static be.agence_interim.model.User.COMPANY_NUMBER_MAX_LENGTH;
import static be.agence_interim.model.User.JOINT_COMMITTEE_MAX_LENGTH;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Mentions légales de l'entreprise utilisatrice, reprises sur les contrats de mission. */
public record EmployerCompanyRequest(
        @NotBlank(message = "Le nom de l'entreprise est obligatoire.")
        @Size(max = COMPANY_NAME_MAX_LENGTH, message = "Le nom de l'entreprise ne peut pas depasser {max} caracteres.")
        String companyName,
        @NotBlank(message = "L'adresse du siege est obligatoire.")
        @Size(max = ADDRESS_MAX_LENGTH, message = "L'adresse ne peut pas depasser {max} caracteres.")
        String address,
        @NotBlank(message = "Le numero d'entreprise est obligatoire.")
        @Size(max = COMPANY_NUMBER_MAX_LENGTH, message = "Le numero d'entreprise ne peut pas depasser {max} caracteres.")
        String companyNumber,
        @NotBlank(message = "La commission paritaire est obligatoire.")
        @Size(max = JOINT_COMMITTEE_MAX_LENGTH, message = "La commission paritaire ne peut pas depasser {max} caracteres.")
        String jointCommittee) {
}
