import { Link } from 'react-router-dom';
import DashboardPlaceholder from '../../components/DashboardPlaceholder';

export default function JobSeekerDashboard() {
  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2">
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
            Consulte les offres ouvertes et gère tes favoris.
          </p>
        </Link>
      </div>

      <DashboardPlaceholder
        title="Espace intérimaire"
        upcoming={[
          'Recevoir des offres correspondant à mon profil',
          'Postuler aux offres et suivre mes candidatures',
          'Consulter mon planning et l’historique de mes missions',
        ]}
      />
    </div>
  );
}
