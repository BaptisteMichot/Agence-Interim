import { apiDownload, apiGet, apiPost, apiPut } from './http';
import { apiGetCount, apiGetPage, type Page } from './page';
import type {
  ApplicationSort,
  CandidateProfile,
  MyApplication,
  OfferApplication,
} from '../applications/types';

// --- Candidatures de l'intérimaire ---

export function applyToOffer(offerId: number): Promise<MyApplication> {
  return apiPost<MyApplication>(`/offers/${offerId}/apply`, {});
}

export function getMyApplications(page: number): Promise<Page<MyApplication>> {
  return apiGetPage<MyApplication>('/applications', page);
}

/** Nombre de candidatures en cours (chiffre du tableau de bord). */
export function getPendingApplicationCount(): Promise<number> {
  return apiGetCount('/applications/pending-count');
}

export function cancelApplication(id: number): Promise<MyApplication> {
  return apiPost<MyApplication>(`/applications/${id}/cancel`, {});
}

// --- Candidatures reçues par l'employeur ---

/** Une page des candidatures reçues sur une offre. Le tri est appliqué en base. */
export function getOfferApplications(
  offerId: number,
  page: number,
  sort: ApplicationSort,
): Promise<Page<OfferApplication>> {
  return apiGetPage<OfferApplication>(`/employer/offers/${offerId}/applications`, page, { sort });
}

/** Nombre total de candidatures en cours reçues (chiffre du tableau de bord). */
export function getReceivedApplicationCount(): Promise<number> {
  return apiGetCount('/employer/applications/pending-count');
}

export function rateApplication(id: number, rating: number): Promise<OfferApplication> {
  return apiPut<OfferApplication>(`/employer/applications/${id}/rating`, { rating });
}

export function getCandidateProfile(applicationId: number): Promise<CandidateProfile> {
  return apiGet<CandidateProfile>(`/employer/applications/${applicationId}/profile`);
}

export function downloadCandidateCv(applicationId: number): Promise<Blob> {
  return apiDownload(`/employer/applications/${applicationId}/cv`);
}
