package be.agence_interim.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import be.agence_interim.model.DailySchedule;

public interface DailyScheduleRepository extends JpaRepository<DailySchedule, Integer> {

    List<DailySchedule> findByMissionIdOrderByDateAscStartTimeAsc(int missionId);

    /** Journées de plusieurs missions en une requête (listes de missions). */
    List<DailySchedule> findByMissionIdInOrderByDateAscStartTimeAsc(Collection<Integer> missionIds);

    void deleteByMissionId(int missionId);
}
