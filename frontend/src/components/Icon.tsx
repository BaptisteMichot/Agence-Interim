export type IconName =
  | 'home'
  | 'user'
  | 'briefcase'
  | 'document'
  | 'calendar'
  | 'chat'
  | 'building'
  | 'check'
  | 'search'
  | 'shield';

/** Tracés d'une seule ligne : de simples repères visuels, sans dépendance externe. */
const PATHS: Record<IconName, string> = {
  home: 'M3 11 12 4l9 7v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z',
  user: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm7 9v-1.5c0-2.5-3.1-4.5-7-4.5s-7 2-7 4.5V21',
  briefcase:
    'M4 8h16a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1Zm5 0V6a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2M3 13h18',
  document: 'M6 3h8l5 5v13a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Zm8 0v5h5M8 13h8M8 17h5',
  calendar: 'M4 6h16a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1Zm4-3v4m8-4v4M3 11h18',
  chat: 'M4 5h16a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H9l-5 4V6a1 1 0 0 1 1-1Z',
  building: 'M5 21V4a1 1 0 0 1 1-1h8a1 1 0 0 1 1 1v17M15 9h3a1 1 0 0 1 1 1v11M3 21h18M8 7h3M8 11h3M8 15h3',
  check: 'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18Zm-4 9 3 3 5-6',
  search: 'M10.5 4a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13Zm4.6 11.1L21 21',
  shield: 'M12 3 5 6v6c0 4.2 2.8 7.6 7 9 4.2-1.4 7-4.8 7-9V6l-7-3Zm-3 9 2.2 2.2L15 10',
};

export default function Icon({
  name,
  className = 'h-5 w-5',
}: {
  name: IconName;
  className?: string;
}) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.6}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden="true"
    >
      <path d={PATHS[name]} />
    </svg>
  );
}
