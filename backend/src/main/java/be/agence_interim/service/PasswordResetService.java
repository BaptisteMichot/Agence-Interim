package be.agence_interim.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.model.AuditAction;
import be.agence_interim.model.User;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.security.Throttles;

/**
 * Réinitialisation du mot de passe par code à usage unique envoyé sur l'adresse du
 * compte.
 *
 * <p>Sans ce parcours, un utilisateur qui a oublié son mot de passe — ou qui a des
 * raisons de le croire compromis — n'a aucun moyen de reprendre la main. C'était le cas
 * jusqu'ici, y compris pour le compte administrateur, dont le mot de passe restait
 * indéfiniment celui inscrit dans le fichier de configuration.
 *
 * <p><strong>Réponse volontairement muette.</strong> La demande répond de la même façon
 * que l'adresse existe ou non. Répondre « aucun compte à cette adresse » offrirait la
 * liste des comptes de la plateforme à qui la demande, une requête à la fois — et sur un
 * site d'emploi, savoir que telle personne y a un compte est déjà une information sur
 * elle.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    /** Court : un code de réinitialisation se saisit dans la foulée de sa réception. */
    private static final int VALIDITY_MINUTES = 15;
    private static final int MAX_ATTEMPTS = 5;

    private final OneTimeCodes codes = new OneTimeCodes(
            Duration.ofMinutes(VALIDITY_MINUTES), MAX_ATTEMPTS, "de réinitialisation");

    private final UserRepository userRepository;
    private final AuthService authService;
    private final AuditService auditService;
    private final MailService mailService;
    private final Throttles throttles;
    private final String frontendUrl;

    public PasswordResetService(
            UserRepository userRepository,
            AuthService authService,
            AuditService auditService,
            MailService mailService,
            Throttles throttles,
            @Value("${app.frontend.url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.auditService = auditService;
        this.mailService = mailService;
        this.throttles = throttles;
        this.frontendUrl = frontendUrl;
    }

    /**
     * Envoie un code de réinitialisation si un compte porte cette adresse. Ne dit jamais
     * si c'est le cas.
     */
    @Transactional(readOnly = true)
    public void requestCode(String rawEmail) {
        String email = Strings.normalizeEmail(rawEmail);
        // Le quota est consommé même pour une adresse inconnue : sans cela, la différence
        // de comportement entre les deux cas rétablit l'énumération qu'on vient de fermer.
        throttles.checkAndRecordMail("reset:" + email);

        userRepository.findByEmail(email).ifPresentOrElse(
                user -> mailService.send(
                        user.getEmail(),
                        "Réinitialisation de votre mot de passe",
                        body(user, codes.issue(email))),
                () -> log.info("Réinitialisation demandée pour une adresse sans compte."));
    }

    /** Vérifie le code, pose le nouveau mot de passe et révoque les sessions ouvertes. */
    @Transactional
    public void reset(String rawEmail, String code, String newPassword) {
        String email = Strings.normalizeEmail(rawEmail);
        codes.verify(email, code);

        // Le compte peut avoir disparu entre l'envoi du code et sa saisie.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucun code valide : demandez un nouveau code de réinitialisation."));
        authService.applyNewPassword(user, newPassword);
        auditService.record(
                AuditAction.PASSWORD_RESET, user.getId(), "USER", user.getId(), "Par code email");
        log.info("Mot de passe réinitialisé pour l'utilisateur {}.", user.getId());
    }

    private String body(User user, String code) {
        return "Bonjour " + user.getFirstName() + ",\n\n"
                + "Votre code de réinitialisation est : " + code + "\n"
                + "Il est valable " + VALIDITY_MINUTES + " minutes.\n\n"
                + "Saisissez-le sur " + frontendUrl + "/mot-de-passe-oublie pour choisir un "
                + "nouveau mot de passe.\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, ignorez ce message : "
                + "votre mot de passe actuel reste valable.\n\n"
                + "L'agence d'intérim";
    }
}
