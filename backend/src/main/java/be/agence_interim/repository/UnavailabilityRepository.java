package be.agence_interim.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import be.agence_interim.model.Unavailability;

public interface UnavailabilityRepository extends JpaRepository<Unavailability, Integer> {

    List<Unavailability> findByUserIdAndDateBetweenOrderByDateAscStartTimeAsc(
            int userId, LocalDate from, LocalDate to);

    /** Indisponibilités déjà déclarées un jour donné (contrôle de chevauchement). */
    List<Unavailability> findByUserIdAndDate(int userId, LocalDate date);
}
