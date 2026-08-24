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

/**
 * Sections des listes de missions. Chacune est une requête paginée à part : le
 * découpage est fait en base, sinon une page ne montrerait qu'une partie de chaque
 * section.
 */
export type JobSeekerMissionGroup = 'to-confirm' | 'waiting' | 'confirmed' | 'history';
export type EmployerMissionGroup = 'current' | 'past' | 'awaiting' | 'rejected';
export type AdminMissionGroup = 'pending' | 'history';

export type SignatureStatus = 'PENDING' | 'SIGNED';

/** Journée envoyée au backend (sans identifiant). */
export interface DailySlotPayload {
  date: string; // yyyy-MM-dd
  startTime: string; // HH:mm[:ss]
  endTime: string;
  breakStart: string | null;
  breakEnd: string | null;
}

/** Journée travaillée d'une mission ; la pause, fixée par l'employeur, n'est pas payée. */
export interface DailySlot extends DailySlotPayload {
  id: number;
}

export interface Contract {
  id: number;
  missionId: number;
  generationTime: string; // ISO
  statusEmployer: SignatureStatus;
  statusWorker: SignatureStatus;
  fileName: string;
  employerSignedAt: string | null;
  workerSignedAt: string | null;
}

/**
 * Contrat tel qu'il apparaît dans « Mes documents » : la mission qu'il couvre,
 * l'état des deux signatures et la date de chacune.
 */
export interface ContractSummary {
  id: number;
  missionId: number;
  position: string;
  companyName: string;
  workerName: string;
  startDate: string;
  endDate: string;
  generationTime: string;
  statusEmployer: SignatureStatus;
  employerSignedAt: string | null;
  statusWorker: SignatureStatus;
  workerSignedAt: string | null;
  /** C'est au lecteur de signer : le backend l'a déduit de son rôle. */
  awaitingMySignature: boolean;
}

/** Journée de mission affichée dans le planning. */
export interface ScheduleEntry {
  missionId: number;
  position: string;
  company: string;
  workplace: string;
  date: string;
  startTime: string;
  endTime: string;
  breakStart: string | null;
  breakEnd: string | null;
}

export interface UnavailabilityPayload {
  date: string;
  startTime: string;
  endTime: string;
  reason: string | null;
}

/** Indisponibilité déclarée par l'intérimaire (editable = modifiable, cf. délai J+8). */
export interface Unavailability extends UnavailabilityPayload {
  id: number;
  editable: boolean;
}

/** Corps envoyé à la création / correction d'une mission. */
export interface MissionPayload {
  startDate: string;
  endDate: string;
  position: string;
  workplace: string;
  description: string;
  jointCommittee: string;
  hourlyWage: number;
  workReason: WorkReason;
  /** Obligatoire lorsque le motif de recours est un remplacement. */
  replacedWorker: string | null;
  notes: string | null;
  slots: DailySlotPayload[];
}

/** Mission complète, servie aux trois portails. */
export interface Mission extends Omit<MissionPayload, 'slots'> {
  id: number;
  status: MissionStatus;
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
