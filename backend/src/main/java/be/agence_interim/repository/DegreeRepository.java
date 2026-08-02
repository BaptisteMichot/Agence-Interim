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

    /**
     * Premier diplôme de ce type et de cette section, quel qu'en soit le créateur.
     * Garantit qu'un même diplôme saisi par un employeur et par un intérimaire désigne
     * bien la même ligne : le matching compare les diplômes par identifiant.
     */
    Optional<Degree> findFirstByTypeAndSectionIgnoreCaseOrderByIdAsc(DegreeType type, String section);

    boolean existsByTypeAndSectionIgnoreCaseAndIsGlobalTrue(DegreeType type, String section);
}
