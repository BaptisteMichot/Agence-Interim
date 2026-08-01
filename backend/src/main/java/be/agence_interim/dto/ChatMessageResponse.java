package be.agence_interim.dto;

import java.time.LocalDateTime;

import be.agence_interim.model.Message;

/** Message d'une conversation, tel qu'affiché dans le fil. */
public record ChatMessageResponse(
        int id,
        int conversationId,
        int senderId,
        String senderFirstName,
        String content,
        LocalDateTime sentTime,
        boolean read) {

    public static ChatMessageResponse fromEntity(Message message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getUser().getId(),
                message.getUser().getFirstName(),
                message.getContent(),
                message.getSentTime(),
                message.isRead());
    }
}
