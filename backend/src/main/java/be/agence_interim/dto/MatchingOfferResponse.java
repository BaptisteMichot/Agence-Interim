package be.agence_interim.dto;

/** Offre correspondant au profil de l'intérimaire, avec son score de correspondance (0-100). */
public record MatchingOfferResponse(JobOfferSummaryResponse offer, int score) {
}
