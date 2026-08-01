package be.agence_interim.dto;

import java.time.LocalDateTime;

/** Conversation vue par un participant : l'autre partie, l'offre concernée et les non-lus. */
public record ConversationResponse(
        int id,
        int applicationId,
        int offerId,
        String offerTitle,
        int otherPartyId,
        String otherPartyName,
        String lastMessage,
        LocalDateTime lastMessageTime,
        long unreadCount) {
}
