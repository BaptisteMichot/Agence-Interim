import { useCallback, useState } from 'react';
import { apiGetPage, type Page } from '../../api/page';
import EmptyState from '../../components/EmptyState';
import PageHeader from '../../components/PageHeader';
import Pagination from '../../components/Pagination';
import { card, errorBox, inputClass, mutedText } from '../../components/ui';
import { usePagedResource } from '../../hooks/usePagedResource';

/** Une ligne du journal, telle que l'API la renvoie. */
interface AuditEvent {
  id: number;
  occurredAt: string;
  action: string;
  actorId: number | null;
  actorEmail: string | null;
  targetType: string | null;
  targetId: number | null;
  ip: string | null;
  detail: string | null;
}

/** Libellés des actes consignés. La clé technique reste visible dans le filtre. */
const ACTION_LABELS: Record<string, string> = {
  CONTRACT_SIGNED: 'Contrat signé',
  MISSION_VALIDATED: 'Mission validée',
  MISSION_REFUSED: 'Mission refusée',
  EMPLOYER_ACCESS_GRANTED: 'Accès employeur accordé',
  EMPLOYER_ACCESS_REFUSED: 'Accès employeur refusé',
  PASSWORD_CHANGED: 'Mot de passe changé',
  PASSWORD_RESET: 'Mot de passe réinitialisé',
  DATA_EXPORTED: 'Données exportées',
  ACCOUNT_CLOSED: 'Compte clôturé',
};

const DATE_TIME = new Intl.DateTimeFormat('fr-BE', {
  dateStyle: 'short',
  timeStyle: 'medium',
});

/**
 * Journal d'audit de l'agence.
 *
 * <p>Lecture seule, et il n'existe aucune route d'écriture : un journal que son lecteur
 * peut retoucher ne prouve rien. C'est ici que l'agence retrouve qui a signé un contrat,
 * quand, et depuis quelle adresse — ce que l'état d'un contrat, à lui seul, ne dit pas.
 */
export default function AdminAuditPage() {
  const [action, setAction] = useState('');

  const fetcher = useCallback(
    (page: number) =>
      apiGetPage<AuditEvent>('/admin/audit', page, { action: action || undefined }),
    [action],
  );

  const { items, pageData, loading, error, goTo } = usePagedResource(
    fetcher,
    "Le journal n'a pas pu être chargé.",
  );

  return (
    <div className="space-y-6">
      <PageHeader
        title="Journal d'audit"
        subtitle="Les actes engageants de la plateforme, du plus récent au plus ancien."
      />

      <div className={card}>
        <label className="flex flex-wrap items-center gap-3 text-sm">
          <span className="font-medium text-slate-700">Type d'acte</span>
          <select
            className={`${inputClass} sm:w-72`}
            value={action}
            onChange={(event) => setAction(event.target.value)}
          >
            <option value="">Tous</option>
            {Object.entries(ACTION_LABELS).map(([key, label]) => (
              <option key={key} value={key}>
                {label}
              </option>
            ))}
          </select>
        </label>

        {error !== null && <p className={`mt-4 ${errorBox}`}>{error}</p>}

        {loading ? (
          <p className={`mt-4 ${mutedText}`}>Chargement…</p>
        ) : items.length === 0 ? (
          <EmptyState title="Aucun acte consigné pour ce filtre." />
        ) : (
          <>
            <AuditTable events={items} />
            <div className="mt-4">
              <Pagination page={pageData as Page<AuditEvent>} onChange={goTo} label="actes" />
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function AuditTable({ events }: { events: AuditEvent[] }) {
  return (
    <div className="mt-4 overflow-x-auto">
      <table className="w-full min-w-[46rem] border-collapse text-sm">
        <thead>
          <tr className="border-b border-line text-left text-xs uppercase tracking-wide text-muted">
            <th className="py-2 pr-4 font-medium">Date</th>
            <th className="py-2 pr-4 font-medium">Acte</th>
            <th className="py-2 pr-4 font-medium">Auteur</th>
            <th className="py-2 pr-4 font-medium">Objet</th>
            <th className="py-2 pr-4 font-medium">Adresse</th>
            <th className="py-2 font-medium">Précision</th>
          </tr>
        </thead>
        <tbody>
          {events.map((event) => (
            <tr key={event.id} className="border-b border-line last:border-0">
              <td className="whitespace-nowrap py-2 pr-4 tabular-nums text-slate-600">
                {DATE_TIME.format(new Date(event.occurredAt))}
              </td>
              <td className="py-2 pr-4 font-medium text-ink">
                {ACTION_LABELS[event.action] ?? event.action}
              </td>
              <td className="py-2 pr-4 text-slate-600">
                {event.actorEmail ?? (event.actorId === null ? 'Système' : `#${event.actorId}`)}
              </td>
              <td className="whitespace-nowrap py-2 pr-4 text-slate-600">
                {event.targetType === null ? '—' : `${event.targetType} #${event.targetId}`}
              </td>
              <td className="whitespace-nowrap py-2 pr-4 text-slate-500">{event.ip ?? '—'}</td>
              <td className="py-2 text-slate-600">{event.detail ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
