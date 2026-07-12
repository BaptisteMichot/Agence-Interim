import { apiDelete, apiGet, apiPost, apiPut } from './http';
import type {
  ExperienceItem,
  ExperiencePayload,
  FormationItem,
  FormationPayload,
  Profile,
  ProfileBasePayload,
} from '../profile/types';

export function getProfile(): Promise<Profile> {
  return apiGet<Profile>('/profile');
}

export function updateProfile(payload: ProfileBasePayload): Promise<Profile> {
  return apiPut<Profile>('/profile', payload);
}

export function addExperience(payload: ExperiencePayload): Promise<ExperienceItem> {
  return apiPost<ExperienceItem>('/profile/experiences', payload);
}

export function updateExperience(id: number, payload: ExperiencePayload): Promise<ExperienceItem> {
  return apiPut<ExperienceItem>(`/profile/experiences/${id}`, payload);
}

export function deleteExperience(id: number): Promise<void> {
  return apiDelete(`/profile/experiences/${id}`);
}

export function addFormation(payload: FormationPayload): Promise<FormationItem> {
  return apiPost<FormationItem>('/profile/formations', payload);
}

export function updateFormation(id: number, payload: FormationPayload): Promise<FormationItem> {
  return apiPut<FormationItem>(`/profile/formations/${id}`, payload);
}

export function deleteFormation(id: number): Promise<void> {
  return apiDelete(`/profile/formations/${id}`);
}
