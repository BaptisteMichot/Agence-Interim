import DashboardPlaceholder from '../../components/DashboardPlaceholder';

export default function AdminDashboard() {
  return (
    <DashboardPlaceholder
      title="Espace administrateur"
      upcoming={[
        'Consulter les demandes d’accès employeur',
        'Attribuer le rôle employeur',
        'Consulter les missions provisoires',
        'Valider ou refuser une mission d’intérim',
        'Gérer les droits d’accès',
      ]}
    />
  );
}
