import { apiGet, apiPost, apiPut } from './http';
import type { JobOfferDetail, JobOfferPayload, JobOfferSummary } from '../offers/types';

// --- Offres de l'employeur courant ---

export function getMyOffers(): Promise<JobOfferSummary[]> {
  return apiGet<JobOfferSummary[]>('/employer/offers');
}

export function getMyOffer(id: number): Promise<JobOfferDetail> {
  return apiGet<JobOfferDetail>(`/employer/offers/${id}`);
}

export function createOffer(payload: JobOfferPayload): Promise<JobOfferDetail> {
  return apiPost<JobOfferDetail>('/employer/offers', payload);
}

export function updateOffer(id: number, payload: JobOfferPayload): Promise<JobOfferDetail> {
  return apiPut<JobOfferDetail>(`/employer/offers/${id}`, payload);
}

export function closeOffer(id: number): Promise<JobOfferDetail> {
  return apiPost<JobOfferDetail>(`/employer/offers/${id}/close`, {});
}
