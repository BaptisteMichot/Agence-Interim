package be.agence_interim.repository;

import be.agence_interim.model.FavoriteJobOffer;
import be.agence_interim.model.FavoriteJobOfferId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteJobOfferRepository extends JpaRepository<FavoriteJobOffer, FavoriteJobOfferId> {
}
