package be.agence_interim.repository;

import be.agence_interim.model.Role;
import be.agence_interim.model.User;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {

    /** Utilisateur d'identifiant donné, ou exception s'il n'existe pas. */
    default User requireById(int id) {
        return findById(id).orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable."));
    }

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);
}
