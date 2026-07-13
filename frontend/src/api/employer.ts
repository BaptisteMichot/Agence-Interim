import type { EmployerAccessStatus } from '../auth/types';
import { apiDelete, apiGet, apiPost } from './http';

export interface AdminEmployerRequest {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  companyName: string;
  requestDate: string;
  status: EmployerAccessStatus;
  message: string | null;
  resubmission: boolean;
}

/** Statut de la demande d'accès employeur de l'utilisateur courant. */
export function getMyEmployerRequest(): Promise<{ status: EmployerAccessStatus | null }> {
  return apiGet<{ status: EmployerAccessStatus | null }>('/employer-requests/me');
}

/** Nouvelle demande après un refus (message facultatif, ≤ 150 caractères). */
export function reapplyEmployer(message: string): Promise<{ message: string }> {
  return apiPost<{ message: string }>('/employer-requests', { message });
}

/** Suppression du compte de l'utilisateur courant. */
export function deleteAccount(): Promise<void> {
  return apiDelete('/account');
}

// --- Administration ---

/** Toutes les demandes (en attente + historique). */
export function getEmployerRequests(): Promise<AdminEmployerRequest[]> {
  return apiGet<AdminEmployerRequest[]>('/admin/employer-requests');
}

export function acceptEmployerRequest(id: number): Promise<void> {
  return apiPost<void>(`/admin/employer-requests/${id}/accept`, {});
}

export function refuseEmployerRequest(id: number): Promise<void> {
  return apiPost<void>(`/admin/employer-requests/${id}/refuse`, {});
}
