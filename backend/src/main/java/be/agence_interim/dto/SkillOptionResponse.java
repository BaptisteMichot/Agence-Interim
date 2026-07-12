package be.agence_interim.dto;

import be.agence_interim.model.Skill;

/** Compétence proposée dans le référentiel (globale ou perso). */
public record SkillOptionResponse(int id, String name, boolean custom) {

    public static SkillOptionResponse fromEntity(Skill skill) {
        return new SkillOptionResponse(skill.getId(), skill.getName(), !Boolean.TRUE.equals(skill.getIsGlobal()));
    }
}
