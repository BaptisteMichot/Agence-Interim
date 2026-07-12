package be.agence_interim.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import be.agence_interim.model.User;
import be.agence_interim.repository.UserRepository;
import jakarta.annotation.PostConstruct;

/** Dépôt, téléchargement et suppression du CV (PDF) de l'utilisateur courant. */
@Service
public class CvService {

    /** Taille maximale : 5 Mo (demande non-fonctionnelle). */
    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    /** Octets de signature d'un fichier PDF : "%PDF". */
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};

    private final Path storageDir;
    private final UserRepository userRepository;

    public CvService(
            @Value("${app.cv.storage-dir:uploads/cv}") String storageDir,
            UserRepository userRepository) {
        this.storageDir = Paths.get(storageDir).toAbsolutePath().normalize();
        this.userRepository = userRepository;
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(storageDir);
    }

    /**
     * Valide puis enregistre le CV sous son nom d'origine, dans le dossier de
     * l'utilisateur, en remplaçant l'éventuel précédent. Retourne le nom conservé.
     */
    public String store(int userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Aucun fichier fourni.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Le CV ne doit pas depasser 5 Mo.");
        }
        byte[] bytes = readBytes(file);
        if (!isPdf(bytes)) {
            throw new IllegalArgumentException("Le CV doit etre un fichier PDF.");
        }

        User user = getUser(userId);
        deleteCurrentFile(user);

        String fileName = safeName(file.getOriginalFilename());
        Path userDir = userDir(userId);
        try {
            Files.createDirectories(userDir);
            Files.write(resolveInUser(userId, fileName), bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Enregistrement du CV impossible.", e);
        }

        user.setCvFilePath(fileName);
        userRepository.save(user);
        return fileName;
    }

    public Resource load(int userId) {
        User user = getUser(userId);
        String fileName = user.getCvFilePath();
        if (fileName == null) {
            throw new NoSuchElementException("Aucun CV n'a ete depose.");
        }
        Path path = resolveInUser(userId, fileName);
        if (!Files.exists(path)) {
            throw new NoSuchElementException("Aucun CV n'a ete depose.");
        }
        return new FileSystemResource(path);
    }

    public void delete(int userId) {
        User user = getUser(userId);
        if (user.getCvFilePath() == null) {
            return;
        }
        deleteCurrentFile(user);
        user.setCvFilePath(null);
        userRepository.save(user);
    }

    private void deleteCurrentFile(User user) {
        String fileName = user.getCvFilePath();
        if (fileName == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolveInUser(user.getId(), fileName));
        } catch (IOException e) {
            throw new IllegalStateException("Suppression du CV impossible.", e);
        }
    }

    private User getUser(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable."));
    }

    private Path userDir(int userId) {
        return storageDir.resolve(String.valueOf(userId)).normalize();
    }

    /** Résout un nom de fichier dans le dossier de l'utilisateur en empêchant toute sortie du dossier. */
    private Path resolveInUser(int userId, String fileName) {
        Path userDir = userDir(userId);
        Path path = userDir.resolve(fileName).normalize();
        if (!path.startsWith(userDir)) {
            throw new IllegalArgumentException("Nom de fichier invalide.");
        }
        return path;
    }

    /** Nettoie le nom d'origine : retire tout chemin, ne garde que des caractères sûrs, garantit l'extension .pdf. */
    private String safeName(String originalName) {
        String name = originalName == null ? "" : originalName.replaceAll("^.*[\\\\/]", "");
        name = name.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (name.isBlank()) {
            name = "cv.pdf";
        }
        if (!name.toLowerCase().endsWith(".pdf")) {
            name = name + ".pdf";
        }
        return name;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Lecture du fichier impossible.", e);
        }
    }

    private boolean isPdf(byte[] bytes) {
        if (bytes.length < PDF_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (bytes[i] != PDF_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }
}
