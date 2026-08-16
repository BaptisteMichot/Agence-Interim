package be.agence_interim.dto;

import be.agence_interim.model.User;

/** Fiche entreprise de l'employeur connecté. */
public record EmployerCompanyResponse(
        String companyName,
        String address,
        String companyNumber,
        String jointCommittee,
        /** Vrai tant qu'une mention légale manque : aucun contrat ne peut alors être établi. */
        boolean incomplete) {

    public static EmployerCompanyResponse fromEntity(User user) {
        boolean incomplete = isBlank(user.getCompanyName()) || isBlank(user.getAddress())
                || isBlank(user.getCompanyNumber()) || isBlank(user.getJointCommittee());
        return new EmployerCompanyResponse(
                user.getCompanyName(),
                user.getAddress(),
                user.getCompanyNumber(),
                user.getJointCommittee(),
                incomplete);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
