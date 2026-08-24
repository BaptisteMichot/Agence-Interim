import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getPendingApplicationCount } from '../../api/applications';
import { getCompletedMissionCount, getMissionDecisionCount } from '../../api/missions';
import { getMatchingOffers } from '../../api/offers';
import { getSchedule } from '../../api/planning';
import { getProfile } from '../../api/profile';
import { useAuth } from '../../auth/AuthContext';
import EmptyState from '../../components/EmptyState';
import PageHeader from '../../components/PageHeader';
import { Skeleton } from '../../components/Skeleton';
import StatTile from '../../components/StatTile';
import { btnPrimary, card, sectionTitle, warningBox } from '../../components/ui';
import { addDays, shortTime, todayIso } from '../../missions/format';
import type { ScheduleEntry } from '../../missions/types';
import { NO_FILTERS } from '../../offers/format';
import { formatDate } from '../../profile/format';

interface HomeData {
  decisions: number;
  applications: number;
  matches: number;
  nextDays: ScheduleEntry[];
  /** Missions acceptées dont la période est écoulée. */
  completed: number;
  /** Vrai tant qu'une mention du contrat manque : aucune mission ne peut être acceptée. */
  contractInfoMissing: boolean;
}

/** Accueil de l'intérimaire : ce qui attend une réponse, puis ses chiffres du moment. */
export default function JobSeekerDashboard() {
  const { user } = useAuth();
  const [data, setData] = useState<HomeData | null>(null);

  useEffect(() => {
    const from = todayIso();
    // Chaque appel retombe sur une valeur neutre : un service indisponible ne doit pas
    // priver l'utilisateur du reste de son accueil.
    // Les chiffres viennent de comptages dédiés ou du total d'une page : depuis la
    // pagination, la longueur d'une liste ne vaut plus pour un total.
    Promise.all([
      getMissionDecisionCount().catch(() => ({ count: 0 })),
      getPendingApplicationCount().catch(() => 0),
      getMatchingOffers(0, NO_FILTERS).catch(() => ({ totalElements: 0 })),
      getSchedule(from, addDays(from, 30)).catch(() => []),
      getCompletedMissionCount().catch(() => 0),
      getProfile()
        .then((profile) => !profile.address || !profile.nationalNumber || !profile.iban)
        .catch(() => false),
    ]).then(([decision, applications, matches, schedule, completed, contractInfoMissing]) =>
      setData({
        decisions: decision.count,
        applications,
        matches: matches.totalElements,
        nextDays: schedule.slice(0, 4),
        completed,
        contractInfoMissing,
      }),
    );
  }, []);

  return (
    <>
      <PageHeader
        title={`Bonjour ${user?.firstName ?? ''}`}
        subtitle="Voici où en sont vos candidatures et vos missions."
      />

      {data?.contractInfoMissing && (
        <p className={`mb-6 ${warningBox}`}>
          Votre adresse, votre numéro de registre national ou votre numéro de compte manquent : vous
          ne pourrez pas accepter de mission tant qu'ils ne sont pas renseignés.{' '}
          <Link to="/interimaire/profil" className="font-medium underline">
            Compléter mon profil →
          </Link>
        </p>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {data === null ? (
          Array.from({ length: 4 }, (_, index) => <Skeleton key={index} className="h-28 w-full" />)
        ) : (
          <>
            <StatTile
              label="Missions à confirmer"
              value={data.decisions}
              hint={data.decisions > 0 ? 'Une réponse est attendue' : 'Rien en attente'}
              to="/interimaire/missions"
              highlight={data.decisions > 0}
            />
            <StatTile
              label="Candidatures en cours"
              value={data.applications}
              to="/interimaire/candidatures"
            />
            <StatTile
              label="Offres correspondant à votre profil"
              value={data.matches}
              to="/interimaire/offres"
            />
            <StatTile
              label="Missions complétées"
              value={data.completed}
              hint="Depuis votre inscription"
            />
          </>
        )}
      </div>

      <section className={`mt-6 ${card}`}>
        <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
          <h2 className={sectionTitle}>Vos prochaines journées</h2>
          <Link to="/interimaire/planning" className="text-sm font-medium text-brand-600 hover:underline">
            Ouvrir mon planning →
          </Link>
        </div>

        {data === null && <Skeleton className="h-20 w-full" />}

        {data !== null && data.nextDays.length === 0 && (
          <EmptyState
            title="Aucune journée de travail programmée"
            description="Vos missions confirmées apparaîtront ici dès qu'un contrat sera établi."
            action={
              <Link to="/interimaire/offres" className={btnPrimary}>
                Voir les offres
              </Link>
            }
          />
        )}

        {data !== null && data.nextDays.length > 0 && (
          <ul className="divide-y divide-line">
            {data.nextDays.map((day) => (
              <li key={`${day.missionId}-${day.date}`} className="flex flex-wrap justify-between gap-2 py-3">
                <div>
                  <p className="text-sm font-medium text-ink">{day.position}</p>
                  <p className="text-sm text-muted">
                    {day.company} · {day.workplace}
                  </p>
                </div>
                <p className="text-sm text-muted">
                  {formatDate(day.date)} · {shortTime(day.startTime)} – {shortTime(day.endTime)}
                </p>
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}
