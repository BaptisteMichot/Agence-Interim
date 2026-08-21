import { Link } from 'react-router-dom';
import { getEmployerMissions } from '../../api/missions';
import EmptyState from '../../components/EmptyState';
import PageHeader from '../../components/PageHeader';
import PagedSection from '../../components/PagedSection';
import { btnSecondary, card, errorBox, sectionTitle } from '../../components/ui';
import MissionListItem from '../../missions/MissionListItem';
import type { Mission } from '../../missions/types';

// Définis au niveau du module : leur identité est stable, ce que `PagedSection` exige.
const loadCurrent = (page: number) => getEmployerMissions('current', page);
const loadPast = (page: number) => getEmployerMissions('past', page);

/** Candidat et offre concernés, en sous-titre de chaque ligne. */
function candidate(mission: Mission) {
  return (
    <>
      {mission.candidateFirstName} {mission.candidateLastName} · offre « {mission.offerTitle} »
    </>
  );
}

/** Actions communes aux deux blocs : renouveler la mission ou consulter son détail. */
function MissionActions({ missionId }: { missionId: number }) {
  return (
    <>
      <Link to={`/employeur/missions/${missionId}/renouveler`} className={btnSecondary}>
        Renouveler
      </Link>
      <Link to={`/employeur/missions/${missionId}`} className={btnSecondary}>
        Détail
      </Link>
    </>
  );
}

/**
 * Missions abouties de l'employeur : celles en cours et celles arrivées à leur terme.
 * Les missions encore en discussion (agence ou candidat) restent sur l'accueil.
 */
export default function EmployerMissionsPage() {
  return (
    <section>
      <PageHeader
        title="Mes missions"
        subtitle="Missions acceptées par toutes les parties, en cours puis terminées."
      />

      <PagedSection
        fetch={loadCurrent}
        label="missions"
        loadError="Impossible de charger les missions."
      >
        {({ items, loading, error, pagination }) => (
          <div className={`mt-6 ${card}`}>
            <h2 className={`mb-4 ${sectionTitle}`}>Missions en cours</h2>
            {error && <p className={errorBox}>{error}</p>}
            {loading && <p className="text-sm text-muted">Chargement…</p>}
            {!loading && items.length === 0 && (
              <EmptyState
                title="Aucune mission en cours"
                description="Les missions apparaissent ici une fois validées par l'agence et acceptées par le candidat."
              />
            )}
            <ul className="space-y-3">
              {items.map((mission) => (
                <MissionListItem
                  key={mission.id}
                  mission={mission}
                  subtitle={candidate(mission)}
                  showStatus={false}
                >
                  <MissionActions missionId={mission.id} />
                </MissionListItem>
              ))}
            </ul>
            {pagination}
          </div>
        )}
      </PagedSection>

      <PagedSection fetch={loadPast} label="missions" loadError="Impossible de charger les missions.">
        {({ items, loading, pagination }) => (
          <div className={`mt-6 ${card}`}>
            <h2 className={`mb-4 ${sectionTitle}`}>Historique</h2>
            {!loading && items.length === 0 && (
              <p className="text-sm text-muted">Aucune mission terminée pour le moment.</p>
            )}
            <ul className="space-y-3">
              {items.map((mission) => (
                <MissionListItem
                  key={mission.id}
                  mission={mission}
                  subtitle={candidate(mission)}
                  showStatus={false}
                >
                  <MissionActions missionId={mission.id} />
                </MissionListItem>
              ))}
            </ul>
            {pagination}
          </div>
        )}
      </PagedSection>
    </section>
  );
}
