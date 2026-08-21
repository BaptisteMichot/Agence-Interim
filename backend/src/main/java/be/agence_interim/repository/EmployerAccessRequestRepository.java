package be.agence_interim.repository;

import be.agence_interim.model.EmployerAccessRequest;
import be.agence_interim.model.EmployerAccessStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmployerAccessRequestRepository extends JpaRepository<EmployerAccessRequest, Integer> {

    /** Dernière demande d'un utilisateur (pour connaître son statut courant). */
    Optional<EmployerAccessRequest> findFirstByUserIdOrderByRequestDateDescIdDesc(int userId);

    /** Une page des demandes dans un statut donné, la plus ancienne d'abord (file d'attente). */
    @Query(value = "select r from EmployerAccessRequest r join fetch r.user where r.status = :status order by r.id",
            countQuery = "select count(r) from EmployerAccessRequest r where r.status = :status")
    Page<EmployerAccessRequest> findByStatusFetchUser(EmployerAccessStatus status, Pageable pageable);

    /** Une page des demandes déjà tranchées, la plus récente d'abord (historique). */
    @Query(value = "select r from EmployerAccessRequest r join fetch r.user where r.status <> :status "
            + "order by r.id desc",
            countQuery = "select count(r) from EmployerAccessRequest r where r.status <> :status")
    Page<EmployerAccessRequest> findByStatusNotFetchUser(EmployerAccessStatus status, Pageable pageable);

    /**
     * Première demande de chacun des utilisateurs donnés. Une demande dont l'identifiant
     * est supérieur à cette première est une re-soumission — c'est ainsi que l'étiquette
     * « nouvelle demande après refus » se calcule sans parcourir toutes les demandes.
     */
    @Query("select r.user.id as userId, min(r.id) as firstRequestId from EmployerAccessRequest r "
            + "where r.user.id in :userIds group by r.user.id")
    List<FirstRequestOfUser> findFirstRequestIdByUserIds(List<Integer> userIds);

    /** Nombre de demandes dans un statut donné (chiffre du tableau de bord de l'agence). */
    long countByStatus(EmployerAccessStatus status);

    long deleteByUserId(int userId);

    interface FirstRequestOfUser {
        Integer getUserId();

        Integer getFirstRequestId();
    }
}
