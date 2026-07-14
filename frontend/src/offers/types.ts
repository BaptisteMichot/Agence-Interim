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
}

/** Offre complète avec exigences. */
export interface JobOfferDetail extends JobOfferSummary {
  description: string;
  experienceTime: string | null;
  vehicleMandatory: boolean | null;
  skills: { skillId: number; name: string; isMandatory: boolean; requiredLevel: SkillLevel }[];
  degrees: { degreeId: number; type: DegreeType; section: string; isMandatory: boolean }[];
  languages: { languageId: number; name: string; isMandatory: boolean; requiredLevel: LanguageLevel }[];
}
