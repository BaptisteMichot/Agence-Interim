import { Link, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { ROLE_LABEL } from '../auth/roleRoutes';
import { useChat } from '../chat/ChatContext';

/** Coquille commune aux pages authentifiées : barre de navigation + contenu. */
export default function Layout() {
  const { user, logout } = useAuth();
  const { unreadCount } = useChat();
  const navigate = useNavigate();
  const canChat = user?.role === 'JOBSEEKER' || user?.role === 'EMPLOYER';

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="min-h-full bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <span className="text-lg font-semibold text-indigo-600">Agence d'intérim</span>
          {user && (
            <div className="flex items-center gap-4 text-sm">
              {canChat && (
                <Link
                  to="/messages"
                  className="relative font-medium text-slate-700 hover:text-indigo-600"
                >
                  Messages
                  {unreadCount > 0 && (
                    <span className="absolute -right-4 -top-2 rounded-full bg-indigo-600 px-1.5 py-0.5 text-xs font-semibold text-white">
                      {unreadCount}
                    </span>
                  )}
                </Link>
              )}
              <span className="text-slate-600">
                {user.firstName} {user.lastName}
                <span className="ml-2 rounded-full bg-indigo-50 px-2 py-0.5 text-xs font-medium text-indigo-700">
                  {ROLE_LABEL[user.role]}
                </span>
              </span>
              <button
                type="button"
                onClick={handleLogout}
                className="rounded-md border border-slate-300 px-3 py-1.5 font-medium text-slate-700 hover:bg-slate-100"
              >
                Déconnexion
              </button>
            </div>
          )}
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  );
}
