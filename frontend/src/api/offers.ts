import { apiDelete, apiGet, apiPost, apiPut } from './http';
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

// --- Consultation par l'intérimaire + favoris ---

export function browseOffers(): Promise<JobOfferSummary[]> {
  return apiGet<JobOfferSummary[]>('/offers');
}

export function getOfferDetail(id: number): Promise<JobOfferDetail> {
  return apiGet<JobOfferDetail>(`/offers/${id}`);
}

export function getFavoriteOffers(): Promise<JobOfferSummary[]> {
  return apiGet<JobOfferSummary[]>('/offers/favorites');
}

export function getFavoriteOfferIds(): Promise<number[]> {
  return apiGet<number[]>('/offers/favorites/ids');
}

export function addFavoriteOffer(id: number): Promise<void> {
  return apiPost<void>(`/offers/${id}/favorite`, {});
}

export function removeFavoriteOffer(id: number): Promise<void> {
  return apiDelete(`/offers/${id}/favorite`);
}
