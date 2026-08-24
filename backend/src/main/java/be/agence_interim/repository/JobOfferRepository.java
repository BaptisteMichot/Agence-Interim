package be.agence_interim.repository;

import be.agence_interim.model.JobOffer;
import be.agence_interim.model.JobOfferStatus;
import be.agence_interim.model.Province;
import be.agence_interim.model.Sector;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JobOfferRepository extends JpaRepository<JobOffer, Integer> {

    /**
     * Critères de recherche des offres, écrits une seule fois pour la page et pour son
     * décompte. Chaque ligne se neutralise quand son paramètre est nul, de sorte qu'une
     * requête unique couvre toutes les combinaisons de filtres.
     *
     * <p>Le salaire demandé est comparé au <em>haut</em> de la fourchette annoncée :
     * une offre est retenue dès qu'elle peut atteindre le montant souhaité. Une offre
     * qui n'annonce aucune fourchette est écartée, sans quoi le filtre ne garantirait
     * plus que ce qu'il affiche vaut le montant demandé. L'expérience est stockée en
     * texte (cf. le MPD) mais comparée en nombre ; une offre qui n'exige aucune
     * expérience convient toujours.
     */
    String OPEN_FILTERS = """
            where o.status = :status
              and (:keyword is null or lower(o.title) like :keyword or lower(o.description) like :keyword)
              and (:sector is null or o.sector = :sector)
              and (:province is null or o.province = :province)
              and (:minHourlyWage is null or coalesce(o.salaryMax, o.salaryMin) >= :minHourlyWage)
              and (:maxExperienceYears is null or o.experienceTime is null
                   or cast(o.experienceTime as Integer) <= :maxExperienceYears)
              and (:noVehicleRequired = false or o.vehicleMandatory is null or o.vehicleMandatory = false)
            """;

    /** Une page des offres d'un employeur, les plus récentes d'abord. */
    @Query(value = "select o from JobOffer o join fetch o.employer where o.employer.id = :employerId "
            + "order by o.publishedAt desc",
            countQuery = "select count(o) from JobOffer o where o.employer.id = :employerId")
    Page<JobOffer> findByEmployerIdFetchEmployer(int employerId, Pageable pageable);

    /** Nombre d'offres d'un employeur dans un statut donné (chiffre du tableau de bord). */
    long countByEmployerIdAndStatus(int employerId, JobOfferStatus status);

    Optional<JobOffer> findByIdAndEmployerId(int id, int employerId);

    /** Une page des offres retenues par les critères, avec leur employeur, les plus récentes d'abord. */
    @Query(value = "select o from JobOffer o join fetch o.employer " + OPEN_FILTERS
            + " order by o.publishedAt desc",
            countQuery = "select count(o) from JobOffer o " + OPEN_FILTERS)
    Page<JobOffer> search(
            JobOfferStatus status,
            String keyword,
            Sector sector,
            Province province,
            BigDecimal minHourlyWage,
            Integer maxExperienceYears,
            boolean noVehicleRequired,
            Pageable pageable);

    /**
     * Toutes les offres retenues par les critères, pour le calcul du score de
     * correspondance. Un score ne se calcule pas en SQL : le classement impose de
     * parcourir toutes les offres candidates avant de pouvoir en découper une page.
     * Les critères, eux, restent filtrés en base — il serait inutile de scorer des
     * offres que l'intérimaire vient d'écarter.
     */
    @Query("select o from JobOffer o join fetch o.employer " + OPEN_FILTERS + " order by o.publishedAt desc")
    List<JobOffer> searchAll(
            JobOfferStatus status,
            String keyword,
            Sector sector,
            Province province,
            BigDecimal minHourlyWage,
            Integer maxExperienceYears,
            boolean noVehicleRequired);
}
