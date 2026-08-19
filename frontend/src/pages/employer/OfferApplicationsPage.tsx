import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { getOfferApplications, rateApplication } from '../../api/applications';
import { openConversationForApplication } from '../../api/chat';
import { errorMessage } from '../../api/http';
import { getMyOffer } from '../../api/offers';
import { btnPrimary, btnSecondary, errorBox, inputClass, linkBack } from '../../components/ui';
import type { OfferApplication } from '../../applications/types';
import type { JobOfferDetail } from '../../offers/types';
import { formatTimestampDate } from '../../profile/format';

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
  const navigate = useNavigate();

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
      setError(errorMessage(err, 'Impossible de charger les candidatures.'));
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
      setError(errorMessage(err, 'Une erreur est survenue.'));
    }
  };

  /** Ouvre (ou retrouve) le fil de discussion avec le candidat, puis y navigue. */
  const startChat = async (applicationId: number) => {
    setError(null);
    try {
      const conversation = await openConversationForApplication(applicationId);
      navigate(`/messages/${conversation.id}`);
    } catch (err) {
      setError(errorMessage(err, "La discussion n'a pas pu être ouverte."));
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
      <Link to="/employeur/offres" className={linkBack}>
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

      <div className="mt-6 rounded-xl border border-line bg-surface p-6">
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
                  Reçue le {formatTimestampDate(application.applicationTime)}
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
                <button
                  type="button"
                  className={btnSecondary}
                  onClick={() => startChat(application.id)}
                >
                  💬 Discuter
                </button>
                <Link
                  to={`/employeur/candidatures/${application.id}/mission`}
                  className={btnPrimary}
                  title="Créer la mission provisoire pour ce candidat"
                >
                  Sélectionner
                </Link>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
