import { useEffect, useState } from 'react';
import { btnPrimary, btnSecondary, inputClass } from './ui';

interface PromptDialogProps {
  open: boolean;
  title: string;
  message: string;
  label: string;
  placeholder?: string;
  confirmLabel?: string;
  onConfirm: (value: string) => void;
  onCancel: () => void;
}

/** Boîte de dialogue demandant une saisie obligatoire (motif de refus, commentaire…). */
export default function PromptDialog({
  open,
  title,
  message,
  label,
  placeholder,
  confirmLabel = 'Confirmer',
  onConfirm,
  onCancel,
}: PromptDialogProps) {
  const [value, setValue] = useState('');

  useEffect(() => {
    if (open) {
      setValue('');
    }
  }, [open]);

  useEffect(() => {
    if (!open) {
      return;
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onCancel();
      }
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [open, onCancel]);

  if (!open) {
    return null;
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 px-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="prompt-dialog-title"
      onClick={onCancel}
    >
      <form
        className="w-full max-w-md rounded-xl bg-white p-6 shadow-xl"
        onClick={(event) => event.stopPropagation()}
        onSubmit={(event) => {
          event.preventDefault();
          onConfirm(value.trim());
        }}
      >
        <h2 id="prompt-dialog-title" className="text-lg font-semibold text-slate-900">
          {title}
        </h2>
        <p className="mt-2 text-sm text-slate-600">{message}</p>

        <label className="mt-4 block text-sm font-medium text-slate-700" htmlFor="prompt-dialog-value">
          {label}
        </label>
        <textarea
          id="prompt-dialog-value"
          className={`${inputClass} mt-1 min-h-24`}
          value={value}
          maxLength={500}
          placeholder={placeholder}
          required
          autoFocus
          onChange={(event) => setValue(event.target.value)}
        />
        <p className="mt-1 text-right text-xs text-slate-400">
          {value.length}/500
        </p>

        <div className="mt-4 flex justify-end gap-3">
          <button type="button" className={btnSecondary} onClick={onCancel}>
            Annuler
          </button>
          <button type="submit" className={btnPrimary} disabled={value.trim().length === 0}>
            {confirmLabel}
          </button>
        </div>
      </form>
    </div>
  );
}
