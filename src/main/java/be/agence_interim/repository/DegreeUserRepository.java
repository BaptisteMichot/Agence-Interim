package be.agence_interim.repository;

import be.agence_interim.model.DegreeUser;
import be.agence_interim.model.DegreeUserId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DegreeUserRepository extends JpaRepository<DegreeUser, DegreeUserId> {
}
