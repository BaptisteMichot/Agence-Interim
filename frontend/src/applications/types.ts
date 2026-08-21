import type { JobOfferSummary } from '../offers/types';
import type {
  ExperienceItem,
  FormationItem,
  UserDegree,
  UserLanguage,
  UserSkill,
} from '../profile/types';

export type ApplicationStatus = 'PENDING' | 'CANCELED';

/** Candidature de l'intérimaire courant, avec l'offre concernée. */
export interface MyApplication {
  id: number;
  applicationTime: string; // ISO
  status: ApplicationStatus;
  offer: JobOfferSummary;
}

/** Tris proposés à l'employeur sur les candidatures reçues (appliqués en base). */
export type ApplicationSort = 'date-desc' | 'date-asc' | 'rating-desc';

/** Candidature reçue sur une offre, vue par l'employeur. */
export interface OfferApplication {
  id: number;
  applicationTime: string;
  rating: number | null;
  candidateId: number;
  firstName: string;
  lastName: string;
  email: string;
}

/** Profil complet d'un candidat, consulté par l'employeur. */
export interface CandidateProfile {
  userId: number;
  lastName: string;
  firstName: string;
  email: string;
  birthdate: string | null;
  hasVehicle: boolean | null;
  hasCv: boolean;
  offerId: number;
  offerTitle: string;
  skills: UserSkill[];
  degrees: UserDegree[];
  languages: UserLanguage[];
  experiences: ExperienceItem[];
  formations: FormationItem[];
}
