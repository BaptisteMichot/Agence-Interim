package be.agence_interim.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Chiffre au repos les champs les plus sensibles du profil : numéro de registre
 * national et numéro de compte.
 *
 * <p><strong>Pourquoi ces deux-là.</strong> Ce sont les données que l'on va chercher en
 * premier dans une base volée, et les seules du modèle dont la divulgation dépasse la
 * plateforme : un NISS sert à usurper une identité auprès de tiers, un IBAN à monter une
 * fraude au virement. Le reste du profil — nom, expériences, candidatures — est certes
 * personnel, mais son exposition ne survit pas à l'incident ; celle du NISS, si.
 *
 * <p><strong>Ce que cela protège, et ce que cela ne protège pas.</strong> Un accès en
 * lecture à la base seule — sauvegarde égarée, export, console d'administration, copie
 * de développement — ne rend plus ces champs lisibles, puisque la clé vit dans la
 * configuration de l'application et non dans la base. En revanche, une application
 * compromise déchiffre pour son compte : le chiffrement au repos ne remplace ni le
 * contrôle d'accès ni le cloisonnement, il limite le rayon d'une fuite de données.
 *
 * <p><strong>AES-GCM</strong> plutôt qu'AES-CBC : le mode fournit l'authentification en
 * même temps que la confidentialité, donc une valeur altérée en base est rejetée au lieu
 * d'être déchiffrée en n'importe quoi. Le vecteur d'initialisation est tiré au hasard à
 * chaque écriture et rangé en tête du message — le réutiliser avec la même clé briserait
 * complètement GCM.
 *
 * <p><strong>Compatibilité avec l'existant.</strong> Une valeur qui ne porte pas le
 * préfixe {@link #PREFIX} est rendue telle quelle : les enregistrements écrits avant la
 * mise en place du chiffrement restent lisibles, et se chiffrent à leur prochaine
 * écriture. Sans cette tolérance, l'activation aurait exigé une migration de données
 * préalable — et aurait cassé toute base déjà remplie.
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    /**
     * Marque les valeurs chiffrées, et porte un numéro de version : le jour où
     * l'algorithme change, l'ancien format reste reconnaissable et déchiffrable.
     */
    private static final String PREFIX = "enc:v1:";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey key;

    public EncryptedStringConverter(@Value("${app.security.encryption-key}") String configuredKey) {
        if (configuredKey == null || configuredKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "app.security.encryption-key doit contenir au moins 32 caracteres.");
        }
        // La clé de configuration est une phrase, pas 32 octets exactement : SHA-256 la
        // ramène à la taille attendue par AES-256 sans imposer de format à l'exploitant.
        this.key = new SecretKeySpec(sha256(configuredKey), "AES");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException e) {
            // Écrire en clair « pour que ça passe » serait le pire des deux mondes : la
            // donnée serait exposée et personne ne le saurait.
            throw new IllegalStateException("Chiffrement de la donnée impossible.", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (!dbData.startsWith(PREFIX)) {
            // Valeur antérieure à la mise en place du chiffrement.
            return dbData;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(dbData.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(TAG_LENGTH_BITS, payload, 0, IV_LENGTH));
            byte[] plain = cipher.doFinal(payload, IV_LENGTH, payload.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Déchiffrement impossible : la clé de configuration n'est pas celle qui a "
                            + "servi à écrire cette donnée.", e);
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 indisponible.", e);
        }
    }
}
