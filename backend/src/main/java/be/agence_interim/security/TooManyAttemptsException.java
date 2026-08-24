package be.agence_interim.security;

import java.time.Duration;

/**
 * Quota de tentatives épuisé. Traduite en {@code 429 Too Many Requests} par le
 * gestionnaire d'erreurs global, avec l'en-tête {@code Retry-After} qui dit au client
 * — humain ou automate — quand revenir.
 */
public class TooManyAttemptsException extends RuntimeException {

    private final transient Duration retryAfter;

    public TooManyAttemptsException(Duration retryAfter, String subject) {
        super("Trop de tentatives " + subject + ". Réessayez dans "
                + Math.max(1, retryAfter.toMinutes() + 1) + " minute(s).");
        this.retryAfter = retryAfter;
    }

    /** Délai avant la prochaine tentative autorisée. */
    public Duration getRetryAfter() {
        return retryAfter;
    }
}
