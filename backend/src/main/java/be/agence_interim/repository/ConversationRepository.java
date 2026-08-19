package be.agence_interim.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import be.agence_interim.model.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Integer> {

    /** Fragment commun : conversation avec les deux participants, la candidature et l'offre. */
    String FETCH_ALL = "select c from Conversation c "
            + "join fetch c.sender join fetch c.receiver "
            + "join fetch c.application a join fetch a.jobOffer ";

    Optional<Conversation> findByApplicationId(int applicationId);

    /** Conversations d'un utilisateur (employeur ou candidat), avec les deux participants et l'offre. */
    @Query(FETCH_ALL + "where c.sender.id = :userId or c.receiver.id = :userId")
    List<Conversation> findByParticipantFetchAll(int userId);

    /** Conversation avec ses participants et son offre chargés. */
    @Query(FETCH_ALL + "where c.id = :conversationId")
    Optional<Conversation> findByIdFetchAll(int conversationId);
}
