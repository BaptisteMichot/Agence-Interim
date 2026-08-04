import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getMissionDecisionCount } from '../../api/missions';

export default function JobSeekerDashboard() {
  const [decisionCount, setDecisionCount] = useState(0);

  useEffect(() => {
    getMissionDecisionCount()
      .then((result) => setDecisionCount(result.count))
      .catch(() => setDecisionCount(0));
  }, []);

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <Link
          to="/interimaire/profil"
          className="block rounded-xl border border-indigo-200 bg-indigo-50 p-6 transition hover:bg-indigo-100"
        >
          <p className="text-lg font-semibold text-indigo-800">Mon profil →</p>
          <p className="mt-1 text-sm text-indigo-700">
            Renseigne tes informations, tes expériences et tes formations.
          </p>
        </Link>
        <Link
          to="/interimaire/offres"
          className="block rounded-xl border border-emerald-200 bg-emerald-50 p-6 transition hover:bg-emerald-100"
        >
          <p className="text-lg font-semibold text-emerald-800">Offres d'emploi →</p>
          <p className="mt-1 text-sm text-emerald-700">
            Consulte les offres ouvertes, postule et gère tes favoris.
          </p>
        </Link>
        <Link
          to="/interimaire/candidatures"
          className="block rounded-xl border border-sky-200 bg-sky-50 p-6 transition hover:bg-sky-100"
        >
          <p className="text-lg font-semibold text-sky-800">Mes candidatures →</p>
          <p className="mt-1 text-sm text-sky-700">
            Suis l'avancement de tes candidatures.
          </p>
        </Link>
        <Link
          to="/interimaire/missions"
          className="block rounded-xl border border-violet-200 bg-violet-50 p-6 transition hover:bg-violet-100"
        >
          <p className="text-lg font-semibold text-violet-800">
            Mes missions →
            {decisionCount > 0 && (
              <span className="ml-2 rounded-full bg-violet-600 px-2 py-0.5 text-xs font-semibold text-white">
                {decisionCount}
              </span>
            )}
          </p>
          <p className="mt-1 text-sm text-violet-700">
            {decisionCount > 0
              ? 'Une mission attend ta réponse.'
              : 'Consulte tes missions et tes contrats.'}
          </p>
        </Link>
        <Link
          to="/interimaire/planning"
          className="block rounded-xl border border-amber-200 bg-amber-50 p-6 transition hover:bg-amber-100"
        >
          <p className="text-lg font-semibold text-amber-800">Mon planning →</p>
          <p className="mt-1 text-sm text-amber-700">
            Visualise tes journées de travail et déclare tes indisponibilités.
          </p>
        </Link>
      </div>
    </div>
  );
}
