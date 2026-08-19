import { useRef, useState } from 'react';
import { inputClass } from './ui';

interface PasswordInputProps {
  id: string;
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
  minLength?: number;
  autoComplete?: string;
  placeholder?: string;
}

/** Au-delà de ce délai, l'appui est une révélation maintenue et non un clic de bascule. */
const HOLD_MS = 250;

/**
 * Champ mot de passe avec œil de révélation : un clic bascule l'affichage, un appui
 * maintenu ne le montre que le temps de l'appui.
 */
export default function PasswordInput({
  id,
  value,
  onChange,
  required = false,
  minLength,
  autoComplete,
  placeholder,
}: PasswordInputProps) {
  const [toggled, setToggled] = useState(false);
  const [held, setHeld] = useState(false);
  const pressedAt = useRef(0);
  const visible = toggled || held;

  return (
    <div className="relative">
      <input
        id={id}
        type={visible ? 'text' : 'password'}
        value={value}
        required={required}
        minLength={minLength}
        autoComplete={autoComplete}
        placeholder={placeholder}
        onChange={(event) => onChange(event.target.value)}
        className={`${inputClass} pr-11`}
      />
      <button
        type="button"
        tabIndex={-1}
        aria-label={visible ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
        aria-pressed={toggled}
        className="absolute inset-y-0 right-0 flex items-center px-3 text-muted transition hover:text-ink"
        onPointerDown={() => {
          pressedAt.current = Date.now();
          setHeld(true);
        }}
        onPointerUp={() => setHeld(false)}
        onPointerLeave={() => setHeld(false)}
        onClick={() => {
          // Un appui long a déjà rempli son office : il ne doit pas laisser le champ ouvert.
          if (Date.now() - pressedAt.current < HOLD_MS) {
            setToggled((shown) => !shown);
          }
        }}
      >
        <EyeIcon crossed={visible} />
      </button>
    </div>
  );
}

function EyeIcon({ crossed }: { crossed: boolean }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.6}
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-5 w-5"
      aria-hidden="true"
    >
      <path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12Z" />
      <circle cx="12" cy="12" r="2.8" />
      {crossed && <path d="m4 20 16-16" />}
    </svg>
  );
}
