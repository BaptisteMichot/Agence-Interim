package be.agence_interim.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Codes à usage unique envoyés par email pour confirmer la signature d'un contrat.
 * Vérifier ce code prouve que le signataire a bien accès à la boîte mail rattachée à
 * son compte : le consentement n'est plus un simple clic.
 *
 * <p>Les codes vivent en mémoire, comme le registre des sessions du chat : ils sont
 * de courte durée et un code perdu au redémarrage se redemande en un clic.
 */
@Service
public class SigningCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();
    /** Au-delà, on considère que le code est cherché au hasard. */
    private static final int MAX_ATTEMPTS = 5;

    private final Map<String, Code> codes = new ConcurrentHashMap<>();
    private final int validityMinutes;

    private record Code(String value, LocalDateTime expiresAt, int attempts) {

        boolean expired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    public SigningCodeService(@Value("${app.signature.code-validity-minutes:15}") int validityMinutes) {
        this.validityMinutes = validityMinutes;
    }

    public int getValidityMinutes() {
        return validityMinutes;
    }

    /** Génère (ou remplace) le code de signature d'un utilisateur pour un contrat. */
    public String generate(int contractId, int userId) {
        String value = String.format("%06d", RANDOM.nextInt(1_000_000));
        codes.put(key(contractId, userId),
                new Code(value, LocalDateTime.now().plusMinutes(validityMinutes), 0));
        return value;
    }

    /**
     * Vérifie le code saisi et le consomme. Lève une exception explicite si le code est
     * absent, expiré, erroné, ou si trop de tentatives ont échoué.
     */
    public void verify(int contractId, int userId, String submitted) {
        String key = key(contractId, userId);
        Code code = codes.get(key);
        if (code == null || code.expired()) {
            codes.remove(key);
            throw new IllegalArgumentException(
                    "Aucun code valide : demandez un nouveau code de signature.");
        }
        if (!code.value().equals(submitted == null ? null : submitted.trim())) {
            if (code.attempts() + 1 >= MAX_ATTEMPTS) {
                codes.remove(key);
                throw new IllegalArgumentException(
                        "Trop de tentatives : demandez un nouveau code de signature.");
            }
            codes.put(key, new Code(code.value(), code.expiresAt(), code.attempts() + 1));
            throw new IllegalArgumentException("Code incorrect.");
        }
        codes.remove(key);
    }

    private String key(int contractId, int userId) {
        return contractId + ":" + userId;
    }
}
