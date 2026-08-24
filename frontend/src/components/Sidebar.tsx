import { Link, NavLink } from 'react-router-dom';
import type { AuthUser } from '../auth/types';
import { homePathForRole, ROLE_LABEL } from '../auth/roleRoutes';
import Icon from './Icon';
import { NAV_BY_ROLE, type NavCounter } from './navigation';

interface SidebarProps {
  user: AuthUser;
  counters: Record<NavCounter, number>;
  onLogout: () => void;
  /** Tiroir ouvert : uniquement sous `lg`, où la barre n'est pas affichée en permanence. */
  open: boolean;
  onClose: () => void;
}

/**
 * Navigation principale : fixe à partir de `lg`, tiroir coulissant en dessous.
 * Les sections viennent du rôle de l'utilisateur (voir {@link NAV_BY_ROLE}).
 */
export default function Sidebar({ user, counters, onLogout, open, onClose }: SidebarProps) {
  const items = NAV_BY_ROLE[user.role];
  // Le compte administrateur représente l'agence elle-même, pas une personne.
  const displayName = user.role === 'ADMIN' ? 'Agence' : `${user.firstName} ${user.lastName}`;

  return (
    <>
      {open && (
        <div
          className="fixed inset-0 z-30 bg-slate-900/50 lg:hidden"
          onClick={onClose}
          aria-hidden="true"
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 shrink-0 flex-col bg-shell text-shell-text transition-transform duration-200 lg:sticky lg:top-0 lg:h-screen lg:translate-x-0 ${
          open ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="flex items-center justify-between px-5 py-5">
          <Link to={homePathForRole(user.role)} className="text-base font-semibold text-white">
            Agence d'intérim
          </Link>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fermer le menu"
            className="rounded-md p-1 text-shell-muted transition hover:bg-shell-hover hover:text-white lg:hidden"
          >
            ✕
          </button>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto px-3 pb-4">
          {items.map((item) => {
            const count = item.counter ? counters[item.counter] : 0;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  `flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition ${
                    isActive
                      ? 'bg-brand-600 text-white'
                      : 'text-shell-text hover:bg-shell-hover hover:text-white'
                  }`
                }
              >
                <Icon name={item.icon} className="h-5 w-5 shrink-0" />
                <span className="flex-1">{item.label}</span>
                {count > 0 && (
                  <span className="rounded-full bg-white px-2 py-0.5 text-xs font-semibold text-shell">
                    {count}
                  </span>
                )}
              </NavLink>
            );
          })}
        </nav>

        <div className="border-t border-shell-line px-5 py-4">
          <p className="truncate text-sm font-medium text-white">{displayName}</p>
          <p className="text-xs text-shell-muted">{ROLE_LABEL[user.role]}</p>
          <button
            type="button"
            onClick={onLogout}
            className="mt-3 w-full rounded-md border border-shell-line px-3 py-1.5 text-sm font-medium text-shell-text transition hover:bg-shell-hover hover:text-white"
          >
            Déconnexion
          </button>
          <NavLink
            to="/vie-privee"
            className="mt-3 block text-center text-xs text-shell-muted transition hover:text-white"
          >
            Vie privée et cookies
          </NavLink>
        </div>
      </aside>
    </>
  );
}
