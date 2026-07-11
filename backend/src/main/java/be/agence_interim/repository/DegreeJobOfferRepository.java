package be.agence_interim.repository;

import be.agence_interim.model.DegreeJobOffer;
import be.agence_interim.model.DegreeJobOfferId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DegreeJobOfferRepository extends JpaRepository<DegreeJobOffer, DegreeJobOfferId> {
}
