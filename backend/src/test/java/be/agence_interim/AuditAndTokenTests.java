package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.transaction.support.TransactionTemplate;

import be.agence_interim.model.AuditAction;
import be.agence_interim.model.AuditEvent;
import be.agence_interim.model.Role;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.AuditEventRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.AuditService;
import be.agence_interim.service.JwtService;

/**
 * Le journal d'audit et le jeton de session.
 *
 * <p>Deux dispositifs discrets dont personne ne constate le bon fonctionnement au
 * quotidien, et qu'on n'interroge qu'après coup : le journal quand il faut établir qui a
 * signé un contrat, le jeton quand il faut couper court à une session. Ils partagent
 * cette propriété désagréable qu'une défaillance ne se voit qu'au moment où l'on en a
 * besoin, c'est-à-dire trop tard.
 */
@SpringBootTest
class AuditAndTokenTests {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private MissionFixtures fixtures;
    private User actor;

    @BeforeEach
    void setUp() {
        fixtures = new MissionFixtures(userRepository, jobOfferRepository, applicationRepository);
        actor = fixtures.user("acteur", Role.ADMIN);
    }

    // -------------------------------------------------------------------- journal d'audit

    @Test
    @DisplayName("Une trace recopie l'adresse de son auteur, pour survivre à son compte")
    void atraceCopiesItsAuthorsAddressToOutliveTheAccount() {
        // La trace ne renvoie pas seulement à un identifiant : un compte clôturé est
        // anonymisé, et l'identifiant ne désignerait plus personne. Le journal doit rester
        // lisible des années après, c'est sa seule raison d'être.
        auditService.record(AuditAction.MISSION_VALIDATED, actor.getId(), "MISSION", 42, "Validation");

        AuditEvent recorded = latest();
        assertThat(recorded.getActorId()).isEqualTo(actor.getId());
        assertThat(recorded.getActorEmail()).isEqualTo(actor.getEmail());
        assertThat(recorded.getTargetType()).isEqualTo("MISSION");
        assertThat(recorded.getTargetId()).isEqualTo(42);
        assertThat(recorded.getOccurredAt()).isNotNull();
    }

    @Test
    @DisplayName("Une trace survit à l'annulation de l'opération qui l'a produite")
    void atraceSurvivesTheRollbackOfTheOperationThatWroteIt() {
        // C'est ce que la propagation REQUIRES_NEW achète, et c'est contre-intuitif : on
        // veut justement retrouver au journal la tentative qui a échoué. Une trace jointe
        // à la transaction métier disparaîtrait avec elle, sans laisser de trace de
        // l'essai.
        long before = auditEventRepository.count();

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() ->
                transactionTemplate.executeWithoutResult(status -> {
                    auditService.record(
                            AuditAction.CONTRACT_SIGNED, actor.getId(), "CONTRACT", 7, "Tentative");
                    throw new IllegalStateException("L'opération métier échoue après la trace.");
                }));

        assertThat(auditEventRepository.count()).isEqualTo(before + 1);
        assertThat(latest().getAction()).isEqualTo(AuditAction.CONTRACT_SIGNED);
    }

    @Test
    @DisplayName("Le journal se lit du plus récent au plus ancien, et se filtre par acte")
    void thejournalReadsNewestFirstAndFiltersByAction() {
        auditService.record(AuditAction.PASSWORD_CHANGED, actor.getId(), "USER", actor.getId(), "Un");
        auditService.record(AuditAction.DATA_EXPORTED, actor.getId(), "USER", actor.getId(), "Deux");

        List<AuditEvent> page = auditEventRepository
                .findByOrderByOccurredAtDescIdDesc(PageRequest.of(0, 2)).getContent();
        assertThat(page).extracting(event -> event.getAction())
                .containsExactly(AuditAction.DATA_EXPORTED, AuditAction.PASSWORD_CHANGED);

        assertThat(auditEventRepository
                .findByActionOrderByOccurredAtDescIdDesc(AuditAction.DATA_EXPORTED, PageRequest.of(0, 1))
                .getContent())
                .singleElement()
                .satisfies(event -> assertThat(event.getDetail()).isEqualTo("Deux"));
    }

    // ------------------------------------------------------------------ jeton de session

    @Test
    @DisplayName("Le jeton porte l'identité, le rôle et la version de session")
    void thetokenCarriesIdentityRoleAndSessionVersion() {
        // Ces trois valeurs évitent une lecture en base à chaque requête. La troisième est
        // celle qui rend le jeton révocable : elle est comparée à celle du compte, et un
        // écart suffit à rejeter un jeton pourtant valablement signé.
        User employer = fixtures.employer;

        Jwt token = jwtDecoder.decode(jwtService.generateToken(employer));

        // Les entiers reviennent en Long : le type est déclaré, sinon l'inférence choisit
        // une interface fonctionnelle et l'assertion ne compile plus.
        Long userId = token.getClaim("userId");
        Long tokenVersion = token.getClaim(JwtService.TOKEN_VERSION_CLAIM);

        assertThat(token.getSubject()).isEqualTo(employer.getEmail());
        assertThat(userId).isEqualTo(employer.getId());
        assertThat(token.getClaimAsString("role")).isEqualTo(Role.EMPLOYER.name());
        assertThat(tokenVersion).isEqualTo(employer.getTokenVersion());
        assertThat(token.getClaimAsString("iss")).isEqualTo("agence-interim");
    }

    @Test
    @DisplayName("Le jeton expire, et son échéance est celle qui est configurée")
    void thetokenExpiresAtTheConfiguredHorizon() {
        // Un jeton sans échéance serait un mot de passe permanent que l'utilisateur ne
        // peut pas changer. Une heure est le compromis retenu, adossé à la révocation par
        // version de session pour les cas urgents.
        Jwt token = jwtDecoder.decode(jwtService.generateToken(fixtures.worker));

        Instant issued = token.getIssuedAt();
        Instant expires = token.getExpiresAt();
        assertThat(issued).isNotNull();
        assertThat(expires).isNotNull().isAfter(issued);
        assertThat(java.time.Duration.between(issued, expires).toMinutes()).isEqualTo(60);
    }

    @Test
    @DisplayName("Un jeton modifié après coup n'est plus accepté")
    void atamperedTokenIsNoLongerAccepted() {
        // La signature est ce qui empêche de se réécrire administrateur : changer un seul
        // caractère de la charge utile invalide le jeton entier.
        String token = jwtService.generateToken(fixtures.worker);
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1].substring(0, parts[1].length() - 2) + "AA." + parts[2];

        assertThatExceptionOfType(org.springframework.security.oauth2.jwt.JwtException.class)
                .isThrownBy(() -> jwtDecoder.decode(tampered));
    }

    // ------------------------------------------------------------------------------ outils

    private AuditEvent latest() {
        return auditEventRepository.findByOrderByOccurredAtDescIdDesc(PageRequest.of(0, 1))
                .getContent().get(0);
    }
}
