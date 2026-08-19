import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import { EMPLOYER_STATUS_PATH, homePathForRole } from './auth/roleRoutes';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import EmployerRegisterPage from './pages/EmployerRegisterPage';
import EmployerStatusPage from './pages/EmployerStatusPage';
import AdminDashboard from './pages/dashboards/AdminDashboard';
import AdminMissionsPage from './pages/dashboards/AdminMissionsPage';
import EmployerDashboard from './pages/dashboards/EmployerDashboard';
import JobSeekerDashboard from './pages/dashboards/JobSeekerDashboard';
import ProfilePage from './pages/profile/ProfilePage';
import OfferFormPage from './pages/employer/OfferFormPage';
import OfferApplicationsPage from './pages/employer/OfferApplicationsPage';
import CandidateProfilePage from './pages/employer/CandidateProfilePage';
import CompanyPage from './pages/employer/CompanyPage';
import EmployerOffersPage from './pages/employer/EmployerOffersPage';
import MissionFormPage from './pages/employer/MissionFormPage';
import EmployerMissionsPage from './pages/employer/EmployerMissionsPage';
import EmployerMissionDetailPage from './pages/employer/EmployerMissionDetailPage';
import OffersBrowsePage from './pages/offers/OffersBrowsePage';
import OfferDetailPage from './pages/offers/OfferDetailPage';
import MyApplicationsPage from './pages/applications/MyApplicationsPage';
import MyMissionsPage from './pages/missions/MyMissionsPage';
import MissionDetailPage from './pages/missions/MissionDetailPage';
import PlanningPage from './pages/planning/PlanningPage';
import ConversationsPage from './pages/chat/ConversationsPage';
import ConversationThreadPage from './pages/chat/ConversationThreadPage';

/** Redirige la racine vers la bonne destination selon l'utilisateur, ou vers la connexion. */
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
      <Route path="/inscription-employeur" element={<EmployerRegisterPage />} />

      {/* Page de statut employeur : authentifiée, hors coquille (pas de nav de rôle). */}
      <Route element={<ProtectedRoute />}>
        <Route path={EMPLOYER_STATUS_PATH} element={<EmployerStatusPage />} />
      </Route>

      {/* Routes nécessitant une authentification */}
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route element={<ProtectedRoute allowedRoles={['JOBSEEKER']} />}>
            <Route path="/interimaire" element={<JobSeekerDashboard />} />
            <Route path="/interimaire/profil" element={<ProfilePage />} />
            <Route path="/interimaire/offres" element={<OffersBrowsePage />} />
            <Route path="/interimaire/offres/:id" element={<OfferDetailPage />} />
            <Route path="/interimaire/candidatures" element={<MyApplicationsPage />} />
            <Route path="/interimaire/missions" element={<MyMissionsPage />} />
            <Route path="/interimaire/missions/:id" element={<MissionDetailPage />} />
            <Route path="/interimaire/planning" element={<PlanningPage />} />
          </Route>
          <Route element={<ProtectedRoute allowedRoles={['EMPLOYER']} />}>
            <Route path="/employeur" element={<EmployerDashboard />} />
            <Route path="/employeur/entreprise" element={<CompanyPage />} />
            <Route path="/employeur/offres" element={<EmployerOffersPage />} />
            <Route path="/employeur/offres/nouvelle" element={<OfferFormPage />} />
            <Route path="/employeur/offres/:id" element={<OfferFormPage />} />
            <Route path="/employeur/offres/:id/candidatures" element={<OfferApplicationsPage />} />
            <Route path="/employeur/candidatures/:id" element={<CandidateProfilePage />} />
            <Route
              path="/employeur/candidatures/:applicationId/mission"
              element={<MissionFormPage />}
            />
            <Route path="/employeur/missions" element={<EmployerMissionsPage />} />
            <Route path="/employeur/missions/:id" element={<EmployerMissionDetailPage />} />
            <Route path="/employeur/missions/:id/corriger" element={<MissionFormPage mode="edit" />} />
            <Route
              path="/employeur/missions/:id/renouveler"
              element={<MissionFormPage mode="renew" />}
            />
          </Route>
          {/* Messagerie : accessible aux deux participants d'une conversation. */}
          <Route element={<ProtectedRoute allowedRoles={['JOBSEEKER', 'EMPLOYER']} />}>
            <Route path="/messages" element={<ConversationsPage />} />
            <Route path="/messages/:id" element={<ConversationThreadPage />} />
          </Route>
          <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
            <Route path="/admin" element={<AdminDashboard />} />
            <Route path="/admin/missions" element={<AdminMissionsPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="/" element={<HomeRedirect />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
