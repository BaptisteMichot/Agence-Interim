package be.agence_interim.dto;

import be.agence_interim.model.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Message envoyé dans une conversation.
 *
 * <p>La contrainte est doublée dans {@code ChatService} : ce DTO ne couvre que la voie
 * REST, alors que la WebSocket désérialise sa trame elle-même et n'a jamais vu passer
 * de validateur.
 */
public record SendMessageRequest(
        @NotBlank(message = "Le message ne peut pas etre vide.")
        @Size(max = Message.CONTENT_MAX_LENGTH, message = "Le message ne peut pas depasser {max} caracteres.")
        String content) {
}
