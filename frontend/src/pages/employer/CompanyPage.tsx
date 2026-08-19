import { Link } from 'react-router-dom';
import { linkBack } from '../../components/ui';
import CompanySection from './CompanySection';

/** Page « Mon entreprise » de l'espace employeur, pendant du profil de l'intérimaire. */
export default function CompanyPage() {
  return (
    <div className="space-y-6">
      <div>
        <Link to="/employeur" className={linkBack}>
          ← Retour au tableau de bord
        </Link>
        <h1 className="mt-2 text-2xl font-semibold text-slate-900">Mon entreprise</h1>
        <p className="mt-1 text-slate-600">Ces mentions figurent sur chaque contrat de mission.</p>
      </div>

      <CompanySection />
    </div>
  );
}
