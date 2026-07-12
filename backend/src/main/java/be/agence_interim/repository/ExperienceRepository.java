package be.agence_interim.repository;

import be.agence_interim.model.Experience;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceRepository extends JpaRepository<Experience, Integer> {

    List<Experience> findByUserIdOrderByStartDateDesc(int userId);

    Optional<Experience> findByIdAndUserId(int id, int userId);
}
