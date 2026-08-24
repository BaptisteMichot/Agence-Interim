package be.agence_interim.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limiteur de débit à fenêtre fixe, en mémoire.
 *
 * <p>Chaque clé — une adresse IP, une adresse email — dispose de {@code maxAttempts}
 * tentatives par fenêtre de {@code window}. Au-delà, {@link #check(String)} refuse
 * jusqu'à la fin de la fenêtre en cours.
 *
 * <p><strong>Pourquoi en mémoire ?</strong> Comme le registre des sessions du chat et
 * les codes de signature, un compteur de tentatives est une donnée de très courte durée
 * de vie : la perdre au redémarrage rouvre au pire une fenêtre d'attaque de quelques
 * minutes. La contrepartie est connue et assumée : le compteur est propre à l'instance.
 * Dès que plusieurs instances tourneront derrière un répartiteur de charge, il faudra le
 * déporter (Redis) ou compter au niveau du reverse proxy — sans quoi le quota réel est
 * multiplié par le nombre d'instances.
 *
 * <p>La fenêtre est fixe et non glissante : c'est moins précis en bordure de fenêtre
 * (jusqu'à deux fois le quota à cheval sur deux fenêtres) mais cela tient en une entrée
 * de table par clé, là où une fenêtre glissante exige de conserver chaque horodatage.
 * Pour freiner une attaque par dictionnaire, cette précision suffit largement.
 */
public final class RateLimiter {

    /** Compteur d'une clé sur la fenêtre ouverte à {@code start}. */
    private record Window(Instant start, int count) {
    }

    /**
     * Au-delà de ce nombre de clés suivies, les fenêtres périmées sont purgées. Sans ce
     * garde-fou, un attaquant qui varie l'email à chaque essai ferait grossir la table
     * indéfiniment : la protection deviendrait elle-même le déni de service.
     */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration window;
    private final String subject;

    /**
     * @param maxAttempts nombre de tentatives autorisées par fenêtre
     * @param window      durée de la fenêtre
     * @param subject     ce que la clé désigne, repris dans le message d'erreur
     */
    public RateLimiter(int maxAttempts, Duration window, String subject) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.subject = subject;
    }

    /**
     * Vérifie que la clé a encore du quota, sans rien consommer.
     *
     * @throws TooManyAttemptsException si le quota est épuisé
     */
    public void check(String key) {
        Window current = windows.get(key);
        if (current != null && !expired(current) && current.count() >= maxAttempts) {
            throw new TooManyAttemptsException(retryAfter(current), subject);
        }
    }

    /** Consomme une tentative sur la clé. À appeler sur les échecs uniquement. */
    public void record(String key) {
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(this::expired);
        }
        windows.merge(
                key,
                new Window(Instant.now(), 1),
                (existing, fresh) -> expired(existing)
                        ? fresh
                        : new Window(existing.start(), existing.count() + 1));
    }

    /** Efface le compteur de la clé : la tentative a réussi, la suspicion tombe. */
    public void clear(String key) {
        windows.remove(key);
    }

    private boolean expired(Window value) {
        return value.start().plus(window).isBefore(Instant.now());
    }

    private Duration retryAfter(Window value) {
        Duration remaining = Duration.between(Instant.now(), value.start().plus(window));
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
