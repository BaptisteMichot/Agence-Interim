package be.agence_interim.repository;

import be.agence_interim.model.Formation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormationRepository extends JpaRepository<Formation, Integer> {

    List<Formation> findByUserIdOrderByStartDateDesc(int userId);

    Optional<Formation> findByIdAndUserId(int id, int userId);
}
