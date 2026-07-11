package be.agence_interim.repository;

import be.agence_interim.model.LanguageJobOffer;
import be.agence_interim.model.LanguageJobOfferId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LanguageJobOfferRepository extends JpaRepository<LanguageJobOffer, LanguageJobOfferId> {
}
