package be.agence_interim.repository;

import be.agence_interim.model.FavoriteJobOffer;
import be.agence_interim.model.FavoriteJobOfferId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FavoriteJobOfferRepository extends JpaRepository<FavoriteJobOffer, FavoriteJobOfferId> {

    /** Une page des favoris d'un intérimaire, avec l'offre et son employeur chargés. */
    @Query(value = "select f from FavoriteJobOffer f join fetch f.jobOffer o join fetch o.employer "
            + "where f.jobSeeker.id = :jobSeekerId order by o.publishedAt desc",
            countQuery = "select count(f) from FavoriteJobOffer f where f.jobSeeker.id = :jobSeekerId")
    Page<FavoriteJobOffer> findByJobSeekerIdFetchOffer(int jobSeekerId, Pageable pageable);

    /** Parmi les offres de la page affichée, celles que l'intérimaire a mises en favori. */
    @Query("select f.jobOffer.id from FavoriteJobOffer f "
            + "where f.jobSeeker.id = :jobSeekerId and f.jobOffer.id in :offerIds")
    List<Integer> findFavoriteOfferIdsIn(int jobSeekerId, List<Integer> offerIds);

    Optional<FavoriteJobOffer> findByJobSeekerIdAndJobOfferId(int jobSeekerId, int jobOfferId);

    boolean existsByJobSeekerIdAndJobOfferId(int jobSeekerId, int jobOfferId);
}
