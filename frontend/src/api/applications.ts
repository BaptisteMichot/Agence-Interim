import { apiDownload, apiGet, apiPost, apiPut } from './http';
import type { CandidateProfile, MyApplication, OfferApplication } from '../applications/types';

// --- Candidatures de l'intérimaire ---

export function applyToOffer(offerId: number): Promise<MyApplication> {
  return apiPost<MyApplication>(`/offers/${offerId}/apply`, {});
}

export function getMyApplications(): Promise<MyApplication[]> {
  return apiGet<MyApplication[]>('/applications');
}

/** Identifiants des offres avec candidature en cours (pour marquer « déjà postulé »). */
export function getAppliedOfferIds(): Promise<number[]> {
  return apiGet<number[]>('/applications/offer-ids');
}

export function cancelApplication(id: number): Promise<MyApplication> {
  return apiPost<MyApplication>(`/applications/${id}/cancel`, {});
}

// --- Candidatures reçues par l'employeur ---

export function getOfferApplications(offerId: number): Promise<OfferApplication[]> {
  return apiGet<OfferApplication[]>(`/employer/offers/${offerId}/applications`);
}

/** Nombre de candidatures en cours par offre (clé = id de l'offre). */
export function getApplicationCounts(): Promise<Record<string, number>> {
  return apiGet<Record<string, number>>('/employer/offers/application-counts');
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
