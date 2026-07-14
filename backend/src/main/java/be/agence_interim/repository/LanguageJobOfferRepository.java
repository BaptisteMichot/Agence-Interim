package be.agence_interim.repository;

import be.agence_interim.model.LanguageJobOffer;
import be.agence_interim.model.LanguageJobOfferId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LanguageJobOfferRepository extends JpaRepository<LanguageJobOffer, LanguageJobOfferId> {

    @Query("select l from LanguageJobOffer l join fetch l.language where l.jobOffer.id = :jobOfferId order by l.language.name")
    List<LanguageJobOffer> findByJobOfferIdFetchLanguage(int jobOfferId);

    void deleteByJobOfferId(int jobOfferId);
}
