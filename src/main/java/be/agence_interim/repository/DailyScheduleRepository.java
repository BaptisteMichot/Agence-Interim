package be.agence_interim.repository;

import be.agence_interim.model.DailySchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyScheduleRepository extends JpaRepository<DailySchedule, Integer> {
}
