package be.agence_interim.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import be.agence_interim.model.JobOffer;
import be.agence_interim.model.JobOfferStatus;
import be.agence_interim.model.Province;
import be.agence_interim.model.Sector;

/**
 * Vue résumée d'une offre pour les listes (sans les exigences).
 *
 * <p>Les deux derniers champs évitent au frontend d'aller chercher, en plus de la
 * page affichée, la liste complète des favoris ou des compteurs de candidatures :
 * ils sont calculés pour les seules offres de la page.
 */
public record JobOfferSummaryResponse(
        int id,
        String title,
        Sector sector,
        String city,
        Province province,
        LocalDateTime publishedAt,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        JobOfferStatus status,
        String companyName,
        /** Faux dès que l'offre est clôturée ou qu'elle a reçu une candidature. */
        boolean editable,
        /** Vue intérimaire : l'offre est dans ses favoris. */
        boolean favorite,
        /** Vue employeur : nombre de candidatures en cours reçues sur l'offre. */
        long applicationCount) {

    /** Vue destinée à l'intérimaire, qui ne modifie jamais une offre. */
    public static JobOfferSummaryResponse fromEntity(JobOffer offer) {
        return forJobSeeker(offer, false);
    }

    public static JobOfferSummaryResponse forJobSeeker(JobOffer offer, boolean favorite) {
        return of(offer, false, favorite, 0);
    }

    public static JobOfferSummaryResponse forEmployer(JobOffer offer, boolean editable, long applicationCount) {
        return of(offer, editable, false, applicationCount);
    }

    private static JobOfferSummaryResponse of(
            JobOffer offer, boolean editable, boolean favorite, long applicationCount) {
        return new JobOfferSummaryResponse(
                offer.getId(),
                offer.getTitle(),
                offer.getSector(),
                offer.getCity(),
                offer.getProvince(),
                offer.getPublishedAt(),
                offer.getSalaryMin(),
                offer.getSalaryMax(),
                offer.getStatus(),
                offer.getEmployer().getCompanyName(),
                editable,
                favorite,
                applicationCount);
    }
}
