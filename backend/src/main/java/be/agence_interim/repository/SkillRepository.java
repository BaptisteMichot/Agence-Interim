package be.agence_interim.repository;

import be.agence_interim.model.Skill;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Integer> {

    /** Compétences disponibles pour un utilisateur : globales + ses créations perso. */
    List<Skill> findByIsGlobalTrueOrCreatedByIdOrderByNameAsc(int userId);

    Optional<Skill> findFirstByNameIgnoreCaseAndIsGlobalTrue(String name);

    /**
     * Première compétence portant ce nom, quel qu'en soit le créateur. Garantit qu'un
     * même libellé saisi par un employeur et par un intérimaire désigne bien la même
     * ligne : le matching compare les compétences par identifiant.
     */
    Optional<Skill> findFirstByNameIgnoreCaseOrderByIdAsc(String name);

    boolean existsByNameIgnoreCaseAndIsGlobalTrue(String name);
}
