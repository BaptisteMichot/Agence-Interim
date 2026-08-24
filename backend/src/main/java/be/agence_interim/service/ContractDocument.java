package be.agence_interim.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import be.agence_interim.config.AgencyProperties;
import be.agence_interim.model.Contract;
import be.agence_interim.model.DailySchedule;
import be.agence_interim.model.Mission;
import be.agence_interim.model.SignatureStatus;
import be.agence_interim.model.User;
import be.agence_interim.model.WorkReason;

/**
 * Mise en page du contrat de travail intérimaire. Le document reprend les mentions
 * imposées par la loi du 24 juillet 1987 : identité des trois parties, motif de
 * recours, fonction et description du poste, lieu d'exécution, durée et horaire,
 * rémunération, et période d'essai.
 */
public class ContractDocument {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", Locale.FRENCH);

    private static final java.awt.Color INK = new java.awt.Color(0x1E, 0x29, 0x3B);
    private static final java.awt.Color ACCENT = new java.awt.Color(0x4F, 0x46, 0xE5);
    private static final java.awt.Color MUTED = new java.awt.Color(0x64, 0x74, 0x8B);
    private static final java.awt.Color LINE = new java.awt.Color(0xCB, 0xD5, 0xE1);
    private static final java.awt.Color BAND = new java.awt.Color(0xF1, 0xF5, 0xF9);

    private static final Font TITLE = font(18, Font.BOLD, INK);
    private static final Font SUBTITLE = font(9, Font.NORMAL, MUTED);
    private static final Font SECTION = font(11, Font.BOLD, ACCENT);
    private static final Font LABEL = font(8, Font.NORMAL, MUTED);
    private static final Font BODY = font(9, Font.NORMAL, INK);
    private static final Font BODY_BOLD = font(9, Font.BOLD, INK);
    private static final Font SMALL = font(8, Font.NORMAL, MUTED);

    private static Font font(float size, int style, java.awt.Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }

    private static final java.awt.Color SIGNED_GREEN = new java.awt.Color(0x15, 0x80, 0x3D);

    private final AgencyProperties agency;

    public ContractDocument(AgencyProperties agency) {
        this.agency = agency;
    }

