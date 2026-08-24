package be.agence_interim.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;

import be.agence_interim.config.AgencyProperties;
import be.agence_interim.dto.PersonalDataExport;

/**
 * Met en forme l'export des données personnelles en un document texte lisible.
 *
 * <p><strong>Pourquoi du texte et non du JSON.</strong> Le destinataire de cet export
 * est la personne concernée, pas un programme. Un JSON répond mieux à la
 * « lisibilité par machine » que cite le RGPD, mais il demande un outil pour être lu, et
 * l'immense majorité des demandes d'accès se règle en ouvrant le fichier. Le document est
 * donc structuré — rubriques titrées, libellés alignés — pour rester exploitable sans
 * être hostile.
 *
 * <p>Les rubriques vides ne sont pas rendues, et la numérotation suit ce qui est
 * effectivement écrit. Un employeur ne verra donc pas huit sections « néant » qui ne le
 * concernent pas, et le document se lit différemment selon le rôle sans qu'aucune
 * condition sur le rôle n'ait été écrite : c'est le contenu qui décide.
 */
public final class PersonalDataDocument {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", Locale.FRENCH);

    /** Largeur des filets : celle d'un terminal étroit, lisible partout. */
    private static final int WIDTH = 74;

    /** Largeur de la colonne des libellés, pour que les valeurs s'alignent. */
    private static final int LABEL_WIDTH = 24;

    private final StringBuilder out = new StringBuilder();
    private final PersonalDataExport data;
    private final AgencyProperties agency;
    private int sectionNumber;

    private PersonalDataDocument(PersonalDataExport data, AgencyProperties agency) {
        this.data = data;
        this.agency = agency;
    }

    /** Rend le document complet. */
    public static String render(PersonalDataExport data, AgencyProperties agency) {
        return new PersonalDataDocument(data, agency).build();
    }

    private String build() {
        header();
        identity();
        company();
        list("Compétences déclarées", data.skills(),
                skill -> skill.name() + " — niveau " + skillLevelLabel(skill.level().name()));
        list("Diplômes", data.degrees(), degree -> {
            String line = degreeTypeLabel(degree.type().name()) + " en " + degree.section();
            if (degree.institution() != null) {
                line += " (" + degree.institution() + ")";
            }
            if (degree.graduationYear() != null) {
                line += ", obtenu en " + degree.graduationYear();
            }
            return line;
        });
        list("Langues", data.languages(),
                language -> language.name() + " — niveau " + language.level().name());
        list("Expériences professionnelles", data.experiences(),
                experience -> experience.position() + " chez " + experience.companyName()
                        + " (" + period(experience.startDate(), experience.endDate()) + ")");
        list("Formations", data.formations(),
                formation -> formation.title() + " — " + formation.institution()
                        + " (" + period(formation.startDate(), formation.endDate()) + ")");
        list("Candidatures déposées", data.applications(),
                application -> application.offerTitle() + " chez " + application.company()
                        + "\n    déposée le " + dateTimeOrUnknown(application.appliedAt())
                        + " — statut : " + applicationStatusLabel(application.status())
                        + (application.rating() == null
                                ? ""
                                : " — note reçue : " + application.rating() + "/5"));
        list("Offres publiées", data.offers(),
                offer -> offer.title() + " (" + offer.city() + ")"
                        + "\n    publiée le " + dateTimeOrUnknown(offer.publishedAt())
                        + " — statut : " + offerStatusLabel(offer.status()));
        list("Contrats", data.contracts(),
                contract -> contract.position()
                        + "\n    établi le " + dateTimeOrUnknown(contract.generatedAt())
                        + "\n    signature employeur : " + signature(
                                contract.employerSignature(), contract.employerSignedAt())
                        + "\n    signature intérimaire : " + signature(
                                contract.workerSignature(), contract.workerSignedAt()));
        messages();
        footer();
        return out.toString();
    }

    // ------------------------------------------------------------------------ rubriques

    private void header() {
        line("=".repeat(WIDTH));
        line("  MES DONNÉES PERSONNELLES");
        line("  " + agency.getName());
        line("  Export du " + dateTimeOrUnknown(data.exportedAt()));
        line("=".repeat(WIDTH));
        blank();
        wrap("Ce document rassemble l'ensemble des informations que la plateforme "
                + "conserve à votre sujet. Il vous est remis au titre du droit d'accès "
                + "prévu par le règlement général sur la protection des données "
                + "(article 15).");
        blank();
    }

    private void identity() {
        section("Identité");
        field("Rôle", roleLabel(data.identity().role()));
        field("Nom", data.identity().lastName());
        field("Prénom", data.identity().firstName());
        field("Adresse email", data.identity().email());
        field("Date de naissance", date(data.identity().birthdate()));
        field("Véhicule personnel", yesNo(data.identity().hasVehicle()));
        field("Adresse", data.identity().address());
        field("Registre national", data.identity().nationalNumber());
        field("Numéro de compte", data.identity().iban());
        field("CV déposé", data.identity().cvFileName());
        field("Dernière connexion", dateTime(data.identity().lastLoginAt()));
        blank();
    }

