import { Link } from 'react-router-dom';
import { getEmployerMissions } from '../../api/missions';
import { btnSecondary, errorBox, linkBack } from '../../components/ui';
import { useResource } from '../../hooks/useResource';
import MissionListItem from '../../missions/MissionListItem';

/** Suivi des missions proposées par l'employeur, de la proposition au contrat. */
export default function EmployerMissionsPage() {
  const { data, loading, error } = useResource(
    getEmployerMissions,
    'Impossible de charger les missions.',
  );
  const missions = data ?? [];

  return (
    <section>
      <Link to="/employeur" className={linkBack}>
        ← Retour à mes offres
      </Link>
      <h1 className="mt-2 text-2xl font-semibold text-slate-900">Mes missions</h1>
      <p className="mt-1 text-slate-600">
        Missions proposées à vos candidats retenus, leur validation par l'agence et leurs contrats.
      </p>

      {error && <p className={`mt-4 ${errorBox}`}>{error}</p>}

      <div className="mt-6 rounded-xl border border-slate-200 bg-white p-6">
        {loading && <p className="text-sm text-slate-500">Chargement…</p>}
        {!loading && missions.length === 0 && (
          <p className="text-sm text-slate-500">
            Aucune mission. Sélectionnez un candidat depuis les candidatures d'une offre pour lui
            proposer une mission.
          </p>
        )}
        <ul className="space-y-3">
          {missions.map((mission) => (
            <MissionListItem
              key={mission.id}
              mission={mission}
              subtitle={
                <>
                  {mission.candidateFirstName} {mission.candidateLastName} · offre «{' '}
                  {mission.offerTitle} »
                </>
              }
            >
              {mission.status === 'REFUSED' && (
                <Link to={`/employeur/missions/${mission.id}/corriger`} className={btnSecondary}>
                  Corriger
                </Link>
              )}
              {mission.status === 'ACTIVE' && (
                <Link to={`/employeur/missions/${mission.id}/renouveler`} className={btnSecondary}>
                  Renouveler
                </Link>
              )}
              <Link to={`/employeur/missions/${mission.id}`} className={btnSecondary}>
                Détail
              </Link>
            </MissionListItem>
          ))}
        </ul>
      </div>
    </section>
  );
}
