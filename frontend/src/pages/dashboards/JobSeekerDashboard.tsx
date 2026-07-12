import { Link } from 'react-router-dom';
import DashboardPlaceholder from '../../components/DashboardPlaceholder';

export default function JobSeekerDashboard() {
  return (
    <div className="space-y-6">
      <Link
        to="/interimaire/profil"
        className="block rounded-xl border border-indigo-200 bg-indigo-50 p-6 transition hover:bg-indigo-100"
      >
        <p className="text-lg font-semibold text-indigo-800">Mon profil →</p>
        <p className="mt-1 text-sm text-indigo-700">
          Renseigne tes informations, tes expériences et tes formations.
        </p>
      </Link>

      <DashboardPlaceholder
        title="Espace intérimaire"
        upcoming={[
          'Compléter mes compétences, diplômes et langues',
          'Déposer mon CV (PDF)',
          'Consulter les offres proposées et postuler',
          'Ajouter des offres en favoris',
          'Suivre l’avancement de mes candidatures',
          'Consulter mon planning et l’historique de mes missions',
        ]}
      />
    </div>
  );
}
