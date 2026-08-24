package be.agence_interim.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import be.agence_interim.model.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Integer> {

    /** Fragment commun : conversation avec les deux participants, la candidature et l'offre. */
    String FETCH_ALL = "select c from Conversation c "
            + "join fetch c.sender join fetch c.receiver "
            + "join fetch c.application a join fetch a.jobOffer ";

    /**
     * Une conversation retirée de la liste d'un participant y revient dès qu'un message
     * postérieur au masquage y est déposé. La condition est écrite deux fois, une par
     * côté, plutôt que par un {@code case} : l'utilisateur est soit l'émetteur, soit le
     * destinataire, jamais les deux.
     */
    String VISIBLE_FOR_USER =
            "where ((c.sender.id = :userId and (c.senderHiddenAt is null "
                    + "or (select max(m.sentTime) from Message m where m.conversation = c) > c.senderHiddenAt)) "
                    + "or (c.receiver.id = :userId and (c.receiverHiddenAt is null "
                    + "or (select max(m.sentTime) from Message m where m.conversation = c) > c.receiverHiddenAt))) ";

    /** Tri : la conversation la plus active en premier, celles sans message à la fin. */
    String MOST_RECENT_FIRST =
            "order by (select max(m2.sentTime) from Message m2 where m2.conversation = c) desc nulls last, c.id desc";

    Optional<Conversation> findByApplicationId(int applicationId);

    /** Une page des conversations visibles de l'utilisateur, la plus active en premier. */
    @Query(value = FETCH_ALL + VISIBLE_FOR_USER + MOST_RECENT_FIRST,
            countQuery = "select count(c) from Conversation c " + VISIBLE_FOR_USER)
    Page<Conversation> findVisibleForUser(int userId, Pageable pageable);

    /** Conversation avec ses participants et son offre chargés. */
    @Query(FETCH_ALL + "where c.id = :conversationId")
    Optional<Conversation> findByIdFetchAll(int conversationId);

    /** Conversations auxquelles l'utilisateur participe (export RGPD, clôture). */
    List<Conversation> findBySenderIdOrReceiverId(int senderId, int receiverId);
}
