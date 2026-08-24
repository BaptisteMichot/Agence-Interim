package be.agence_interim.dto;

import be.agence_interim.config.AgencyProperties;

/**
 * Identité de l'agence, telle qu'elle figure déjà sur chaque contrat. Publique par
 * nature : c'est elle que la politique de confidentialité désigne comme responsable
 * du traitement.
 */
public record AgencyResponse(
        String name,
        String address,
        String companyNumber,
        String licenceNumber,
        String jointCommittee) {

    public static AgencyResponse fromProperties(AgencyProperties agency) {
        return new AgencyResponse(
                agency.getName(),
                agency.getAddress(),
                agency.getCompanyNumber(),
                agency.getLicenceNumber(),
                agency.getJointCommittee());
    }
}
