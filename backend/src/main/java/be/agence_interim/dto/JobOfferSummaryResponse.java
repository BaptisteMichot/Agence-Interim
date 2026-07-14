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
        String companyName) {

    public static JobOfferSummaryResponse fromEntity(JobOffer offer) {
        return new JobOfferSummaryResponse(
                offer.getId(),
                offer.getTitle(),
                offer.getSector(),
                offer.getCity(),
                offer.getPublishedAt(),
                offer.getSalaryMin(),
                offer.getSalaryMax(),
                offer.getStatus(),
                offer.getEmployer().getCompanyName());
    }
}
