import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { closeOffer, getMyOffers } from '../../api/offers';
import ConfirmDialog from '../../components/ConfirmDialog';
import { btnDanger, btnPrimary, btnSecondary, errorBox } from '../../components/ui';
import type { JobOfferSummary } from '../../offers/types';
import { formatDate } from '../../profile/format';

export default function EmployerDashboard() {
  const [offers, setOffers] = useState<JobOfferSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [closing, setClosing] = useState<JobOfferSummary | null>(null);

  const reload = useCallback(async () => {
    setError(null);
    try {
      setOffers(await getMyOffers());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Impossible de charger les offres.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

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
      setError(err instanceof Error ? err.message : 'Une erreur est survenue.');
    }
  };

  return (
    <section>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Espace employeur</h1>
          <p className="mt-1 text-slate-600">Vos offres d'emploi.</p>
        </div>
        <Link to="/employeur/offres/nouvelle" className={btnPrimary}>
          + Nouvelle offre
        </Link>
      </div>

      {error && <p className={`mt-4 ${errorBox}`}>{error}</p>}

      <div className="mt-6 rounded-xl border border-slate-200 bg-white p-6">
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
                  {offer.sector} · {offer.city}
                  {offer.salaryMin !== null || offer.salaryMax !== null
                    ? ` · ${offer.salaryMin ?? '?'} – ${offer.salaryMax ?? '?'} €/h`
                    : ''}
                </p>
                {offer.publishedAt && (
                  <p className="text-xs text-slate-400">
                    Publiée le {formatDate(offer.publishedAt.slice(0, 10))}
                  </p>
                )}
              </div>
              <div className="flex shrink-0 gap-2">
                {offer.status === 'OPEN' && (
                  <>
                    <Link to={`/employeur/offres/${offer.id}`} className={btnSecondary}>
                      Modifier
                    </Link>
                    <button type="button" className={btnDanger} onClick={() => setClosing(offer)}>
                      Clôturer
                    </button>
                  </>
                )}
                {offer.status === 'CLOSED' && (
                  <Link to={`/employeur/offres/${offer.id}`} className={btnSecondary}>
                    Consulter
                  </Link>
                )}
              </div>
            </li>
          ))}
        </ul>
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
