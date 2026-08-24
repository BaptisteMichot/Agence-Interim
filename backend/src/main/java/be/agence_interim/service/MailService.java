package be.agence_interim.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envoi d'emails : via SMTP si activé dans la configuration ({@code MAIL_ENABLED=true}),
 * sinon l'email est journalisé (simulation en développement).
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean enabled;
    private final String from;

    public MailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${app.mail.from:no-reply@agence-interim.be}") String from) {
        this.mailSenderProvider = mailSenderProvider;
        this.enabled = enabled;
        this.from = from;
    }

    /** Envoie (ou journalise) un email texte. Ne lève jamais : un échec d'envoi est loggé. */
    public void send(String to, String subject, String body) {
        String safeSubject = singleLine(subject);
        JavaMailSender sender = enabled ? mailSenderProvider.getIfAvailable() : null;
        if (sender == null) {
            simulate(to, safeSubject, body);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(safeSubject);
            message.setText(body);
            sender.send(message);
            log.info("Email envoyé à {} ({})", to, safeSubject);
        } catch (Exception e) {
            log.error("Échec d'envoi de l'email à {} : {}", to, e.getMessage());
        }
    }

    /**
     * Email non envoyé, journalisé à la place.
     *
     * <p>Le corps ne part qu'en {@code DEBUG}, jamais en {@code INFO} : il contient les
     * codes de signature et de réinitialisation, c'est-à-dire des secrets à usage unique.
     * En développement, où le niveau {@code DEBUG} est de mise, la simulation reste
     * pleinement lisible ; en production, un basculement accidentel de
     * {@code MAIL_ENABLED} ne déverse plus ces codes dans les journaux d'application,
     * qui sont souvent centralisés et lus par plus de monde que la base de données.
     */
    private void simulate(String to, String subject, String body) {
        log.info("[EMAIL SIMULÉ] to={} | subject={} | corps en DEBUG", to, subject);
        log.debug("[EMAIL SIMULÉ] to={} | subject={} | body:\n{}", to, subject, body);
    }

    /**
     * Réduit une valeur à une seule ligne.
     *
     * <p>Le sujet reprend du texte saisi par un utilisateur — le titre d'une offre, celui
     * d'une mission. JavaMail encode ce qu'on lui donne et ferme l'injection d'en-tête en
     * pratique ; en dépendre reste inutile quand couper les retours à la ligne coûte une
     * ligne de code.
     */
    private static String singleLine(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]+", " ").trim();
    }
}
