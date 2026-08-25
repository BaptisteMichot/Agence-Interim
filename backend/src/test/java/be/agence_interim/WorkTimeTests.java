package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.agence_interim.model.DailySchedule;
import be.agence_interim.service.WorkTime;

/**
 * Le décompte du temps de travail d'une journée de mission.
 *
 * <p>C'est ce nombre de minutes qui apparaît sur le contrat sous « volume rémunéré » et
 * qui, multiplié par le salaire horaire, détermine ce que touche l'intérimaire. La règle
 * tient en une phrase — le temps payé est la présence moins la pause non rémunérée —
 * mais elle est facile à perdre : il suffit qu'un appel oublie la pause pour que chaque
 * journée soit surpayée d'une demi-heure sans que rien ne le signale.
 */
class WorkTimeTests {

    @Test
    @DisplayName("La journée type de 8 h payées : 08:00-16:30 moins une pause de 30 minutes")
    void theStandardEightHourDayIsCountedRight() {
        // Ce sont les valeurs proposées par défaut à l'employeur dans le formulaire de
        // mission. Si elles ne donnaient pas exactement 8 h, tout le reste serait faux.
        long paid = WorkTime.paidMinutes(
                LocalTime.of(8, 0), LocalTime.of(16, 30),
                LocalTime.of(12, 0), LocalTime.of(12, 30));

        assertThat(paid).isEqualTo(480);
    }

    @Test
    @DisplayName("Sans pause déclarée, tout le temps de présence est rémunéré")
    void withoutABreakThePresenceIsFullyPaid() {
        // Une demi-journée ou un poste court n'impose pas de pause : l'absence de pause
        // ne doit rien retrancher, sous peine de payer moins que le temps presté.
        long paid = WorkTime.paidMinutes(LocalTime.of(8, 0), LocalTime.of(12, 0), null, null);

        assertThat(paid).isEqualTo(240);
    }

    @Test
    @DisplayName("Une pause dont une seule borne est renseignée ne retranche rien")
    void aHalfDeclaredBreakDeductsNothing() {
        // Cette situation ne devrait jamais arriver : MissionService.checkBreak exige les
        // deux heures ou aucune. WorkTime reste malgré tout défensif, parce que retrancher
        // une durée calculée sur une borne manquante reviendrait à lever en pleine
        // génération de contrat, ou pire, à retenir une durée arbitraire sur la paie.
        assertThat(WorkTime.paidMinutes(LocalTime.of(8, 0), LocalTime.of(16, 0), LocalTime.of(12, 0), null))
                .isEqualTo(480);
        assertThat(WorkTime.paidMinutes(LocalTime.of(8, 0), LocalTime.of(16, 0), null, LocalTime.of(12, 30)))
                .isEqualTo(480);
    }

    @Test
    @DisplayName("Une pause longue est retranchée en entier")
    void aLongBreakIsFullyDeducted() {
        // Journée coupée : 8 h de présence apparente, 2 h de pause, 6 h payées. C'est le
        // cas où l'écart entre présence et rémunération est le plus visible sur le contrat.
        long paid = WorkTime.paidMinutes(
                LocalTime.of(7, 30), LocalTime.of(15, 30),
                LocalTime.of(11, 30), LocalTime.of(13, 30));

        assertThat(paid).isEqualTo(360);
    }

    @Test
    @DisplayName("Le décompte d'une journée enregistrée donne le même résultat que celui de ses heures")
    void aStoredDayIsCountedLikeItsRawHours() {
        // Les deux formes coexistent : le formulaire calcule sur des heures qui ne sont pas
        // encore enregistrées, le contrat sur les journées relues en base. Elles doivent
        // rester d'accord, sinon l'employeur ne voit pas le volume qui sera facturé.
        DailySchedule day = dayOf(LocalTime.of(8, 0), LocalTime.of(16, 30), LocalTime.of(12, 0), LocalTime.of(12, 30));

        assertThat(WorkTime.paidMinutes(day)).isEqualTo(480);
        assertThat(WorkTime.paidMinutes(dayOf(LocalTime.of(9, 0), LocalTime.of(13, 0), null, null))).isEqualTo(240);
    }

    @Test
    @DisplayName("Une durée s'écrit « 7 h 30 », et les minutes sont sur deux chiffres")
    void aDurationIsWrittenTheWayItIsReadAloud() {
        // Le zéro de tête n'est pas cosmétique : « 1 h 5 » se lit une heure cinq minutes
        // ou une heure cinquante selon le lecteur, sur un document qui vaut engagement.
        assertThat(WorkTime.format(450)).isEqualTo("7 h 30");
        assertThat(WorkTime.format(65)).isEqualTo("1 h 05");
    }

    @Test
    @DisplayName("Une durée en heures entières s'écrit sans minutes")
    void aWholeNumberOfHoursIsWrittenWithoutMinutes() {
        // « 8 h 00 » serait juste mais bavard ; on écrit ce qu'on dirait.
        assertThat(WorkTime.format(480)).isEqualTo("8 h");
        assertThat(WorkTime.format(60)).isEqualTo("1 h");
    }

    @Test
    @DisplayName("Une durée nulle s'écrit « 0 h » et non une chaîne vide")
    void aZeroDurationIsStillWritten() {
        // Rendu tel quel dans le contrat : une case vide passerait pour un oubli de
        // remplissage, alors qu'un « 0 h » se voit et se conteste.
        assertThat(WorkTime.format(0)).isEqualTo("0 h");
    }

    @Test
    @DisplayName("Le volume d'une mission entière reste lisible en heures")
    void aWholeMissionVolumeStaysReadable() {
        // Le total d'une mission d'un mois se compte en centaines d'heures : il ne bascule
        // pas en jours, l'unité du contrat de travail intérimaire étant l'heure.
        assertThat(WorkTime.format(20 * 480)).isEqualTo("160 h");
        assertThat(WorkTime.format(20 * 480 + 45)).isEqualTo("160 h 45");
    }

    private static DailySchedule dayOf(LocalTime start, LocalTime end, LocalTime breakStart, LocalTime breakEnd) {
        DailySchedule day = new DailySchedule();
        day.setStartTime(start);
        day.setEndTime(end);
        day.setBreakStart(breakStart);
        day.setBreakEnd(breakEnd);
        return day;
    }
}
