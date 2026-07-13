import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { deleteAccount, getMyEmployerRequest, reapplyEmployer } from '../api/employer';
import { useAuth } from '../auth/AuthContext';
import type { EmployerAccessStatus } from '../auth/types';
import ConfirmDialog from '../components/ConfirmDialog';
import { btnPrimary, btnSecondary, errorBox } from '../components/ui';

const MESSAGE_MAX = 150;

const CONTENT: Record<
  EmployerAccessStatus,
  { emoji: string; color: string; title: string; message: string }
> = {
  PENDING: {
    emoji: '⏳',
    color: 'bg-amber-100 text-amber-600',
    title: 'Demande en attente',
    message:
      "Votre demande d'accès employeur est en cours d'examen par l'agence. Vous accéderez à votre espace une fois la demande acceptée.",
  },
  REFUSED: {
    emoji: '✕',
    color: 'bg-red-100 text-red-600',
    title: 'Demande refusée',
    message:
      "Votre demande d'accès employeur a été refusée par l'agence. Vous pouvez soumettre une nouvelle demande.",
  },
  ACCEPTED: {
    emoji: '✓',
    color: 'bg-green-100 text-green-600',
    title: 'Demande acceptée',
    message: 'Votre demande a été acceptée ! Reconnectez-vous pour accéder à votre espace employeur.',
  },
};

export default function EmployerStatusPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [status, setStatus] = useState<EmployerAccessStatus | null | undefined>(undefined);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const reload = useCallback(() => {
    getMyEmployerRequest()
      .then((r) => setStatus(r.status))
      .catch(() => setStatus(null));
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  const handleDelete = async () => {
    setConfirmDelete(false);
    try {
      await deleteAccount();
    } finally {
      handleLogout();
    }
  };

  if (status === null) {
    return <Navigate to="/interimaire" replace />;
  }

  if (status === undefined) {
    return (
      <div className="flex min-h-full items-center justify-center bg-slate-50">
        <p className="text-slate-500">Chargement…</p>
      </div>
    );
  }

  const content = CONTENT[status];

  return (
    <div className="flex min-h-full items-center justify-center bg-slate-50 px-4 py-8">
      <div className="w-full max-w-md rounded-xl border border-slate-200 bg-white p-8 text-center shadow-sm">
        <div
          className={`mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full text-2xl ${content.color}`}
        >
          {content.emoji}
        </div>
        <h1 className="text-xl font-semibold text-slate-900">{content.title}</h1>
        <p className="mt-1 text-sm text-slate-500">
          {user?.firstName} {user?.lastName}
        </p>
        <p className="mt-4 text-slate-600">{content.message}</p>

        {status === 'REFUSED' && <ReapplyForm onDone={reload} />}

        <div className="mt-6 flex flex-col gap-3">
          <button
            type="button"
            onClick={handleLogout}
            className="rounded-md border border-slate-300 px-4 py-2 font-medium text-slate-700 hover:bg-slate-100"
          >
            {status === 'ACCEPTED' ? 'Se reconnecter' : 'Se déconnecter'}
          </button>
          <button
            type="button"
            onClick={() => setConfirmDelete(true)}
            className="text-sm text-red-600 hover:underline"
          >
            Supprimer mon compte
          </button>
        </div>
      </div>

      <ConfirmDialog
        open={confirmDelete}
        title="Supprimer mon compte"
        message="Votre compte et vos demandes seront définitivement supprimés. Cette action est irréversible."
        confirmLabel="Supprimer"
        onConfirm={handleDelete}
        onCancel={() => setConfirmDelete(false)}
      />
    </div>
  );
}

function ReapplyForm({ onDone }: { onDone: () => void }) {
  const [message, setMessage] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [open, setOpen] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await reapplyEmployer(message.trim());
      onDone();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Une erreur est survenue.');
    } finally {
      setSubmitting(false);
    }
  };

  if (!open) {
    return (
      <button type="button" className={`mt-6 ${btnPrimary}`} onClick={() => setOpen(true)}>
        Refaire une demande
      </button>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="mt-6 text-left">
      {error && <p className={`mb-3 ${errorBox}`}>{error}</p>}
      <label className="mb-1 block text-sm font-medium text-slate-700" htmlFor="reapply-message">
        Message (facultatif)
      </label>
      <textarea
        id="reapply-message"
        rows={3}
        maxLength={MESSAGE_MAX}
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        placeholder="Précisez votre demande à l'agence…"
        className="w-full rounded-md border border-slate-300 px-3 py-2 outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
      />
      <p className="mt-1 text-right text-xs text-slate-400">
        {message.length}/{MESSAGE_MAX}
      </p>
      <div className="mt-3 flex gap-3">
        <button type="submit" disabled={submitting} className={btnPrimary}>
          {submitting ? 'Envoi…' : 'Envoyer la demande'}
        </button>
        <button type="button" className={btnSecondary} onClick={() => setOpen(false)}>
          Annuler
        </button>
      </div>
    </form>
  );
}
