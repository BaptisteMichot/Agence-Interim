package be.agence_interim.repository;

import be.agence_interim.model.Role;
import be.agence_interim.model.User;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Integer> {

    /** Utilisateur d'identifiant donné, ou exception s'il n'existe pas. */
    default User requireById(int id) {
        return findById(id).orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable."));
    }

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Version de session courante de l'utilisateur, ou vide s'il n'existe plus.
     *
     * <p>Une seule colonne est lue, et non l'entité entière : cette requête part sur
     * chaque appel authentifié, c'est le prix de la révocation des jetons.
     */
    @Query("select u.tokenVersion from User u where u.id = :id")
    Optional<Integer> findTokenVersionById(int id);

    /**
     * Utilisateurs vivants d'un rôle donné, les comptes clôturés exclus.
     *
     * <p>La clôture d'un compte engagé l'anonymise sans le supprimer : sans la condition
     * sur {@code closedAt}, il resterait dans le vivier interrogé à chaque publication
     * d'offre. C'est le seul point d'entrée pour parcourir une population d'utilisateurs ;
     * il vaut mieux qu'il ne laisse pas le choix d'oublier la condition.
     */
    List<User> findByRoleAndClosedAtIsNull(Role role);

    /** Comptes dormants qui détiennent encore un CV (politique de conservation). */
    List<User> findByLastLoginAtBeforeAndCvFilePathIsNotNull(java.time.LocalDateTime limit);
}
