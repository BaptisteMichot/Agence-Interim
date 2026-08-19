package be.agence_interim.chat;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Sessions WebSocket ouvertes, indexées par utilisateur (un utilisateur peut avoir
 * plusieurs onglets). Registre <b>en mémoire</b> : il sera remplacé par une diffusion
 * Redis à l'incrément 10, quand plusieurs instances du backend tourneront.
 */
@Component
public class ChatSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(ChatSessionRegistry.class);

    private final Map<Integer, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public ChatSessionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(int userId, WebSocketSession session) {
        sessionsByUser.computeIfAbsent(userId, _ -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(int userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByUser.remove(userId);
        }
    }

    /** Envoie un objet sérialisé en JSON à toutes les sessions ouvertes d'un utilisateur. */
    public void sendToUser(int userId, Object payload) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            log.error("Sérialisation du message temps réel impossible.", e);
            return;
        }
        for (WebSocketSession session : sessions) {
            sendText(session, json);
        }
    }

    private void sendText(WebSocketSession session, String json) {
        // Le test de nullite convainc l'analyse : Jackson n'annote pas ses retours.
        if (json == null || !session.isOpen()) {
            return;
        }
        try {
            // Une session WebSocket n'est pas sûre en écriture concurrente.
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.warn("Envoi du message temps réel impossible sur la session {}.", session.getId(), e);
        }
    }
}
