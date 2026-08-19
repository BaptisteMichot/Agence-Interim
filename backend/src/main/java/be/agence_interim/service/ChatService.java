package be.agence_interim.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.dto.ChatMessageResponse;
import be.agence_interim.dto.ConversationResponse;
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
    public List<ConversationResponse> myConversations(int userId) {
        List<Conversation> conversations = conversationRepository.findByParticipantFetchAll(userId);
        if (conversations.isEmpty()) {
            return List.of();
        }

        Map<Integer, Message> lastMessages = lastMessageByConversation(
                conversations.stream().map(conversation -> conversation.getId()).toList());
        Map<Integer, Long> unread = new HashMap<>();
        for (MessageRepository.ConversationUnreadCount count : messageRepository.countUnreadByConversation(userId)) {
            unread.put(count.getConversationId(), count.getTotal());
        }

        return conversations.stream()
                .map(conversation -> toResponse(
                        conversation,
                        userId,
                        lastMessages.get(conversation.getId()),
                        unread.getOrDefault(conversation.getId(), 0L)))
                .sorted(Comparator.comparing(
                        (ConversationResponse response) -> response.lastMessageTime(),
                        Comparator.nullsFirst(Comparator.naturalOrder()))
                        .reversed())
                .toList();
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
     * Historique d'une conversation ; les messages reçus y sont marqués comme lus.
     */
    @Transactional
    public List<ChatMessageResponse> messages(int userId, int conversationId) {
        participantConversation(userId, conversationId);
        messageRepository.markConversationRead(conversationId, userId);
        return messageRepository.findByConversationIdFetchSender(conversationId)
                .stream().map(ChatMessageResponse::fromEntity).toList();
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
