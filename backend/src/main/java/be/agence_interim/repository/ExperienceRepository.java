package be.agence_interim.repository;

import be.agence_interim.model.Experience;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceRepository extends JpaRepository<Experience, Integer> {

    List<Experience> findByUserIdOrderByStartDateDesc(int userId);

    /** Expériences de plusieurs utilisateurs en une requête (contact automatique des candidats). */
    List<Experience> findByUserIdInOrderByStartDateDesc(Collection<Integer> userIds);

    Optional<Experience> findByIdAndUserId(int id, int userId);

    /** Efface les expériences d'un compte clôturé. */
    void deleteByUserId(int userId);
}
