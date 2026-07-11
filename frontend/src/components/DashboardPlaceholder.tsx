import { useAuth } from '../auth/AuthContext';

interface DashboardPlaceholderProps {
    title: string;
    /** Fonctionnalités prévues pour ce rôle (issues de l'analyse), à implémenter plus tard. */
    upcoming: string[];
}

/** Tableau de bord vide affiché pour chaque rôle en attendant les fonctionnalités métier. */
export default function DashboardPlaceholder({ title, upcoming }: DashboardPlaceholderProps) {
    const { user } = useAuth();

    return (
        <section>
            <h1 className="text-2xl font-semibold text-slate-900">{title}</h1>
            <p className="mt-2 text-slate-600">
                Bienvenue {user?.firstName}. Cet espace sera complété plus tard.
            </p>

            <div className="mt-6 rounded-xl border border-slate-200 bg-white p-6">
                <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Fonctionnalités à venir
                </h2>
                <ul className="mt-3 space-y-2">
                    {upcoming.map((feature) => (
                        <li key={feature} className="flex items-center gap-2 text-slate-700">
                            <span className="h-1.5 w-1.5 rounded-full bg-indigo-400" aria-hidden />
                            {feature}
                        </li>
                    ))}
                </ul>
            </div>
        </section>
    );
}
