package be.agence_interim.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.config.AgencyProperties;
import be.agence_interim.dto.AgencyResponse;

/**
 * Identité de l'agence.
 *
 * <p>Route publique : la politique de confidentialité doit nommer le responsable du
 * traitement, et elle se consulte avant toute inscription. L'information vient de la
 * configuration, comme pour les contrats — une seule source, pas de valeur recopiée
 * dans le frontend.
 */
@RestController
@RequestMapping("/api/agency")
public class AgencyController {

    private final AgencyProperties agency;

    public AgencyController(AgencyProperties agency) {
        this.agency = agency;
    }

    @GetMapping
    public AgencyResponse get() {
        return AgencyResponse.fromProperties(agency);
    }
}
