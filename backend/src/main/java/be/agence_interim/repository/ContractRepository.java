package be.agence_interim.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import be.agence_interim.model.Contract;
import be.agence_interim.model.SignatureStatus;

public interface ContractRepository extends JpaRepository<Contract, Integer> {

    /** Fragment commun : le contrat avec sa mission, sa candidature et les deux parties. */
    String FETCH_ALL = "select c from Contract c join fetch c.mission m join fetch m.application a "
            + "join fetch a.jobSeeker join fetch a.jobOffer o join fetch o.employer ";

    /** Le lecteur est partie au contrat : travailleur intérimaire ou entreprise utilisatrice. */
    String PARTY = "(c.mission.application.jobSeeker.id = :userId "
            + "or c.mission.application.jobOffer.employer.id = :userId)";

    Optional<Contract> findByMissionId(int missionId);

    /** Contrats de plusieurs missions en une requête (listes de missions). */
    List<Contract> findByMissionIdIn(Collection<Integer> missionIds);

    /** Une page des contrats du lecteur, les plus récents d'abord (« Mes documents »). */
    @Query(value = FETCH_ALL + "where (a.jobSeeker.id = :userId or o.employer.id = :userId) "
            + "order by c.generationTime desc, c.id desc",
            countQuery = "select count(c) from Contract c where " + PARTY)
    Page<Contract> findForUser(int userId, Pageable pageable);

    /**
     * Nombre de contrats qui attendent la signature du lecteur. Chaque partie ne répond
     * que de sa propre signature : l'état de l'autre ne le concerne pas ici.
     */
    @Query("select count(c) from Contract c where "
            + "(c.mission.application.jobOffer.employer.id = :userId and c.statusEmployer = :pending) "
            + "or (c.mission.application.jobSeeker.id = :userId and c.statusWorker = :pending)")
    long countAwaitingSignature(int userId, SignatureStatus pending);
}
