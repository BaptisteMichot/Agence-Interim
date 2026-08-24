package be.agence_interim.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Codes à usage unique envoyés par email.
 *
 * <p>Mécanique partagée par la signature de contrat et la réinitialisation de mot de
 * passe : dans les deux cas, saisir le code prouve que l'appelant a accès à la boîte
 * mail rattachée au compte. Les deux usages diffèrent par la durée de validité et par
 * ce qu'ils autorisent ensuite, pas par la façon de tirer, de garder et de vérifier le
 * code — d'où cette classe commune.
 *
 * <p>Les codes vivent en mémoire : ils sont de très courte durée, et un code perdu au
 * redémarrage se redemande en un clic.
 *
 * <p>Deux précautions valent d'être relevées. La vérification tient dans un
 * {@code compute}, opération atomique de {@link ConcurrentHashMap} : un {@code get}
 * suivi d'un {@code put} laisse deux tentatives simultanées lire le même compteur et
 * dépasser le plafond d'essais. Et la comparaison passe par
 * {@link MessageDigest#isEqual}, qui parcourt les deux valeurs en entier :
 * {@code String.equals} s'arrête au premier caractère différent et révèle donc, par son
 * temps d'exécution, combien de caractères de tête sont corrects.
 */
public final class OneTimeCodes {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Code émis pour une clé, avec son échéance et le nombre d'essais déjà consommés. */
    private record Code(String value, Instant expiresAt, int attempts) {

        boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    /** Issue possible d'une vérification. */
    private enum Outcome {
        /** Code correct : consommé. */
        ACCEPTED,
        /** Aucun code en cours, ou code expiré. */
        MISSING,
        /** Code erroné, essais restants. */
        WRONG,
        /** Code erroné, plafond d'essais atteint : le code est retiré. */
        EXHAUSTED
    }

    /** Levée quand le code est absent, expiré, erroné, ou que les essais sont épuisés. */
    public static class InvalidCodeException extends IllegalArgumentException {
        public InvalidCodeException(String message) {
            super(message);
        }
    }

    private final Map<String, Code> codes = new ConcurrentHashMap<>();
    private final Duration validity;
    private final int maxAttempts;
    private final String label;

    /**
     * @param validity    durée de vie d'un code
     * @param maxAttempts essais erronés tolérés avant invalidation du code
     * @param label       ce que le code autorise, repris dans les messages d'erreur
     */
    public OneTimeCodes(Duration validity, int maxAttempts, String label) {
        this.validity = validity;
        this.maxAttempts = maxAttempts;
        this.label = label;
    }

    public Duration getValidity() {
        return validity;
    }

    /** Tire un code à six chiffres pour cette clé, en remplaçant celui qui s'y trouvait. */
    public String issue(String key) {
        String value = String.format("%06d", RANDOM.nextInt(1_000_000));
        codes.put(key, new Code(value, Instant.now().plus(validity), 0));
        return value;
    }

    /**
     * Vérifie le code saisi et le consomme.
     *
     * @throws InvalidCodeException si le code est absent, expiré, erroné, ou si trop de
     *                              tentatives ont échoué
     */
    public void verify(String key, String submitted) {
        String cleaned = submitted == null ? "" : submitted.trim();
        AtomicReference<Outcome> outcome = new AtomicReference<>(Outcome.MISSING);

        // Le corps s'exécute sous le verrou du compartiment : lecture du compteur,
        // comparaison et réécriture forment une seule opération indivisible.
        codes.compute(key, (unusedKey, current) -> {
            if (current == null || current.expired()) {
                outcome.set(Outcome.MISSING);
                return null;
            }
            if (matches(current.value(), cleaned)) {
                outcome.set(Outcome.ACCEPTED);
                return null;
            }
            int attempts = current.attempts() + 1;
            if (attempts >= maxAttempts) {
                outcome.set(Outcome.EXHAUSTED);
                return null;
            }
            outcome.set(Outcome.WRONG);
            return new Code(current.value(), current.expiresAt(), attempts);
        });

        switch (outcome.get()) {
            case ACCEPTED -> {
                // Rien à faire : le code est consommé.
            }
            case MISSING -> throw new InvalidCodeException(
                    "Aucun code valide : demandez un nouveau code " + label + ".");
            case EXHAUSTED -> throw new InvalidCodeException(
                    "Trop de tentatives : demandez un nouveau code " + label + ".");
            case WRONG -> throw new InvalidCodeException("Code incorrect.");
        }
    }

    /** Retire le code de cette clé, qu'il existe ou non. */
    public void invalidate(String key) {
        codes.remove(key);
    }

    private static boolean matches(String expected, String submitted) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                submitted.getBytes(StandardCharsets.UTF_8));
    }
}
