import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import { homePathForUser } from './auth/roleRoutes';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import EmployerRegisterPage from './pages/EmployerRegisterPage';
import EmployerStatusPage from './pages/EmployerStatusPage';
import AdminDashboard from './pages/dashboards/AdminDashboard';
import EmployerDashboard from './pages/dashboards/EmployerDashboard';
import JobSeekerDashboard from './pages/dashboards/JobSeekerDashboard';
import RecruiterDashboard from './pages/dashboards/RecruiterDashboard';
import ProfilePage from './pages/profile/ProfilePage';

/** Redirige la racine vers la bonne destination selon l'utilisateur, ou vers la connexion. */
function HomeRedirect() {
  const { isAuthenticated, user } = useAuth();
  if (isAuthenticated && user) {
    return <Navigate to={homePathForUser(user)} replace />;
  }
  return <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/inscription-employeur" element={<EmployerRegisterPage />} />

      {/* Page de statut employeur : authentifiée, hors coquille (pas de nav de rôle). */}
      <Route element={<ProtectedRoute />}>
        <Route path="/statut-employeur" element={<EmployerStatusPage />} />
      </Route>

      {/* Routes nécessitant une authentification */}
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route element={<ProtectedRoute allowedRoles={['JOBSEEKER']} />}>
            <Route path="/interimaire" element={<JobSeekerDashboard />} />
            <Route path="/interimaire/profil" element={<ProfilePage />} />
          </Route>
          <Route element={<ProtectedRoute allowedRoles={['EMPLOYER']} />}>
            <Route path="/employeur" element={<EmployerDashboard />} />
          </Route>
          <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
            <Route path="/admin" element={<AdminDashboard />} />
          </Route>
          <Route element={<ProtectedRoute allowedRoles={['INTERIM_RECRUITER']} />}>
            <Route path="/agence" element={<RecruiterDashboard />} />
          </Route>
        </Route>
      </Route>

      <Route path="/" element={<HomeRedirect />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
