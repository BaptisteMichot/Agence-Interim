package be.agence_interim.service;

import java.time.Duration;
import java.time.LocalTime;

import be.agence_interim.model.DailySchedule;

/**
 * Décompte du temps de travail d'une journée. La pause fixée par l'employeur
 * n'est pas rémunérée : le temps payé est la présence moins cette pause.
 */
public final class WorkTime {

    private WorkTime() {
    }

    /** Temps de présence sur le lieu de travail, en minutes. */
    private static long presenceMinutes(LocalTime start, LocalTime end) {
        return Duration.between(start, end).toMinutes();
    }

    private static long presenceMinutes(DailySchedule slot) {
        return presenceMinutes(slot.getStartTime(), slot.getEndTime());
    }

    /** Durée de la pause non rémunérée, en minutes (0 si la journée n'en comporte pas). */
    private static long breakMinutes(LocalTime breakStart, LocalTime breakEnd) {
        return breakStart == null || breakEnd == null ? 0 : Duration.between(breakStart, breakEnd).toMinutes();
    }

    private static long breakMinutes(DailySchedule slot) {
        return breakMinutes(slot.getBreakStart(), slot.getBreakEnd());
    }

    /** Temps effectivement rémunéré, en minutes. */
    public static long paidMinutes(LocalTime start, LocalTime end, LocalTime breakStart, LocalTime breakEnd) {
        return presenceMinutes(start, end) - breakMinutes(breakStart, breakEnd);
    }

    public static long paidMinutes(DailySchedule slot) {
        return presenceMinutes(slot) - breakMinutes(slot);
    }

    /** Formate une durée en minutes sous la forme « 7 h 30 ». */
    public static String format(long minutes) {
        long hours = minutes / 60;
        long rest = minutes % 60;
        return rest == 0 ? hours + " h" : hours + " h " + String.format("%02d", rest);
    }
}
