import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getReceivedApplicationCount } from '../../api/applications';
import { getMyCompany } from '../../api/employer';
import { getEmployerAwaitingMissionCount, getEmployerMissions } from '../../api/missions';
import { getOpenOfferCount } from '../../api/offers';
import { useAuth } from '../../auth/AuthContext';
import EmptyState from '../../components/EmptyState';
import PageHeader from '../../components/PageHeader';
import PagedSection from '../../components/PagedSection';
import { Skeleton } from '../../components/Skeleton';
import StatTile from '../../components/StatTile';
import { card, errorBox, sectionTitle, warningBox } from '../../components/ui';
import MissionStatusBadge from '../../missions/MissionStatusBadge';
import type { Mission } from '../../missions/types';
import { formatDate } from '../../profile/format';

// Définis au niveau du module : leur identité est stable, ce que `PagedSection` exige.
const loadAwaiting = (page: number) => getEmployerMissions('awaiting', page);
const loadRejected = (page: number) => getEmployerMissions('rejected', page);

interface HomeData {
  openOffers: number;
  applications: number;
  awaiting: number;
  companyIncomplete: boolean;
}

/** Accueil de l'employeur : l'état de ses offres et les missions non abouties. */
export default function EmployerDashboard() {
  const { user } = useAuth();
  const [data, setData] = useState<HomeData | null>(null);

  useEffect(() => {
    // Les chiffres viennent de comptages dédiés, pas des listes : une page de dix
    // missions ne dirait rien du total. Valeurs neutres en cas d'échec, pour qu'un
    // service indisponible ne vide pas tout l'accueil.
    Promise.all([
      getOpenOfferCount().catch(() => 0),
      getReceivedApplicationCount().catch(() => 0),
      getEmployerAwaitingMissionCount().catch(() => 0),
      getMyCompany()
        .then((company) => company.incomplete)
        .catch(() => false),
    ]).then(([openOffers, applications, awaiting, companyIncomplete]) =>
      setData({ openOffers, applications, awaiting, companyIncomplete }),
    );
  }, []);

  return (
    <>
      <PageHeader
        title={`Bonjour ${user?.firstName ?? ''}`}
        subtitle="Voici l'état de vos offres et de vos missions."
      />

      {data?.companyIncomplete && (
        <p className={`mb-6 ${warningBox}`}>
          Votre fiche entreprise est incomplète : tant qu'une mention manque, vous ne pouvez pas
          proposer de mission à un candidat.{' '}
          <Link to="/employeur/entreprise" className="font-medium underline">
            Compléter ma fiche →
          </Link>
        </p>
      )}

      <div className="grid gap-4 sm:grid-cols-3">
        {data === null ? (
          Array.from({ length: 3 }, (_, index) => <Skeleton key={index} className="h-28 w-full" />)
        ) : (
          <>
            <StatTile label="Offres ouvertes" value={data.openOffers} to="/employeur/offres" />
            <StatTile
              label="Candidatures reçues"
              value={data.applications}
              hint="Toutes offres confondues"
              to="/employeur/offres"
            />
            <StatTile
              label="Missions en attente"
              value={data.awaiting}
              hint={data.awaiting > 0 ? 'À suivre ci-dessous' : 'Rien en attente'}
              to="/employeur/missions"
              highlight={data.awaiting > 0}
            />
          </>
        )}
      </div>

      <PagedSection
        fetch={loadAwaiting}
        label="missions"
        loadError="Impossible de charger les missions."
      >
        {({ items, loading, error, pagination }) => (
          <section className={`mt-6 ${card}`}>
            <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
              <h2 className={sectionTitle}>Missions en cours de décision</h2>
              <Link
                to="/employeur/missions"
                className="text-sm font-medium text-brand-600 hover:underline"
              >
                Toutes mes missions →
              </Link>
            </div>

            {error && <p className={errorBox}>{error}</p>}
            {loading && <Skeleton className="h-20 w-full" />}
            {!loading && items.length === 0 && (
              <EmptyState
                title="Aucune mission en attente"
                description="Sélectionnez un candidat depuis les candidatures d'une offre pour lui proposer une mission."
              />
            )}
            {items.length > 0 && <MissionRows missions={items} />}
            {pagination}
          </section>
        )}
      </PagedSection>

      <PagedSection
        fetch={loadRejected}
        label="missions"
        loadError="Impossible de charger les missions."
      >
        {({ items, loading, pagination }) => (
          <section className={`mt-6 ${card}`}>
            <h2 className={`mb-4 ${sectionTitle}`}>Missions refusées</h2>

            {loading && <Skeleton className="h-20 w-full" />}
            {!loading && items.length === 0 && (
              <p className="text-sm text-muted">Aucune mission refusée.</p>
            )}
            {items.length > 0 && (
              <>
                <p className="mb-3 text-sm text-muted">
                  Une mission refusée par l'agence peut être corrigée ; refusée par le candidat, elle
                  remet l'offre en ligne avec les candidatures déjà reçues.
                </p>
                <MissionRows missions={items} />
              </>
            )}
            {pagination}
          </section>
        )}
      </PagedSection>
    </>
  );
}

/** Lignes compactes d'une liste de missions : identité, période, état et accès au détail. */
function MissionRows({ missions }: { missions: Mission[] }) {
  return (
    <ul className="divide-y divide-line">
      {missions.map((mission) => (
        <li key={mission.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
          <div>
            <p className="text-sm font-medium text-ink">
              {mission.position} — {mission.candidateFirstName} {mission.candidateLastName}
            </p>
            <p className="text-sm text-muted">
              du {formatDate(mission.startDate)} au {formatDate(mission.endDate)}
            </p>
          </div>
          <div className="flex items-center gap-3">
            <MissionStatusBadge status={mission.status} renewal={mission.renewal} />
            <Link
              to={`/employeur/missions/${mission.id}`}
              className="text-sm font-medium text-brand-600 hover:underline"
            >
              Détail →
            </Link>
          </div>
        </li>
      ))}
    </ul>
  );
}
