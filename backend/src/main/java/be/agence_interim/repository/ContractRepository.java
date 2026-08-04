package be.agence_interim.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import be.agence_interim.model.Contract;

public interface ContractRepository extends JpaRepository<Contract, Integer> {

    Optional<Contract> findByMissionId(int missionId);

    /** Contrats de plusieurs missions en une requête (listes de missions). */
    List<Contract> findByMissionIdIn(Collection<Integer> missionIds);
}
