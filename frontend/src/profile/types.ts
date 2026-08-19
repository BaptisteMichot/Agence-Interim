import type { Role } from '../auth/types';

export type FormationStatus = 'EN_COURS' | 'TERMINE';

/** État du formulaire d'ajout/édition d'une section du profil. */
export type FormMode<T> = { type: 'closed' } | { type: 'new' } | { type: 'edit'; item: T };

export interface ExperienceItem extends ExperiencePayload {
  id: number;
}

export interface FormationItem extends FormationPayload {
  id: number;
  status: FormationStatus; // déduit côté backend
}

export interface Profile {
  userId: number;
  lastName: string;
  firstName: string;
  email: string;
  role: Role;
  birthdate: string | null;
  hasVehicle: boolean | null;
  cvFilePath: string | null;
  /** Mentions légales reprises sur le contrat de mission. */
  address: string | null;
  nationalNumber: string | null;
  /** IBAN sur lequel le salaire des missions est versé. */
  iban: string | null;
  experiences: ExperienceItem[];
  formations: FormationItem[];
}

/** Corps envoyé à PUT /api/profile. */
export interface ProfileBasePayload {
  lastName: string;
  firstName: string;
  birthdate: string | null;
  hasVehicle: boolean | null;
  address: string | null;
  nationalNumber: string | null;
  iban: string | null;
}

/** Corps envoyé aux endpoints expériences. */
export interface ExperiencePayload {
  companyName: string;
  position: string;
  startDate: string; // ISO yyyy-MM-dd
  endDate: string | null; // null = en cours
}

/** Corps envoyé aux endpoints formations (le statut est déduit du endDate). */
export interface FormationPayload {
  title: string;
  institution: string;
  startDate: string;
  endDate: string | null; // null = en cours
}

// --- Compétences / diplômes / langues (incrément 3b) ---

export type SkillLevel = 'DEBUTANT' | 'INTERMEDIAIRE' | 'AVANCE' | 'EXPERT';
export type LanguageLevel = 'A1' | 'A2' | 'B1' | 'B2' | 'C1' | 'C2';
export type DegreeType = 'BACHELIER' | 'MASTER';

export interface SkillOption {
  id: number;
  name: string;
  custom: boolean;
}

export interface DegreeOption {
  id: number;
  type: DegreeType;
  section: string;
  custom: boolean;
}

export interface LanguageOption {
  id: number;
  name: string;
}

export interface UserSkill {
  skillId: number;
  name: string;
  custom: boolean;
  level: SkillLevel;
}

export interface UserDegree {
  degreeId: number;
  type: DegreeType;
  section: string;
  custom: boolean;
  institution: string | null;
  graduationYear: number | null;
}

export interface UserLanguage {
  languageId: number;
  name: string;
  level: LanguageLevel;
}

/** Ajout d'une compétence : par nom (le backend réutilise ou crée une perso) + niveau. */
export interface SkillPayload {
  name: string;
  level: SkillLevel;
}

/** Ajout d'un diplôme : type + section (réutilisé ou créé) + établissement/année. */
export interface DegreePayload {
  type: DegreeType;
  section: string;
  institution: string | null;
  graduationYear: number | null;
}

/** Mise à jour des infos perso d'un diplôme. */
export interface DegreeUpdatePayload {
  institution: string | null;
  graduationYear: number | null;
}

/** Ajout d'une langue (choisie dans la liste fixe) + niveau. */
export interface LanguagePayload {
  languageId: number;
  level: LanguageLevel;
}
