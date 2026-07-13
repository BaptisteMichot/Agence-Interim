package be.agence_interim.repository;

import be.agence_interim.model.EmployerAccessRequest;
import be.agence_interim.model.EmployerAccessStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmployerAccessRequestRepository extends JpaRepository<EmployerAccessRequest, Integer> {

    /** Dernière demande d'un utilisateur (pour connaître son statut courant). */
    Optional<EmployerAccessRequest> findFirstByUserIdOrderByRequestDateDescIdDesc(int userId);

    boolean existsByUserIdAndStatus(int userId, EmployerAccessStatus status);

    /** Toutes les demandes (en attente + traitées), avec le demandeur, ordonnées par id. */
    @Query("select r from EmployerAccessRequest r join fetch r.user order by r.id")
    List<EmployerAccessRequest> findAllFetchUser();

    long deleteByUserId(int userId);
}
