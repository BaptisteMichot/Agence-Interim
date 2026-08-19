import type { DegreeType, FormationStatus, LanguageLevel, SkillLevel } from './types';

/** Convertit une date ISO (yyyy-MM-dd) en format lisible jj/mm/aaaa. */
export function formatDate(iso: string | null): string {
  if (!iso) {
    return '';
  }
  const [year, month, day] = iso.split('-');
  return `${day}/${month}/${year}`;
}

/** Convertit un horodatage ISO en « jj/mm/aaaa hh:mm ». */
export function formatDateTime(iso: string): string {
  const [date, time] = iso.split('T');
  return `${formatDate(date)} ${time.slice(0, 5)}`;
}

/** Date seule (jj/mm/aaaa) d'un horodatage ISO. */
export function formatTimestampDate(iso: string): string {
  return formatDate(iso.slice(0, 10));
}

/** Libellé d'affichage d'un statut de formation. */
export function formationStatusLabel(status: FormationStatus): string {
  return status === 'EN_COURS' ? 'En cours' : 'Terminé';
}

/** Cohérence de la période saisie (expérience, formation) ; message d'erreur, ou null. */
export function dateRangeError(startDate: string, endDate: string, ongoing: boolean): string | null {
  if (!ongoing && !endDate) {
    return 'La date de fin est obligatoire, ou cochez « En cours ».';
  }
  if (!ongoing && endDate < startDate) {
    return 'La date de fin ne peut pas être antérieure à la date de début.';
  }
  return null;
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
