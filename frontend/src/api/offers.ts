import { apiDelete, apiGet, apiPost, apiPut } from './http';
import { apiGetCount, apiGetPage, type Page } from './page';
import type { JobOfferDetail, JobOfferPayload, JobOfferSummary, MatchingOffer } from '../offers/types';

// --- Offres de l'employeur courant ---

export function getMyOffers(page: number): Promise<Page<JobOfferSummary>> {
  return apiGetPage<JobOfferSummary>('/employer/offers', page);
}

/** Nombre d'offres encore ouvertes (chiffre du tableau de bord). */
export function getOpenOfferCount(): Promise<number> {
  return apiGetCount('/employer/offers/open-count');
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

export function browseOffers(page: number): Promise<Page<JobOfferSummary>> {
  return apiGetPage<JobOfferSummary>('/offers', page);
}

/** Offres correspondant au profil (triées par score décroissant). */
export function getMatchingOffers(page: number): Promise<Page<MatchingOffer>> {
  return apiGetPage<MatchingOffer>('/offers/matching', page);
}

export function getOfferDetail(id: number): Promise<JobOfferDetail> {
  return apiGet<JobOfferDetail>(`/offers/${id}`);
}

export function getFavoriteOffers(page: number): Promise<Page<JobOfferSummary>> {
  return apiGetPage<JobOfferSummary>('/offers/favorites', page);
}

export function addFavoriteOffer(id: number): Promise<void> {
  return apiPost<void>(`/offers/${id}/favorite`, {});
}

export function removeFavoriteOffer(id: number): Promise<void> {
  return apiDelete(`/offers/${id}/favorite`);
}
