import { apiDownload, apiGet, apiPost, apiPut } from './http';
import type { Contract, Mission, MissionPayload } from '../missions/types';

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

export function getEmployerMissions(): Promise<Mission[]> {
  return apiGet<Mission[]>('/employer/missions');
}

export function getEmployerMission(missionId: number): Promise<Mission> {
  return apiGet<Mission>(`/employer/missions/${missionId}`);
}

// --- Agence (admin) ---

export function getAdminMissions(): Promise<Mission[]> {
  return apiGet<Mission[]>('/admin/missions');
}

export function validateMission(missionId: number): Promise<Mission> {
  return apiPost<Mission>(`/admin/missions/${missionId}/validate`, {});
}

export function refuseMission(missionId: number, reason: string): Promise<Mission> {
  return apiPost<Mission>(`/admin/missions/${missionId}/refuse`, { reason });
}

// --- Intérimaire ---

export function getMyMissions(): Promise<Mission[]> {
  return apiGet<Mission[]>('/missions');
}

export function getMyMission(missionId: number): Promise<Mission> {
  return apiGet<Mission>(`/missions/${missionId}`);
}

/** Nombre de missions attendant une réponse de l'intérimaire (badge du portail). */
export function getMissionDecisionCount(): Promise<{ count: number }> {
  return apiGet<{ count: number }>('/missions/decision-count');
}

export function acceptMission(missionId: number): Promise<Mission> {
  return apiPost<Mission>(`/missions/${missionId}/accept`, {});
}

export function declineMission(missionId: number): Promise<Mission> {
  return apiPost<Mission>(`/missions/${missionId}/decline`, {});
}

// --- Contrat (les deux parties) ---

export function signContract(missionId: number): Promise<Contract> {
  return apiPost<Contract>(`/contracts/${missionId}/sign`, {});
}

export function downloadContract(missionId: number): Promise<Blob> {
  return apiDownload(`/contracts/${missionId}/file`);
}
