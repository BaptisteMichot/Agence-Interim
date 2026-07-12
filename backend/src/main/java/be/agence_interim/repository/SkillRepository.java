package be.agence_interim.repository;

import be.agence_interim.model.Skill;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Integer> {

    /** Compétences disponibles pour un utilisateur : globales + ses créations perso. */
    List<Skill> findByIsGlobalTrueOrCreatedByIdOrderByNameAsc(int userId);

    Optional<Skill> findFirstByNameIgnoreCaseAndIsGlobalTrue(String name);

    Optional<Skill> findFirstByNameIgnoreCaseAndCreatedById(String name, int userId);

    boolean existsByNameIgnoreCaseAndIsGlobalTrue(String name);
}
