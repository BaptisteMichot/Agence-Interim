import { useState } from 'react';
import { Link } from 'react-router-dom';
import { errorMessage } from '../../api/http';
import { closeOffer, getMyOffers } from '../../api/offers';
import ConfirmDialog from '../../components/ConfirmDialog';
import PageHeader from '../../components/PageHeader';
import Pagination from '../../components/Pagination';
import { btnDanger, btnPrimary, btnSecondary, errorBox } from '../../components/ui';
import { usePagedResource } from '../../hooks/usePagedResource';
import { salarySuffix, sectorLabel } from '../../offers/format';
import type { JobOfferSummary } from '../../offers/types';
import { formatTimestampDate } from '../../profile/format';

/** Offres publiées par l'employeur, avec le nombre de candidatures reçues par offre. */
export default function EmployerOffersPage() {
  const [closing, setClosing] = useState<JobOfferSummary | null>(null);

  const {
    items: offers,
    pageData,
    loading,
    error,
    setError,
    reload,
    goTo,
  } = usePagedResource(getMyOffers, 'Impossible de charger les offres.');

  const confirmClose = async () => {
    if (!closing) {
      return;
    }
    const id = closing.id;
    setClosing(null);
    setError(null);
    try {
      await closeOffer(id);
      reload();
    } catch (err) {
      setError(errorMessage(err, 'Une erreur est survenue.'));
    }
  };

  return (
    <section>
      <PageHeader
        title="Mes offres d'emploi"
        subtitle="Vos offres publiées et les candidatures reçues."
        actions={
          <Link to="/employeur/offres/nouvelle" className={btnPrimary}>
            + Nouvelle offre
          </Link>
        }
      />

      {error && <p className={errorBox}>{error}</p>}

      <div className="mt-6 rounded-xl border border-line bg-surface p-6">
        {loading && <p className="text-sm text-slate-500">Chargement…</p>}
        {!loading && offers.length === 0 && (
          <p className="text-sm text-slate-500">
            Aucune offre publiée. Créez votre première offre avec « Nouvelle offre ».
          </p>
        )}

        <ul className="space-y-3">
          {offers.map((offer) => (
            <li
              key={offer.id}
              className="flex flex-wrap items-center justify-between gap-4 rounded-lg border border-slate-200 p-4"
            >
              <div>
                <p className="font-medium text-slate-900">
                  {offer.title}
                  <span
                    className={`ml-2 rounded-full px-2 py-0.5 text-xs font-medium ${
                      offer.status === 'OPEN'
                        ? 'bg-green-100 text-green-700'
                        : 'bg-slate-200 text-slate-600'
                    }`}
                  >
                    {offer.status === 'OPEN' ? 'Ouverte' : 'Clôturée'}
                  </span>
                </p>
                <p className="text-sm text-slate-500">
                  {sectorLabel(offer.sector)} · {offer.city}
                  {salarySuffix(offer.salaryMin, offer.salaryMax)}
                </p>
                {offer.publishedAt && (
                  <p className="text-xs text-slate-400">
                    Publiée le {formatTimestampDate(offer.publishedAt)}
                  </p>
                )}
              </div>
              <div className="flex shrink-0 gap-2">
                <Link to={`/employeur/offres/${offer.id}/candidatures`} className={btnSecondary}>
                  Candidatures ({offer.applicationCount})
                </Link>
                <Link to={`/employeur/offres/${offer.id}`} className={btnSecondary}>
                  {offer.editable ? 'Modifier' : 'Consulter'}
                </Link>
                {offer.status === 'OPEN' && (
                  <button type="button" className={btnDanger} onClick={() => setClosing(offer)}>
                    Clôturer
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>

        <Pagination page={pageData} onChange={goTo} label="offres" />
      </div>

      <ConfirmDialog
        open={closing !== null}
        title="Clôturer l'offre"
        message={`L'offre « ${closing?.title} » ne sera plus visible des candidats et ne pourra plus être modifiée.`}
        confirmLabel="Clôturer"
        onConfirm={confirmClose}
        onCancel={() => setClosing(null)}
      />
    </section>
  );
}
