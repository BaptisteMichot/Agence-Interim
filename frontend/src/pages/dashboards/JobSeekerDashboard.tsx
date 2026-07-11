import DashboardPlaceholder from '../../components/DashboardPlaceholder';

export default function JobSeekerDashboard() {
  return (
    <DashboardPlaceholder
      title="Espace intérimaire"
      upcoming={[
        'Compléter mon profil (compétences, diplômes, langues, expériences, CV)',
        'Consulter les offres proposées et postuler',
        'Ajouter des offres en favoris',
        'Suivre l’avancement de mes candidatures',
        'Consulter mon planning et l’historique de mes missions',
        'Gérer mes disponibilités',
      ]}
    />
  );
}
