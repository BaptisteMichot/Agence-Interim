package be.agence_interim.repository;

import be.agence_interim.model.Degree;
import be.agence_interim.model.DegreeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DegreeRepository extends JpaRepository<Degree, Integer> {

    /** Diplômes disponibles pour un utilisateur : globaux + ses créations perso. */
    List<Degree> findByIsGlobalTrueOrCreatedByIdOrderByTypeAscSectionAsc(int userId);

    Optional<Degree> findFirstByTypeAndSectionIgnoreCaseAndIsGlobalTrue(DegreeType type, String section);

    Optional<Degree> findFirstByTypeAndSectionIgnoreCaseAndCreatedById(DegreeType type, String section, int userId);

    boolean existsByTypeAndSectionIgnoreCaseAndIsGlobalTrue(DegreeType type, String section);
}
