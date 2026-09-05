package be.agence_interim.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Vérifie que les colonnes chiffrées peuvent contenir une valeur chiffrée.
 *
 * <p><strong>Pourquoi ce contrôle existe.</strong> Le chiffrement du numéro de registre
 * national et de l'IBAN rend les valeurs plus longues que le clair. Les colonnes ont donc
 * été redéclarées en {@code TEXT} — mais {@code ddl-auto=update} sait ajouter une colonne,
 * pas changer le type d'une colonne existante. Sur une base déjà remplie, elles restent
 * en {@code varchar(15)} et {@code varchar(42)}, et la première sauvegarde d'un profil
 * échoue sur une erreur de troncature parfaitement incompréhensible.
 *
 * <p>Le contrôle transforme cette panne différée en un refus de démarrer accompagné de la
 * commande à exécuter. C'est aussi une illustration de la limite qui a motivé le constat
 * sur {@code ddl-auto} : sans migrations versionnées, un changement de type de colonne
 * reste une opération manuelle, que rien ne rappelle au bon moment.
 */
@Component
public class SchemaGuard {

    private static final Logger log = LoggerFactory.getLogger(SchemaGuard.class);

    /** En deçà, la colonne ne peut pas contenir un chiffré (une centaine de caractères). */
    private static final int MINIMUM_LENGTH = 256;

    private static final List<String> ENCRYPTED_COLUMNS = List.of("national_number", "iban");

    private final JdbcTemplate jdbcTemplate;

    public SchemaGuard(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void check() {
        List<String> tooShort = new ArrayList<>();
        for (String column : ENCRYPTED_COLUMNS) {
            Integer length = maxLength(column);
            if (length != null && length < MINIMUM_LENGTH) {
                tooShort.add(column);
            }
        }
        if (tooShort.isEmpty()) {
            return;
        }

        StringBuilder sql = new StringBuilder();
        for (String column : tooShort) {
            sql.append("\n  ALTER TABLE users ALTER COLUMN ").append(column).append(" TYPE text;");
        }
        throw new IllegalStateException(
                "Ces colonnes sont trop courtes pour contenir une valeur chiffrée : "
                        + String.join(", ", tooShort)
                        + ".\nHibernate n'élargit pas une colonne qui existe déjà. Exécutez :"
                        + sql
                        + "\npuis relancez l'application.");
    }

    /**
     * Longueur maximale déclarée de la colonne, ou {@code null} si elle n'en a pas
     * (type texte) ou si la question n'a pas de sens sur cette base.
     */
    private Integer maxLength(String column) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    select character_maximum_length
                      from information_schema.columns
                     where lower(table_name) = 'users' and lower(column_name) = ?
                    """,
                    Integer.class,
                    column);
        } catch (RuntimeException e) {
            // Base sans information_schema exploitable : le contrôle ne s'applique pas.
            log.debug("Contrôle de schéma ignoré pour {} : {}", column, e.getMessage());
            return null;
        }
    }
}
