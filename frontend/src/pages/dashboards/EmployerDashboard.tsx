import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getMyCompany, type EmployerCompany } from '../../api/employer';

export default function EmployerDashboard() {
  const [company, setCompany] = useState<EmployerCompany | null>(null);

  // Une fiche entreprise incomplète empêche de proposer une mission : la carte le signale
  // dès l'accueil, la fiche s'éditant désormais sur sa propre page.
  useEffect(() => {
    getMyCompany()
      .then(setCompany)
      .catch(() => setCompany(null));
  }, []);

  const incomplete = company?.incomplete ?? false;

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <Link
          to="/employeur/entreprise"
          className="block rounded-xl border border-indigo-200 bg-indigo-50 p-6 transition hover:bg-indigo-100"
        >
          <p className="text-lg font-semibold text-indigo-800">
            Mon entreprise →
            {incomplete && (
              <span className="ml-2 rounded-full bg-amber-500 px-2 py-0.5 text-xs font-semibold text-white">
                À compléter
              </span>
            )}
          </p>
          <p className="mt-1 text-sm text-indigo-700">
            {incomplete
              ? 'Ces mentions figurent sur les contrats : sans elles, vous ne pouvez pas proposer de mission.'
              : 'Les mentions légales de votre entreprise, reprises sur chaque contrat.'}
          </p>
        </Link>
        <Link
          to="/employeur/offres"
          className="block rounded-xl border border-emerald-200 bg-emerald-50 p-6 transition hover:bg-emerald-100"
        >
          <p className="text-lg font-semibold text-emerald-800">Mes offres d'emploi →</p>
          <p className="mt-1 text-sm text-emerald-700">
            Publiez vos offres, suivez leur état et consultez les candidatures reçues.
          </p>
        </Link>
        <Link
          to="/employeur/missions"
          className="block rounded-xl border border-violet-200 bg-violet-50 p-6 transition hover:bg-violet-100"
        >
          <p className="text-lg font-semibold text-violet-800">Mes missions →</p>
          <p className="mt-1 text-sm text-violet-700">
            Suivez vos missions, leur validation par l'agence et leurs contrats.
          </p>
        </Link>
      </div>
    </div>
  );
}