    /** Produit le PDF complet du contrat. */
    public byte[] render(Contract contract, Mission mission, List<DailySchedule> slots) {
        User employer = mission.getApplication().getJobOffer().getEmployer();
        User worker = mission.getApplication().getJobSeeker();
        long paidMinutes = slots.stream().mapToLong(WorkTime::paidMinutes).sum();

        Document document = new Document(PageSize.A4, 42, 42, 40, 46);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            header(document, contract);
            parties(document, employer, worker, mission.getJointCommittee());
            mission(document, mission);
            schedule(document, mission, slots, paidMinutes);
            pay(document, mission, paidMinutes);
            section(document, "5. Conditions particulières");
            document.add(paragraph(
                    mission.getNotes() == null || mission.getNotes().isBlank()
                            ? "Aucune condition particulière n'a été convenue entre les parties."
                            : mission.getNotes()));
            legalTerms(document);
            signatures(document, contract, employer, worker);

            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Impossible de générer le contrat en PDF.", e);
        }
        return out.toByteArray();
    }

    // ------------------------------------------------------------------ blocs

    private void header(Document document, Contract contract) throws DocumentException {
        Paragraph title = new Paragraph("CONTRAT DE TRAVAIL INTÉRIMAIRE", TITLE);
        title.setSpacingAfter(2);
        document.add(title);

        document.add(new Paragraph(
                "Contrat n° " + contract.getId() + " — établi le "
                        + TIMESTAMP.format(contract.getGenerationTime()),
                SUBTITLE));
        document.add(new Paragraph(
                "Conclu conformément à la loi du 24 juillet 1987 sur le travail temporaire, le travail "
                        + "intérimaire et la mise de travailleurs à la disposition d'utilisateurs.",
                SUBTITLE));
        document.add(rule());
    }

    private void parties(Document document, User employer, User worker, String jointCommittee)
            throws DocumentException {
        section(document, "1. Parties");

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingAfter(4);
        table.addCell(partyCell("Entreprise de travail intérimaire (employeur juridique)",
                agency.getName(),
                agency.getAddress(),
                "N° d'entreprise : " + agency.getCompanyNumber(),
                "Agrément : " + agency.getLicenceNumber(),
                "Commission paritaire : " + agency.getJointCommittee()));
        table.addCell(partyCell("Entreprise utilisatrice",
                employer.getCompanyName(),
                employer.getAddress(),
                "N° d'entreprise : " + value(employer.getCompanyNumber()),
                "Commission paritaire : " + value(jointCommittee),
                "Contact : " + employer.getFirstName() + " " + employer.getLastName()));
        table.addCell(partyCell("Travailleur intérimaire",
                worker.getFirstName() + " " + worker.getLastName(),
                value(worker.getAddress()),
                "Né(e) le : " + (worker.getBirthdate() == null ? "—" : DATE.format(worker.getBirthdate())),
                "Registre national : " + value(worker.getNationalNumber()),
                "Email : " + worker.getEmail()));
        document.add(table);
    }

    private void mission(Document document, Mission mission) throws DocumentException {
        section(document, "2. Objet de la mission");

        PdfPTable table = facts();
        addFact(table, "Fonction", mission.getPosition());
        addFact(table, "Motif de recours", reasonLabel(mission.getWorkReason()));
        addFact(table, "Lieu d'exécution", mission.getWorkplace());
        addFact(table, "Travailleur remplacé",
                mission.getReplacedWorker() == null ? "Sans objet" : mission.getReplacedWorker());
        addFacts(document, table);

        document.add(labelled("Description du poste", mission.getDescription()));
    }

    private void schedule(Document document, Mission mission, List<DailySchedule> slots, long paidMinutes)
            throws DocumentException {
        section(document, "3. Durée et horaire de travail");

        PdfPTable facts = facts();
        addFact(facts, "Début de la mission", DATE.format(mission.getStartDate()));
        addFact(facts, "Fin de la mission", DATE.format(mission.getEndDate()));
        addFact(facts, "Journées prestées", String.valueOf(slots.size()));
        addFact(facts, "Total rémunéré", WorkTime.format(paidMinutes));
        addFacts(document, facts);

        PdfPTable table = new PdfPTable(new float[] { 3.2f, 2.2f, 2.2f, 1.6f });
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setHeaderRows(1);
        for (String head : new String[] { "Journée", "Horaire", "Pause (non rémunérée)", "Rémunéré" }) {
            table.addCell(headCell(head));
        }
        for (DailySchedule slot : slots) {
            table.addCell(bodyCell(capitalize(DAY.format(slot.getDate())), Element.ALIGN_LEFT));
            table.addCell(bodyCell(
                    TIME.format(slot.getStartTime()) + " – " + TIME.format(slot.getEndTime()),
                    Element.ALIGN_LEFT));
            table.addCell(bodyCell(
                    slot.getBreakStart() == null
                            ? "—"
                            : TIME.format(slot.getBreakStart()) + " – " + TIME.format(slot.getBreakEnd()),
                    Element.ALIGN_LEFT));
            table.addCell(bodyCell(WorkTime.format(WorkTime.paidMinutes(slot)), Element.ALIGN_RIGHT));
        }
        document.add(table);
        document.add(note("La pause n'est pas rémunérée : elle est déduite du temps de travail payé."));
    }

    private void pay(Document document, Mission mission, long paidMinutes) throws DocumentException {
        section(document, "4. Rémunération");

        BigDecimal gross = mission.getHourlyWage()
                .multiply(BigDecimal.valueOf(paidMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        PdfPTable table = facts();
        addFact(table, "Salaire horaire brut", euros(mission.getHourlyWage()));
        addFact(table, "Volume rémunéré", WorkTime.format(paidMinutes));
        addFact(table, "Rémunération brute estimée", euros(gross));
        addFact(table, "Périodicité", "Mensuelle");
        // Avantage facultatif : la ligne n'apparaît que s'il a été convenu. La loi du
        // 24 juillet 1987 impose de reprendre au contrat la rémunération et les avantages.
        addFact(table, "Titres-repas", mission.getMealVoucherAmount() == null
                ? "Non octroyés"
                : euros(mission.getMealVoucherAmount()) + " par journée prestée");
        addFacts(document, table);
        document.add(note("Le travailleur intérimaire a droit au même salaire que celui qu'il "
                + "percevrait s'il était engagé directement par l'entreprise utilisatrice."));
    }

    private void legalTerms(Document document) throws DocumentException {
        section(document, "6. Dispositions légales");
        document.add(bullet("Les trois premiers jours ouvrables de la mission sont considérés comme "
                + "période d'essai. Durant cette période, chaque partie peut mettre fin au contrat "
                + "sans préavis ni indemnité."));
        document.add(bullet("Le présent contrat doit être signé au plus tard dans les deux jours "
                + "ouvrables à compter du début de la mission."));
        document.add(bullet("Le travailleur relève de la commission paritaire "
                + agency.getJointCommittee() + " pour le travail intérimaire ; les conditions de "
                + "travail applicables sont celles de l'entreprise utilisatrice."));
        document.add(bullet("Toute modification des conditions convenues fait l'objet d'un avenant "
                + "soumis à l'accord des parties."));
    }

    /**
     * Page de signature. Les deux zones sont placées à des coordonnées fixes : ce sont
     * elles que remplira la signature électronique, apposée après coup en mise à jour
     * incrémentale du fichier.
     */
    private void signatures(Document document, Contract contract, User employer, User worker)
            throws DocumentException {
        section(document, "7. Signatures");
        document.add(paragraph(
                "Chaque partie signe le contrat sur la plateforme, après avoir confirmé un code à "
                        + "usage unique envoyé à l'adresse email de son compte. La date de signature "
                        + "et l'adresse à laquelle le code a été envoyé sont reprises ci-dessous."));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.addCell(signatureCell("Pour l'entreprise utilisatrice",
                employer.getFirstName() + " " + employer.getLastName(),
                employer.getEmail(), contract.getStatusEmployer(), contract.getEmployerSignedAt()));
        table.addCell(signatureCell("Le travailleur intérimaire",
                worker.getFirstName() + " " + worker.getLastName(),
                worker.getEmail(), contract.getStatusWorker(), contract.getWorkerSignedAt()));
        document.add(table);
    }

    /** Bloc de signature d'une partie : identité, état et date. */
    private PdfPCell signatureCell(
            String role, String name, String email, SignatureStatus status, LocalDateTime signedAt) {
        boolean signed = status == SignatureStatus.SIGNED;
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(LINE);
        cell.setPadding(10);
        cell.setMinimumHeight(70);
        cell.addElement(new Paragraph(role.toUpperCase(Locale.FRENCH), LABEL));
        cell.addElement(new Paragraph(name, BODY_BOLD));
        cell.addElement(new Paragraph(
                signed ? "Signé électroniquement le " + TIMESTAMP.format(signedAt)
                        : "En attente de signature",
                font(9, Font.BOLD, signed ? SIGNED_GREEN : MUTED)));
        if (signed) {
            cell.addElement(new Paragraph("après confirmation du code envoyé à " + email, SMALL));
        }
        return cell;
    }

    // --------------------------------------------------------------- éléments

    private void section(Document document, String title) throws DocumentException {
        Paragraph paragraph = new Paragraph(title, SECTION);
        paragraph.setSpacingBefore(12);
        paragraph.setSpacingAfter(4);
        document.add(paragraph);
    }

    private Paragraph rule() {
        Paragraph paragraph = new Paragraph(new Chunk(new org.openpdf.text.pdf.draw.LineSeparator(
                0.6f, 100, LINE, Element.ALIGN_CENTER, -4)));
        paragraph.setSpacingBefore(6);
        return paragraph;
    }

    private Paragraph paragraph(String text) {
        Paragraph paragraph = new Paragraph(text, BODY);
        paragraph.setAlignment(Element.ALIGN_JUSTIFIED);
        paragraph.setSpacingAfter(2);
        return paragraph;
    }

    private Paragraph labelled(String label, String text) {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Phrase(label.toUpperCase(Locale.FRENCH) + "\n", LABEL));
        paragraph.add(new Phrase(text, BODY));
        paragraph.setSpacingBefore(6);
        paragraph.setAlignment(Element.ALIGN_JUSTIFIED);
        return paragraph;
    }

    private Paragraph bullet(String text) {
        Paragraph paragraph = new Paragraph("•  " + text, BODY);
        paragraph.setIndentationLeft(8);
        paragraph.setSpacingAfter(3);
        paragraph.setAlignment(Element.ALIGN_JUSTIFIED);
        return paragraph;
    }

    private Paragraph note(String text) {
        Paragraph paragraph = new Paragraph(text, SMALL);
        paragraph.setSpacingBefore(4);
        return paragraph;
    }

    /** Grille de faits sur deux colonnes (libellé au-dessus de la valeur). */
    private PdfPTable facts() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(2);
        return table;
    }

    /**
     * Ajoute un tableau de rubriques au document, dernière ligne complétée.
     *
     * <p>Le tableau compte deux colonnes : un nombre impair de rubriques laisse une
     * ligne inachevée, qu'OpenPDF supprime <em>silencieusement</em> à la génération.
     * La rubrique concernée disparaîtrait du contrat sans la moindre erreur.
     */
    private void addFacts(Document document, PdfPTable table) throws DocumentException {
        table.completeRow();
        document.add(table);
    }

    private void addFact(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(6);
        cell.addElement(new Paragraph(label.toUpperCase(Locale.FRENCH), LABEL));
        cell.addElement(new Paragraph(value, BODY_BOLD));
        table.addCell(cell);
    }

    private PdfPCell partyCell(String role, String name, String... lines) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(LINE);
        cell.setBackgroundColor(BAND);
        cell.setPadding(8);
        cell.addElement(new Paragraph(role.toUpperCase(Locale.FRENCH), LABEL));
        Paragraph title = new Paragraph(value(name), BODY_BOLD);
        title.setSpacingAfter(2);
        cell.addElement(title);
        for (String line : lines) {
            cell.addElement(new Paragraph(line, SMALL));
        }
        return cell;
    }

    private PdfPCell headCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text.toUpperCase(Locale.FRENCH), LABEL));
        cell.setBackgroundColor(BAND);
        cell.setBorderColor(LINE);
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell bodyCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BODY));
        cell.setBorderColor(LINE);
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    /** « 16,50 € » : séparateur décimal francophone. */
    private String euros(BigDecimal amount) {
        return String.format(Locale.FRENCH, "%,.2f €", amount);
    }

    private String value(String text) {
        return text == null || text.isBlank() ? "—" : text;
    }

    private String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private String reasonLabel(WorkReason reason) {
        return switch (reason) {
            case REPLACEMENT -> "Remplacement d'un travailleur permanent";
            case OVERLOAD -> "Surcroît temporaire de travail";
            case EXCEPTION -> "Travail exceptionnel";
        };
    }
}
