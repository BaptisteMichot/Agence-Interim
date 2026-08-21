import { apiDownload, apiGet, apiPost, apiPut } from './http';
import { apiGetCount, apiGetPage, type Page } from './page';
import type {
  AdminMissionGroup,
  Contract,
  EmployerMissionGroup,
  JobSeekerMissionGroup,
  Mission,
  MissionPayload,
} from '../missions/types';

// --- Employeur ---

/** Sélectionne le candidat d'une candidature et crée la mission provisoire. */
export function createMission(applicationId: number, payload: MissionPayload): Promise<Mission> {
  return apiPost<Mission>(`/employer/applications/${applicationId}/mission`, payload);
}

/** Corrige une mission refusée par l'agence et la soumet à nouveau. */
export function updateMission(missionId: number, payload: MissionPayload): Promise<Mission> {
  return apiPut<Mission>(`/employer/missions/${missionId}`, payload);
}

/** Demande le renouvellement d'une mission confirmée (nouvelle mission proposée au candidat). */
export function renewMission(missionId: number, payload: MissionPayload): Promise<Mission> {
  return apiPost<Mission>(`/employer/missions/${missionId}/renew`, payload);
}

/** Une section des missions de l'employeur (filtrée en base). */
export function getEmployerMissions(
  group: EmployerMissionGroup,
  page: number,
): Promise<Page<Mission>> {
  return apiGetPage<Mission>('/employer/missions', page, { group });
}

/** Nombre de missions encore en décision (chiffre du tableau de bord). */
export function getEmployerAwaitingMissionCount(): Promise<number> {
  return apiGetCount('/employer/missions/awaiting-count');
}

export function getEmployerMission(missionId: number): Promise<Mission> {
  return apiGet<Mission>(`/employer/missions/${missionId}`);
}

// --- Agence (admin) ---

/** Une section des missions vues par l'agence (filtrée en base). */
export function getAdminMissions(group: AdminMissionGroup, page: number): Promise<Page<Mission>> {
  return apiGetPage<Mission>('/admin/missions', page, { group });
}

/** Nombre de missions en attente de validation (badge de la barre latérale). */
export function getAdminPendingMissionCount(): Promise<number> {
  return apiGetCount('/admin/missions/pending-count');
}

export function validateMission(missionId: number): Promise<Mission> {
  return apiPost<Mission>(`/admin/missions/${missionId}/validate`, {});
}

export function refuseMission(missionId: number, reason: string): Promise<Mission> {
  return apiPost<Mission>(`/admin/missions/${missionId}/refuse`, { reason });
}

// --- Intérimaire ---

/** Une section des missions de l'intérimaire (filtrée en base). */
export function getMyMissions(group: JobSeekerMissionGroup, page: number): Promise<Page<Mission>> {
  return apiGetPage<Mission>('/missions', page, { group });
}

export function getMyMission(missionId: number): Promise<Mission> {
  return apiGet<Mission>(`/missions/${missionId}`);
}

/** Nombre de missions attendant une réponse de l'intérimaire (badge du portail). */
export function getMissionDecisionCount(): Promise<{ count: number }> {
  return apiGet<{ count: number }>('/missions/decision-count');
}

/** Nombre de missions menées à leur terme (chiffre du tableau de bord). */
export function getCompletedMissionCount(): Promise<number> {
  return apiGetCount('/missions/completed-count');
}

export function acceptMission(missionId: number): Promise<Mission> {
  return apiPost<Mission>(`/missions/${missionId}/accept`, {});
}

export function declineMission(missionId: number): Promise<Mission> {
  return apiPost<Mission>(`/missions/${missionId}/decline`, {});
}

// --- Contrat (les deux parties) ---

/** Envoie par email le code à usage unique confirmant la signature. */
export function requestSigningCode(missionId: number): Promise<void> {
  return apiPost<void>(`/contracts/${missionId}/signing-code`, {});
}

export function signContract(missionId: number, code: string): Promise<Contract> {
  return apiPost<Contract>(`/contracts/${missionId}/sign`, { code });
}

export function downloadContract(missionId: number): Promise<Blob> {
  return apiDownload(`/contracts/${missionId}/file`);
}
