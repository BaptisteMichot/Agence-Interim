package be.agence_interim.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import be.agence_interim.model.JobOffer;
import be.agence_interim.model.JobOfferStatus;

/** Vue résumée d'une offre pour les listes (sans les exigences). */
public record JobOfferSummaryResponse(
        int id,
        String title,
        String sector,
        String city,
        LocalDateTime publishedAt,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        JobOfferStatus status,
        String companyName,
        /** Faux dès que l'offre est clôturée ou qu'elle a reçu une candidature. */
        boolean editable) {

    /** Vue destinée à l'intérimaire, qui ne modifie jamais une offre. */
    public static JobOfferSummaryResponse fromEntity(JobOffer offer) {
        return fromEntity(offer, false);
    }

    public static JobOfferSummaryResponse fromEntity(JobOffer offer, boolean editable) {
        return new JobOfferSummaryResponse(
                offer.getId(),
                offer.getTitle(),
                offer.getSector(),
                offer.getCity(),
                offer.getPublishedAt(),
                offer.getSalaryMin(),
                offer.getSalaryMax(),
                offer.getStatus(),
                offer.getEmployer().getCompanyName(),
                editable);
    }
}
