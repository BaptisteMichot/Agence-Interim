package be.agence_interim.repository;

import be.agence_interim.model.SkillJobOffer;
import be.agence_interim.model.SkillJobOfferId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SkillJobOfferRepository extends JpaRepository<SkillJobOffer, SkillJobOfferId> {

    @Query("select s from SkillJobOffer s join fetch s.skill where s.jobOffer.id = :jobOfferId order by s.skill.name")
    List<SkillJobOffer> findByJobOfferIdFetchSkill(int jobOfferId);

    void deleteByJobOfferId(int jobOfferId);
}
