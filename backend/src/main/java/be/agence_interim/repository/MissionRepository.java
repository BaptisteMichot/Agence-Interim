package be.agence_interim.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import be.agence_interim.model.Mission;
import be.agence_interim.model.MissionStatus;

public interface MissionRepository extends JpaRepository<Mission, Integer> {

    /** Fragment commun : mission avec sa candidature, son offre, les deux parties et la mission renouvelée. */
    String FETCH_ALL = "select m from Mission m join fetch m.application a join fetch a.jobSeeker "
            + "join fetch a.jobOffer o join fetch o.employer left join fetch m.previousMission ";

    /** Tri commun à toutes les listes : les missions les plus récentes d'abord. */
    String NEWEST_FIRST = "order by m.startDate desc, m.id desc";

    /** Une candidature ne peut porter qu'une mission en cours à la fois. */
    boolean existsByApplicationIdAndStatusIn(int applicationId, Collection<MissionStatus> statuses);

    @Query(FETCH_ALL + "where m.id = :id")
    Optional<Mission> findByIdFetchAll(int id);

    // ------------------------------------------------------------------ employeur

    /** Missions de l'employeur dans un des statuts donnés (en décision, refusées). */
    @Query(value = FETCH_ALL + "where o.employer.id = :employerId and m.status in :statuses " + NEWEST_FIRST,
            countQuery = "select count(m) from Mission m where m.application.jobOffer.employer.id = :employerId "
                    + "and m.status in :statuses")
    Page<Mission> findForEmployerByStatuses(
            int employerId, Collection<MissionStatus> statuses, Pageable pageable);

    /** Missions confirmées de l'employeur dont la période n'est pas écoulée. */
    @Query(value = FETCH_ALL + "where o.employer.id = :employerId and m.status = :active "
            + "and m.endDate >= :today " + NEWEST_FIRST,
            countQuery = "select count(m) from Mission m where m.application.jobOffer.employer.id = :employerId "
                    + "and m.status = :active and m.endDate >= :today")
    Page<Mission> findForEmployerCurrent(
            int employerId, MissionStatus active, LocalDate today, Pageable pageable);

    /** Missions confirmées de l'employeur arrivées à leur terme. */
    @Query(value = FETCH_ALL + "where o.employer.id = :employerId and m.status = :active "
            + "and m.endDate < :today " + NEWEST_FIRST,
            countQuery = "select count(m) from Mission m where m.application.jobOffer.employer.id = :employerId "
                    + "and m.status = :active and m.endDate < :today")
    Page<Mission> findForEmployerPast(
            int employerId, MissionStatus active, LocalDate today, Pageable pageable);

    /** Nombre de missions de l'employeur dans un des statuts donnés (chiffre du tableau de bord). */
    @Query("select count(m) from Mission m where m.application.jobOffer.employer.id = :employerId "
            + "and m.status in :statuses")
    long countForEmployerByStatuses(int employerId, Collection<MissionStatus> statuses);

    // ---------------------------------------------------------------- intérimaire

    /** Missions de l'intérimaire dans un des statuts donnés (propositions à confirmer). */
    @Query(value = FETCH_ALL + "where a.jobSeeker.id = :jobSeekerId and m.status in :statuses " + NEWEST_FIRST,
            countQuery = "select count(m) from Mission m where m.application.jobSeeker.id = :jobSeekerId "
                    + "and m.status in :statuses")
    Page<Mission> findForJobSeekerByStatuses(
            int jobSeekerId, Collection<MissionStatus> statuses, Pageable pageable);

