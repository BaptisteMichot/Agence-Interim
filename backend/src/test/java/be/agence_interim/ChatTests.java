package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import be.agence_interim.dto.MessageHistoryResponse;
import be.agence_interim.model.Application;
import be.agence_interim.model.ApplicationStatus;
import be.agence_interim.model.Message;
import be.agence_interim.model.Role;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.ChatService;

/**
 * La messagerie entre l'employeur et le candidat.
 *
 * <p>Elle n'existe qu'adossée à une candidature (demande 10) : on ne s'écrit pas sur
 * cette plateforme sans avoir postulé, et c'est l'employeur qui ouvre la conversation.
 * Cela met les deux personnes en relation sans jamais leur donner un annuaire.
 *
 * <p>Le chat a deux chemins d'entrée — l'API et la WebSocket — et c'est ce qui rend ces
 * tests utiles : une règle posée sur un seul des deux n'est pas une règle. Ils portent
 * donc sur le service, qui est le point commun aux deux.
 */
@SpringBootTest
class ChatTests {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private MissionFixtures fixtures;
    private User employer;
    private User candidate;
    private int conversationId;

    @BeforeEach
    void setUp() {
        fixtures = new MissionFixtures(userRepository, jobOfferRepository, applicationRepository);
        employer = fixtures.employer;
        candidate = fixtures.worker;
        conversationId = chatService.openForApplication(
                employer.getId(), fixtures.application().getId()).id();
    }

    @Test
    @DisplayName("La conversation naît d'une candidature, et une seule fois")
    void theconversationIsBornFromAnApplicationAndOnlyOnce() {
        // Rouvrir depuis la même candidature doit retrouver le fil existant : en créer un
        // second couperait l'historique en deux, chacun n'en voyant qu'une moitié.
        int reopened = chatService.openForApplication(employer.getId(), fixtures.application().getId()).id();

        assertThat(reopened).isEqualTo(conversationId);
    }

