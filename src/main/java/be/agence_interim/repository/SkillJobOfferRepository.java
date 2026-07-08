package be.agence_interim.repository;

import be.agence_interim.model.SkillJobOffer;
import be.agence_interim.model.SkillJobOfferId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillJobOfferRepository extends JpaRepository<SkillJobOffer, SkillJobOfferId> {
}