    private void company() {
        if (data.identity().companyName() == null) {
            return;
        }
        section("Entreprise utilisatrice");
        wrap("Ces mentions désignent une personne morale et figurent sur les contrats "
                + "établis par l'agence. Elles sont conservées même après la clôture du "
                + "compte de son représentant.");
        blank();
        field("Dénomination", data.identity().companyName());
        field("Numéro d'entreprise", data.identity().companyNumber());
        field("Commission paritaire", data.identity().jointCommittee());
        blank();
    }

    /**
     * Rubrique de liste : rendue seulement si elle contient quelque chose. C'est ce qui
     * fait qu'un même code produit un document différent selon le rôle.
     */
    private <T> void list(String title, List<T> items, Function<T, String> format) {
        if (items == null || items.isEmpty()) {
            return;
        }
        section(title);
        for (T item : items) {
            line("  - " + format.apply(item));
        }
        blank();
    }

    /**
     * Messages, regroupés par fil de discussion.
     *
     * <p>Le regroupement remplace le rappel du fil sur chaque ligne : répéter
     * l'interlocuteur à chaque message serait aussi bavard que le numéro de conversation
     * qu'il remplace. {@link LinkedHashMap} conserve l'ordre d'apparition, donc le fil le
     * plus ancien vient en premier.
     */
    private void messages() {
        if (data.messages().isEmpty()) {
            return;
        }
        section("Messages envoyés");
        wrap("Seuls vos propres messages figurent ici. Ceux de vos interlocuteurs sont "
                + "leurs données, pas les vôtres.");
        blank();

        Map<String, List<PersonalDataExport.MessageLine>> threads = data.messages().stream()
                .collect(Collectors.groupingBy(
                        message -> message.conversation(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        for (Map.Entry<String, List<PersonalDataExport.MessageLine>> thread : threads.entrySet()) {
            line("  Échange avec " + thread.getKey());
            for (PersonalDataExport.MessageLine message : thread.getValue()) {
                line("    " + dateTimeOrUnknown(message.sentTime()));
                for (String fragment : message.content().split("\\R")) {
                    wrapIndented(fragment, "      ");
                }
            }
            blank();
        }
    }

    /**
     * Ce que le document ne contient pas, et à qui s'adresser.
     *
     * <p>La liste se construit à partir de ce que le compte détient réellement : annoncer
     * à un employeur que son CV n'est pas joint, ou à un compte sans contrat que ses
     * contrats ne le sont pas, décrit une application qui n'est pas la sienne. Le mot de
     * passe, lui, concerne tout le monde.
     */
    private void footer() {
        line("-".repeat(WIDTH));

        List<String> absent = new ArrayList<>();
        absent.add("votre mot de passe, qui n'est conservé que sous forme de condensat non "
                + "réversible et ne vous serait d'aucun usage");
        if (data.identity().cvFileName() != null) {
            absent.add("le fichier PDF de votre CV");
        }
        if (!data.contracts().isEmpty()) {
            absent.add("les fichiers PDF de vos contrats");
        }

        String intro = absent.size() == 1
                ? "Ne figure pas dans ce document : "
                : "Ne figurent pas dans ce document : ";
        String note = intro + join(absent) + ".";
        if (absent.size() > 1) {
            note += " Ces documents se téléchargent depuis votre espace personnel.";
        }
        wrap(note);
        blank();

        // L'agence n'a personne à qui écrire : elle est le responsable du traitement.
        if (!"ADMIN".equals(data.identity().role())) {
            wrap("Pour toute question sur le traitement de vos données, ou pour exercer vos "
                    + "droits de rectification et d'effacement, adressez-vous à "
                    + agency.getName() + ", " + agency.getAddress() + ".");
            blank();
        }
        line("-".repeat(WIDTH));
    }

    /**
     * Énumération séparée par des points-virgules.
     *
     * <p>Pas de « a, b et c » : le premier élément de la liste contient déjà un « et »
     * dans sa propre phrase, et l'enchaînement produisait « ne vous serait d'aucun usage
     * et les fichiers PDF de vos contrats ». Le point-virgule sépare sans ambiguïté des
     * membres qui sont eux-mêmes des propositions.
     */
    private static String join(List<String> parts) {
        return String.join(" ; ", parts);
    }

    // ------------------------------------------------------------------------- écriture

    private void section(String title) {
        sectionNumber++;
        line("-".repeat(WIDTH));
        line(" " + sectionNumber + ". " + title.toUpperCase(Locale.FRENCH));
        line("-".repeat(WIDTH));
    }

    /** Champ libellé/valeur, omis lorsque la valeur est absente. */
    private void field(String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        line("  " + padded(label) + " : " + value);
    }

    private void line(String text) {
        out.append(text).append('\n');
    }

    private void blank() {
        out.append('\n');
    }

    private void wrap(String text) {
        wrapIndented(text, "");
    }

    /**
     * Ponctuation double, qui ne peut pas commencer une ligne.
     *
     * <p>En typographie française, elle se compose précédée d'une espace insécable. Sans
     * cette précaution, la coupure de ligne rejetait le point-virgule d'une énumération en
     * tête de ligne suivante, ce qui se remarque immédiatement à la lecture.
     */
    private static final Set<String> GLUED_PUNCTUATION = Set.of(";", ":", "?", "!", "»");

    /** Coupe le texte à la largeur du document, sans couper les mots. */
    private void wrapIndented(String text, String indent) {
        StringBuilder current = new StringBuilder(indent);
        for (String word : glue(text.split(" "))) {
            if (current.length() > indent.length() && current.length() + word.length() + 1 > WIDTH) {
                line(current.toString());
                current = new StringBuilder(indent);
            }
            if (current.length() > indent.length()) {
                current.append(' ');
            }
            current.append(word);
        }
        if (current.length() > indent.length()) {
            line(current.toString());
        }
    }

    /**
     * Recolle la ponctuation double au mot qui la précède, par une espace insécable.
     * Les deux ne forment plus qu'un mot pour l'algorithme de coupure.
     */
    private static List<String> glue(String[] words) {
        List<String> glued = new ArrayList<>(words.length);
        for (String word : words) {
            if (!glued.isEmpty() && GLUED_PUNCTUATION.contains(word)) {
                glued.set(glued.size() - 1, glued.get(glued.size() - 1) + ' ' + word);
            } else {
                glued.add(word);
            }
        }
        return glued;
    }

    private static String padded(String label) {
        return label.length() >= LABEL_WIDTH
                ? label
                : label + " ".repeat(LABEL_WIDTH - label.length());
    }

    // ------------------------------------------------------------------------- formats

    /**
     * Traductions des valeurs d'énumération.
     *
     * <p>Un document remis à la personne concernée ne peut pas afficher {@code PENDING}
     * ou {@code AVANCE} : ce sont des identifiants techniques, pas du français. Ils sont
     * traduits ici plutôt que dans les énumérations elles-mêmes, qui n'ont pas à connaître
     * la langue de l'interface — c'est la règle déjà suivie par les libellés de secteur
     * des emails de mise en relation.
     */
    private static String skillLevelLabel(String level) {
        return switch (level) {
            case "DEBUTANT" -> "débutant";
            case "INTERMEDIAIRE" -> "intermédiaire";
            case "AVANCE" -> "avancé";
            case "EXPERT" -> "expert";
            default -> level.toLowerCase(Locale.FRENCH);
        };
    }

    private static String degreeTypeLabel(String type) {
        return switch (type) {
            case "CESS" -> "CESS";
            case "BACHELIER" -> "Bachelier";
            case "MASTER" -> "Master";
            case "DOCTORAT" -> "Doctorat";
            case "AUTRE" -> "Autre diplôme";
            default -> type.charAt(0) + type.substring(1).toLowerCase(Locale.FRENCH);
        };
    }

    private static String applicationStatusLabel(String status) {
        return switch (status) {
            case "PENDING" -> "en cours";
            case "CANCELED" -> "annulée";
            default -> status;
        };
    }

    private static String offerStatusLabel(String status) {
        return switch (status) {
            case "OPEN" -> "ouverte";
            case "CLOSED" -> "clôturée";
            default -> status;
        };
    }

    private static String roleLabel(String role) {
        return switch (role) {
            case "JOBSEEKER" -> "Intérimaire";
            case "EMPLOYER" -> "Employeur";
            case "EMPLOYER_PENDING" -> "Employeur en attente de validation";
            case "ADMIN" -> "Agence";
            default -> role;
        };
    }

    /**
     * État d'une signature.
     *
     * <p>Un contrat peut porter le statut « signé » sans horodatage — c'est le cas des
     * lignes écrites avant que la date de signature ne soit enregistrée. Composer
     * « signé le » avec un {@code null} produisait « signé le null », ce qui ressemble à
     * un défaut d'affichage alors que c'est simplement une donnée absente. Le document le
     * dit désormais dans ces termes.
     */
    private static String signature(String status, LocalDateTime signedAt) {
        if (!"SIGNED".equals(status)) {
            return "en attente";
        }
        return signedAt == null ? "signé (date non enregistrée)" : "signé le " + dateTime(signedAt);
    }

    /** Date et heure, ou la mention de leur absence : jamais « null » dans le document. */
    private static String dateTimeOrUnknown(LocalDateTime value) {
        return value == null ? "date non enregistrée" : DATE_TIME.format(value);
    }

    private static String period(LocalDate start, LocalDate end) {
        String from = start == null ? "?" : DATE.format(start);
        return end == null ? "depuis le " + from : "du " + from + " au " + DATE.format(end);
    }

    private static String date(LocalDate value) {
        return value == null ? null : DATE.format(value);
    }

    private static String dateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME.format(value);
    }

    private static String yesNo(Boolean value) {
        if (value == null) {
            return null;
        }
        return Boolean.TRUE.equals(value) ? "oui" : "non";
    }
}
