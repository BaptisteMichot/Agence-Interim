package be.agence_interim.repository;

import be.agence_interim.model.Language;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LanguageRepository extends JpaRepository<Language, Integer> {

    List<Language> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
