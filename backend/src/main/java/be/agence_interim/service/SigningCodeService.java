package be.agence_interim.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Codes à usage unique envoyés par email pour confirmer la signature d'un contrat.
 * Vérifier ce code prouve que le signataire a bien accès à la boîte mail rattachée à
 * son compte : le consentement n'est plus un simple clic.
 *
 * <p>La mécanique du code — tirage, expiration, plafond d'essais, comparaison à temps
 * constant — est celle de {@link OneTimeCodes}, partagée avec la réinitialisation de
 * mot de passe.
 */
@Service
public class SigningCodeService {

    /** Au-delà, on considère que le code est cherché au hasard. */
    private static final int MAX_ATTEMPTS = 5;

    private final OneTimeCodes codes;

    public SigningCodeService(@Value("${app.signature.code-validity-minutes:15}") int validityMinutes) {
        this.codes = new OneTimeCodes(
                Duration.ofMinutes(validityMinutes), MAX_ATTEMPTS, "de signature");
    }

    public int getValidityMinutes() {
        return (int) codes.getValidity().toMinutes();
    }

    /** Génère (ou remplace) le code de signature d'un utilisateur pour un contrat. */
    public String generate(int contractId, int userId) {
        return codes.issue(key(contractId, userId));
    }

    /** Vérifie le code saisi et le consomme. */
    public void verify(int contractId, int userId, String submitted) {
        codes.verify(key(contractId, userId), submitted);
    }

    private String key(int contractId, int userId) {
        return contractId + ":" + userId;
    }
}
