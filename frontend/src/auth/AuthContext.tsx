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
import {
  SESSION_EXPIRED_EVENT,
  clearSession,
  readToken,
  readUser,
  saveSession,
} from './session';
import type { AuthResponse, AuthUser, RegisterPayload } from './types';

interface AuthContextValue {
  user: AuthUser | null;
  token: string | null;
  isAuthenticated: boolean;
  /** Vrai quand la session a été refusée par le serveur (à afficher sur la page de connexion). */
  sessionExpired: boolean;
  login: (email: string, password: string) => Promise<AuthUser>;
  register: (payload: RegisterPayload) => Promise<AuthUser>;
  logout: () => void;
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
  const [token, setToken] = useState<string | null>(readToken);
  const [user, setUser] = useState<AuthUser | null>(readUser);
  const [sessionExpired, setSessionExpired] = useState(false);

  const persist = useCallback((response: AuthResponse): AuthUser => {
    const nextUser = toUser(response);
    saveSession(response.token, nextUser);
    setToken(response.token);
    setUser(nextUser);
    setSessionExpired(false);
    return nextUser;
  }, []);

  const login = useCallback(
    async (email: string, password: string) => persist(await authApi.login(email, password)),
    [persist],
  );

  const register = useCallback(
    async (payload: RegisterPayload) => persist(await authApi.register(payload)),
    [persist],
  );

  const logout = useCallback(() => {
    clearSession();
    setToken(null);
    setUser(null);
    setSessionExpired(false);
  }, []);

  // Le serveur a refusé le token (401/403) : la couche HTTP a purgé la session,
  // on aligne l'état React pour que les routes protégées renvoient vers /login.
  useEffect(() => {
    const onExpired = () => {
      setToken(null);
      setUser(null);
      setSessionExpired(true);
    };
    window.addEventListener(SESSION_EXPIRED_EVENT, onExpired);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, onExpired);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      token,
      isAuthenticated: token !== null,
      sessionExpired,
      login,
      register,
      logout,
    }),
    [user, token, sessionExpired, login, register, logout],
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
