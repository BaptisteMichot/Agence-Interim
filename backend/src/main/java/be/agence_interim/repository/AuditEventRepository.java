package be.agence_interim.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import be.agence_interim.model.AuditAction;
import be.agence_interim.model.AuditEvent;

/**
 * Journal d'audit.
 *
 * <p>Aucune méthode de modification n'est exposée au-delà de {@code save} : la table est
 * en ajout seul, et le rester est une propriété du dispositif, pas un oubli.
 */
public interface AuditEventRepository extends JpaRepository<AuditEvent, Integer> {

    Page<AuditEvent> findByOrderByOccurredAtDescIdDesc(Pageable pageable);

    Page<AuditEvent> findByActionOrderByOccurredAtDescIdDesc(AuditAction action, Pageable pageable);
}
