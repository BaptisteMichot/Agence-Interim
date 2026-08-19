import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getApplicationCounts } from '../../api/applications';
import { getMyCompany } from '../../api/employer';
import { getEmployerMissions } from '../../api/missions';
import { getMyOffers } from '../../api/offers';
import { useAuth } from '../../auth/AuthContext';
import EmptyState from '../../components/EmptyState';
import PageHeader from '../../components/PageHeader';
import { Skeleton } from '../../components/Skeleton';
import StatTile from '../../components/StatTile';
import { card, sectionTitle, warningBox } from '../../components/ui';
import MissionStatusBadge from '../../missions/MissionStatusBadge';
import type { Mission } from '../../missions/types';
import { formatDate } from '../../profile/format';

/** Missions dont l'issue dépend encore de l'agence ou du candidat. */
const AWAITING = ['PENDING', 'APPROVED', 'RENEWAL'];

/** Missions écartées, par l'agence ou par le candidat : elles appellent une reprise. */
const REJECTED = ['REFUSED', 'DECLINED'];

interface HomeData {
  openOffers: number;
  applications: number;
  waiting: Mission[];
  rejected: Mission[];
  companyIncomplete: boolean;
}

/** Accueil de l'employeur : l'état de ses offres et les missions non abouties. */
export default function EmployerDashboard() {
  const { user } = useAuth();
  const [data, setData] = useState<HomeData | null>(null);

  useEffect(() => {
    // Valeurs neutres en cas d'échec : un service indisponible ne vide pas tout l'accueil.
    Promise.all([
      getMyOffers().catch(() => []),
      getApplicationCounts().catch(() => ({}) as Record<string, number>),
      getEmployerMissions().catch(() => []),
      getMyCompany()
        .then((company) => company.incomplete)
        .catch(() => false),
    ]).then(([offers, counts, missions, companyIncomplete]) =>
      setData({
        openOffers: offers.filter((offer) => offer.status === 'OPEN').length,
        applications: Object.values(counts).reduce((total, count) => total + count, 0),
        waiting: missions.filter((mission) => AWAITING.includes(mission.status)),
        rejected: missions.filter((mission) => REJECTED.includes(mission.status)),
        companyIncomplete,
      }),
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
              value={data.waiting.length}
              hint={data.waiting.length > 0 ? 'À suivre ci-dessous' : 'Rien en attente'}
              to="/employeur/missions"
              highlight={data.waiting.length > 0}
            />
          </>
        )}
      </div>

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

        {data === null && <Skeleton className="h-20 w-full" />}

        {data !== null && data.waiting.length === 0 && (
          <EmptyState
            title="Aucune mission en attente"
            description="Sélectionnez un candidat depuis les candidatures d'une offre pour lui proposer une mission."
          />
        )}

        {data !== null && data.waiting.length > 0 && <MissionRows missions={data.waiting} />}
      </section>

      <section className={`mt-6 ${card}`}>
        <h2 className={`mb-4 ${sectionTitle}`}>Missions refusées</h2>

        {data === null && <Skeleton className="h-20 w-full" />}

        {data !== null && data.rejected.length === 0 && (
          <p className="text-sm text-muted">Aucune mission refusée.</p>
        )}

        {data !== null && data.rejected.length > 0 && (
          <>
            <p className="mb-3 text-sm text-muted">
              Une mission refusée par l'agence peut être corrigée ; refusée par le candidat, elle
              remet l'offre en ligne avec les candidatures déjà reçues.
            </p>
            <MissionRows missions={data.rejected} />
          </>
        )}
      </section>
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
