package be.agence_interim.repository;

import be.agence_interim.model.SkillUser;
import be.agence_interim.model.SkillUserId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SkillUserRepository extends JpaRepository<SkillUser, SkillUserId> {

    @Query("select su from SkillUser su join fetch su.skill where su.user.id = :userId order by su.skill.name")
    List<SkillUser> findByUserIdFetchSkill(int userId);

    /** Compétences de plusieurs utilisateurs en une requête (contact automatique des candidats). */
    @Query("select su from SkillUser su join fetch su.skill where su.user.id in :userIds order by su.skill.name")
    List<SkillUser> findByUserIdInFetchSkill(Collection<Integer> userIds);

    Optional<SkillUser> findByUserIdAndSkillId(int userId, int skillId);

    boolean existsByUserIdAndSkillId(int userId, int skillId);

    /** Efface les compétences déclarées par un compte clôturé. */
    void deleteByUserId(int userId);
}
