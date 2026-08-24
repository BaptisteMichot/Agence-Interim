package be.agence_interim.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.dto.ChatMessageResponse;
import be.agence_interim.dto.ConversationResponse;
import be.agence_interim.dto.MessageHistoryResponse;
import be.agence_interim.dto.PageResponse;
import be.agence_interim.model.Application;
import be.agence_interim.model.ApplicationStatus;
import be.agence_interim.model.Conversation;
import be.agence_interim.model.Message;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.ConversationRepository;
import be.agence_interim.repository.MessageRepository;

/**
 * Messagerie employeur ↔ candidat. Une conversation est rattachée à une
 * candidature ;
 * seul l'employeur peut la démarrer, le candidat y répond.
 */
@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ApplicationRepository applicationRepository;

    public ChatService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ApplicationRepository applicationRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.applicationRepository = applicationRepository;
    }

    /**
     * Message enregistré et identifiant du destinataire, pour la diffusion temps
     * réel.
     */
    public record SentMessage(ChatMessageResponse message, int recipientId, int senderId) {
    }

    /**
     * Ouvre (ou retrouve) la conversation liée à une candidature reçue par
     * l'employeur.
     * L'employeur est l'émetteur de la conversation, le candidat le destinataire.
     */
    @Transactional
    public ConversationResponse openForApplication(int employerId, int applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .filter(a -> a.getJobOffer().getEmployer().getId() == employerId)
                .orElseThrow(() -> new NoSuchElementException("Candidature introuvable."));
        if (application.getStatus() == ApplicationStatus.CANCELED) {
            throw new IllegalArgumentException("Cette candidature a été annulée.");
        }

        Conversation conversation = conversationRepository.findByApplicationId(applicationId)
                .orElseGet(() -> {
                    Conversation created = new Conversation();
                    created.setApplication(application);
                    created.setSender(application.getJobOffer().getEmployer());
                    created.setReceiver(application.getJobSeeker());
                    return conversationRepository.save(created);
                });
        return toResponse(conversation, employerId, null, 0);
    }

    /**
     * Conversations de l'utilisateur, la plus active en premier, avec dernier
     * message et non-lus.
     */
    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> myConversations(int userId, Pageable pageable) {
        // Le masquage et le classement par dernier message sont faits en base : sur une
        // liste paginée, filtrer ou trier après coup ne porterait que sur la page reçue.
        Page<Conversation> page = conversationRepository.findVisibleForUser(userId, pageable);
        List<Integer> ids = page.getContent().stream().map(conversation -> conversation.getId()).toList();
        if (ids.isEmpty()) {
            return PageResponse.empty(page.getNumber(), page.getSize());
        }

        Map<Integer, Message> lastMessages = lastMessageByConversation(ids);
        Map<Integer, Long> unread = new HashMap<>();
        for (MessageRepository.ConversationUnreadCount count
                : messageRepository.countUnreadByConversation(userId, ids)) {
            unread.put(count.getConversationId(), count.getTotal());
        }

        return PageResponse.of(page, conversation -> toResponse(
                conversation,
                userId,
                lastMessages.get(conversation.getId()),
                unread.getOrDefault(conversation.getId(), 0L)));
    }

    /** Nombre total de messages non lus (badge de la barre de navigation). */
    public long unreadCount(int userId) {
        return messageRepository.countUnreadForUser(userId);
    }

    /** Détail d'une conversation à laquelle l'utilisateur participe. */
    @Transactional(readOnly = true)
    public ConversationResponse conversation(int userId, int conversationId) {
        Conversation conversation = participantConversation(userId, conversationId);
        return toResponse(conversation, userId, null, 0);
    }

    /**
     * Un lot d'historique d'une conversation ; les messages reçus y sont marqués comme lus.
     *
     * <p>Sans {@code beforeId}, ce sont les derniers messages ; avec, les messages qui
     * précèdent celui-là. Un identifiant sert de repère plutôt qu'un numéro de page :
     * un message envoyé pendant la lecture décalerait toutes les pages.
     */
    @Transactional
    public MessageHistoryResponse messages(
            int userId, int conversationId, @Nullable Integer beforeId, int limit) {
        participantConversation(userId, conversationId);
        messageRepository.markConversationRead(conversationId, userId);

        // Un message de plus que demandé : sa présence dit qu'il reste de l'historique.
        Pageable batch = PageRequest.of(0, limit + 1);
        List<Message> found = beforeId == null
                ? messageRepository.findRecent(conversationId, batch)
                : messageRepository.findBefore(conversationId, beforeId, batch);

        boolean hasMore = found.size() > limit;
        List<Message> kept = hasMore ? found.subList(0, limit) : found;
        // La requête renvoie du plus récent au plus ancien : le fil s'affiche à l'endroit.
        List<ChatMessageResponse> messages = kept.reversed()
                .stream().map(ChatMessageResponse::fromEntity).toList();
        return new MessageHistoryResponse(messages, hasMore);
    }

    /**
     * Enregistre un message envoyé par un participant. La diffusion temps réel est
     * faite par l'appelant.
     */
    @Transactional
    public SentMessage send(int userId, int conversationId, String content) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Le message ne peut pas etre vide.");
        }
        // La longueur est vérifiée ici et non seulement sur le DTO : la WebSocket
        // désérialise sa trame à la main et n'a jamais vu passer de validateur. Une règle
        // posée sur un seul des deux chemins d'entrée n'est pas une règle — la colonne
        // étant un TEXT, la voie temps réel acceptait des messages de taille arbitraire.
        if (text.length() > Message.CONTENT_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Le message ne peut pas depasser " + Message.CONTENT_MAX_LENGTH + " caracteres.");
        }
        Conversation conversation = participantConversation(userId, conversationId);
        User sender = self(conversation, userId);

        Message message = new Message();
        message.setConversation(conversation);
        message.setUser(sender);
        message.setContent(text);
        message.setSentTime(LocalDateTime.now());
        message.setRead(false);
        Message saved = messageRepository.save(message);

        return new SentMessage(ChatMessageResponse.fromEntity(saved), other(conversation, userId).getId(), userId);
    }

    /**
     * Masque la conversation pour ce participant : elle quitte sa liste mais reste
     * intacte pour l'autre, et reviendra dès qu'un message y sera posté.
     */
    @Transactional
    public void hide(int userId, int conversationId) {
        Conversation conversation = participantConversation(userId, conversationId);
        conversation.hideFor(userId, LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    /** Charge une conversation en vérifiant que l'utilisateur y participe. */
    private Conversation participantConversation(int userId, int conversationId) {
        return conversationRepository.findByIdFetchAll(conversationId)
                .filter(c -> c.getSender().getId() == userId || c.getReceiver().getId() == userId)
                .orElseThrow(() -> new NoSuchElementException("Conversation introuvable."));
    }

    /** Le participant de la conversation qui est l'utilisateur connecté. */
    private User self(Conversation conversation, int userId) {
        return conversation.getSender().getId() == userId
                ? conversation.getSender()
                : conversation.getReceiver();
    }

    /** L'autre participant de la conversation. */
    private User other(Conversation conversation, int userId) {
        return conversation.getSender().getId() == userId
                ? conversation.getReceiver()
                : conversation.getSender();
    }

    /** Dernier message de chaque conversation (un seul message est chargé par conversation). */
    private Map<Integer, Message> lastMessageByConversation(List<Integer> conversationIds) {
        return messageRepository.findLastByConversationIds(conversationIds).stream()
                .collect(Collectors.toMap(message -> message.getConversation().getId(), Function.identity()));
    }

    private ConversationResponse toResponse(
            Conversation conversation, int userId, Message lastMessage, long unreadCount) {
        User other = other(conversation, userId);
        Application application = conversation.getApplication();
        return new ConversationResponse(
                conversation.getId(),
                application.getId(),
                application.getJobOffer().getId(),
                application.getJobOffer().getTitle(),
                other.getId(),
                other.getFirstName() + " " + other.getLastName(),
                lastMessage == null ? null : lastMessage.getContent(),
                lastMessage == null ? null : lastMessage.getSentTime(),
                unreadCount);
    }
}
