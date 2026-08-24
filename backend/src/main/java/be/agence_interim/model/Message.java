package be.agence_interim.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Message échangé au sein d'une conversation. {@code user} est l'émetteur du message.
 */
@Entity
@Table(name = "message")
@Getter
@Setter
@NoArgsConstructor
public class Message {

    /**
     * Longueur maximale d'un message.
     *
     * <p>Déclarée sur l'entité, comme les autres bornes du modèle, pour que le DTO et le
     * service citent la même valeur. La colonne reste un {@code TEXT} : la contrainte est
     * appliquée par {@code ChatService}, qui est le point de passage obligé des deux
     * canaux d'envoi.
     */
    public static final int CONTENT_MAX_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_conversation", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_user", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime sentTime;

    /** Vrai dès que le destinataire a ouvert la conversation. */
    @Column(name = "read", nullable = false)
    private boolean read = false;
}
