import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  addFavoriteOffer,
  browseOffers,
  getFavoriteOfferIds,
  getFavoriteOffers,
  getMatchingOffers,
  removeFavoriteOffer,
} from '../../api/offers';
import { errorBox } from '../../components/ui';
import type { JobOfferSummary, MatchingOffer } from '../../offers/types';
import { formatDate } from '../../profile/format';

type Tab = 'match' | 'all' | 'favorites';

/** Consultation des offres ouvertes + favoris (espace intérimaire). */
export default function OffersBrowsePage() {
  const [tab, setTab] = useState<Tab>('match');
  const [offers, setOffers] = useState<JobOfferSummary[]>([]);
  const [matching, setMatching] = useState<MatchingOffer[]>([]);
  const [favorites, setFavorites] = useState<JobOfferSummary[]>([]);
  const [favoriteIds, setFavoriteIds] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setError(null);
    try {
      const [open, match, favs, ids] = await Promise.all([
        browseOffers(),
        getMatchingOffers(),
        getFavoriteOffers(),
        getFavoriteOfferIds(),
      ]);
      setOffers(open);
      setMatching(match);
      setFavorites(favs);
      setFavoriteIds(new Set(ids));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Impossible de charger les offres.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const toggleFavorite = async (offer: JobOfferSummary) => {
    setError(null);
    try {
      if (favoriteIds.has(offer.id)) {
        await removeFavoriteOffer(offer.id);
      } else {
        await addFavoriteOffer(offer.id);
      }
      reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Une erreur est survenue.');
    }
  };

  /** Liste affichée, normalisée en { offer, score? } selon l'onglet. */
  const shown = useMemo<{ offer: JobOfferSummary; score?: number }[]>(() => {
    if (tab === 'match') {
      return matching.map((m) => ({ offer: m.offer, score: m.score }));
    }
    if (tab === 'favorites') {
      return favorites.map((offer) => ({ offer }));
    }
    return offers.map((offer) => ({ offer }));
  }, [tab, offers, matching, favorites]);

  return (
    <section>
      <div>
        <Link to="/interimaire" className="text-sm text-indigo-600 hover:underline">
          ← Retour au tableau de bord
        </Link>
        <h1 className="mt-2 text-2xl font-semibold text-slate-900">Offres d'emploi</h1>
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => setTab('match')}
          className={`rounded-full px-4 py-1.5 text-sm font-medium ${
            tab === 'match' ? 'bg-indigo-600 text-white' : 'border border-slate-300 text-slate-700 hover:bg-slate-100'
          }`}
        >
          Pour moi ({matching.length})
        </button>
        <button
          type="button"
          onClick={() => setTab('all')}
          className={`rounded-full px-4 py-1.5 text-sm font-medium ${
            tab === 'all' ? 'bg-indigo-600 text-white' : 'border border-slate-300 text-slate-700 hover:bg-slate-100'
          }`}
        >
          Toutes les offres
        </button>
        <button
          type="button"
          onClick={() => setTab('favorites')}
          className={`rounded-full px-4 py-1.5 text-sm font-medium ${
            tab === 'favorites' ? 'bg-indigo-600 text-white' : 'border border-slate-300 text-slate-700 hover:bg-slate-100'
          }`}
        >
          Mes favoris ({favorites.length})
        </button>
      </div>

      {error && <p className={`mt-4 ${errorBox}`}>{error}</p>}

      <div className="mt-4 rounded-xl border border-slate-200 bg-white p-6">
        {loading && <p className="text-sm text-slate-500">Chargement…</p>}
        {!loading && shown.length === 0 && (
          <p className="text-sm text-slate-500">
            {tab === 'match'
              ? 'Aucune offre ne correspond à votre profil pour le moment. Complétez vos compétences, diplômes et langues pour recevoir des propositions.'
              : tab === 'all'
                ? 'Aucune offre ouverte pour le moment.'
                : 'Aucune offre en favori.'}
          </p>
        )}

        <ul className="space-y-3">
          {shown.map(({ offer, score }) => (
            <li
              key={offer.id}
              className="flex flex-wrap items-center justify-between gap-4 rounded-lg border border-slate-200 p-4"
            >
              <div className="min-w-0">
                <p className="font-medium text-slate-900">
                  <Link to={`/interimaire/offres/${offer.id}`} className="hover:text-indigo-600 hover:underline">
                    {offer.title}
                  </Link>
                  {offer.status === 'CLOSED' && (
                    <span className="ml-2 rounded-full bg-slate-200 px-2 py-0.5 text-xs font-medium text-slate-600">
                      Clôturée
                    </span>
                  )}
                  {score !== undefined && (
                    <span
                      className={`ml-2 rounded-full px-2 py-0.5 text-xs font-semibold ${
                        score >= 75
                          ? 'bg-green-100 text-green-700'
                          : score >= 50
                            ? 'bg-amber-100 text-amber-700'
                            : 'bg-slate-100 text-slate-600'
                      }`}
                    >
                      {score} % de correspondance
                    </span>
                  )}
                </p>
                <p className="text-sm text-slate-500">
                  {offer.companyName} · {offer.sector} · {offer.city}
                  {offer.salaryMin !== null || offer.salaryMax !== null
                    ? ` · ${offer.salaryMin ?? '?'} – ${offer.salaryMax ?? '?'} €/h`
                    : ''}
                </p>
                {offer.publishedAt && (
                  <p className="text-xs text-slate-400">Publiée le {formatDate(offer.publishedAt.slice(0, 10))}</p>
                )}
              </div>
              <button
                type="button"
                onClick={() => toggleFavorite(offer)}
                title={favoriteIds.has(offer.id) ? 'Retirer des favoris' : 'Ajouter aux favoris'}
                aria-label={favoriteIds.has(offer.id) ? 'Retirer des favoris' : 'Ajouter aux favoris'}
                className={`shrink-0 text-2xl leading-none ${
                  favoriteIds.has(offer.id) ? 'text-amber-400 hover:text-amber-500' : 'text-slate-300 hover:text-amber-400'
                }`}
              >
                ★
              </button>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
