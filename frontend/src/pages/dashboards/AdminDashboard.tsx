import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  acceptEmployerRequest,
  getEmployerRequests,
  refuseEmployerRequest,
  type AdminEmployerRequest,
} from '../../api/employer';
import ConfirmDialog from '../../components/ConfirmDialog';
import { btnDanger, btnPrimary, errorBox } from '../../components/ui';
import { formatDate } from '../../profile/format';

type PendingAction = { id: number; company: string; action: 'accept' | 'refuse' };

const STATUS_LABEL: Record<string, string> = {
  ACCEPTED: 'Acceptée',
  REFUSED: 'Refusée',
};

export default function AdminDashboard() {
  const [requests, setRequests] = useState<AdminEmployerRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState<PendingAction | null>(null);

  const reload = useCallback(async () => {
    setError(null);
    try {
      setRequests(await getEmployerRequests());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Impossible de charger les demandes.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const waiting = useMemo(() => requests.filter((r) => r.status === 'PENDING'), [requests]);
  const history = useMemo(
    () => requests.filter((r) => r.status !== 'PENDING').reverse(),
    [requests],
  );

  const confirm = async () => {
    if (!pending) {
      return;
    }
    const { id, action } = pending;
    setPending(null);
    setError(null);
    try {
      await (action === 'accept' ? acceptEmployerRequest(id) : refuseEmployerRequest(id));
      reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Une erreur est survenue.');
    }
  };

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Espace administrateur</h1>
        <p className="mt-1 text-slate-600">Demandes d'accès employeur.</p>
      </div>

      {error && <p className={errorBox}>{error}</p>}

      <div className="rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="mb-4 text-lg font-semibold text-slate-900">En attente</h2>
        {loading && <p className="text-sm text-slate-500">Chargement…</p>}
        {!loading && waiting.length === 0 && (
          <p className="text-sm text-slate-500">Aucune demande en attente.</p>
        )}
        <ul className="space-y-3">
          {waiting.map((request) => (
            <RequestCard key={request.id} request={request}>
              <button
                type="button"
                className={btnPrimary}
                onClick={() => setPending({ id: request.id, company: request.companyName, action: 'accept' })}
              >
                Accepter
              </button>
              <button
                type="button"
                className={btnDanger}
                onClick={() => setPending({ id: request.id, company: request.companyName, action: 'refuse' })}
              >
                Refuser
              </button>
            </RequestCard>
          ))}
        </ul>
      </div>

      {history.length > 0 && (
        <div className="rounded-xl border border-slate-200 bg-white p-6">
          <h2 className="mb-4 text-lg font-semibold text-slate-900">Historique</h2>
          <ul className="space-y-3">
            {history.map((request) => (
              <RequestCard key={request.id} request={request}>
                <span
                  className={`rounded-full px-3 py-1 text-xs font-medium ${
                    request.status === 'ACCEPTED'
                      ? 'bg-green-100 text-green-700'
                      : 'bg-red-100 text-red-700'
                  }`}
                >
                  {STATUS_LABEL[request.status]}
                </span>
              </RequestCard>
            ))}
          </ul>
        </div>
      )}

      <ConfirmDialog
        open={pending !== null}
        title={pending?.action === 'accept' ? 'Accepter la demande' : 'Refuser la demande'}
        message={
          pending?.action === 'accept'
            ? `Accorder le rôle employeur pour « ${pending?.company} » ?`
            : `Refuser la demande de « ${pending?.company} » ?`
        }
        confirmLabel={pending?.action === 'accept' ? 'Accepter' : 'Refuser'}
        onConfirm={confirm}
        onCancel={() => setPending(null)}
      />
    </section>
  );
}

function RequestCard({
  request,
  children,
}: {
  request: AdminEmployerRequest;
  children: React.ReactNode;
}) {
  const [showMessage, setShowMessage] = useState(false);

  return (
    <li className="rounded-lg border border-slate-200 p-4">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="font-medium text-slate-900">
            {request.companyName}
            {request.resubmission && (
              <span className="ml-2 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700">
                Nouvelle demande après refus
              </span>
            )}
          </p>
          <p className="text-sm text-slate-500">
            {request.firstName} {request.lastName} · {request.email}
          </p>
          <p className="text-xs text-slate-400">Demande du {formatDate(request.requestDate)}</p>
          {request.message && (
            <button
              type="button"
              className="mt-1 text-xs font-medium text-indigo-600 hover:underline"
              onClick={() => setShowMessage((v) => !v)}
            >
              {showMessage ? 'Masquer le message' : 'Voir le message'}
            </button>
          )}
        </div>
        <div className="flex shrink-0 items-center gap-2">{children}</div>
      </div>
      {showMessage && request.message && (
        <p className="mt-3 rounded-md bg-slate-50 p-3 text-sm text-slate-700">{request.message}</p>
      )}
    </li>
  );
}
