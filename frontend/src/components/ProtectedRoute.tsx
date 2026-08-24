import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { homePathForRole } from '../auth/roleRoutes';
import type { Role } from '../auth/types';

interface ProtectedRouteProps {
  /** Rôles autorisés. Si absent, toute personne authentifiée est acceptée. */
  allowedRoles?: Role[];
}

/**
 * Protège un ensemble de routes : redirige vers /login si non connecté, et
 * vers le tableau de bord du rôle si le rôle n'est pas autorisé.
 */
export default function ProtectedRoute({ allowedRoles }: ProtectedRouteProps) {
  const { isAuthenticated, loading, user } = useAuth();

  // L'identité vient du serveur : tant qu'elle n'est pas revenue, rediriger
  // renverrait tout visiteur déjà connecté vers l'écran de connexion.
  if (loading) {
    return null;
  }

  if (!isAuthenticated || user === null) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to={homePathForRole(user.role)} replace />;
  }

  return <Outlet />;
}
