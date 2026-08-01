package be.agence_interim.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import be.agence_interim.model.Application;
import be.agence_interim.model.ApplicationStatus;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    Optional<Application> findByJobSeekerIdAndJobOfferId(int jobSeekerId, int jobOfferId);

    /** Candidatures d'un intérimaire, avec l'offre et son employeur chargés, les plus récentes d'abord. */
    @Query("select a from Application a join fetch a.jobOffer o join fetch o.employer "
            + "where a.jobSeeker.id = :jobSeekerId order by a.applicationTime desc")
    List<Application> findByJobSeekerIdFetchOffer(int jobSeekerId);

    /** Identifiants des offres auxquelles l'intérimaire a une candidature en cours. */
    @Query("select a.jobOffer.id from Application a "
            + "where a.jobSeeker.id = :jobSeekerId and a.status = :status")
    List<Integer> findOfferIdsByJobSeekerIdAndStatus(int jobSeekerId, ApplicationStatus status);

    /** Candidatures d'une offre dans un statut donné, avec le candidat chargé, les plus récentes d'abord. */
    @Query("select a from Application a join fetch a.jobSeeker "
            + "where a.jobOffer.id = :offerId and a.status = :status order by a.applicationTime desc")
    List<Application> findByJobOfferIdAndStatusFetchJobSeeker(int offerId, ApplicationStatus status);

    /** Nombre de candidatures par offre pour un employeur (offres sans candidature absentes). */
    @Query("select a.jobOffer.id as offerId, count(a) as total from Application a "
            + "where a.jobOffer.employer.id = :employerId and a.status = :status group by a.jobOffer.id")
    List<OfferApplicationCount> countByOfferForEmployer(int employerId, ApplicationStatus status);

    interface OfferApplicationCount {
        Integer getOfferId();

        Long getTotal();
    }
}
