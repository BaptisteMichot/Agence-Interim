import { apiDelete, apiGet, apiPost, apiPut } from './http';
import { apiGetCount, apiGetPage, type Page } from './page';
import type {
  JobOfferDetail,
  JobOfferPayload,
  JobOfferSummary,
  MatchingOffer,
  OfferFilters,
} from '../offers/types';

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

/**
 * Critères sous la forme attendue par la requête. Un critère vide n'est pas envoyé :
 * c'est l'absence du paramètre qui dit au serveur de ne pas filtrer là-dessus.
 */
function filterParams(filters: OfferFilters): Record<string, string | undefined> {
  return {
    keyword: filters.keyword.trim() || undefined,
    sector: filters.sector || undefined,
    province: filters.province || undefined,
    minHourlyWage: filters.minHourlyWage || undefined,
    maxExperienceYears: filters.maxExperienceYears || undefined,
    noVehicleRequired: filters.noVehicleRequired ? 'true' : undefined,
  };
}

export function browseOffers(page: number, filters: OfferFilters): Promise<Page<JobOfferSummary>> {
  return apiGetPage<JobOfferSummary>('/offers', page, filterParams(filters));
}

/** Offres correspondant au profil (triées par score décroissant), mêmes critères. */
export function getMatchingOffers(page: number, filters: OfferFilters): Promise<Page<MatchingOffer>> {
  return apiGetPage<MatchingOffer>('/offers/matching', page, filterParams(filters));
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
