import { useEffect, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  getAdminPendingMissionCount,
  getContractsToSignCount,
  getMissionDecisionCount,
} from '../api/missions';
import { useAuth } from '../auth/AuthContext';
import { useChat } from '../chat/ChatContext';
import ConfirmDialog from './ConfirmDialog';
import Sidebar from './Sidebar';

/** Coquille commune aux pages authentifiées : barre latérale + contenu. */
export default function Layout() {
  const { user, logout } = useAuth();
  const { unreadCount } = useChat();
  const navigate = useNavigate();
  const location = useLocation();

  const [menuOpen, setMenuOpen] = useState(false);
  const [confirmLogout, setConfirmLogout] = useState(false);
  const [missionsToConfirm, setMissionsToConfirm] = useState(0);
  const [missionsToValidate, setMissionsToValidate] = useState(0);
  const [contractsToSign, setContractsToSign] = useState(0);

  const isJobSeeker = user?.role === 'JOBSEEKER';
  const isAdmin = user?.role === 'ADMIN';
  // Les deux parties d'une mission signent : la pastille vaut pour l'une comme pour l'autre.
  const isParty = isJobSeeker || user?.role === 'EMPLOYER';

  // Compteurs de la barre latérale, rafraîchis à chaque navigation : les décisions
  // qu'ils annoncent se prennent depuis une page de l'application.
  useEffect(() => {
    if (!isJobSeeker) {
      setMissionsToConfirm(0);
      return;
    }
    getMissionDecisionCount()
      .then((result) => setMissionsToConfirm(result.count))
      .catch(() => setMissionsToConfirm(0));
  }, [isJobSeeker, location.pathname]);

  useEffect(() => {
    if (!isAdmin) {
      setMissionsToValidate(0);
      return;
    }
    getAdminPendingMissionCount()
      .then(setMissionsToValidate)
      .catch(() => setMissionsToValidate(0));
  }, [isAdmin, location.pathname]);

  useEffect(() => {
    if (!isParty) {
      setContractsToSign(0);
      return;
    }
    getContractsToSignCount()
      .then(setContractsToSign)
      .catch(() => setContractsToSign(0));
  }, [isParty, location.pathname]);

  // Le tiroir mobile se referme dès qu'une page est ouverte.
  useEffect(() => {
    setMenuOpen(false);
  }, [location.pathname]);

  const handleLogout = async () => {
    setConfirmLogout(false);
    // Seul le serveur peut effacer un cookie HttpOnly : on attend sa réponse avant
    // de quitter la page, sinon l'utilisateur se croirait déconnecté à tort.
    await logout();
    navigate('/login', { replace: true });
  };

  if (!user) {
    return null;
  }

  return (
    <div className="min-h-full lg:flex">
      <Sidebar
        user={user}
        counters={{
          missionsToConfirm,
          unreadMessages: unreadCount,
          missionsToValidate,
          contractsToSign,
        }}
        onLogout={() => setConfirmLogout(true)}
        open={menuOpen}
        onClose={() => setMenuOpen(false)}
      />

      <div className="flex min-w-0 flex-1 flex-col">
        {/* En-tête réduit au strict nécessaire : il n'existe que sous `lg`. */}
        <header className="flex items-center gap-3 border-b border-line bg-surface px-4 py-3 lg:hidden">
          <button
            type="button"
            onClick={() => setMenuOpen(true)}
            aria-label="Ouvrir le menu"
            className="rounded-md border border-line px-2.5 py-1.5 text-slate-700 transition hover:bg-slate-50"
          >
            ☰
          </button>
          <span className="text-base font-semibold text-brand-600">Agence d'intérim</span>
        </header>

        <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 lg:px-8">
          <Outlet />
        </main>
      </div>

      <ConfirmDialog
        open={confirmLogout}
        title="Se déconnecter"
        message="Vous devrez saisir à nouveau votre email et votre mot de passe pour revenir."
        confirmLabel="Se déconnecter"
        onConfirm={handleLogout}
        onCancel={() => setConfirmLogout(false)}
      />
    </div>
  );
}
