import type { Mission, MissionStatus, WorkReason } from './types';

/** Libellé et couleur du badge de statut, selon le point de vue. */
export const MISSION_STATUS: Record<MissionStatus, { label: string; className: string }> = {
  PENDING: { label: "En attente de l'agence", className: 'bg-amber-100 text-amber-800' },
  REFUSED: { label: "Refusée par l'agence", className: 'bg-red-100 text-red-700' },
  APPROVED: { label: "En attente de l'intérimaire", className: 'bg-sky-100 text-sky-800' },
  RENEWAL: { label: 'Renouvellement proposé', className: 'bg-violet-100 text-violet-800' },
  ACTIVE: { label: 'Confirmée', className: 'bg-green-100 text-green-700' },
  DECLINED: { label: "Refusée par l'intérimaire", className: 'bg-red-100 text-red-700' },
};

export const WORK_REASONS: { value: WorkReason; label: string }[] = [
  { value: 'REPLACEMENT', label: "Remplacement d'un travailleur permanent" },
  { value: 'OVERLOAD', label: 'Surcroît temporaire de travail' },
  { value: 'EXCEPTION', label: 'Travail exceptionnel' },
];

export function workReasonLabel(reason: WorkReason): string {
  return WORK_REASONS.find((r) => r.value === reason)?.label ?? reason;
}

/** « HH:mm » à partir d'une heure backend (HH:mm ou HH:mm:ss). */
export function shortTime(time: string): string {
  return time.slice(0, 5);
}

/** Plage horaire minimale nécessaire aux calculs de durée. */
interface TimeRange {
  startTime: string;
  endTime: string;
}

/** Durée d'un créneau, en minutes. */
export function slotMinutes(slot: TimeRange): number {
  const [startHour, startMinute] = shortTime(slot.startTime).split(':').map(Number);
  const [endHour, endMinute] = shortTime(slot.endTime).split(':').map(Number);
  return endHour * 60 + endMinute - (startHour * 60 + startMinute);
}

/** Total des heures d'une mission, en minutes. */
export function totalMinutes(slots: TimeRange[]): number {
  return slots.reduce((total, slot) => total + Math.max(0, slotMinutes(slot)), 0);
}

/** Vrai si l'indisponibilité couvre la journée entière (00:00 → 23:59). */
export function isFullDay(range: TimeRange): boolean {
  return shortTime(range.startTime) === '00:00' && shortTime(range.endTime) === '23:59';
}

/** « 7 h 30 » / « 8 h ». */
export function formatMinutes(minutes: number): string {
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return rest === 0 ? `${hours} h` : `${hours} h ${String(rest).padStart(2, '0')}`;
}

/** Montant brut estimé d'une mission. */
export function estimatedPay(minutes: number, hourlyWage: number): string {
  return ((minutes / 60) * hourlyWage).toLocaleString('fr-BE', {
    style: 'currency',
    currency: 'EUR',
  });
}

/** Jour de la semaine abrégé (lun., mar., …) d'une date ISO. */
export function weekdayLabel(iso: string): string {
  return new Date(`${iso}T12:00:00`).toLocaleDateString('fr-BE', { weekday: 'short' });
}

/** Nom du mois et année (« septembre 2026 ») d'une date ISO. */
export function monthLabel(iso: string): string {
  return new Date(`${iso}T12:00:00`).toLocaleDateString('fr-BE', { month: 'long', year: 'numeric' });
}

/** Vrai si la date ISO tombe un samedi ou un dimanche. */
export function isWeekend(iso: string): boolean {
  const day = new Date(`${iso}T12:00:00`).getDay();
  return day === 0 || day === 6;
}

/** Date du jour au format yyyy-MM-dd (fuseau local). */
export function todayIso(): string {
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  return now.toISOString().slice(0, 10);
}

/** Ajoute des jours à une date ISO. */
export function addDays(iso: string, days: number): string {
  const date = new Date(`${iso}T12:00:00`);
  date.setDate(date.getDate() + days);
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
  return date.toISOString().slice(0, 10);
}

/** Toutes les dates ISO comprises entre deux bornes incluses. */
export function datesBetween(startIso: string, endIso: string): string[] {
  const dates: string[] = [];
  for (let date = startIso; date <= endIso; date = addDays(date, 1)) {
    dates.push(date);
  }
  return dates;
}

/** Premier jour du mois d'une date ISO. */
export function startOfMonth(iso: string): string {
  return `${iso.slice(0, 7)}-01`;
}

/** Décale une date ISO de n mois (en restant sur le 1er du mois). */
export function addMonths(iso: string, months: number): string {
  const date = new Date(`${startOfMonth(iso)}T12:00:00`);
  date.setMonth(date.getMonth() + months);
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
  return startOfMonth(date.toISOString().slice(0, 10));
}

/**
 * Dates affichées par la grille d'un mois : semaines complètes commençant le lundi,
 * du lundi précédant le 1er au dimanche suivant le dernier jour.
 */
export function monthGrid(monthStart: string): string[] {
  const first = new Date(`${startOfMonth(monthStart)}T12:00:00`);
  const daysInMonth = new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate();
  const offset = (first.getDay() + 6) % 7; // 0 = lundi
  const total = Math.ceil((offset + daysInMonth) / 7) * 7;
  const start = addDays(startOfMonth(monthStart), -offset);
  return Array.from({ length: total }, (_, index) => addDays(start, index));
}

/** Libellés des jours de la semaine, du lundi au dimanche. */
export const WEEKDAYS = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

/** Classement d'une mission confirmée dans le temps (pour les regroupements). */
export function missionPeriod(mission: Mission): 'upcoming' | 'current' | 'past' {
  const today = todayIso();
  if (mission.endDate < today) {
    return 'past';
  }
  return mission.startDate > today ? 'upcoming' : 'current';
}
