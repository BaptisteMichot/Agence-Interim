package be.agence_interim.repository;

import be.agence_interim.model.SkillUser;
import be.agence_interim.model.SkillUserId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillUserRepository extends JpaRepository<SkillUser, SkillUserId> {
}
