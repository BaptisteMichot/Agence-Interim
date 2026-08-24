package be.agence_interim.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Trace d'un acte engageant.
 *
 * <p>Table en ajout seul : rien dans l'application ne modifie ni ne supprime une ligne
 * déjà écrite — c'est ce qui fait la valeur d'un journal. Les colonnes recopient
 * l'identité de l'acteur au moment des faits plutôt que de pointer vers son compte :
 * une trace doit rester lisible après la suppression du compte concerné, et elle ne doit
 * pas se réécrire quand celui-ci change de nom.
 *
 * <p>Ce que l'application produit sont des contrats de travail : la non-répudiation
 * n'est pas un raffinement technique mais une exigence métier. Sans journal, l'état
 * final d'un contrat dit qu'il est signé, jamais par qui, ni quand, ni depuis où.
 */
@Entity
@Table(name = "audit_event")
@Getter
@Setter
@NoArgsConstructor
public class AuditEvent {

    public static final int DETAIL_MAX_LENGTH = 500;
    public static final int IP_MAX_LENGTH = 45; // une adresse IPv6 en toutes lettres
    public static final int ACTOR_MAX_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    /** Identifiant de l'auteur au moment des faits ; nul pour une action du système. */
    private Integer actorId;

    /** Email de l'auteur, recopié : la trace survit à la suppression du compte. */
    @Column(length = ACTOR_MAX_LENGTH)
    private String actorEmail;

    /** Nature de l'objet visé (CONTRACT, MISSION, USER…). */
    @Column(length = 40)
    private String targetType;

    private Integer targetId;

    @Column(length = IP_MAX_LENGTH)
    private String ip;

    /** Précision libre et courte : motif d'un refus, rôle du signataire… */
    @Column(length = DETAIL_MAX_LENGTH)
    private String detail;
}
