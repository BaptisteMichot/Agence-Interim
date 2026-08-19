package be.agence_interim.repository;

import be.agence_interim.model.JobOffer;
import be.agence_interim.model.JobOfferStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JobOfferRepository extends JpaRepository<JobOffer, Integer> {

    /** Offres d'un employeur, les plus récentes d'abord. */
    List<JobOffer> findByEmployerIdOrderByPublishedAtDesc(int employerId);

    Optional<JobOffer> findByIdAndEmployerId(int id, int employerId);

    /** Offres d'un statut donné avec leur employeur (consultation par les intérimaires), les plus récentes d'abord. */
    @Query("select o from JobOffer o join fetch o.employer where o.status = :status order by o.publishedAt desc")
    List<JobOffer> findByStatusFetchEmployer(JobOfferStatus status);
}
