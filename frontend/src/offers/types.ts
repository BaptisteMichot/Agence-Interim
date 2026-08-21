import type { DegreeType, LanguageLevel, SkillLevel } from '../profile/types';

export type JobOfferStatus = 'OPEN' | 'CLOSED';

export interface OfferSkillRequirement {
  name: string;
  isMandatory: boolean;
  requiredLevel: SkillLevel;
}

export interface OfferDegreeRequirement {
  type: DegreeType;
  section: string;
  isMandatory: boolean;
}

export interface OfferLanguageRequirement {
  languageId: number;
  isMandatory: boolean;
  requiredLevel: LanguageLevel;
}

/** Corps envoyé à POST/PUT /api/employer/offers. */
export interface JobOfferPayload {
  title: string;
  sector: string;
  city: string;
  description: string;
  salaryMin: number | null;
  salaryMax: number | null;
  experienceTime: string | null;
  vehicleMandatory: boolean;
  skills: OfferSkillRequirement[];
  degrees: OfferDegreeRequirement[];
  languages: OfferLanguageRequirement[];
}

/** Vue résumée d'une offre (listes). */
export interface JobOfferSummary {
  id: number;
  title: string;
  sector: string;
  city: string;
  publishedAt: string | null;
  salaryMin: number | null;
  salaryMax: number | null;
  status: JobOfferStatus;
  companyName: string;
  /** Faux dès que l'offre est clôturée ou qu'elle a reçu une candidature. */
  editable: boolean;
  /** Vue intérimaire : l'offre est dans ses favoris. */
  favorite: boolean;
  /** Vue employeur : nombre de candidatures en cours reçues sur l'offre. */
  applicationCount: number;
}

/** Offre correspondant au profil, avec son score de correspondance (0-100). */
export interface MatchingOffer {
  offer: JobOfferSummary;
  score: number;
}

/** Offre complète avec exigences. */
export interface JobOfferDetail extends JobOfferSummary {
  /** Vue intérimaire : il a déjà une candidature en cours sur cette offre. */
  applied: boolean;
  description: string;
  experienceTime: string | null;
  vehicleMandatory: boolean | null;
  skills: (OfferSkillRequirement & { skillId: number })[];
  degrees: (OfferDegreeRequirement & { degreeId: number })[];
  languages: (OfferLanguageRequirement & { name: string })[];
}
