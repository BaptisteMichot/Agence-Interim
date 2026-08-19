// Classes Tailwind partagées par les pages. Les couleurs passent toutes par les jetons
// définis dans index.css : aucune teinte n'est choisie composant par composant.

// --- Formulaires ---
export const inputClass =
  'w-full rounded-md border border-slate-300 bg-surface px-3 py-2 text-ink outline-none transition focus:border-brand-500 focus:ring-1 focus:ring-brand-500';

export const labelClass = 'mb-1 block text-sm font-medium text-slate-700';

// Case à cocher intégrée comme un champ : encadrée, même hauteur/padding que les inputs.
export const checkboxRow =
  'flex w-full cursor-pointer select-none items-center gap-2 rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-50';

export const checkboxInput = 'h-4 w-4 rounded border-slate-300 accent-brand-600';

// --- Boutons ---
export const btnPrimary =
  'inline-flex items-center justify-center rounded-md bg-brand-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-700 disabled:opacity-60';

export const btnSecondary =
  'inline-flex items-center justify-center rounded-md border border-slate-300 bg-surface px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50';

export const btnDanger =
  'inline-flex items-center justify-center rounded-md border border-red-300 px-3 py-1.5 text-sm font-medium text-red-700 transition hover:bg-red-50';

export const btnDangerSolid =
  'inline-flex items-center justify-center rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-red-700 disabled:opacity-60';

// Bouton de soumission pleine largeur des pages d'authentification.
export const btnAuthSubmit =
  'w-full rounded-md bg-brand-600 px-4 py-2 font-medium text-white transition hover:bg-brand-700 disabled:opacity-60';

// --- Surfaces ---
export const card = 'rounded-xl border border-line bg-surface p-6';

export const listRow =
  'flex flex-wrap items-center justify-between gap-4 rounded-lg border border-line p-4 transition hover:border-brand-200';

// --- Textes ---
export const sectionTitle = 'text-lg font-semibold text-ink';

export const mutedText = 'text-sm text-muted';

// Lien / bouton « ← Retour » en tête de page.
export const linkBack = 'text-sm text-brand-600 transition hover:underline';

// --- Messages ---
export const errorBox = 'whitespace-pre-line rounded-md bg-red-50 px-3 py-2 text-sm text-red-700';

export const warningBox =
  'rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900';
