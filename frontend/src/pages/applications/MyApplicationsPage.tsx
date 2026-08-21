import { useState } from 'react';
import { Link } from 'react-router-dom';
import { cancelApplication, getMyApplications } from '../../api/applications';
import { errorMessage } from '../../api/http';
import ConfirmDialog from '../../components/ConfirmDialog';
import PageHeader from '../../components/PageHeader';
import Pagination from '../../components/Pagination';
import { btnDanger, btnSecondary, errorBox } from '../../components/ui';
import { usePagedResource } from '../../hooks/usePagedResource';
import type { MyApplication } from '../../applications/types';
import { formatTimestampDate } from '../../profile/format';

/** Chip de suivi : statut de la candidature, en tenant compte d'une offre clôturée. */
function statusChip(application: MyApplication) {
  if (application.status === 'CANCELED') {
    return <span className="rounded-full bg-slate-200 px-2 py-0.5 text-xs font-medium text-slate-600">Annulée</span>;
  }
  if (application.offer.status === 'CLOSED') {
    return (
      <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700">
        Offre clôturée
      </span>
    );
  }
  return <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">En attente</span>;
}

/** Suivi des candidatures de l'intérimaire, avec annulation d'une candidature en cours. */
export default function MyApplicationsPage() {
  const [canceling, setCanceling] = useState<MyApplication | null>(null);

  const {
    items: applications,
    pageData,
    loading,
    error,
    setError,
    reload,
    goTo,
  } = usePagedResource(getMyApplications, 'Impossible de charger les candidatures.');

  const confirmCancel = async () => {
    if (!canceling) {
      return;
    }
    const id = canceling.id;
    setCanceling(null);
    setError(null);
    try {
      await cancelApplication(id);
      reload();
    } catch (err) {
      setError(errorMessage(err, 'Une erreur est survenue.'));
    }
  };

  return (
    <section>
      <PageHeader title="Mes candidatures" subtitle="Suivez l'avancement de vos candidatures." />

      {error && <p className={errorBox}>{error}</p>}

      <div className="mt-6 rounded-xl border border-line bg-surface p-6">
        {loading && <p className="text-sm text-slate-500">Chargement…</p>}
        {!loading && applications.length === 0 && (
          <p className="text-sm text-slate-500">
            Aucune candidature pour le moment. Postulez depuis la page{' '}
            <Link to="/interimaire/offres" className="text-brand-600 hover:underline">
              Offres d'emploi
            </Link>
            .
          </p>
        )}

        <ul className="space-y-3">
          {applications.map((application) => (
            <li
              key={application.id}
              className="flex flex-wrap items-center justify-between gap-4 rounded-lg border border-slate-200 p-4"
            >
              <div>
                <p className="font-medium text-slate-900">
                  {application.offer.title}
                  <span className="ml-2">{statusChip(application)}</span>
                </p>
                <p className="text-sm text-slate-500">
                  {application.offer.companyName} · {application.offer.sector} · {application.offer.city}
                </p>
                <p className="text-xs text-slate-400">
                  Postulée le {formatTimestampDate(application.applicationTime)}
                </p>
              </div>
              <div className="flex shrink-0 gap-2">
                <Link to={`/interimaire/offres/${application.offer.id}`} className={btnSecondary}>
                  Voir l'offre
                </Link>
                {application.status === 'PENDING' && application.offer.status === 'OPEN' && (
                  <button type="button" className={btnDanger} onClick={() => setCanceling(application)}>
                    Annuler
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>

        <Pagination page={pageData} onChange={goTo} label="candidatures" />
      </div>

      <ConfirmDialog
        open={canceling !== null}
        title="Annuler la candidature"
        message={`Votre candidature à « ${canceling?.offer.title} » ne sera plus visible de l'employeur.`}
        confirmLabel="Annuler la candidature"
        onConfirm={confirmCancel}
        onCancel={() => setCanceling(null)}
      />
    </section>
  );
}
