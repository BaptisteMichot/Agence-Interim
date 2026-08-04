/**
 * PENDING  : mission provisoire, en attente de validation de l'agence
 * REFUSED  : refusée par l'agence (motif transmis à l'employeur)
 * APPROVED : validée par l'agence, en attente de la réponse de l'intérimaire
 * RENEWAL  : demande de renouvellement en attente de la réponse de l'intérimaire
 * ACTIVE   : acceptée — contrat généré et mission au planning
 * DECLINED : refusée par l'intérimaire
 */
export type MissionStatus = 'PENDING' | 'REFUSED' | 'APPROVED' | 'RENEWAL' | 'ACTIVE' | 'DECLINED';

export type WorkReason = 'REPLACEMENT' | 'OVERLOAD' | 'EXCEPTION';

export type SignatureStatus = 'PENDING' | 'SIGNED';

/** Journée travaillée d'une mission. */
export interface DailySlot {
  id: number;
  date: string; // yyyy-MM-dd
  startTime: string; // HH:mm[:ss]
  endTime: string;
}

/** Journée envoyée au backend (sans identifiant). */
export interface DailySlotPayload {
  date: string;
  startTime: string;
  endTime: string;
}

export interface Contract {
  id: number;
  missionId: number;
  generationTime: string; // ISO
  statusEmployer: SignatureStatus;
  statusWorker: SignatureStatus;
  fileName: string;
}

/** Corps envoyé à la création / correction d'une mission. */
export interface MissionPayload {
  startDate: string;
  endDate: string;
  position: string;
  workplace: string;
  hourlyWage: number;
  workReason: WorkReason;
  notes: string | null;
  slots: DailySlotPayload[];
}

/** Mission complète, servie aux trois portails. */
export interface Mission {
  id: number;
  status: MissionStatus;
  startDate: string;
  endDate: string;
  position: string;
  workplace: string;
  hourlyWage: number;
  workReason: WorkReason;
  notes: string | null;
  refusalReason: string | null;
  renewal: boolean;
  previousStartDate: string | null;
  previousEndDate: string | null;
  applicationId: number;
  offerId: number;
  offerTitle: string;
  candidateId: number;
  candidateFirstName: string;
  candidateLastName: string;
  candidateEmail: string;
  employerId: number;
  employerCompanyName: string | null;
  employerFirstName: string;
  employerLastName: string;
  employerEmail: string;
  slots: DailySlot[];
  contract: Contract | null;
}
