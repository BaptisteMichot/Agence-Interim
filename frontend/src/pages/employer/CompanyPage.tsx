import PageHeader from '../../components/PageHeader';
import CompanySection from './CompanySection';

/** Page « Mon entreprise » de l'espace employeur, pendant du profil de l'intérimaire. */
export default function CompanyPage() {
  return (
    <>
      <PageHeader
        title="Mon entreprise"
        subtitle="Ces mentions figurent sur chaque contrat de mission."
      />
      <CompanySection />
    </>
  );
}
