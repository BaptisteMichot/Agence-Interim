package be.agence_interim.dto;

import java.math.BigDecimal;

import be.agence_interim.model.Province;
import be.agence_interim.model.Sector;

/**
 * Critères de recherche d'offres saisis par l'intérimaire.
 *
 * <p>Un champ nul signifie « pas de filtre sur ce critère » : la requête compare
 * chaque paramètre à {@code null} avant de l'appliquer, ce qui permet à une seule
 * requête de couvrir toutes les combinaisons de critères.
 */
public record OfferFilter(
        String keyword,
        Sector sector,
        Province province,
        BigDecimal minHourlyWage,
        Integer maxExperienceYears,
        boolean noVehicleRequired) {

    /**
     * Motif {@code like} du mot-clé, en minuscules et entouré de {@code %}, ou
     * {@code null} si aucun mot-clé n'a été saisi.
     */
    public String keywordPattern() {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }
}
