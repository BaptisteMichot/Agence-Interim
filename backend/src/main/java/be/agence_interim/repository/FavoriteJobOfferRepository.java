package be.agence_interim.repository;

import be.agence_interim.model.FavoriteJobOffer;
import be.agence_interim.model.FavoriteJobOfferId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FavoriteJobOfferRepository extends JpaRepository<FavoriteJobOffer, FavoriteJobOfferId> {

    /** Favoris d'un intérimaire, avec l'offre et son employeur chargés. */
    @Query("select f from FavoriteJobOffer f join fetch f.jobOffer o join fetch o.employer where f.jobSeeker.id = :jobSeekerId order by o.publishedAt desc")
    List<FavoriteJobOffer> findByJobSeekerIdFetchOffer(int jobSeekerId);

    /** Identifiants des offres mises en favori par un intérimaire. */
    @Query("select f.jobOffer.id from FavoriteJobOffer f where f.jobSeeker.id = :jobSeekerId")
    List<Integer> findOfferIdsByJobSeekerId(int jobSeekerId);

    Optional<FavoriteJobOffer> findByJobSeekerIdAndJobOfferId(int jobSeekerId, int jobOfferId);

    boolean existsByJobSeekerIdAndJobOfferId(int jobSeekerId, int jobOfferId);
}
