import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { getMyMissions } from '../../api/missions';
import PageHeader from '../../components/PageHeader';
import { btnPrimary, btnSecondary, errorBox } from '../../components/ui';
import { useResource } from '../../hooks/useResource';
import { missionPeriod } from '../../missions/format';
import MissionListItem from '../../missions/MissionListItem';
import type { Mission } from '../../missions/types';

/** Référence stable pour la liste vide, comme l'état initial `[]` d'avant. */
const NO_MISSIONS: Mission[] = [];

/** Missions de l'intérimaire : propositions à confirmer, missions confirmées et historique. */
export default function MyMissionsPage() {
  const { data, loading, error } = useResource(getMyMissions, 'Impossible de charger vos missions.');
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
      <PageHeader
        title="Mes missions"
        subtitle="Les missions que vous acceptez apparaissent dans votre planning et donnent lieu à un contrat."
      />

      {error && <p className={errorBox}>{error}</p>}
      {loading && <p className="text-sm text-slate-500">Chargement…</p>}

      {toConfirm.length > 0 && (
        <div className="rounded-xl border border-sky-200 bg-sky-50 p-6">
          <h2 className="mb-1 text-lg font-semibold text-sky-900">
            {toConfirm.length === 1
              ? 'Une mission attend votre réponse'
              : `${toConfirm.length} missions attendent votre réponse`}
          </h2>
          <p className="mb-4 text-sm text-sky-800">
            Consultez les conditions proposées, puis acceptez ou refusez.
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
        <div className="rounded-xl border border-line bg-surface p-6">
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

      <div className="rounded-xl border border-line bg-surface p-6">
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
        <div className="rounded-xl border border-line bg-surface p-6">
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
