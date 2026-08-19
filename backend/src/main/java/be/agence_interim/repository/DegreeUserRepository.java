package be.agence_interim.repository;

import be.agence_interim.model.DegreeUser;
import be.agence_interim.model.DegreeUserId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DegreeUserRepository extends JpaRepository<DegreeUser, DegreeUserId> {

    @Query("select du from DegreeUser du join fetch du.degree where du.user.id = :userId order by du.degree.type, du.degree.section")
    List<DegreeUser> findByUserIdFetchDegree(int userId);

    /** Diplômes de plusieurs utilisateurs en une requête (contact automatique des candidats). */
    @Query("select du from DegreeUser du join fetch du.degree where du.user.id in :userIds order by du.degree.type, du.degree.section")
    List<DegreeUser> findByUserIdInFetchDegree(Collection<Integer> userIds);

    Optional<DegreeUser> findByUserIdAndDegreeId(int userId, int degreeId);

    boolean existsByUserIdAndDegreeId(int userId, int degreeId);
}
