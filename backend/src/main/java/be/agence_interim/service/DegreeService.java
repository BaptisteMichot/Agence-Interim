package be.agence_interim.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.model.Degree;
import be.agence_interim.model.DegreeType;
import be.agence_interim.model.DegreeUser;
import be.agence_interim.repository.DegreeRepository;
import be.agence_interim.repository.DegreeUserRepository;
import be.agence_interim.repository.UserRepository;

/** Référentiel de diplômes et diplômes de l'utilisateur courant. */
@Service
public class DegreeService {

    private final DegreeRepository degreeRepository;
    private final DegreeUserRepository degreeUserRepository;
    private final UserRepository userRepository;

    public DegreeService(
            DegreeRepository degreeRepository,
            DegreeUserRepository degreeUserRepository,
            UserRepository userRepository) {
        this.degreeRepository = degreeRepository;
        this.degreeUserRepository = degreeUserRepository;
        this.userRepository = userRepository;
    }

    public List<Degree> available(int userId) {
        return degreeRepository.findByIsGlobalTrueOrCreatedByIdOrderByTypeAscSectionAsc(userId);
    }

    @Transactional(readOnly = true)
    public List<DegreeUser> userDegrees(int userId) {
        return degreeUserRepository.findByUserIdFetchDegree(userId);
    }

    @Transactional
    public DegreeUser add(
            int userId, Integer degreeId, DegreeType type, String section,
            String institution, Integer graduationYear) {
        Degree degree = resolveDegree(userId, degreeId, type, section);
        if (degreeUserRepository.existsByUserIdAndDegreeId(userId, degree.getId())) {
            throw new IllegalArgumentException("Ce diplôme est déjà dans votre profil.");
        }
        DegreeUser degreeUser = new DegreeUser();
        degreeUser.setDegree(degree);
        degreeUser.setUser(userRepository.getReferenceById(userId));
        degreeUser.setInstitution(institution);
        degreeUser.setGraduationYear(graduationYear);
        return degreeUserRepository.save(degreeUser);
    }

    @Transactional
    public DegreeUser update(int userId, int degreeId, String institution, Integer graduationYear) {
        DegreeUser degreeUser = degreeUserRepository.findByUserIdAndDegreeId(userId, degreeId)
                .orElseThrow(() -> new NoSuchElementException("Diplôme introuvable dans votre profil."));
        degreeUser.setInstitution(institution);
        degreeUser.setGraduationYear(graduationYear);
        return degreeUserRepository.save(degreeUser);
    }

    @Transactional
    public void remove(int userId, int degreeId) {
        DegreeUser degreeUser = degreeUserRepository.findByUserIdAndDegreeId(userId, degreeId)
                .orElseThrow(() -> new NoSuchElementException("Diplôme introuvable dans votre profil."));
        degreeUserRepository.delete(degreeUser);
    }

    /** Trouve le diplôme à rattacher : par id (global/perso), sinon par type+section (réutilise ou crée un perso). */
    @Transactional
    public Degree resolveDegree(int userId, Integer degreeId, DegreeType type, String section) {
        if (degreeId != null) {
            Degree degree = degreeRepository.findById(degreeId)
                    .orElseThrow(() -> new NoSuchElementException("Diplôme introuvable."));
            if (!isAccessible(degree, userId)) {
                throw new NoSuchElementException("Diplôme introuvable.");
            }
            return degree;
        }
        if (type == null || section == null || section.isBlank()) {
            throw new IllegalArgumentException("Indiquez le type et la section du diplôme.");
        }
        String trimmed = section.trim();
        return degreeRepository.findFirstByTypeAndSectionIgnoreCaseAndIsGlobalTrue(type, trimmed)
                .or(() -> degreeRepository.findFirstByTypeAndSectionIgnoreCaseAndCreatedById(type, trimmed, userId))
                .orElseGet(() -> createCustom(userId, type, trimmed));
    }

    private Degree createCustom(int userId, DegreeType type, String section) {
        Degree degree = new Degree();
        degree.setType(type);
        degree.setSection(section);
        degree.setIsGlobal(false);
        degree.setCreatedBy(userRepository.getReferenceById(userId));
        return degreeRepository.save(degree);
    }

    private boolean isAccessible(Degree degree, int userId) {
        return Boolean.TRUE.equals(degree.getIsGlobal())
                || (degree.getCreatedBy() != null && degree.getCreatedBy().getId() == userId);
    }
}
