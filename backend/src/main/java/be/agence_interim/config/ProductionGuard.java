package be.agence_interim.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Refuse de démarrer sur une configuration incohérente avec un déploiement réel.
 *
 * <p>Les incidents de ce type ne viennent presque jamais d'un défaut de code : ils
 * viennent d'un fichier d'environnement de développement recopié tel quel sur un
 * serveur. Le jeu de démonstration crée alors deux comptes dont le mot de passe est
 * écrit dans le dépôt, la simulation d'email déverse les codes à usage unique dans les
 * journaux, et Hibernate se met à modifier le schéma tout seul. Chacun de ces réglages
 * est parfaitement justifié en développement, et indéfendable ailleurs.
 *
 * <p><strong>Comment l'application sait-elle qu'elle est « ailleurs » ?</strong> Par
 * {@code app.security.cookie-secure}. C'est le seul réglage qui ne peut pas être vrai en
 * développement — un cookie {@code Secure} est ignoré par le navigateur sur une
 * connexion en clair — et qui doit l'être partout ailleurs. Il fait donc un indicateur
 * fiable, sans introduire un profil Spring supplémentaire à tenir à jour.
 *
 * <p>L'échec est franc et la liste est complète : un démarrage qui s'arrête sur le
 * premier problème fait découvrir les suivants un par un, à chaque redéploiement.
 */
@Component
public class ProductionGuard {

    private static final Logger log = LoggerFactory.getLogger(ProductionGuard.class);

    private final boolean cookieSecure;
    private final boolean demoData;
    private final boolean mailEnabled;
    private final String ddlAuto;
    private final String frontendUrl;

    public ProductionGuard(
            @Value("${app.security.cookie-secure}") boolean cookieSecure,
            @Value("${app.demo-data.enabled:false}") boolean demoData,
            @Value("${app.mail.enabled:false}") boolean mailEnabled,
            @Value("${spring.jpa.hibernate.ddl-auto:none}") String ddlAuto,
            @Value("${app.frontend.url}") String frontendUrl) {
        this.cookieSecure = cookieSecure;
        this.demoData = demoData;
        this.mailEnabled = mailEnabled;
        this.ddlAuto = ddlAuto;
        this.frontendUrl = frontendUrl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void check() {
        if (!cookieSecure) {
            log.warn("Cookie de session sans attribut Secure : configuration de développement. "
                    + "En production, COOKIE_SECURE=true et HTTPS obligatoire.");
            return;
        }

        List<String> problems = new ArrayList<>();
        if (demoData) {
            problems.add("app.demo-data.enabled=true : le jeu de démonstration crée des comptes "
                    + "dont le mot de passe figure dans le code source.");
        }
        if (!mailEnabled) {
            problems.add("app.mail.enabled=false : les emails seraient simulés, et les codes à "
                    + "usage unique journalisés au lieu d'être envoyés.");
        }
        if ("update".equalsIgnoreCase(ddlAuto) || "create".equalsIgnoreCase(ddlAuto)
                || "create-drop".equalsIgnoreCase(ddlAuto)) {
            problems.add("spring.jpa.hibernate.ddl-auto=" + ddlAuto + " : Hibernate modifierait "
                    + "le schéma sans revue. Attendu : validate, avec des migrations versionnées.");
        }
        if (frontendUrl != null && frontendUrl.startsWith("http://")) {
            problems.add("app.frontend.url en http:// alors que le cookie est marqué Secure : "
                    + "les deux réglages se contredisent.");
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "Configuration incompatible avec un déploiement en HTTPS :\n  - "
                            + String.join("\n  - ", problems));
        }
        log.info("Contrôle de configuration : déploiement sécurisé, aucun réglage de "
                + "développement actif.");
    }
}
