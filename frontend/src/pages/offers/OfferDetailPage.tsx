import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  addFavoriteOffer,
  getFavoriteOfferIds,
  getOfferDetail,
  removeFavoriteOffer,
} from '../../api/offers';
import { btnSecondary, errorBox } from '../../components/ui';
import type { JobOfferDetail } from '../../offers/types';
import { degreeTypeLabel, formatDate, skillLevelLabel } from '../../profile/format';

/** Détail d'une offre pour l'intérimaire (exigences + favori). Postuler viendra à l'incrément 7. */
export default function OfferDetailPage() {
  const { id } = useParams();
  const offerId = Number(id);

  const [offer, setOffer] = useState<JobOfferDetail | null>(null);
  const [favorite, setFavorite] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setError(null);
    try {
      const [detail, ids] = await Promise.all([getOfferDetail(offerId), getFavoriteOfferIds()]);
      setOffer(detail);
      setFavorite(ids.includes(offerId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Impossible de charger l'offre.");
    } finally {
      setLoading(false);
    }
  }, [offerId]);

  useEffect(() => {
    reload();
  }, [reload]);

  const toggleFavorite = async () => {
    setError(null);
    try {
      if (favorite) {
        await removeFavoriteOffer(offerId);
      } else {
        await addFavoriteOffer(offerId);
      }
      setFavorite((v) => !v);
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
    <div className="space-y-6">
      <div>
        <Link to="/interimaire/offres" className="text-sm text-indigo-600 hover:underline">
          ← Retour aux offres
        </Link>
        <div className="mt-2 flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold text-slate-900">
              {offer.title}
              {offer.status === 'CLOSED' && (
                <span className="ml-3 rounded-full bg-slate-200 px-2 py-0.5 text-sm font-medium text-slate-600">
                  Clôturée
                </span>
              )}
            </h1>
            <p className="mt-1 text-slate-600">
              {offer.companyName} · {offer.sector} · {offer.city}
            </p>
            {offer.publishedAt && (
              <p className="text-xs text-slate-400">Publiée le {formatDate(offer.publishedAt.slice(0, 10))}</p>
            )}
          </div>
          {offer.status === 'OPEN' && (
            <button type="button" className={btnSecondary} onClick={toggleFavorite}>
              {favorite ? '★ Retirer des favoris' : '☆ Ajouter aux favoris'}
            </button>
          )}
        </div>
      </div>

      {error && <p className={errorBox}>{error}</p>}

      <section className="rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="mb-3 text-lg font-semibold text-slate-900">Description</h2>
        <p className="whitespace-pre-line text-slate-700">{offer.description}</p>

        <dl className="mt-4 grid gap-2 text-sm text-slate-600 sm:grid-cols-2">
          {(offer.salaryMin !== null || offer.salaryMax !== null) && (
            <div>
              <dt className="font-medium text-slate-700">Salaire</dt>
              <dd>
                {offer.salaryMin ?? '?'} – {offer.salaryMax ?? '?'} €/h
              </dd>
            </div>
          )}
          {offer.experienceTime && (
            <div>
              <dt className="font-medium text-slate-700">Expérience minimum</dt>
              <dd>{offer.experienceTime} an(s)</dd>
            </div>
          )}
          <div>
            <dt className="font-medium text-slate-700">Véhicule</dt>
            <dd>{offer.vehicleMandatory ? 'Obligatoire' : 'Non requis'}</dd>
          </div>
        </dl>
      </section>

      {(offer.skills.length > 0 || offer.degrees.length > 0 || offer.languages.length > 0) && (
        <section className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-4 text-lg font-semibold text-slate-900">Profil recherché</h2>

          {offer.skills.length > 0 && (
            <div className="mb-4">
              <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">Compétences</h3>
              <ul className="flex flex-wrap gap-2">
                {offer.skills.map((s) => (
                  <li
                    key={s.skillId}
                    className="rounded-full bg-indigo-50 px-3 py-1 text-sm text-indigo-800"
                  >
                    {s.name} · {skillLevelLabel(s.requiredLevel)}
                    {s.isMandatory && <span className="ml-1 font-semibold" title="Obligatoire">*</span>}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {offer.degrees.length > 0 && (
            <div className="mb-4">
              <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">Diplômes</h3>
              <ul className="flex flex-wrap gap-2">
                {offer.degrees.map((d) => (
                  <li key={d.degreeId} className="rounded-full bg-emerald-50 px-3 py-1 text-sm text-emerald-800">
                    {degreeTypeLabel(d.type)} — {d.section}
                    {d.isMandatory && <span className="ml-1 font-semibold" title="Obligatoire">*</span>}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {offer.languages.length > 0 && (
            <div className="mb-2">
              <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">Langues</h3>
              <ul className="flex flex-wrap gap-2">
                {offer.languages.map((l) => (
                  <li key={l.languageId} className="rounded-full bg-amber-50 px-3 py-1 text-sm text-amber-800">
                    {l.name} · {l.requiredLevel}
                    {l.isMandatory && <span className="ml-1 font-semibold" title="Obligatoire">*</span>}
                  </li>
                ))}
              </ul>
            </div>
          )}

          <p className="mt-2 text-xs text-slate-400">* exigence obligatoire</p>
        </section>
      )}
    </div>
  );
}
