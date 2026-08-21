package be.agence_interim.controller;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.chat.ChatSessionRegistry;
import be.agence_interim.chat.ChatWebSocketHandler.OutgoingFrame;
import be.agence_interim.dto.ChatMessageResponse;
import be.agence_interim.dto.ConversationResponse;
import be.agence_interim.dto.MessageHistoryResponse;
import be.agence_interim.dto.PageResponse;
import be.agence_interim.dto.SendMessageRequest;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.ChatService;
import be.agence_interim.service.ChatService.SentMessage;
import jakarta.validation.Valid;

/**
 * Messagerie : conversations et historique (le temps réel passe par la WebSocket
 * {@code /ws/chat}). Accessible à l'employeur et au candidat ; l'appartenance à la
 * conversation est vérifiée dans le service.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatSessionRegistry sessionRegistry;

    public ChatController(ChatService chatService, ChatSessionRegistry sessionRegistry) {
        this.chatService = chatService;
        this.sessionRegistry = sessionRegistry;
    }

    @GetMapping("/conversations")
    public PageResponse<ConversationResponse> conversations(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page) {
        return chatService.myConversations(CurrentUser.id(jwt), Pages.of(page));
    }

    /** Total des messages non lus (badge de la barre de navigation). */
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("count", chatService.unreadCount(CurrentUser.id(jwt)));
    }

    @GetMapping("/conversations/{id}")
    public ConversationResponse conversation(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        return chatService.conversation(CurrentUser.id(jwt), id);
    }

    /**
     * Un lot d'historique ; marque au passage les messages reçus comme lus.
     * Sans {@code before}, les derniers messages ; avec, ceux qui le précèdent.
     */
    @GetMapping("/conversations/{id}/messages")
    public MessageHistoryResponse messages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int id,
            @RequestParam(required = false) @Nullable Integer before) {
        return chatService.messages(CurrentUser.id(jwt), id, before, Pages.MESSAGE_PAGE_SIZE);
    }

    /**
     * Envoi de secours si la WebSocket est indisponible : enregistre le message et le
     * diffuse aux sessions ouvertes, exactement comme la voie temps réel.
     */
    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<ChatMessageResponse> send(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int id,
            @Valid @RequestBody SendMessageRequest request) {
        SentMessage sent = chatService.send(CurrentUser.id(jwt), id, request.content());
        OutgoingFrame frame = new OutgoingFrame("MESSAGE", sent.message(), null);
        // Comme sur la WebSocket : l'émetteur reçoit aussi l'écho, pour que ses autres
        // onglets ouverts sur la conversation affichent le message sans rechargement.
        sessionRegistry.sendToUser(sent.senderId(), frame);
        sessionRegistry.sendToUser(sent.recipientId(), frame);
        return ResponseEntity.status(HttpStatus.CREATED).body(sent.message());
    }

    /**
     * Retire la conversation de la liste de l'appelant. L'autre partie la conserve, et un
     * nouveau message la fera revenir chez celui qui l'avait retirée.
     */
    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> hide(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        chatService.hide(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }

    /** Démarre (ou retrouve) la conversation liée à une candidature reçue — réservé à l'employeur. */
    @PostMapping("/conversations/application/{applicationId}")
    public ConversationResponse openForApplication(
            @AuthenticationPrincipal Jwt jwt, @PathVariable int applicationId) {
        return chatService.openForApplication(CurrentUser.id(jwt), applicationId);
    }
}
