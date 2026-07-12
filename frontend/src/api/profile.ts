import { apiDelete, apiDownload, apiGet, apiPost, apiPut, apiUpload } from './http';
import type {
  DegreeOption,
  DegreePayload,
  DegreeUpdatePayload,
  ExperienceItem,
  ExperiencePayload,
  FormationItem,
  FormationPayload,
  LanguageLevel,
  LanguageOption,
  LanguagePayload,
  Profile,
  ProfileBasePayload,
  SkillLevel,
  SkillOption,
  SkillPayload,
  UserDegree,
  UserLanguage,
  UserSkill,
} from '../profile/types';

// --- Profil de base + expériences + formations ---

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

// --- Référentiels (listes pour les sélecteurs) ---

export function getSkillOptions(): Promise<SkillOption[]> {
  return apiGet<SkillOption[]>('/skills');
}

export function getDegreeOptions(): Promise<DegreeOption[]> {
  return apiGet<DegreeOption[]>('/degrees');
}

export function getLanguageOptions(): Promise<LanguageOption[]> {
  return apiGet<LanguageOption[]>('/languages');
}

// --- Compétences du profil ---

export function getUserSkills(): Promise<UserSkill[]> {
  return apiGet<UserSkill[]>('/profile/skills');
}

export function addSkill(payload: SkillPayload): Promise<UserSkill> {
  return apiPost<UserSkill>('/profile/skills', payload);
}

export function updateSkillLevel(skillId: number, level: SkillLevel): Promise<UserSkill> {
  return apiPut<UserSkill>(`/profile/skills/${skillId}`, { level });
}

export function deleteSkill(skillId: number): Promise<void> {
  return apiDelete(`/profile/skills/${skillId}`);
}

// --- Diplômes du profil ---

export function getUserDegrees(): Promise<UserDegree[]> {
  return apiGet<UserDegree[]>('/profile/degrees');
}

export function addDegree(payload: DegreePayload): Promise<UserDegree> {
  return apiPost<UserDegree>('/profile/degrees', payload);
}

export function updateDegree(degreeId: number, payload: DegreeUpdatePayload): Promise<UserDegree> {
  return apiPut<UserDegree>(`/profile/degrees/${degreeId}`, payload);
}

export function deleteDegree(degreeId: number): Promise<void> {
  return apiDelete(`/profile/degrees/${degreeId}`);
}

// --- Langues du profil ---

export function getUserLanguages(): Promise<UserLanguage[]> {
  return apiGet<UserLanguage[]>('/profile/languages');
}

export function addLanguage(payload: LanguagePayload): Promise<UserLanguage> {
  return apiPost<UserLanguage>('/profile/languages', payload);
}

export function updateLanguageLevel(languageId: number, level: LanguageLevel): Promise<UserLanguage> {
  return apiPut<UserLanguage>(`/profile/languages/${languageId}`, { level });
}

export function deleteLanguage(languageId: number): Promise<void> {
  return apiDelete(`/profile/languages/${languageId}`);
}

// --- CV (PDF) ---

export function uploadCv(file: File): Promise<{ fileName: string }> {
  const formData = new FormData();
  formData.append('file', file);
  return apiUpload<{ fileName: string }>('/profile/cv', formData);
}

export function downloadCv(): Promise<Blob> {
  return apiDownload('/profile/cv');
}

export function deleteCv(): Promise<void> {
  return apiDelete('/profile/cv');
}
