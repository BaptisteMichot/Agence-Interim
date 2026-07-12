package be.agence_interim.repository;

import be.agence_interim.model.LanguageUser;
import be.agence_interim.model.LanguageUserId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LanguageUserRepository extends JpaRepository<LanguageUser, LanguageUserId> {

    @Query("select lu from LanguageUser lu join fetch lu.language where lu.user.id = :userId order by lu.language.name")
    List<LanguageUser> findByUserIdFetchLanguage(int userId);

    Optional<LanguageUser> findByUserIdAndLanguageId(int userId, int languageId);

    boolean existsByUserIdAndLanguageId(int userId, int languageId);
}
