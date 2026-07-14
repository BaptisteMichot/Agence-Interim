package be.agence_interim.repository;

import be.agence_interim.model.DegreeJobOffer;
import be.agence_interim.model.DegreeJobOfferId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DegreeJobOfferRepository extends JpaRepository<DegreeJobOffer, DegreeJobOfferId> {

    @Query("select d from DegreeJobOffer d join fetch d.degree where d.jobOffer.id = :jobOfferId order by d.degree.type, d.degree.section")
    List<DegreeJobOffer> findByJobOfferIdFetchDegree(int jobOfferId);

    void deleteByJobOfferId(int jobOfferId);
}
