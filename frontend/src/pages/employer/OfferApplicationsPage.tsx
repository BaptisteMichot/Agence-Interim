import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getOfferApplications, rateApplication } from '../../api/applications';
import { getMyOffer } from '../../api/offers';
import { btnSecondary, errorBox, inputClass } from '../../components/ui';
import type { OfferApplication } from '../../applications/types';
import type { JobOfferDetail } from '../../offers/types';
import { formatDate } from '../../profile/format';

type SortKey = 'date-desc' | 'date-asc' | 'rating-desc';

/** Tri des candidatures (FR12) : par date ou par note (non notées en dernier). */
function sortApplications(applications: OfferApplication[], sort: SortKey): OfferApplication[] {
  const sorted = [...applications];
  if (sort === 'rating-desc') {
    sorted.sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0));
  } else {
    sorted.sort((a, b) =>
      sort === 'date-asc'
        ? a.applicationTime.localeCompare(b.applicationTime)
        : b.applicationTime.localeCompare(a.applicationTime),
    );
  }
  return sorted;
}

/** Note 1-5 cliquable (FR11). */
function RatingStars({
  rating,
  onRate,
}: {
  rating: number | null;
  onRate: (value: number) => void;
}) {
  return (
    <div className="flex items-center gap-0.5" title="Noter la candidature">
      {[1, 2, 3, 4, 5].map((value) => (
        <button
          key={value}
          type="button"
          onClick={() => onRate(value)}
          className={`text-xl leading-none ${
            rating !== null && value <= rating ? 'text-amber-400' : 'text-slate-300 hover:text-amber-300'
          }`}
          aria-label={`Noter ${value} sur 5`}
        >
          ★
        </button>
      ))}
    </div>
  );
}

/** Candidatures reçues sur une offre de l'employeur : consultation, note et tri. */
export default function OfferApplicationsPage() {
  const { id } = useParams();
  const offerId = Number(id);

  const [offer, setOffer] = useState<JobOfferDetail | null>(null);
  const [applications, setApplications] = useState<OfferApplication[]>([]);
  const [sort, setSort] = useState<SortKey>('date-desc');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setError(null);
    try {
      const [detail, list] = await Promise.all([getMyOffer(offerId), getOfferApplications(offerId)]);
      setOffer(detail);
      setApplications(list);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Impossible de charger les candidatures.');
    } finally {
      setLoading(false);
    }
  }, [offerId]);

  useEffect(() => {
    reload();
  }, [reload]);

  const rate = async (applicationId: number, rating: number) => {
    setError(null);
    try {
      const updated = await rateApplication(applicationId, rating);
      setApplications((list) => list.map((a) => (a.id === updated.id ? updated : a)));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Une erreur est survenue.');
    }
  };

  if (loading) {
    return <p className="text-slate-500">Chargement…</p>;
  }

  if (!offer) {
    return <p className={errorBox}>{error ?? 'Offre introuvable.'}</p>;
  }

  return (
    <section>
      <Link to="/employeur" className="text-sm text-indigo-600 hover:underline">
        ← Retour à mes offres
      </Link>
      <div className="mt-2 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Candidatures — {offer.title}</h1>
          <p className="mt-1 text-slate-600">
            {offer.sector} · {offer.city}
          </p>
        </div>
        {applications.length > 1 && (
          <label className="flex items-center gap-2 text-sm text-slate-700">
            Trier par
            <select
              className={`${inputClass} w-auto`}
              value={sort}
              onChange={(e) => setSort(e.target.value as SortKey)}
            >
              <option value="date-desc">Plus récentes</option>
              <option value="date-asc">Plus anciennes</option>
              <option value="rating-desc">Meilleure note</option>
            </select>
          </label>
        )}
      </div>

      {error && <p className={`mt-4 ${errorBox}`}>{error}</p>}

      <div className="mt-6 rounded-xl border border-slate-200 bg-white p-6">
        {applications.length === 0 && (
          <p className="text-sm text-slate-500">Aucune candidature reçue pour cette offre.</p>
        )}

        <ul className="space-y-3">
          {sortApplications(applications, sort).map((application) => (
            <li
              key={application.id}
              className="flex flex-wrap items-center justify-between gap-4 rounded-lg border border-slate-200 p-4"
            >
              <div>
                <p className="font-medium text-slate-900">
                  {application.firstName} {application.lastName}
                </p>
                <p className="text-sm text-slate-500">{application.email}</p>
                <p className="text-xs text-slate-400">
                  Reçue le {formatDate(application.applicationTime.slice(0, 10))}
                </p>
              </div>
              <div className="flex shrink-0 items-center gap-4">
                <RatingStars
                  rating={application.rating}
                  onRate={(value) => rate(application.id, value)}
                />
                <Link to={`/employeur/candidatures/${application.id}`} className={btnSecondary}>
                  Voir le profil
                </Link>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
