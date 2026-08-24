package be.agence_interim.chat;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.NoSuchElementException;

import be.agence_interim.service.ChatService;
import be.agence_interim.service.ChatService.SentMessage;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Point d'entrée temps réel du chat. Le client envoie {@code {"type":"SEND",...}} ;
 * le serveur renvoie {@code {"type":"MESSAGE",...}} aux deux participants, ou
 * {@code {"type":"ERROR",...}} à l'émetteur.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ChatService chatService;
    private final ChatSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(
            ChatService chatService, ChatSessionRegistry sessionRegistry, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    /** Trame reçue du client : seul le type SEND est accepté pour l'instant. */
    public record IncomingFrame(String type, Integer conversationId, String content) {
    }

    /**
      * Taille maximale d'une trame reçue.
      *
      * <p>Un message plafonne à {@value be.agence_interim.model.Message#CONTENT_MAX_LENGTH}
      * caractères ; la marge couvre l'encodage UTF-8 et l'enveloppe JSON. Sans cette borne,
      * le conteneur accepte par défaut des trames bien plus grosses, et un client
      * authentifié peut faire réserver la mémoire correspondante avant même que le service
      * ait eu l'occasion de refuser le contenu.
      *
      * <p>La limite est posée sur la session plutôt que sur le conteneur : un
      * {@code ServletServerContainerFactoryBean} exige un vrai conteneur de servlets et
      * empêche le contexte de démarrer dans les tests en environnement simulé.
      */
    private static final int MAX_FRAME_BYTES = 16 * 1024;

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        session.setTextMessageSizeLimit(MAX_FRAME_BYTES);
        session.setBinaryMessageSizeLimit(MAX_FRAME_BYTES);
        sessionRegistry.register(userId(session), session);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        sessionRegistry.unregister(userId(session), session);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage textMessage) {
        int senderId = userId(session);
        IncomingFrame frame;
        try {
            frame = objectMapper.readValue(textMessage.getPayload(), IncomingFrame.class);
        } catch (JacksonException e) {
            sendError(session, "Message illisible.");
            return;
        }

        if (!"SEND".equals(frame.type()) || frame.conversationId() == null) {
            sendError(session, "Type de message non pris en charge.");
            return;
        }

        try {
            // Le message est d'abord enregistré (transaction validée), puis diffusé.
            SentMessage sent = chatService.send(senderId, frame.conversationId(), frame.content());
            sessionRegistry.sendToUser(sent.senderId(), new OutgoingFrame("MESSAGE", sent.message(), null));
            sessionRegistry.sendToUser(sent.recipientId(), new OutgoingFrame("MESSAGE", sent.message(), null));
        } catch (IllegalArgumentException | NoSuchElementException e) {
            // Exceptions métier : leur message est rédigé pour l'utilisateur.
            log.warn("Envoi de message refusé pour l'utilisateur {} : {}", senderId, e.getMessage());
            sendError(session, e.getMessage());
        } catch (RuntimeException e) {
            // Tout le reste est un incident technique : le détail part au journal, pas au
            // client. Le chemin REST bénéficie déjà de cette séparation par le
            // gestionnaire d'erreurs global ; la WebSocket, elle, renvoyait le message brut.
            log.error("Erreur technique sur l'envoi de message de l'utilisateur {}.", senderId, e);
            sendError(session, null);
        }
    }

    private void sendError(WebSocketSession session, String error) {
        sessionRegistry.sendToUser(
                userId(session),
                new OutgoingFrame("ERROR", null, error == null ? "Une erreur est survenue." : error));
    }

    private int userId(WebSocketSession session) {
        Object attribute = session.getAttributes().get(ChatHandshakeInterceptor.USER_ID_ATTRIBUTE);
        if (attribute instanceof Integer id) {
            return id;
        }
        throw new IllegalStateException("Session WebSocket sans utilisateur authentifié.");
    }

    /** Trame envoyée au client : MESSAGE (avec le message) ou ERROR (avec le motif). */
    public record OutgoingFrame(String type, Object message, String error) {
    }
}
