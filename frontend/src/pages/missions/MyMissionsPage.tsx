import { Link } from 'react-router-dom';
import { getMyMissions } from '../../api/missions';
import PageHeader from '../../components/PageHeader';
import PagedSection from '../../components/PagedSection';
import { btnPrimary, btnSecondary, errorBox } from '../../components/ui';
import MissionListItem from '../../missions/MissionListItem';
import type { Mission } from '../../missions/types';

// Définis au niveau du module : leur identité est stable, ce que `PagedSection` exige.
const loadToConfirm = (page: number) => getMyMissions('to-confirm', page);
const loadWaiting = (page: number) => getMyMissions('waiting', page);
const loadConfirmed = (page: number) => getMyMissions('confirmed', page);
const loadHistory = (page: number) => getMyMissions('history', page);

/** Employeur et lieu de la mission, en sous-titre de chaque ligne. */
function company(mission: Mission) {
  return (
    <>
      {mission.employerCompanyName ?? `${mission.employerFirstName} ${mission.employerLastName}`} ·{' '}
      {mission.workplace}
    </>
  );
}

/**
 * Missions de l'intérimaire : propositions à confirmer, missions confirmées et historique.
 * Chaque bloc est une liste paginée à part, filtrée côté serveur.
 */
export default function MyMissionsPage() {
  return (
    <section className="space-y-6">
      <PageHeader
        title="Mes missions"
        subtitle="Les missions que vous acceptez apparaissent dans votre planning et donnent lieu à un contrat."
      />

      <PagedSection
        fetch={loadToConfirm}
        label="missions"
        loadError="Impossible de charger vos missions."
      >
        {({ items, total, error, pagination }) =>
          error ? (
            <p className={errorBox}>{error}</p>
          ) : total === 0 ? null : (
            <div className="rounded-xl border border-sky-200 bg-sky-50 p-6">
              <h2 className="mb-1 text-lg font-semibold text-sky-900">
                {total === 1
                  ? 'Une mission attend votre réponse'
                  : `${total} missions attendent votre réponse`}
              </h2>
              <p className="mb-4 text-sm text-sky-800">
                Consultez les conditions proposées, puis acceptez ou refusez.
              </p>
              <ul className="space-y-3">
                {items.map((mission) => (
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
              {pagination}
            </div>
          )
        }
      </PagedSection>

      <PagedSection
        fetch={loadWaiting}
        label="missions"
        loadError="Impossible de charger vos missions."
      >
        {({ items, total, pagination }) =>
          total === 0 ? null : (
            <div className="rounded-xl border border-line bg-surface p-6">
              <h2 className="mb-4 text-lg font-semibold text-slate-900">En attente de l'agence</h2>
              <ul className="space-y-3">
                {items.map((mission) => (
                  <MissionListItem key={mission.id} mission={mission} subtitle={company(mission)}>
                    <Link to={`/interimaire/missions/${mission.id}`} className={btnSecondary}>
                      Détail
                    </Link>
                  </MissionListItem>
                ))}
              </ul>
              {pagination}
            </div>
          )
        }
      </PagedSection>

      <PagedSection
        fetch={loadConfirmed}
        label="missions"
        loadError="Impossible de charger vos missions."
      >
        {({ items, loading, error, pagination }) => (
          <div className="rounded-xl border border-line bg-surface p-6">
            <h2 className="mb-4 text-lg font-semibold text-slate-900">Missions confirmées</h2>
            {error && <p className={errorBox}>{error}</p>}
            {loading && <p className="text-sm text-slate-500">Chargement…</p>}
            {!loading && items.length === 0 && (
              <p className="text-sm text-slate-500">Aucune mission confirmée pour le moment.</p>
            )}
            <ul className="space-y-3">
              {items.map((mission) => (
                <MissionListItem key={mission.id} mission={mission} subtitle={company(mission)}>
                  <Link to={`/interimaire/missions/${mission.id}`} className={btnSecondary}>
                    Détail et contrat
                  </Link>
                </MissionListItem>
              ))}
            </ul>
            {pagination}
          </div>
        )}
      </PagedSection>

      <PagedSection
        fetch={loadHistory}
        label="missions"
        loadError="Impossible de charger vos missions."
      >
        {({ items, total, pagination }) =>
          total === 0 ? null : (
            <div className="rounded-xl border border-line bg-surface p-6">
              <h2 className="mb-4 text-lg font-semibold text-slate-900">Historique</h2>
              <ul className="space-y-3">
                {items.map((mission) => (
                  <MissionListItem key={mission.id} mission={mission} subtitle={company(mission)}>
                    <Link to={`/interimaire/missions/${mission.id}`} className={btnSecondary}>
                      Détail
                    </Link>
                  </MissionListItem>
                ))}
              </ul>
              {pagination}
            </div>
          )
        }
      </PagedSection>
    </section>
  );
}
