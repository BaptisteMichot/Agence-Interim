package be.agence_interim.repository;

import be.agence_interim.model.Unavailability;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnavailabilityRepository extends JpaRepository<Unavailability, Integer> {
}
