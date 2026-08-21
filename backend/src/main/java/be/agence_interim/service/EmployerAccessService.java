package be.agence_interim.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.dto.AdminEmployerRequestResponse;
import be.agence_interim.dto.EmployerCompanyRequest;
import be.agence_interim.dto.MyEmployerRequestResponse;
import be.agence_interim.dto.PageResponse;
import be.agence_interim.dto.EmployerRegisterRequest;
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
    public void registerEmployer(EmployerRegisterRequest request) {
        String normalizedEmail = Strings.normalizeEmail(request.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }

        User user = new User();
        user.setRole(Role.EMPLOYER_PENDING);
        user.setLastName(request.lastName());
        user.setFirstName(request.firstName());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        applyCompany(user, request.companyName(), request.address(), request.companyNumber(),
                request.jointCommittee());
        User savedUser = userRepository.save(user);

        createRequest(savedUser, null);
    }

    /** Fiche entreprise de l'employeur connecté (mentions légales reprises sur les contrats). */
    @Transactional
    public User updateCompany(int userId, EmployerCompanyRequest request) {
        User user = userRepository.requireById(userId);
        applyCompany(user, request.companyName(), request.address(), request.companyNumber(),
                request.jointCommittee());
        return userRepository.save(user);
    }

    /** Contrôle et normalise les mentions légales de l'entreprise utilisatrice. */
    private void applyCompany(
            User user, String companyName, String address, String companyNumber, String jointCommittee) {
        if (!BelgianIdentifiers.isValidCompanyNumber(companyNumber)) {
            throw new IllegalArgumentException(
                    "Le numéro d'entreprise est invalide : il compte 10 chiffres commençant par 0 ou 1, "
                            + "et sa clé de contrôle doit correspondre.");
        }
        user.setCompanyName(companyName.trim());
        user.setAddress(address.trim());
        user.setCompanyNumber(BelgianIdentifiers.formatCompanyNumber(companyNumber));
        user.setJointCommittee(jointCommittee.trim());
    }

    /** Nouvelle demande après un refus, avec un message justificatif facultatif. */
    @Transactional
    public void reapply(int userId, String message) {
        User user = userRepository.requireById(userId);
        if (user.getRole() != Role.EMPLOYER_PENDING) {
            throw new IllegalArgumentException("Action non autorisee.");
        }
        EmployerAccessRequest latest = latestRequest(userId);
        if (latest == null || latest.getStatus() != EmployerAccessStatus.REFUSED) {
            throw new IllegalArgumentException("Vous ne pouvez pas refaire de demande pour le moment.");
        }
        if (latest.isReapplyBlocked()) {
            throw new IllegalArgumentException(
                    "Votre demande a été refusée définitivement : vous ne pouvez plus en soumettre de nouvelle.");
        }
        String cleaned = Strings.blankToNull(message);
        createRequest(user, cleaned);
    }

    /** Dernière demande de l'utilisateur, ou null s'il n'en a pas. */
    public EmployerAccessRequest latestRequest(int userId) {
        return requestRepository.findFirstByUserIdOrderByRequestDateDescIdDesc(userId).orElse(null);
    }

    /** Statut de la dernière demande de l'utilisateur, ou null s'il n'en a pas. */
    public EmployerAccessStatus latestStatus(int userId) {
        EmployerAccessRequest latest = latestRequest(userId);
        return latest == null ? null : latest.getStatus();
    }

    /**
     * Statut de la demande courante, avec l'indication d'un refus définitif : la page de
     * statut n'a pas à proposer un formulaire que le backend refusera.
     */
    public MyEmployerRequestResponse myRequest(int userId) {
        EmployerAccessRequest latest = latestRequest(userId);
        return latest == null
                ? new MyEmployerRequestResponse(null, false)
                : new MyEmployerRequestResponse(latest.getStatus(), latest.isReapplyBlocked());
    }

    /** Toutes les demandes (en attente + historique), ordonnées par id. */
    /**
     * Une page des demandes d'accès employeur, en attente ou déjà tranchées.
     *
     * @param group pending (en attente) ou history
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminEmployerRequestResponse> list(String group, Pageable pageable) {
        Page<EmployerAccessRequest> page = "history".equals(group)
                ? requestRepository.findByStatusNotFetchUser(EmployerAccessStatus.PENDING, pageable)
                : requestRepository.findByStatusFetchUser(EmployerAccessStatus.PENDING, pageable);

        // Une re-soumission est une demande qui n'est pas la première de son auteur.
        List<Integer> userIds = page.getContent().stream().map(r -> r.getUser().getId()).distinct().toList();
        Map<Integer, Integer> firstRequestByUser = userIds.isEmpty()
                ? Map.of()
                : requestRepository.findFirstRequestIdByUserIds(userIds).stream()
                        .collect(Collectors.toMap(
                                row -> row.getUserId(), row -> row.getFirstRequestId()));

        return PageResponse.of(page, request -> AdminEmployerRequestResponse.fromEntity(
                request,
                request.getId() > firstRequestByUser.getOrDefault(request.getUser().getId(), request.getId())));
    }

    /** Nombre de demandes en attente de traitement (chiffre du tableau de bord de l'agence). */
    public long pendingCount() {
        return requestRepository.countByStatus(EmployerAccessStatus.PENDING);
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

    /**
     * Refus de la demande. {@code block} ferme définitivement la porte : l'utilisateur ne
     * pourra plus resoumettre, ce qui évite qu'un compte éconduit inonde l'agence.
     */
    @Transactional
    public void refuse(int requestId, boolean block) {
        EmployerAccessRequest request = pendingRequest(requestId);
        request.setStatus(EmployerAccessStatus.REFUSED);
        request.setReapplyBlocked(block);
        requestRepository.save(request);
    }

    /** Suppression du compte par un employeur en attente (et de ses demandes). */
    @Transactional
    public void deleteAccount(int userId) {
        User user = userRepository.requireById(userId);
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
}
