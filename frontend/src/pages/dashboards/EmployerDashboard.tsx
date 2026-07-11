import DashboardPlaceholder from '../../components/DashboardPlaceholder';

export default function EmployerDashboard() {
  return (
    <DashboardPlaceholder
      title="Espace employeur"
      upcoming={[
        'Publier une offre d’emploi',
        'Suivre l’état de mes offres',
        'Consulter et trier les candidatures reçues',
        'Noter les candidatures',
        'Discuter avec un candidat via le chat',
        'Sélectionner un intérimaire pour une mission',
      ]}
    />
  );
}
