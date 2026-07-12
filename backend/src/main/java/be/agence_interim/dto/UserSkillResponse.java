package be.agence_interim.dto;

import be.agence_interim.model.Skill;
import be.agence_interim.model.SkillLevel;
import be.agence_interim.model.SkillUser;

/** Compétence du profil de l'utilisateur, avec son niveau. */
public record UserSkillResponse(int skillId, String name, boolean custom, SkillLevel level) {

    public static UserSkillResponse fromEntity(SkillUser skillUser) {
        Skill skill = skillUser.getSkill();
        return new UserSkillResponse(
                skill.getId(),
                skill.getName(),
                !Boolean.TRUE.equals(skill.getIsGlobal()),
                skillUser.getLevel());
    }
}
