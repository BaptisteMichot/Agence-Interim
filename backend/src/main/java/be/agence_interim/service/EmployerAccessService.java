package be.agence_interim.service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.model.EmployerAccessRequest;
import be.agence_interim.model.EmployerAccessStatus;
import be.agence_interim.model.Role;
import be.agence_interim.model.User;
import be.agence_interim.repository.EmployerAccessRequestRepository;
import be.agence_interim.repository.UserRepository;

/**
 * Inscription employeur (compte + demande d'accès), re-demande après refus, suppression
 * de compte, et traitement des demandes par l'admin. Flux distinct de l'inscription intérimaire.
 */
@Service
public class EmployerAccessService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployerAccessRequestRepository requestRepository;

    public EmployerAccessService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmployerAccessRequestRepository requestRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.requestRepository = requestRepository;
    }

    /**
     * Crée le compte de l'employeur (rôle EMPLOYER_PENDING en attendant validation) et une
     * demande d'accès employeur au statut PENDING.
     */
    @Transactional
    public void registerEmployer(
            String lastName, String firstName, String email, String rawPassword, String companyName) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }

        User user = new User();
        user.setRole(Role.EMPLOYER_PENDING);
        user.setLastName(lastName);
        user.setFirstName(firstName);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setCompanyName(companyName);
        User savedUser = userRepository.save(user);

        createRequest(savedUser, null);
    }

    /** Nouvelle demande après un refus, avec un message justificatif facultatif. */
    @Transactional
    public void reapply(int userId, String message) {
        User user = getUser(userId);
        if (user.getRole() != Role.EMPLOYER_PENDING) {
            throw new IllegalArgumentException("Action non autorisee.");
        }
        EmployerAccessStatus latest = latestStatus(userId);
        if (latest != EmployerAccessStatus.REFUSED) {
            throw new IllegalArgumentException("Vous ne pouvez pas refaire de demande pour le moment.");
        }
        String cleaned = message == null || message.isBlank() ? null : message.trim();
        createRequest(user, cleaned);
    }

    /** Statut de la dernière demande de l'utilisateur, ou null s'il n'en a pas. */
    public EmployerAccessStatus latestStatus(int userId) {
        return requestRepository.findFirstByUserIdOrderByRequestDateDescIdDesc(userId)
                .map(request -> request.getStatus())
                .orElse(null);
    }

    /** Toutes les demandes (en attente + historique), ordonnées par id. */
    public List<EmployerAccessRequest> listAll() {
        return requestRepository.findAllFetchUser();
    }

    @Transactional
    public void accept(int requestId) {
        EmployerAccessRequest request = pendingRequest(requestId);
        User user = request.getUser();
        user.setRole(Role.EMPLOYER);
        userRepository.save(user);
        request.setStatus(EmployerAccessStatus.ACCEPTED);
        requestRepository.save(request);
    }

    @Transactional
    public void refuse(int requestId) {
        EmployerAccessRequest request = pendingRequest(requestId);
        request.setStatus(EmployerAccessStatus.REFUSED);
        requestRepository.save(request);
    }

    /** Suppression du compte par un employeur en attente (et de ses demandes). */
    @Transactional
    public void deleteAccount(int userId) {
        User user = getUser(userId);
        if (user.getRole() != Role.EMPLOYER_PENDING) {
            throw new IllegalArgumentException("Action non autorisee.");
        }
        requestRepository.deleteByUserId(userId);
        userRepository.delete(user);
    }

    private void createRequest(User user, String message) {
        EmployerAccessRequest request = new EmployerAccessRequest();
        request.setUser(user);
        request.setRequestDate(LocalDate.now());
        request.setStatus(EmployerAccessStatus.PENDING);
        request.setMessage(message);
        requestRepository.save(request);
    }

    private EmployerAccessRequest pendingRequest(int requestId) {
        EmployerAccessRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Demande introuvable."));
        if (request.getStatus() != EmployerAccessStatus.PENDING) {
            throw new IllegalArgumentException("Cette demande a déjà été traitée.");
        }
        return request;
    }

    private User getUser(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable."));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
