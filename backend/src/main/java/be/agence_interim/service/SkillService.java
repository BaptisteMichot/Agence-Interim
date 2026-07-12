package be.agence_interim.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.model.Skill;
import be.agence_interim.model.SkillLevel;
import be.agence_interim.model.SkillUser;
import be.agence_interim.repository.SkillRepository;
import be.agence_interim.repository.SkillUserRepository;
import be.agence_interim.repository.UserRepository;

/** Référentiel de compétences et compétences de l'utilisateur courant. */
@Service
public class SkillService {

    private final SkillRepository skillRepository;
    private final SkillUserRepository skillUserRepository;
    private final UserRepository userRepository;

    public SkillService(
            SkillRepository skillRepository,
            SkillUserRepository skillUserRepository,
            UserRepository userRepository) {
        this.skillRepository = skillRepository;
        this.skillUserRepository = skillUserRepository;
        this.userRepository = userRepository;
    }

    /** Compétences que l'utilisateur peut sélectionner (globales + ses créations). */
    public List<Skill> available(int userId) {
        return skillRepository.findByIsGlobalTrueOrCreatedByIdOrderByNameAsc(userId);
    }

    @Transactional(readOnly = true)
    public List<SkillUser> userSkills(int userId) {
        return skillUserRepository.findByUserIdFetchSkill(userId);
    }

    @Transactional
    public SkillUser add(int userId, Integer skillId, String name, SkillLevel level) {
        Skill skill = resolve(userId, skillId, name);
        if (skillUserRepository.existsByUserIdAndSkillId(userId, skill.getId())) {
            throw new IllegalArgumentException("Cette compétence est déjà dans votre profil.");
        }
        SkillUser skillUser = new SkillUser();
        skillUser.setSkill(skill);
        skillUser.setUser(userRepository.getReferenceById(userId));
        skillUser.setLevel(level);
        return skillUserRepository.save(skillUser);
    }

    @Transactional
    public SkillUser updateLevel(int userId, int skillId, SkillLevel level) {
        SkillUser skillUser = skillUserRepository.findByUserIdAndSkillId(userId, skillId)
                .orElseThrow(() -> new NoSuchElementException("Compétence introuvable dans votre profil."));
        skillUser.setLevel(level);
        return skillUserRepository.save(skillUser);
    }

    @Transactional
    public void remove(int userId, int skillId) {
        SkillUser skillUser = skillUserRepository.findByUserIdAndSkillId(userId, skillId)
                .orElseThrow(() -> new NoSuchElementException("Compétence introuvable dans votre profil."));
        skillUserRepository.delete(skillUser);
    }

    /** Trouve la compétence à rattacher : par id (globale/perso), sinon par nom (réutilise ou crée une perso). */
    private Skill resolve(int userId, Integer skillId, String name) {
        if (skillId != null) {
            Skill skill = skillRepository.findById(skillId)
                    .orElseThrow(() -> new NoSuchElementException("Compétence introuvable."));
            if (!isAccessible(skill, userId)) {
                throw new NoSuchElementException("Compétence introuvable.");
            }
            return skill;
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Indiquez une compétence.");
        }
        String trimmed = name.trim();
        return skillRepository.findFirstByNameIgnoreCaseAndIsGlobalTrue(trimmed)
                .or(() -> skillRepository.findFirstByNameIgnoreCaseAndCreatedById(trimmed, userId))
                .orElseGet(() -> createCustom(userId, trimmed));
    }

    private Skill createCustom(int userId, String name) {
        Skill skill = new Skill();
        skill.setName(name);
        skill.setIsGlobal(false);
        skill.setCreatedBy(userRepository.getReferenceById(userId));
        return skillRepository.save(skill);
    }

    private boolean isAccessible(Skill skill, int userId) {
        return Boolean.TRUE.equals(skill.getIsGlobal())
                || (skill.getCreatedBy() != null && skill.getCreatedBy().getId() == userId);
    }
}
