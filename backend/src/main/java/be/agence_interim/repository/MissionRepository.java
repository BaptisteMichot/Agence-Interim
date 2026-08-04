package be.agence_interim.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import be.agence_interim.model.Mission;
import be.agence_interim.model.MissionStatus;

public interface MissionRepository extends JpaRepository<Mission, Integer> {

    /** Fragment commun : mission avec sa candidature, son offre, les deux parties et la mission renouvelée. */
    String FETCH_ALL = "select m from Mission m join fetch m.application a join fetch a.jobSeeker "
            + "join fetch a.jobOffer o join fetch o.employer left join fetch m.previousMission ";

    /** Une candidature ne peut porter qu'une mission en cours à la fois. */
    boolean existsByApplicationIdAndStatusIn(int applicationId, Collection<MissionStatus> statuses);

    @Query(FETCH_ALL + "where m.id = :id")
    Optional<Mission> findByIdFetchAll(int id);

    /** Missions portant sur les offres d'un employeur, les plus récentes d'abord. */
    @Query(FETCH_ALL + "where o.employer.id = :employerId order by m.startDate desc, m.id desc")
    List<Mission> findByEmployerIdFetchAll(int employerId);

    /** Missions d'un intérimaire, les plus récentes d'abord. */
    @Query(FETCH_ALL + "where a.jobSeeker.id = :jobSeekerId order by m.startDate desc, m.id desc")
    List<Mission> findByJobSeekerIdFetchAll(int jobSeekerId);

    /** Toutes les missions, pour l'agence, les plus récentes d'abord. */
    @Query(FETCH_ALL + "order by m.startDate desc, m.id desc")
    List<Mission> findAllFetchAll();

    /** Missions d'un intérimaire, dans un des statuts donnés, chevauchant la période. */
    @Query("select m from Mission m where m.application.jobSeeker.id = :jobSeekerId "
            + "and m.status in :statuses and m.startDate <= :endDate and m.endDate >= :startDate")
    List<Mission> findOverlapping(
            int jobSeekerId, Collection<MissionStatus> statuses, LocalDate startDate, LocalDate endDate);

    /** Nombre de missions attendant une décision de l'intérimaire (badge du portail). */
    long countByApplicationJobSeekerIdAndStatusIn(int jobSeekerId, Collection<MissionStatus> statuses);
}
