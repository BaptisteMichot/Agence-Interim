import type { Page } from '../api/page';

/**
 * Numéros de pages à afficher : les premières et dernières restent toujours
 * accessibles, le reste est replié en « … » autour de la page courante.
 */
function pageNumbers(current: number, total: number): (number | 'gap')[] {
  if (total <= 7) {
    return Array.from({ length: total }, (_, index) => index);
  }

  const wanted = new Set([0, total - 1, current - 1, current, current + 1]);
  // Près d'un bord, la fenêtre se décale vers l'intérieur pour garder une largeur stable.
  if (current <= 2) {
    [1, 2, 3].forEach((page) => wanted.add(page));
  }
  if (current >= total - 3) {
    [total - 2, total - 3, total - 4].forEach((page) => wanted.add(page));
  }

  const shown = [...wanted].filter((page) => page >= 0 && page < total).sort((a, b) => a - b);
  const result: (number | 'gap')[] = [];
  shown.forEach((page, index) => {
    if (index > 0 && page - shown[index - 1] > 1) {
      result.push('gap');
    }
    result.push(page);
  });
  return result;
}

const stepButton =
  'rounded-md border border-line px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40';

/**
 * Pagination d'une liste : « 1–10 sur 137 » et les numéros de pages.
 * N'affiche rien tant que tout tient sur une seule page.
 */
export default function Pagination<T>({
  page,
  onChange,
  label = 'éléments',
}: {
  page: Page<T>;
  onChange: (page: number) => void;
  /** Nom des éléments comptés, au pluriel : « offres », « candidatures »… */
  label?: string;
}) {
  if (page.totalPages <= 1) {
    return null;
  }

  const first = page.page * page.size + 1;
  const last = page.page * page.size + page.content.length;

  return (
    <nav
      aria-label="Pagination"
      className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-line pt-4"
    >
      <p className="text-sm text-muted">
        {first}–{last} sur {page.totalElements} {label}
      </p>

      <div className="flex flex-wrap items-center gap-1">
        <button
          type="button"
          className={stepButton}
          onClick={() => onChange(page.page - 1)}
          disabled={page.page === 0}
        >
          ‹ Précédent
        </button>

        {pageNumbers(page.page, page.totalPages).map((entry, index) =>
          entry === 'gap' ? (
            <span key={`gap-${index}`} className="px-1 text-sm text-slate-400" aria-hidden="true">
              …
            </span>
          ) : (
            <button
              key={entry}
              type="button"
              onClick={() => onChange(entry)}
              aria-label={`Page ${entry + 1}`}
              aria-current={entry === page.page ? 'page' : undefined}
              className={`min-w-9 rounded-md px-2.5 py-1.5 text-sm font-medium transition ${
                entry === page.page
                  ? 'bg-brand-600 text-white'
                  : 'border border-line text-slate-700 hover:bg-slate-50'
              }`}
            >
              {entry + 1}
            </button>
          ),
        )}

        <button
          type="button"
          className={stepButton}
          onClick={() => onChange(page.page + 1)}
          disabled={page.page >= page.totalPages - 1}
        >
          Suivant ›
        </button>
      </div>
    </nav>
  );
}
