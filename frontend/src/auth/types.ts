// Types partagés avec le backend (voir be.agence_interim.model.Role et dto.AuthResponse).

export type Role = 'ADMIN' | 'EMPLOYER' | 'EMPLOYER_PENDING' | 'JOBSEEKER';

export type EmployerAccessStatus = 'PENDING' | 'ACCEPTED' | 'REFUSED';

/** Utilisateur authentifié conservé côté frontend (sans le token). */
export interface AuthUser {
  userId: number;
  lastName: string;
  firstName: string;
  email: string;
  role: Role;
  employerRequestStatus: EmployerAccessStatus | null;
}

/**
  * Réponse renvoyée par /api/auth/register, /api/auth/login et /api/auth/me.
  * Le jeton n'y figure pas : il voyage dans un cookie HttpOnly, hors de portée de la page.
  */
export interface AuthResponse extends AuthUser {
  message: string;
}

/** Corps attendu par /api/auth/register. */
export interface RegisterPayload {
  lastName: string;
  firstName: string;
  email: string;
  password: string;
}

/** Corps attendu par /api/auth/register-employer. */
export interface EmployerRegisterPayload extends RegisterPayload {
  companyName: string;
  /** Mentions légales de l'entreprise utilisatrice, reprises sur les contrats. */
  address: string;
  companyNumber: string;
  jointCommittee: string;
}
