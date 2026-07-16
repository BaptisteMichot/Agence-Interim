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
        JavaMailSender sender = enabled ? mailSenderProvider.getIfAvailable() : null;
        if (sender == null) {
            log.info("[EMAIL SIMULÉ] to={} | subject={} | body:\n{}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            log.info("Email envoyé à {} ({})", to, subject);
        } catch (Exception e) {
            log.error("Échec d'envoi de l'email à {} : {}", to, e.getMessage());
        }
    }
}
