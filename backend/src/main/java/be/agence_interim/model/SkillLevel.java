package be.agence_interim.model;

/**
 * Niveau de maîtrise d'une compétence. L'ordre des valeurs est significatif
 * (du plus faible au plus fort) car il est stocké en base sous forme ordinale.
 */
public enum SkillLevel {
    DEBUTANT,
    INTERMEDIAIRE,
    AVANCE,
    EXPERT
}