    /**
     * Renouvellements que l'intérimaire a acceptés et qui attendent l'agence. C'est le
     * seul cas où une mission en attente de validation lui est visible : les autres sont
     * des échanges entre l'employeur et l'agence.
     */
    @Query(value = FETCH_ALL + "where a.jobSeeker.id = :jobSeekerId and m.status = :pending "
            + "and m.previousMission is not null " + NEWEST_FIRST,
            countQuery = "select count(m) from Mission m where m.application.jobSeeker.id = :jobSeekerId "
                    + "and m.status = :pending and m.previousMission is not null")
    Page<Mission> findForJobSeekerWaitingAgency(int jobSeekerId, MissionStatus pending, Pageable pageable);

    /** Missions confirmées de l'intérimaire dont la période n'est pas écoulée. */
    @Query(value = FETCH_ALL + "where a.jobSeeker.id = :jobSeekerId and m.status = :active "
            + "and m.endDate >= :today " + NEWEST_FIRST,
            countQuery = "select count(m) from Mission m where m.application.jobSeeker.id = :jobSeekerId "
                    + "and m.status = :active and m.endDate >= :today")
    Page<Mission> findForJobSeekerConfirmed(
            int jobSeekerId, MissionStatus active, LocalDate today, Pageable pageable);

    /** Nombre de missions menées à leur terme par l'intérimaire (chiffre du tableau de bord). */
    @Query("select count(m) from Mission m where m.application.jobSeeker.id = :jobSeekerId "
            + "and m.status = :active and m.endDate < :today")
    long countCompletedForJobSeeker(int jobSeekerId, MissionStatus active, LocalDate today);

    /** Historique de l'intérimaire : missions qu'il a refusées, et missions terminées. */
    @Query(value = FETCH_ALL + "where a.jobSeeker.id = :jobSeekerId "
            + "and (m.status = :declined or (m.status = :active and m.endDate < :today)) " + NEWEST_FIRST,
            countQuery = "select count(m) from Mission m where m.application.jobSeeker.id = :jobSeekerId "
                    + "and (m.status = :declined or (m.status = :active and m.endDate < :today))")
    Page<Mission> findForJobSeekerHistory(
            int jobSeekerId, MissionStatus declined, MissionStatus active, LocalDate today, Pageable pageable);

    // --------------------------------------------------------------------- agence

    /** Missions dans un des statuts donnés, pour l'agence (missions à valider). */
    @Query(value = FETCH_ALL + "where m.status in :statuses " + NEWEST_FIRST,
            countQuery = "select count(m) from Mission m where m.status in :statuses")
    Page<Mission> findForAdminByStatuses(Collection<MissionStatus> statuses, Pageable pageable);

    /** Missions hors des statuts donnés, pour l'agence (historique). */
    @Query(value = FETCH_ALL + "where m.status not in :statuses " + NEWEST_FIRST,
            countQuery = "select count(m) from Mission m where m.status not in :statuses")
    Page<Mission> findForAdminByStatusesNot(Collection<MissionStatus> statuses, Pageable pageable);

    /** Nombre de missions dans un des statuts donnés (badge de l'agence). */
    long countByStatusIn(Collection<MissionStatus> statuses);

    // ------------------------------------------------------------------- contrôles

    /** Une autre mission de l'intérimaire, dans un des statuts donnés, chevauche-t-elle la période ? */
    @Query("select count(m) > 0 from Mission m where m.application.jobSeeker.id = :jobSeekerId "
            + "and m.status in :statuses and m.startDate <= :endDate and m.endDate >= :startDate "
            + "and m.id <> :excludedId")
    boolean existsOverlapping(
            int jobSeekerId, Collection<MissionStatus> statuses, LocalDate startDate, LocalDate endDate,
            int excludedId);

    /** Le poste d'une offre est-il déjà tenu (mission acceptée, ou en attente de réponse) ? */
    boolean existsByApplicationJobOfferIdAndStatusIn(int jobOfferId, Collection<MissionStatus> statuses);

    /** Nombre de missions attendant une décision de l'intérimaire (badge du portail). */
    long countByApplicationJobSeekerIdAndStatusIn(int jobSeekerId, Collection<MissionStatus> statuses);
}
