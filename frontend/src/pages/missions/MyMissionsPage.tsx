import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { getMyMissions } from '../../api/missions';
import { btnPrimary, btnSecondary, errorBox, linkBack } from '../../components/ui';
import { useResource } from '../../hooks/useResource';
import { missionPeriod } from '../../missions/format';
import MissionListItem from '../../missions/MissionListItem';
import type { Mission } from '../../missions/types';

/** Référence stable pour la liste vide, comme l'état initial `[]` d'avant. */
const NO_MISSIONS: Mission[] = [];

/** Missions de l'intérimaire : propositions à confirmer, missions confirmées et historique. */
export default function MyMissionsPage() {
  const { data, loading, error } = useResource(getMyMissions, 'Impossible de charger tes missions.');
  const missions = data ?? NO_MISSIONS;

  const toConfirm = useMemo(
    () => missions.filter((m) => m.status === 'APPROVED' || m.status === 'RENEWAL'),
    [missions],
  );
  const confirmed = useMemo(
    () => missions.filter((m) => m.status === 'ACTIVE' && missionPeriod(m) !== 'past'),
    [missions],
  );
  const history = useMemo(
    () =>
      missions.filter(
        (m) =>
          m.status === 'DECLINED' || (m.status === 'ACTIVE' && missionPeriod(m) === 'past'),
      ),
    [missions],
  );
  const waitingAgency = useMemo(() => missions.filter((m) => m.status === 'PENDING'), [missions]);

  const company = (mission: Mission) => (
    <>
      {mission.employerCompanyName ?? `${mission.employerFirstName} ${mission.employerLastName}`} ·{' '}
      {mission.workplace}
    </>
  );

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <Link to="/interimaire" className={linkBack}>
            ← Retour à mon espace
          </Link>
          <h1 className="mt-2 text-2xl font-semibold text-slate-900">Mes missions</h1>
          <p className="mt-1 text-slate-600">
            Les missions que tu acceptes apparaissent dans ton planning et donnent lieu à un contrat.
          </p>
        </div>
        <Link to="/interimaire/planning" className={btnSecondary}>
          Mon planning
        </Link>
      </div>

      {error && <p className={errorBox}>{error}</p>}
      {loading && <p className="text-sm text-slate-500">Chargement…</p>}

      {toConfirm.length > 0 && (
        <div className="rounded-xl border border-sky-200 bg-sky-50 p-6">
          <h2 className="mb-1 text-lg font-semibold text-sky-900">
            {toConfirm.length === 1
              ? 'Une mission attend ta réponse'
              : `${toConfirm.length} missions attendent ta réponse`}
          </h2>
          <p className="mb-4 text-sm text-sky-800">
            Consulte les conditions proposées, puis accepte ou refuse.
          </p>
          <ul className="space-y-3">
            {toConfirm.map((mission) => (
              <MissionListItem
                key={mission.id}
                mission={mission}
                subtitle={company(mission)}
                className="bg-white"
              >
                <Link to={`/interimaire/missions/${mission.id}`} className={btnPrimary}>
                  Voir et répondre
                </Link>
              </MissionListItem>
            ))}
          </ul>
        </div>
      )}

      {waitingAgency.length > 0 && (
        <div className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-4 text-lg font-semibold text-slate-900">En attente de l'agence</h2>
          <ul className="space-y-3">
            {waitingAgency.map((mission) => (
              <MissionListItem key={mission.id} mission={mission} subtitle={company(mission)}>
                <Link to={`/interimaire/missions/${mission.id}`} className={btnSecondary}>
                  Détail
                </Link>
              </MissionListItem>
            ))}
          </ul>
        </div>
      )}

      <div className="rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="mb-4 text-lg font-semibold text-slate-900">Missions confirmées</h2>
        {!loading && confirmed.length === 0 && (
          <p className="text-sm text-slate-500">Aucune mission confirmée pour le moment.</p>
        )}
        <ul className="space-y-3">
          {confirmed.map((mission) => (
            <MissionListItem key={mission.id} mission={mission} subtitle={company(mission)}>
              <Link to={`/interimaire/missions/${mission.id}`} className={btnSecondary}>
                Détail et contrat
              </Link>
            </MissionListItem>
          ))}
        </ul>
      </div>

      {history.length > 0 && (
        <div className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-4 text-lg font-semibold text-slate-900">Historique</h2>
          <ul className="space-y-3">
            {history.map((mission) => (
              <MissionListItem key={mission.id} mission={mission} subtitle={company(mission)}>
                <Link to={`/interimaire/missions/${mission.id}`} className={btnSecondary}>
                  Détail
                </Link>
              </MissionListItem>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}
