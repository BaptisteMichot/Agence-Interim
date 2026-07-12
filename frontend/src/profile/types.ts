import type { Role } from '../auth/types';

export type FormationStatus = 'EN_COURS' | 'TERMINE';

export interface ExperienceItem {
  id: number;
  companyName: string;
  position: string;
  startDate: string; // ISO yyyy-MM-dd
  endDate: string | null; // null = en cours
}

export interface FormationItem {
  id: number;
  title: string;
  institution: string;
  startDate: string;
  endDate: string | null; // null = en cours
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
  experiences: ExperienceItem[];
  formations: FormationItem[];
}

/** Corps envoyé à PUT /api/profile. */
export interface ProfileBasePayload {
  lastName: string;
  firstName: string;
  birthdate: string | null;
  hasVehicle: boolean | null;
}

/** Corps envoyé aux endpoints expériences. */
export interface ExperiencePayload {
  companyName: string;
  position: string;
  startDate: string;
  endDate: string | null;
}

/** Corps envoyé aux endpoints formations (le statut est déduit du endDate). */
export interface FormationPayload {
  title: string;
  institution: string;
  startDate: string;
  endDate: string | null;
}
