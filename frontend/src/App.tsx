import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import { homePathForRole } from './auth/roleRoutes';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import AdminDashboard from './pages/dashboards/AdminDashboard';
import EmployerDashboard from './pages/dashboards/EmployerDashboard';
import JobSeekerDashboard from './pages/dashboards/JobSeekerDashboard';
import RecruiterDashboard from './pages/dashboards/RecruiterDashboard';

/** Redirige la racine vers le tableau de bord du rôle, ou vers la connexion. */
function HomeRedirect() {
  const { isAuthenticated, user } = useAuth();
  if (isAuthenticated && user) {
    return <Navigate to={homePathForRole(user.role)} replace />;
  }
  return <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* Routes nécessitant une authentification */}
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route element={<ProtectedRoute allowedRoles={['JOBSEEKER']} />}>
            <Route path="/interimaire" element={<JobSeekerDashboard />} />
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
