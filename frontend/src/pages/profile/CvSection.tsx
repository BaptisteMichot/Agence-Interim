import { useRef, useState } from 'react';
import { deleteCv, downloadCv, uploadCv } from '../../api/profile';
import ConfirmDialog from '../../components/ConfirmDialog';
import { btnDanger, btnPrimary, errorBox } from '../../components/ui';

const MAX_SIZE_BYTES = 5 * 1024 * 1024;

interface CvSectionProps {
  cvFilePath: string | null;
  onChanged: () => void;
}

/** Dépôt / consultation / suppression du CV (PDF ≤ 5 Mo). */
export default function CvSection({ cvFilePath, onChanged }: CvSectionProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const hasCv = cvFilePath !== null;

  const resetInput = () => {
    setFile(null);
    if (inputRef.current) {
      inputRef.current.value = '';
    }
  };

  const upload = async () => {
    setError(null);
    if (!file) {
      setError('Sélectionnez un fichier PDF.');
      return;
    }
    if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) {
      setError('Le CV doit être un fichier PDF.');
      return;
    }
    if (file.size > MAX_SIZE_BYTES) {
      setError('Le CV ne doit pas dépasser 5 Mo.');
      return;
    }
    setBusy(true);
    try {
      await uploadCv(file);
      resetInput();
      onChanged();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Une erreur est survenue.');
    } finally {
      setBusy(false);
    }
  };

  const view = async () => {
    setError(null);
    try {
      const blob = await downloadCv();
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank', 'noopener');
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Une erreur est survenue.');
    }
  };

  const confirmDelete = async () => {
    setConfirmOpen(false);
    setError(null);
    setBusy(true);
    try {
      await deleteCv();
      resetInput();
      onChanged();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Une erreur est survenue.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6">
      <h2 className="mb-4 text-lg font-semibold text-slate-900">CV</h2>

      {error && <p className={`mb-4 ${errorBox}`}>{error}</p>}

      {hasCv ? (
        <button
          type="button"
          onClick={view}
          title="Ouvrir le CV"
          className="mb-4 flex w-full items-center gap-3 rounded-lg border border-slate-200 p-3 text-left transition hover:border-indigo-300 hover:bg-indigo-50/40"
        >
          <PdfIcon />
          <span className="min-w-0 flex-1">
            <span className="block truncate font-medium text-slate-900">{cvFilePath}</span>
            <span className="block text-xs text-slate-500">Document PDF</span>
          </span>
          <span className="shrink-0 text-sm font-medium text-indigo-600">Ouvrir →</span>
        </button>
      ) : (
        <p className="mb-4 text-sm text-slate-500">Aucun CV déposé.</p>
      )}

      <div className="space-y-3">
        <input
          ref={inputRef}
          type="file"
          accept="application/pdf,.pdf"
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          className="block w-full text-sm text-slate-600 file:mr-3 file:rounded-md file:border-0 file:bg-indigo-50 file:px-3 file:py-2 file:text-sm file:font-medium file:text-indigo-700 hover:file:bg-indigo-100"
        />
        <div className="flex flex-wrap gap-3">
          <button type="button" className={btnPrimary} disabled={busy || !file} onClick={upload}>
            {busy ? 'Envoi…' : hasCv ? 'Remplacer' : 'Déposer'}
          </button>
          {hasCv && (
            <button type="button" className={btnDanger} disabled={busy} onClick={() => setConfirmOpen(true)}>
              Supprimer
            </button>
          )}
        </div>
      </div>
      <p className="mt-2 text-xs text-slate-500">Format PDF, 5 Mo maximum.</p>

      <ConfirmDialog
        open={confirmOpen}
        title="Supprimer le CV"
        message="Votre CV sera définitivement supprimé."
        confirmLabel="Supprimer"
        onConfirm={confirmDelete}
        onCancel={() => setConfirmOpen(false)}
      />
    </section>
  );
}

/** Petite icône « document PDF ». */
function PdfIcon() {
  return (
    <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-red-50 text-[10px] font-bold text-red-600">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path
          d="M6 2h8l4 4v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2Z"
          fill="currentColor"
          opacity="0.15"
        />
        <path
          d="M14 2v4a1 1 0 0 0 1 1h4"
          stroke="currentColor"
          strokeWidth="1.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <text x="12" y="17" textAnchor="middle" fontSize="6" fontWeight="700" fill="currentColor">
          PDF
        </text>
      </svg>
    </span>
  );
}
