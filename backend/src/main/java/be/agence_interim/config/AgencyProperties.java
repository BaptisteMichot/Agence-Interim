package be.agence_interim.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Identité de l'agence d'intérim, partie signataire de tous les contrats. Elle ne
 * varie pas d'un contrat à l'autre : elle est donc configurée et non stockée en base.
 */
@Component
@ConfigurationProperties(prefix = "app.agency")
@Getter
@Setter
public class AgencyProperties {

    /** Dénomination sociale de l'agence. */
    private String name;

    /** Siège social. */
    private String address;

    /** Numéro d'entreprise (BCE). */
    private String companyNumber;

    /** Numéro d'agrément régional comme entreprise de travail intérimaire. */
    private String licenceNumber;

    /** Commission paritaire du travail intérimaire (CP 322). */
    private String jointCommittee;
}
