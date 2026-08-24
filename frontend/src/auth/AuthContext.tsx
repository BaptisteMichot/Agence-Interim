import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import * as authApi from '../api/client';
import { SESSION_EXPIRED_EVENT } from './session';
import type { AuthResponse, AuthUser, RegisterPayload } from './types';

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  /**
   * Vrai tant que l'identité de la session n'est pas connue. Les routes protégées
   * doivent attendre : sans cela, elles renverraient vers /login le temps de l'aller-retour.
   */
  loading: boolean;
  /** Vrai quand la session a été refusée par le serveur (à afficher sur la page de connexion). */
  sessionExpired: boolean;
  login: (email: string, password: string) => Promise<AuthUser>;
  register: (payload: RegisterPayload) => Promise<AuthUser>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function toUser(response: AuthResponse): AuthUser {
  return {
    userId: response.userId,
    lastName: response.lastName,
    firstName: response.firstName,
    email: response.email,
    role: response.role,
    employerRequestStatus: response.employerRequestStatus,
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [sessionExpired, setSessionExpired] = useState(false);

  // Le cookie de session est HttpOnly : la page ne peut pas y lire qui elle représente,
  // elle le demande au serveur au démarrage. Cet appel dépose au passage le cookie
  // XSRF-TOKEN dont la première écriture aura besoin.
  useEffect(() => {
    authApi
      .me()
      .then((response) => setUser(response === null ? null : toUser(response)))
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  const accept = useCallback((response: AuthResponse): AuthUser => {
    const nextUser = toUser(response);
    setUser(nextUser);
    setSessionExpired(false);
    return nextUser;
  }, []);

  const login = useCallback(
    async (email: string, password: string) => accept(await authApi.login(email, password)),
    [accept],
  );

  const register = useCallback(
    async (payload: RegisterPayload) => accept(await authApi.register(payload)),
    [accept],
  );

  // La déconnexion passe par le serveur : lui seul peut effacer un cookie HttpOnly.
  // L'état local est vidé quoi qu'il arrive, pour ne pas laisser l'utilisateur devant
  // une interface qui le croit encore connecté.
  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      setUser(null);
      setSessionExpired(false);
    }
  }, []);

  // Le serveur a refusé la session (401/403) : on aligne l'état React pour que les
  // routes protégées renvoient vers /login.
  useEffect(() => {
    const onExpired = () => {
      setUser(null);
      setSessionExpired(true);
    };
    window.addEventListener(SESSION_EXPIRED_EVENT, onExpired);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, onExpired);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: user !== null,
      loading,
      sessionExpired,
      login,
      register,
      logout,
    }),
    [user, loading, sessionExpired, login, register, logout],
  );

  return <AuthContext value={value}>{children}</AuthContext>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth doit être utilisé à l'intérieur d'un AuthProvider.");
  }
  return context;
}
