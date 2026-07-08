package be.agence_interim.repository;

import be.agence_interim.model.EmployerAccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployerAccessRequestRepository extends JpaRepository<EmployerAccessRequest, Integer> {
}
