package be.agence_interim.repository;

import be.agence_interim.model.LanguageUser;
import be.agence_interim.model.LanguageUserId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LanguageUserRepository extends JpaRepository<LanguageUser, LanguageUserId> {
}
