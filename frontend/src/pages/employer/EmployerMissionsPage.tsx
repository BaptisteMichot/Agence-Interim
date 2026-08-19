import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { getEmployerMissions } from '../../api/missions';
import EmptyState from '../../components/EmptyState';
import PageHeader from '../../components/PageHeader';
import { btnSecondary, card, errorBox, sectionTitle } from '../../components/ui';
import { useResource } from '../../hooks/useResource';
import { missionPeriod } from '../../missions/format';
import MissionListItem from '../../missions/MissionListItem';
import type { Mission } from '../../missions/types';

/** Référence stable pour la liste vide, comme l'état initial `[]` d'avant. */
const NO_MISSIONS: Mission[] = [];

/**
 * Missions abouties de l'employeur : celles en cours et celles arrivées à leur terme.
 * Les missions encore en discussion (agence ou candidat) restent sur l'accueil.
 */
export default function EmployerMissionsPage() {
  const { data, loading, error } = useResource(
    getEmployerMissions,
    'Impossible de charger les missions.',
  );
  const missions = data ?? NO_MISSIONS;

  const current = useMemo(
    () => missions.filter((m) => m.status === 'ACTIVE' && missionPeriod(m) !== 'past'),
    [missions],
  );
  const past = useMemo(
    () => missions.filter((m) => m.status === 'ACTIVE' && missionPeriod(m) === 'past'),
    [missions],
  );

  const candidate = (mission: Mission) => (
    <>
      {mission.candidateFirstName} {mission.candidateLastName} · offre « {mission.offerTitle} »
    </>
  );

  return (
    <section>
      <PageHeader
        title="Mes missions"
        subtitle="Missions acceptées par toutes les parties, en cours puis terminées."
      />

      {error && <p className={errorBox}>{error}</p>}

      <div className={`mt-6 ${card}`}>
        <h2 className={`mb-4 ${sectionTitle}`}>Missions en cours</h2>
        {loading && <p className="text-sm text-muted">Chargement…</p>}
        {!loading && current.length === 0 && (
          <EmptyState
            title="Aucune mission en cours"
            description="Les missions apparaissent ici une fois validées par l'agence et acceptées par le candidat."
          />
        )}
        <ul className="space-y-3">
          {current.map((mission) => (
            <MissionListItem
              key={mission.id}
              mission={mission}
              subtitle={candidate(mission)}
              showStatus={false}
            >
              <Link to={`/employeur/missions/${mission.id}/renouveler`} className={btnSecondary}>
                Renouveler
              </Link>
              <Link to={`/employeur/missions/${mission.id}`} className={btnSecondary}>
                Détail
              </Link>
            </MissionListItem>
          ))}
        </ul>
      </div>

      <div className={`mt-6 ${card}`}>
        <h2 className={`mb-4 ${sectionTitle}`}>Historique</h2>
        {!loading && past.length === 0 && (
          <p className="text-sm text-muted">Aucune mission terminée pour le moment.</p>
        )}
        <ul className="space-y-3">
          {past.map((mission) => (
            <MissionListItem
              key={mission.id}
              mission={mission}
              subtitle={candidate(mission)}
              showStatus={false}
            >
              <Link to={`/employeur/missions/${mission.id}/renouveler`} className={btnSecondary}>
                Renouveler
              </Link>
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
