package be.agence_interim.repository;

import be.agence_interim.model.JobOffer;
import be.agence_interim.model.JobOfferStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JobOfferRepository extends JpaRepository<JobOffer, Integer> {

    /** Une page des offres d'un employeur, les plus récentes d'abord. */
    @Query(value = "select o from JobOffer o join fetch o.employer where o.employer.id = :employerId "
            + "order by o.publishedAt desc",
            countQuery = "select count(o) from JobOffer o where o.employer.id = :employerId")
    Page<JobOffer> findByEmployerIdFetchEmployer(int employerId, Pageable pageable);

    /** Nombre d'offres d'un employeur dans un statut donné (chiffre du tableau de bord). */
    long countByEmployerIdAndStatus(int employerId, JobOfferStatus status);

    Optional<JobOffer> findByIdAndEmployerId(int id, int employerId);

    /** Une page des offres d'un statut donné, avec leur employeur, les plus récentes d'abord. */
    @Query(value = "select o from JobOffer o join fetch o.employer where o.status = :status "
            + "order by o.publishedAt desc",
            countQuery = "select count(o) from JobOffer o where o.status = :status")
    Page<JobOffer> findByStatusFetchEmployer(JobOfferStatus status, Pageable pageable);

    /**
     * Toutes les offres d'un statut, pour le calcul du score de correspondance.
     * Un score ne se calcule pas en SQL : le classement impose de parcourir toutes
     * les offres ouvertes avant de pouvoir en découper une page.
     */
    @Query("select o from JobOffer o join fetch o.employer where o.status = :status order by o.publishedAt desc")
    List<JobOffer> findAllByStatusFetchEmployer(JobOfferStatus status);
}
