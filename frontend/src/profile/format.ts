import type { DegreeType, FormationStatus, LanguageLevel, SkillLevel } from './types';

/** Convertit une date ISO (yyyy-MM-dd) en format lisible jj/mm/aaaa. */
export function formatDate(iso: string | null): string {
  if (!iso) {
    return '';
  }
  const [year, month, day] = iso.split('-');
  return `${day}/${month}/${year}`;
}

/** Libellé d'affichage d'un statut de formation. */
export function formationStatusLabel(status: FormationStatus): string {
  return status === 'EN_COURS' ? 'En cours' : 'Terminé';
}

/** Niveaux de compétence (valeur backend → libellé), dans l'ordre. */
export const SKILL_LEVELS: { value: SkillLevel; label: string }[] = [
  { value: 'DEBUTANT', label: 'Débutant' },
  { value: 'INTERMEDIAIRE', label: 'Intermédiaire' },
  { value: 'AVANCE', label: 'Avancé' },
  { value: 'EXPERT', label: 'Expert' },
];

/** Niveaux de langue (CECR). */
export const LANGUAGE_LEVELS: LanguageLevel[] = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2'];

/** Types de diplôme (valeur backend → libellé). */
export const DEGREE_TYPES: { value: DegreeType; label: string }[] = [
  { value: 'BACHELIER', label: 'Bachelier' },
  { value: 'MASTER', label: 'Master' },
];

export function skillLevelLabel(level: SkillLevel): string {
  return SKILL_LEVELS.find((l) => l.value === level)?.label ?? level;
}

export function degreeTypeLabel(type: DegreeType): string {
  return DEGREE_TYPES.find((t) => t.value === type)?.label ?? type;
}
