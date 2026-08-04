import { apiDelete, apiGet, apiPost } from './http';
import type { ScheduleEntry, Unavailability, UnavailabilityPayload } from '../missions/types';

/** Journées de mission confirmées sur une période (planning de l'intérimaire). */
export function getSchedule(from: string, to: string): Promise<ScheduleEntry[]> {
  return apiGet<ScheduleEntry[]>(`/missions/schedule?from=${from}&to=${to}`);
}

export function getUnavailabilities(from: string, to: string): Promise<Unavailability[]> {
  return apiGet<Unavailability[]>(`/profile/availability?from=${from}&to=${to}`);
}

/** Première date modifiable (aujourd'hui + 8 jours), calculée par le backend. */
export function getEditableFrom(): Promise<{ editableFrom: string }> {
  return apiGet<{ editableFrom: string }>('/profile/availability/editable-from');
}

export function addUnavailability(payload: UnavailabilityPayload): Promise<Unavailability> {
  return apiPost<Unavailability>('/profile/availability', payload);
}

export function removeUnavailability(id: number): Promise<void> {
  return apiDelete(`/profile/availability/${id}`);
}
