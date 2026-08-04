package be.agence_interim.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import be.agence_interim.model.DailySchedule;
import be.agence_interim.model.MissionStatus;

public interface DailyScheduleRepository extends JpaRepository<DailySchedule, Integer> {

    /** Journées d'un intérimaire sur une période, pour les missions dans un statut donné. */
    @Query("select d from DailySchedule d join fetch d.mission m join fetch m.application a "
            + "join fetch a.jobOffer o join fetch o.employer "
            + "where a.jobSeeker.id = :jobSeekerId and m.status = :status "
            + "and d.date between :from and :to order by d.date, d.startTime")
    List<DailySchedule> findForJobSeekerBetween(
            int jobSeekerId, MissionStatus status, LocalDate from, LocalDate to);

    List<DailySchedule> findByMissionIdOrderByDateAscStartTimeAsc(int missionId);

    /** Journées de plusieurs missions en une requête (listes de missions). */
    List<DailySchedule> findByMissionIdInOrderByDateAscStartTimeAsc(Collection<Integer> missionIds);

    void deleteByMissionId(int missionId);
}
