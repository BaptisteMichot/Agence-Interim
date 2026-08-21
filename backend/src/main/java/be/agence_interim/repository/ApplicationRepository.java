package be.agence_interim.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import be.agence_interim.model.Application;
import be.agence_interim.model.ApplicationStatus;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    Optional<Application> findByJobSeekerIdAndJobOfferId(int jobSeekerId, int jobOfferId);

    /** Vrai dès qu'une candidature a été déposée sur l'offre, même annulée depuis. */
    boolean existsByJobOfferId(int jobOfferId);

    /** Candidatures d'un intérimaire, avec l'offre et son employeur chargés, les plus récentes d'abord. */
    @Query(value = "select a from Application a join fetch a.jobOffer o join fetch o.employer "
            + "where a.jobSeeker.id = :jobSeekerId order by a.applicationTime desc",
            countQuery = "select count(a) from Application a where a.jobSeeker.id = :jobSeekerId")
    Page<Application> findByJobSeekerIdFetchOffer(int jobSeekerId, Pageable pageable);

    /** Nombre de candidatures d'un intérimaire dans un statut donné (chiffre du tableau de bord). */
    long countByJobSeekerIdAndStatus(int jobSeekerId, ApplicationStatus status);

    /** L'intérimaire a-t-il une candidature dans ce statut sur cette offre ? */
    boolean existsByJobSeekerIdAndJobOfferIdAndStatus(
            int jobSeekerId, int jobOfferId, ApplicationStatus status);

    /**
     * Candidatures d'une offre dans un statut donné, avec le candidat chargé.
     * Le tri est porté par le {@link Pageable} : trier en mémoire ne trierait plus
     * que les quelques lignes de la page affichée.
     */
    @Query(value = "select a from Application a join fetch a.jobSeeker "
            + "where a.jobOffer.id = :offerId and a.status = :status",
            countQuery = "select count(a) from Application a "
                    + "where a.jobOffer.id = :offerId and a.status = :status")
    Page<Application> findByJobOfferIdAndStatusFetchJobSeeker(
            int offerId, ApplicationStatus status, Pageable pageable);

    /** Nombre de candidatures en cours par offre, restreint aux offres de la page affichée. */
    @Query("select a.jobOffer.id as offerId, count(a) as total from Application a "
            + "where a.jobOffer.id in :offerIds and a.status = :status group by a.jobOffer.id")
    List<OfferApplicationCount> countByOfferIds(List<Integer> offerIds, ApplicationStatus status);

    /** Nombre total de candidatures en cours reçues par un employeur (chiffre du tableau de bord). */
    long countByJobOfferEmployerIdAndStatus(int employerId, ApplicationStatus status);

    /** Offres de la page affichée ayant déjà reçu au moins une candidature (annulées comprises). */
    @Query("select distinct a.jobOffer.id from Application a where a.jobOffer.id in :offerIds")
    List<Integer> findOfferIdsWithApplicationsIn(List<Integer> offerIds);

    interface OfferApplicationCount {
        Integer getOfferId();

        Long getTotal();
    }
}
