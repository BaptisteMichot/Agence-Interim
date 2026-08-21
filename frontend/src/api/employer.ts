import type { EmployerAccessStatus } from '../auth/types';
import { apiDelete, apiGet, apiPost, apiPut } from './http';
import { apiGetPage, type Page } from './page';

/** Mentions légales de l'entreprise utilisatrice, reprises sur les contrats. */
export interface EmployerCompany {
  companyName: string;
  address: string | null;
  companyNumber: string | null;
  jointCommittee: string | null;
  /** Vrai tant qu'une mention manque : aucune mission ne peut alors être proposée. */
  incomplete: boolean;
}

export type EmployerCompanyPayload = Omit<EmployerCompany, 'incomplete'>;

/** Statut de la demande courante ; `reapplyBlocked` = refus définitif, plus de nouvelle demande. */
export interface MyEmployerRequest {
  status: EmployerAccessStatus | null;
  reapplyBlocked: boolean;
}

export function getMyCompany(): Promise<EmployerCompany> {
  return apiGet<EmployerCompany>('/employer/company');
}

export function updateMyCompany(payload: EmployerCompanyPayload): Promise<EmployerCompany> {
  return apiPut<EmployerCompany>('/employer/company', payload);
}

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
export function getMyEmployerRequest(): Promise<MyEmployerRequest> {
  return apiGet<MyEmployerRequest>('/employer-requests/me');
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

/** Sections de la liste des demandes d'accès employeur. */
export type EmployerRequestGroup = 'pending' | 'history';

/** Une section des demandes d'accès employeur (filtrée en base). */
export function getEmployerRequests(
  group: EmployerRequestGroup,
  page: number,
): Promise<Page<AdminEmployerRequest>> {
  return apiGetPage<AdminEmployerRequest>('/admin/employer-requests', page, { group });
}

export function acceptEmployerRequest(id: number): Promise<void> {
  return apiPost<void>(`/admin/employer-requests/${id}/accept`, {});
}

/** `block` interdit définitivement une nouvelle demande de cet utilisateur. */
export function refuseEmployerRequest(id: number, block: boolean): Promise<void> {
  return apiPost<void>(`/admin/employer-requests/${id}/refuse?block=${block}`, {});
}
