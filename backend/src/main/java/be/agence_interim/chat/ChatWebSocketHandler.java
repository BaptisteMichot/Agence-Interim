package be.agence_interim.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

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

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionRegistry.register(userId(session), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(userId(session), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
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
        } catch (RuntimeException e) {
            log.warn("Envoi de message refusé pour l'utilisateur {} : {}", senderId, e.getMessage());
            sendError(session, e.getMessage());
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
