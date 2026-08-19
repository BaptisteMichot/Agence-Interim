import { useCallback } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { downloadCandidateCv, getCandidateProfile } from '../../api/applications';
import { openConversationForApplication } from '../../api/chat';
import { errorMessage } from '../../api/http';
import { btnPrimary, btnSecondary, errorBox, linkBack } from '../../components/ui';
import { useResource } from '../../hooks/useResource';
import {
  degreeTypeLabel,
  formatDate,
  formationStatusLabel,
  skillLevelLabel,
} from '../../profile/format';

/** Profil complet d'un candidat, consulté par l'employeur depuis une candidature. */
export default function CandidateProfilePage() {
  const { id } = useParams();
  const applicationId = Number(id);
  const navigate = useNavigate();

  const fetcher = useCallback(() => getCandidateProfile(applicationId), [applicationId]);
  const {
    data: profile,
    loading,
    error,
    setError,
  } = useResource(fetcher, 'Impossible de charger le profil.');

  const openCv = async () => {
    setError(null);
    try {
      const blob = await downloadCandidateCv(applicationId);
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank', 'noopener');
    } catch (err) {
      setError(errorMessage(err, 'Une erreur est survenue.'));
    }
  };

  /** Ouvre (ou retrouve) le fil de discussion avec ce candidat, puis y navigue. */
  const startChat = async () => {
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

  if (!profile) {
    return <p className={errorBox}>{error ?? 'Candidat introuvable.'}</p>;
  }

  return (
    <div className="space-y-6">
      <div>
        <button
          type="button"
          onClick={() => navigate(-1)}
          className={linkBack}
        >
          ← Retour aux candidatures
        </button>
        <div className="mt-2 flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold text-slate-900">
              {profile.firstName} {profile.lastName}
            </h1>
            <p className="mt-1 text-slate-600">{profile.email}</p>
            <p className="text-sm text-slate-500">
              {profile.birthdate && <>Né(e) le {formatDate(profile.birthdate)} · </>}
              Véhicule : {profile.hasVehicle ? 'oui' : 'non'}
            </p>
          </div>
          <div className="flex shrink-0 flex-wrap gap-2">
            {profile.hasCv && (
              <button type="button" className={btnSecondary} onClick={openCv}>
                📄 Consulter le CV
              </button>
            )}
            <button type="button" className={btnSecondary} onClick={startChat}>
              💬 Discuter
            </button>
            <Link to={`/employeur/candidatures/${applicationId}/mission`} className={btnPrimary}>
              Sélectionner ce candidat
            </Link>
          </div>
        </div>
      </div>

      {error && <p className={errorBox}>{error}</p>}

      {(profile.skills.length > 0 || profile.degrees.length > 0 || profile.languages.length > 0) && (
        <section className="rounded-xl border border-line bg-surface p-6">
          <h2 className="mb-4 text-lg font-semibold text-slate-900">Compétences et diplômes</h2>

          {profile.skills.length > 0 && (
            <div className="mb-4">
              <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">Compétences</h3>
              <ul className="flex flex-wrap gap-2">
                {profile.skills.map((s) => (
                  <li key={s.skillId} className="rounded-full bg-brand-50 px-3 py-1 text-sm text-brand-700">
                    {s.name} · {skillLevelLabel(s.level)}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {profile.degrees.length > 0 && (
            <div className="mb-4">
              <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">Diplômes</h3>
              <ul className="flex flex-wrap gap-2">
                {profile.degrees.map((d) => (
                  <li key={d.degreeId} className="rounded-full bg-emerald-50 px-3 py-1 text-sm text-emerald-800">
                    {degreeTypeLabel(d.type)} — {d.section}
                    {d.institution && ` (${d.institution}${d.graduationYear ? `, ${d.graduationYear}` : ''})`}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {profile.languages.length > 0 && (
            <div>
              <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">Langues</h3>
              <ul className="flex flex-wrap gap-2">
                {profile.languages.map((l) => (
                  <li key={l.languageId} className="rounded-full bg-amber-50 px-3 py-1 text-sm text-amber-800">
                    {l.name} · {l.level}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </section>
      )}

      {profile.experiences.length > 0 && (
        <section className="rounded-xl border border-line bg-surface p-6">
          <h2 className="mb-4 text-lg font-semibold text-slate-900">Expériences professionnelles</h2>
          <ul className="space-y-3">
            {profile.experiences.map((e) => (
              <li key={e.id} className="rounded-lg border border-slate-200 p-4">
                <p className="font-medium text-slate-900">
                  {e.position} — {e.companyName}
                </p>
                <p className="text-sm text-slate-500">
                  {formatDate(e.startDate)} → {e.endDate ? formatDate(e.endDate) : "aujourd'hui"}
                </p>
              </li>
            ))}
          </ul>
        </section>
      )}

      {profile.formations.length > 0 && (
        <section className="rounded-xl border border-line bg-surface p-6">
          <h2 className="mb-4 text-lg font-semibold text-slate-900">Formations</h2>
          <ul className="space-y-3">
            {profile.formations.map((f) => (
              <li key={f.id} className="rounded-lg border border-slate-200 p-4">
                <p className="font-medium text-slate-900">
                  {f.title} — {f.institution}
                  <span className="ml-2 rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
                    {formationStatusLabel(f.status)}
                  </span>
                </p>
                <p className="text-sm text-slate-500">
                  {formatDate(f.startDate)} → {f.endDate ? formatDate(f.endDate) : "aujourd'hui"}
                </p>
              </li>
            ))}
          </ul>
        </section>
      )}

      {profile.skills.length === 0 &&
        profile.degrees.length === 0 &&
        profile.languages.length === 0 &&
        profile.experiences.length === 0 &&
        profile.formations.length === 0 && (
          <p className="text-sm text-slate-500">
            Ce candidat n'a pas encore complété son profil.{' '}
            <Link to="/employeur/offres" className="text-brand-600 hover:underline">
              Retour à mes offres
            </Link>
          </p>
        )}
    </div>
  );
}
