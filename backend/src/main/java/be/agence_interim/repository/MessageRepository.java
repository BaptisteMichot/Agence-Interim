package be.agence_interim.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import be.agence_interim.model.Message;

public interface MessageRepository extends JpaRepository<Message, Integer> {

    /** Historique d'une conversation, du plus ancien au plus récent. */
    @Query("select m from Message m join fetch m.user where m.conversation.id = :conversationId "
            + "order by m.sentTime asc")
    List<Message> findByConversationIdFetchSender(int conversationId);

    /** Dernier message de chaque conversation (départage par identifiant le plus élevé en cas d'égalité). */
    @Query("select m from Message m where m.conversation.id in :conversationIds "
            + "and m.id = (select max(m2.id) from Message m2 where m2.conversation.id = m.conversation.id "
            + "and m2.sentTime = (select max(m3.sentTime) from Message m3 "
            + "where m3.conversation.id = m.conversation.id))")
    List<Message> findLastByConversationIds(List<Integer> conversationIds);

    /** Nombre total de messages non lus reçus par un utilisateur (messages des autres). */
    @Query("select count(m) from Message m where m.user.id <> :userId and m.read = false "
            + "and (m.conversation.sender.id = :userId or m.conversation.receiver.id = :userId)")
    long countUnreadForUser(int userId);

    /** Nombre de messages non lus par conversation, pour un utilisateur. */
    @Query("select m.conversation.id as conversationId, count(m) as total from Message m "
            + "where m.user.id <> :userId and m.read = false "
            + "and (m.conversation.sender.id = :userId or m.conversation.receiver.id = :userId) "
            + "group by m.conversation.id")
    List<ConversationUnreadCount> countUnreadByConversation(int userId);

    /** Marque comme lus les messages reçus par l'utilisateur dans une conversation. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Message m set m.read = true "
            + "where m.conversation.id = :conversationId and m.user.id <> :userId and m.read = false")
    int markConversationRead(int conversationId, int userId);

    interface ConversationUnreadCount {
        Integer getConversationId();

        Long getTotal();
    }
}
