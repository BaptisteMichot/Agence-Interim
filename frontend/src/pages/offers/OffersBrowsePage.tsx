import { useCallback, useState } from 'react';
import { Link } from 'react-router-dom';
import { errorMessage } from '../../api/http';
import type { Page } from '../../api/page';
import {
  addFavoriteOffer,
  browseOffers,
  getFavoriteOffers,
  getMatchingOffers,
  removeFavoriteOffer,
} from '../../api/offers';
import PageHeader from '../../components/PageHeader';
import Pagination from '../../components/Pagination';
import { errorBox } from '../../components/ui';
import { useDebounced } from '../../hooks/useDebounced';
import { usePagedResource } from '../../hooks/usePagedResource';
import { hasActiveFilters, NO_FILTERS, salarySuffix, sectorLabel } from '../../offers/format';
import type { JobOfferSummary, OfferFilters } from '../../offers/types';
import { formatTimestampDate } from '../../profile/format';
import OfferFilterBar from './OfferFilterBar';

type Tab = 'match' | 'all' | 'favorites';

/** Ligne affichée : une offre, avec son score quand l'onglet en fournit un. */
type Row = { offer: JobOfferSummary; score?: number };

const TABS: { key: Tab; label: string }[] = [
  { key: 'match', label: 'Pour moi' },
  { key: 'all', label: 'Toutes les offres' },
  { key: 'favorites', label: 'Mes favoris' },
];

const EMPTY_MESSAGE: Record<Tab, string> = {
  match: 'Aucune offre ne correspond à votre profil pour le moment. Complétez vos compétences, diplômes et langues pour recevoir des propositions.',
  all: 'Aucune offre ouverte pour le moment.',
  favorites: 'Aucune offre en favori.',
};

/** Les favoris sont une liste courte déjà choisie : les critères ne s'y appliquent pas. */
const FILTERABLE: Tab[] = ['match', 'all'];

/** Ramène les trois formes de réponse à une même ligne affichable. */
function toRows(page: Page<JobOfferSummary>): Page<Row> {
  return { ...page, content: page.content.map((offer) => ({ offer })) };
}

/** Consultation des offres ouvertes + favoris (espace intérimaire). */
export default function OffersBrowsePage() {
  const [tab, setTab] = useState<Tab>('match');
  const [criteria, setCriteria] = useState<OfferFilters>(NO_FILTERS);

  // Les critères ne partent qu'une fois la saisie retombée : sans ce délai, chaque
  // frappe dans le champ mot-clé donnerait un fetcher différent, donc une requête.
  const filters = useDebounced(criteria, 300);
  const filtered = FILTERABLE.includes(tab) && hasActiveFilters(filters);

  // L'identité du fetcher change avec l'onglet et les critères : le hook repart
  // alors de la page 1, plutôt que de demander une page 7 qui n'existe peut-être plus.
  const fetcher = useCallback(
    (page: number): Promise<Page<Row>> => {
      if (tab === 'match') {
        return getMatchingOffers(page, filters).then((result) => ({
          ...result,
          content: result.content.map((match) => ({ offer: match.offer, score: match.score })),
        }));
      }
      return (tab === 'favorites' ? getFavoriteOffers(page) : browseOffers(page, filters)).then(toRows);
    },
    [tab, filters],
  );

  const { items, pageData, loading, error, setError, reload, goTo } = usePagedResource(
    fetcher,
    'Impossible de charger les offres.',
  );

  const toggleFavorite = async (offer: JobOfferSummary) => {
    setError(null);
    try {
      if (offer.favorite) {
        await removeFavoriteOffer(offer.id);
      } else {
        await addFavoriteOffer(offer.id);
      }
      // Rechargement plutôt que mise à jour en place : dans l'onglet des favoris,
      // la ligne doit disparaître et la page se recomposer.
      reload();
    } catch (err) {
      setError(errorMessage(err, 'Une erreur est survenue.'));
    }
  };

  return (
    <section>
      <PageHeader
        title="Offres d'emploi"
        subtitle="Les offres ouvertes, celles qui correspondent à votre profil et vos favoris."
      />

      <div className="flex flex-wrap gap-2">
        {TABS.map(({ key, label }) => (
          <button
            key={key}
            type="button"
            onClick={() => setTab(key)}
            className={`rounded-full px-4 py-1.5 text-sm font-medium ${
              tab === key
                ? 'bg-brand-600 text-white'
                : 'border border-slate-300 text-slate-700 hover:bg-slate-100'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {FILTERABLE.includes(tab) && (
        <div className="mt-4">
          <OfferFilterBar filters={criteria} onChange={setCriteria} />
        </div>
      )}

      {error && <p className={`mt-4 ${errorBox}`}>{error}</p>}

      <div className="mt-4 rounded-xl border border-line bg-surface p-6">
        {loading && <p className="text-sm text-slate-500">Chargement…</p>}
        {!loading && items.length === 0 && (
          <p className="text-sm text-slate-500">
            {filtered ? 'Aucune offre ne correspond à vos critères.' : EMPTY_MESSAGE[tab]}
          </p>
        )}

        <ul className="space-y-3">
          {items.map(({ offer, score }) => (
            <li
              key={offer.id}
              className="flex flex-wrap items-center justify-between gap-4 rounded-lg border border-slate-200 p-4"
            >
              <div className="min-w-0">
                <p className="font-medium text-slate-900">
                  <Link to={`/interimaire/offres/${offer.id}`} className="hover:text-brand-600 hover:underline">
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
                  {offer.companyName} · {sectorLabel(offer.sector)} · {offer.city}
                  {salarySuffix(offer.salaryMin, offer.salaryMax)}
                </p>
                {offer.publishedAt && (
                  <p className="text-xs text-slate-400">Publiée le {formatTimestampDate(offer.publishedAt)}</p>
                )}
              </div>
              <button
                type="button"
                onClick={() => toggleFavorite(offer)}
                title={offer.favorite ? 'Retirer des favoris' : 'Ajouter aux favoris'}
                aria-label={offer.favorite ? 'Retirer des favoris' : 'Ajouter aux favoris'}
                className={`shrink-0 text-2xl leading-none ${
                  offer.favorite ? 'text-amber-400 hover:text-amber-500' : 'text-slate-300 hover:text-amber-400'
                }`}
              >
                ★
              </button>
            </li>
          ))}
        </ul>

        <Pagination page={pageData} onChange={goTo} label="offres" />
      </div>
    </section>
  );
}