    @Test
    @DisplayName("Seul l'employeur destinataire de la candidature ouvre la conversation")
    void onlyTheEmployerWhoReceivedTheApplicationOpensTheConversation() {
        // C'est ce qui empêche de se servir de la plateforme comme d'un carnet d'adresses :
        // il faut avoir reçu la candidature de quelqu'un pour pouvoir lui écrire.
        User outsider = fixtures.employer("tiers");

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> chatService.openForApplication(
                        outsider.getId(), fixtures.application().getId()));
    }

    @Test
    @DisplayName("Une candidature annulée ne s'ouvre plus à la discussion")
    void awithdrawnApplicationOpensNoConversation() {
        // Le candidat s'est retiré : lui écrire reviendrait à ignorer son désistement.
        MissionFixtures other = new MissionFixtures(userRepository, jobOfferRepository, applicationRepository);
        Application withdrawn = other.application();
        withdrawn.setStatus(ApplicationStatus.CANCELED);
        other.save(withdrawn);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> chatService.openForApplication(other.employer.getId(), withdrawn.getId()))
                .withMessageContaining("annulée");
    }

    @Test
    @DisplayName("Un message part vers l'autre participant, jamais vers son auteur")
    void amessageGoesToTheOtherParticipant() {
        // Le service dit à l'appelant qui doit être averti en temps réel. Se désigner
        // soi-même ferait revenir à l'expéditeur sa propre notification, et laisserait le
        // destinataire sans rien.
        ChatService.SentMessage sent = chatService.send(employer.getId(), conversationId, "Bonjour, êtes-vous libre ?");

        assertThat(sent.senderId()).isEqualTo(employer.getId());
        assertThat(sent.recipientId()).isEqualTo(candidate.getId());
        assertThat(sent.message().content()).isEqualTo("Bonjour, êtes-vous libre ?");
    }

    @Test
    @DisplayName("Un message vide ou trop long est refusé par le service lui-même")
    void anemptyOrOverlongMessageIsRejectedByTheServiceItself() {
        // La WebSocket désérialise sa trame à la main et n'a jamais vu passer de
        // validateur : une règle posée seulement sur le DTO laisserait la voie temps réel
        // écrire dans une colonne TEXT sans aucune borne.
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> chatService.send(employer.getId(), conversationId, "   "))
                .withMessageContaining("vide");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> chatService.send(employer.getId(), conversationId, null))
                .withMessageContaining("vide");

        String tooLong = "a".repeat(Message.CONTENT_MAX_LENGTH + 1);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> chatService.send(employer.getId(), conversationId, tooLong))
                .withMessageContaining("depasser");
    }

    @Test
    @DisplayName("Un tiers ne lit ni n'écrit dans une conversation qui n'est pas la sienne")
    void athirdPartyNeitherReadsNorWrites() {
        User outsider = fixtures.user("curieux", Role.JOBSEEKER);
        chatService.send(employer.getId(), conversationId, "Bonjour");

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> chatService.conversation(outsider.getId(), conversationId));
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> chatService.messages(outsider.getId(), conversationId, null, 20));
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> chatService.send(outsider.getId(), conversationId, "Bonjour"));
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> chatService.hide(outsider.getId(), conversationId));
    }

    @Test
    @DisplayName("Les non-lus sont ceux de l'autre : lire le fil les efface")
    void unreadMessagesAreTheOtherPartysAndReadingClearsThem() {
        // Le badge est compté pour le destinataire seul : compter aussi ses propres
        // messages afficherait un non-lu permanent à qui vient d'écrire.
        chatService.send(employer.getId(), conversationId, "Bonjour");
        chatService.send(employer.getId(), conversationId, "Êtes-vous disponible lundi ?");

        assertThat(chatService.unreadCount(candidate.getId())).isEqualTo(2);
        assertThat(chatService.unreadCount(employer.getId())).isZero();

        chatService.messages(candidate.getId(), conversationId, null, 20);

        assertThat(chatService.unreadCount(candidate.getId())).isZero();
    }

    @Test
    @DisplayName("Le fil s'affiche à l'endroit et se remonte par lots")
    void thethreadReadsInOrderAndScrollsBackInBatches() {
        // L'historique se remonte à partir d'un message repère plutôt que d'un numéro de
        // page : un message reçu pendant la lecture décalerait toutes les pages et ferait
        // sauter une ligne au lecteur.
        for (int number = 1; number <= 5; number++) {
            chatService.send(employer.getId(), conversationId, "Message " + number);
        }

        MessageHistoryResponse lastTwo = chatService.messages(candidate.getId(), conversationId, null, 2);
        assertThat(contents(lastTwo)).containsExactly("Message 4", "Message 5");
        assertThat(lastTwo.hasMore()).as("il reste de l'historique au-dessus").isTrue();

        int oldestShown = lastTwo.messages().get(0).id();
        MessageHistoryResponse previous = chatService.messages(candidate.getId(), conversationId, oldestShown, 2);
        assertThat(contents(previous)).containsExactly("Message 2", "Message 3");

        MessageHistoryResponse start = chatService.messages(
                candidate.getId(), conversationId, previous.messages().get(0).id(), 2);
        assertThat(contents(start)).containsExactly("Message 1");
        assertThat(start.hasMore()).as("on est remonté au début du fil").isFalse();
    }

    @Test
    @DisplayName("Masquer une conversation ne la retire qu'à celui qui masque")
    void hidingAConversationOnlyRemovesItForTheOneWhoHides() {
        // Le fil appartient aux deux : le faire disparaître chez l'autre effacerait des
        // échanges dont il peut avoir besoin.
        chatService.send(employer.getId(), conversationId, "Bonjour");

        chatService.hide(candidate.getId(), conversationId);

        assertThat(conversationIds(candidate)).doesNotContain(conversationId);
        assertThat(conversationIds(employer)).contains(conversationId);
    }

    @Test
    @DisplayName("Une conversation masquée revient dès qu'un message y est posté")
    void ahiddenConversationComesBackOnTheNextMessage() {
        // C'est ce qui distingue « masquer » de « supprimer » : le candidat range un
        // échange terminé, et il le retrouve si l'employeur le relance.
        chatService.send(employer.getId(), conversationId, "Bonjour");
        chatService.hide(candidate.getId(), conversationId);

        chatService.send(employer.getId(), conversationId, "Une dernière question");

        assertThat(conversationIds(candidate)).contains(conversationId);
    }

    // ------------------------------------------------------------------------------ outils

    private List<String> contents(MessageHistoryResponse history) {
        return history.messages().stream().map(message -> message.content()).toList();
    }

    private List<Integer> conversationIds(User user) {
        return chatService.myConversations(user.getId(), PageRequest.of(0, 50))
                .content().stream().map(conversation -> conversation.id()).toList();
    }
}
