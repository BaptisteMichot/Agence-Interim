package be.agence_interim.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.model.Application;
import be.agence_interim.model.ApplicationStatus;
import be.agence_interim.model.AuditAction;
import be.agence_interim.model.AuditEvent;
import be.agence_interim.model.Conversation;
import be.agence_interim.model.DailySchedule;
import be.agence_interim.model.DegreeJobOffer;
import be.agence_interim.model.DegreeType;
import be.agence_interim.model.DegreeUser;
import be.agence_interim.model.EmployerAccessRequest;
import be.agence_interim.model.EmployerAccessStatus;
import be.agence_interim.model.Experience;
import be.agence_interim.model.Formation;
import be.agence_interim.model.FormationStatus;
import be.agence_interim.model.JobOffer;
import be.agence_interim.model.JobOfferStatus;
import be.agence_interim.model.Language;
import be.agence_interim.model.LanguageJobOffer;
import be.agence_interim.model.LanguageLevel;
import be.agence_interim.model.LanguageUser;
import be.agence_interim.model.Message;
import be.agence_interim.model.Mission;
import be.agence_interim.model.MissionStatus;
import be.agence_interim.model.Province;
import be.agence_interim.model.Role;
import be.agence_interim.model.Sector;
import be.agence_interim.model.SignatureStatus;
import be.agence_interim.model.SkillJobOffer;
import be.agence_interim.model.SkillLevel;
import be.agence_interim.model.SkillUser;
import be.agence_interim.model.User;
import be.agence_interim.model.WorkReason;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.AuditEventRepository;
import be.agence_interim.repository.ContractRepository;
import be.agence_interim.repository.ConversationRepository;
import be.agence_interim.repository.DailyScheduleRepository;
import be.agence_interim.repository.DegreeJobOfferRepository;
import be.agence_interim.repository.DegreeRepository;
import be.agence_interim.repository.DegreeUserRepository;
import be.agence_interim.repository.EmployerAccessRequestRepository;
import be.agence_interim.repository.ExperienceRepository;
import be.agence_interim.repository.FormationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.LanguageJobOfferRepository;
import be.agence_interim.repository.LanguageRepository;
import be.agence_interim.repository.LanguageUserRepository;
import be.agence_interim.repository.MessageRepository;
import be.agence_interim.repository.MissionRepository;
import be.agence_interim.repository.SkillJobOfferRepository;
import be.agence_interim.repository.SkillRepository;
import be.agence_interim.repository.SkillUserRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.ContractService;
import be.agence_interim.service.Strings;

/**
 * Jeu de données de démonstration, activé par {@code app.demo-data.enabled}.
 *
 * <p>Les volumes sont ceux d'une petite agence en activité, non ceux d'un jeu de charge :
 * quatre candidatures libres, douze missions réparties sur leurs six états, cinq contrats,
 * six candidats sur l'offre en vedette, neuf demandes d'accès. Ils ont longtemps été bien
 * plus élevés, au motif qu'une liste ne montre sa pagination qu'au-delà de dix éléments —
 * mais un intérimaire qui traîne soixante candidatures et cinquante missions chez le même
 * employeur ne démontre rien d'autre qu'un jeu de test. Seul le catalogue d'offres reste
 * fourni, à vingt-huit annonces : c'est le contenu même de la plateforme, ce qui donne à
 * la recherche, aux filtres et au score de correspondance de quoi s'exercer, et la liste
 * par laquelle la pagination se voit encore.
 *
 * <p>Une conséquence à connaître avant de vouloir descendre plus bas : chaque mission
 * porte sa propre candidature, puisque le service en exige une par mission. « Mes
 * candidatures » compte donc les quatre candidatures libres plus une par mission, et ne
 * peut se réduire qu'en réduisant les missions.
 *
 * <p>Ce qui est écrit dans ces listes n'est pas indifférent. Un jeu peuplé de
 * « Candidat 3 » et d'« Entreprise 12 » remplit les pages sans rien démontrer : il ne
 * dit pas si un intitulé d'offre tient dans sa colonne, ni si un contrat reste lisible
 * avec un vrai nom, un vrai barème et une vraie commission paritaire. Chaque valeur est
 * donc celle qu'un utilisateur aurait pu saisir — noms, adresses, métiers, salaires et
 * conversations compris — sans qu'aucune ne désigne une personne ou une société réelle.
 *
 * <p>Sept entreprises publient, une par secteur. Les missions restent toutes chez celle
 * de l'employeur de démonstration, qui est le seul de ces comptes à être ouvert : c'est
 * ce qui donne à ses écrans les volumes attendus, les six autres n'existant que pour que
 * la recherche d'offres ne fasse pas recruter un aide-soignant par un entrepôt.
 *
 * <p>Les deux comptes de démonstration peuvent déjà exister : ils sont alors réutilisés
 * tels quels, et seules les mentions manquantes indispensables à une mission (numéro
 * d'entreprise, registre national, IBAN…) sont complétées. Aucune valeur déjà saisie
 * n'est écrasée.
 *
 * <p>Le garde-fou porte sur l'adresse du premier candidat, un compte que seul ce seeder
 * crée : une fois le jeu en place, un redémarrage ne le rejoue pas.
 *
 * <p>Les contrats des missions acceptées sont produits par {@code ContractService}, donc
 * avec leur PDF sur disque. Ceux du compte de démonstration intérimaire sont signés par
 * les deux parties, aux dates où ils l'auraient été : une mission terminée depuis trois
 * mois, ou qui commence après-demain, n'a pas son contrat en souffrance. Les trois autres
 * attendent encore les deux signatures : le parcours de signature reste jouable sur les
 * données de démonstration, du côté de l'employeur.
 *
 * <p>Les entités sont créées directement par les repositories, sans passer par les
 * services : les règles métier (chevauchement de missions, clôture automatique de
 * l'offre, une seule mission en cours par candidature) sont respectées par construction
 * — chaque mission a sa propre candidature et sa propre période.
 */
@Component
@Order(2) // Après DataSeeder : les référentiels et le compte admin doivent exister.
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    /**
     * Mot de passe des comptes de démonstration, lu dans la configuration.
     *
     * <p>Il était écrit en clair dans ce fichier. Le jeu de démonstration est certes
     * désactivé par défaut, mais un identifiant valide publié dans un dépôt reste un
     * identifiant valide : il suffit que la propriété passe à vrai quelque part pour que
     * deux comptes connus de tous s'ouvrent. La valeur vit désormais dans le {@code .env},
     * au même endroit que les autres secrets, et {@link ProductionGuard} refuse de toute
     * façon un démarrage en HTTPS avec la démonstration active.
     */
    private final String demoPassword;

    /**
     * Adresses des deux comptes de démonstration, sur lesquels tout le parcours se joue.
     *
     * <p>Elles se configurent, et à défaut se déduisent de la boîte d'envoi : voir
     * {@link #demoAddress}. Ce sont les deux seules adresses du jeu qui doivent recevoir
     * pour de bon — codes de signature, propositions de mission, réinitialisation de mot
     * de passe. Le compte agence, lui, garde son adresse de configuration : rien ne lui
     * est envoyé.
     */
    private final String employerEmail;
    private final String jobSeekerEmail;

    /**
     * Adresse du compte agence, celui qui a pris les décisions consignées au journal
     * d'audit. Vide, il n'existe pas et le journal reste vide : une trace sans auteur
     * n'aurait aucune valeur, et la colonne l'interdit.
     */
    private final String adminEmail;

    /**
     * Domaine de toutes les adresses fabriquées ici.
     *
     * <p>{@code example.com} est réservé par la RFC 2606 et ne peut appartenir à
     * personne. C'est la seule garantie qui vaille : l'agence de démonstration accepte
     * ou refuse des demandes d'accès, et chaque décision envoie un email. Avec un
     * domaine qui aurait l'air vrai sans l'être, une démonstration finirait tôt ou tard
     * par écrire à un inconnu.
     */
    private static final String DOMAIN = "@example.com";

    /** Ville et province vont de pair : une offre ne peut pas être à Namur en Hainaut. */
    private record Place(String city, Province province) {
    }

    /** Le dépôt de l'employeur de démonstration, et le lieu de toutes ses missions. */
    private static final Place HOST_PLACE = new Place("Liège", Province.LIEGE);

    /**
     * Une entreprise utilisatrice, avec la personne qui la représente et son siège.
     *
     * <p>Les numéros d'entreprise portent une clé modulo 97 valide, et les commissions
     * paritaires sont celles du secteur : 124 pour la construction, 302 pour l'horeca,
     * 121 pour le nettoyage, 202 pour le commerce, 111 pour le métal, 330 pour la santé.
     * Ces deux mentions figurent sur le contrat ; approximatives, elles rendraient faux
     * le document même que l'application produit.
     */
    private record Company(
            String companyName,
            String firstName,
            String lastName,
            String email,
            String address,
            String companyNumber,
            String jointCommittee,
            Sector sector,
            Place place) {
    }

    /**
     * Les six autres entreprises qui publient sur la plateforme.
     *
     * <p>Elles existent parce qu'un seul employeur ne suffisait pas : la liste des offres
     * affiche le nom de l'entreprise à côté du secteur, et voir une société de logistique
     * recruter un aide-soignant suffit à ruiner la crédibilité de tout le jeu. Chaque
     * secteur a donc l'employeur qui lui correspond, dans une province différente — ce qui
     * donne au passage au filtre par province de quoi filtrer.
     */
    private static final Company[] COMPANIES = {
        new Company("Entreprise Wauters SRL", "Thibaut", "Wauters", "t.wauters" + DOMAIN,
                "Chaussée de Bruxelles 118, 1300 Wavre, Belgique", "0487.231.592", "124",
                Sector.CONSTRUCTION, new Place("Wavre", Province.BRABANT_WALLON)),
        new Company("Brasserie du Perron SRL", "Nadia", "Fassin", "n.fassin" + DOMAIN,
                "Place Saint-Aubain 7, 5000 Namur, Belgique", "0631.542.749", "302",
                Sector.HORECA, new Place("Namur", Province.NAMUR)),
        new Company("Net Services Wallonie SRL", "Rachid", "Belkacem", "r.belkacem" + DOMAIN,
                "Rue de la Villette 33, 6000 Charleroi, Belgique", "0812.394.695", "121",
                Sector.NETTOYAGE, new Place("Charleroi", Province.HAINAUT)),
        new Company("Supermarché Delcourt SA", "Aurélie", "Delcourt", "a.delcourt" + DOMAIN,
                "Rue Xhavée 52, 4800 Verviers, Belgique", "0543.721.820", "202",
                Sector.COMMERCE, new Place("Verviers", Province.LIEGE)),
        new Company("Ateliers du Val SA", "Grégory", "Nihoul", "g.nihoul" + DOMAIN,
                "Route de Longwy 245, 6700 Arlon, Belgique", "0921.473.670", "111",
                Sector.INDUSTRIE, new Place("Arlon", Province.LUXEMBOURG)),
        new Company("Clinique Saint-Vincent", "Dominique", "Marot", "d.marot" + DOMAIN,
                "Avenue de la Couronne 210, 1050 Bruxelles, Belgique", "0756.823.197", "330",
                Sector.SANTE, new Place("Bruxelles", Province.BRUXELLES))
    };

    /** L'employeur d'une offre et le lieu où elle s'exécute : les deux vont de pair. */
    private record Owner(User account, Place place) {
    }

    /**
     * Une exigence d'offre. Trois formes, parce que l'application en connaît trois : une
     * compétence et une langue se comparent par niveau, un diplôme se possède ou non.
     *
     * <p>Le drapeau {@code mandatory} n'est pas décoratif : une exigence obligatoire pèse
     * double dans le score et, non satisfaite, écarte le candidat de l'onglet « Pour moi ».
     */
    private record SkillNeed(String name, SkillLevel level, boolean mandatory) {
    }

    private record LanguageNeed(String name, LanguageLevel level, boolean mandatory) {
    }

    private record DegreeNeed(DegreeType type, String section, boolean mandatory) {
    }

    private static SkillNeed skill(String name, SkillLevel level, boolean mandatory) {
        return new SkillNeed(name, level, mandatory);
    }

    private static LanguageNeed tongue(String name, LanguageLevel level, boolean mandatory) {
        return new LanguageNeed(name, level, mandatory);
    }

    private static DegreeNeed diploma(DegreeType type, String section, boolean mandatory) {
        return new DegreeNeed(type, section, mandatory);
    }

    /**
     * Un métier tel qu'une agence le publie : intitulé, secteur, barème, et les exigences
     * que son annonce porterait.
     *
     * <p>Les montants sont écrits en texte parce que {@link BigDecimal} n'est exact que
     * construit ainsi : passer par un {@code double} ferait apparaître des centimes
     * fantômes sur un document contractuel.
     *
     * <p>Chaque métier exige quelque chose, et c'est la condition pour que le score de
     * correspondance veuille dire quelque chose : {@code MatchingService} calcule une
     * moyenne pondérée des critères de l'offre et, faute de critère, rend 100 %. Un jeu où
     * la plupart des offres n'exigeaient rien affichait donc « 100 % de correspondance »
     * partout — soit exactement le contraire de ce que la fonctionnalité doit démontrer.
     *
     * <p>Les niveaux demandés sont choisis en connaissance du profil de démonstration :
     * cariste et logistique avancés, gestion de stock et sécurité intermédiaires, nettoyage
     * et informatique débutants, néerlandais A2. C'est ce qui étale les scores entre le
     * quart et les neuf dixièmes, et ce qui écarte les métiers qu'un magasinier n'exerce
     * pas — maçon, soudeur, infirmier — au lieu de les lui proposer à 100 %.
     */
    private record Job(
            String title,
            Sector sector,
            String experienceTime,
            boolean vehicleMandatory,
            String salaryMin,
            String salaryMax,
            List<SkillNeed> skills,
            LanguageNeed language,
            DegreeNeed degree,
            String tasks) {
    }

    /**
     * Vingt-huit métiers, quatre par secteur, rangés de façon que les secteurs se
     * succèdent. Chacun donne une offre consultable, et une seule : la liste ne répète
     * donc jamais deux fois le même intitulé, et ne donne pas l'impression d'une seule
     * entreprise qui recrute toujours au même poste.
     */
    private static final Job[] JOBS = {
        new Job("Préparateur de commandes", Sector.LOGISTIQUE, null, false, "13.80", "15.20",
                List.of(skill("Logistique", SkillLevel.INTERMEDIAIRE, false),
                        skill("Gestion de stock", SkillLevel.AVANCE, false)),
                tongue("Néerlandais", LanguageLevel.B1, false), null,
                "Prélèvement des articles au scanner, montage et filmage des palettes, "
                        + "contrôle des quantités avant expédition."),
        new Job("Aide-maçon", Sector.CONSTRUCTION, null, true, "14.20", "16.00",
                List.of(skill("Maçonnerie", SkillLevel.DEBUTANT, true)), null, null,
                "Préparation du mortier, approvisionnement du chantier, pose de blocs et "
                        + "remise en ordre des abords en fin de journée."),
        new Job("Commis de cuisine", Sector.HORECA, null, false, "13.50", "14.90",
                List.of(skill("Cuisine", SkillLevel.DEBUTANT, true)), null, null,
                "Épluchage et découpe des légumes, mise en place avant le service, dressage "
                        + "des entrées froides et entretien du poste de travail."),
        new Job("Agent d'entretien de bureaux", Sector.NETTOYAGE, null, false, "13.30", "14.40",
                List.of(skill("Nettoyage", SkillLevel.INTERMEDIAIRE, false),
                        skill("Sécurité", SkillLevel.DEBUTANT, false)),
                null, null,
                "Nettoyage des bureaux et des sanitaires, dépoussiérage du mobilier, "
                        + "évacuation des déchets et réapprovisionnement des consommables."),
        new Job("Vendeur en magasin", Sector.COMMERCE, null, false, "13.60", "15.00",
                List.of(skill("Vente", SkillLevel.INTERMEDIAIRE, true)),
                tongue("Néerlandais", LanguageLevel.B1, false), null,
                "Accueil et conseil de la clientèle, mise en rayon, étiquetage des prix et "
                        + "tenue de la caisse en fin de journée."),
        new Job("Opérateur de production", Sector.INDUSTRIE, null, false, "14.10", "15.60",
                List.of(skill("Sécurité", SkillLevel.INTERMEDIAIRE, true),
                        skill("Mécanique", SkillLevel.DEBUTANT, false)),
                null, null,
                "Conduite d'une ligne de production, contrôle visuel des pièces, relevé des "
                        + "quantités produites et signalement des arrêts machine."),
        new Job("Infirmier hospitalier", Sector.SANTE, "2", false, "18.50", "22.00",
                List.of(), null, diploma(DegreeType.BACHELIER, "Sciences infirmières", true),
                "Préparation et distribution des médicaments, soins et pansements, "
                        + "surveillance des paramètres et tenue du dossier de soins."),
        new Job("Cariste", Sector.LOGISTIQUE, "2", true, "15.10", "17.00",
                List.of(skill("Cariste", SkillLevel.AVANCE, true),
                        skill("Sécurité", SkillLevel.AVANCE, false)),
                null, null,
                "Déchargement des camions au chariot élévateur, mise en stock des palettes et "
                        + "approvisionnement des lignes de préparation."),
        new Job("Peintre en bâtiment", Sector.CONSTRUCTION, "3", true, "15.50", "17.60",
                List.of(skill("Peinture", SkillLevel.INTERMEDIAIRE, true)), null, null,
                "Préparation des supports, enduisage et ponçage, application des couches de "
                        + "finition en intérieur comme en façade."),
        new Job("Serveur en brasserie", Sector.HORECA, "1", false, "13.90", "15.10",
                List.of(skill("Accueil", SkillLevel.INTERMEDIAIRE, false)),
                tongue("Néerlandais", LanguageLevel.B1, false), null,
                "Accueil et placement des clients, prise des commandes au terminal, service en "
                        + "salle et encaissement."),
        new Job("Nettoyeur industriel", Sector.NETTOYAGE, "2", true, "14.60", "16.20",
                List.of(skill("Nettoyage", SkillLevel.AVANCE, false),
                        skill("Sécurité", SkillLevel.AVANCE, false)),
                null, null,
                "Nettoyage des machines et des sols en zone de production, conduite d'une "
                        + "autolaveuse et respect des protocoles d'hygiène."),
        new Job("Caissier", Sector.COMMERCE, null, false, "13.40", "14.70",
                List.of(skill("Vente", SkillLevel.DEBUTANT, false)),
                tongue("Néerlandais", LanguageLevel.A2, false), null,
                "Enregistrement des achats, encaissement en espèces et par carte, comptage du "
                        + "fonds de caisse et accueil des clients."),
        new Job("Agent de conditionnement", Sector.INDUSTRIE, null, false, "13.70", "15.00",
                List.of(skill("Gestion de stock", SkillLevel.INTERMEDIAIRE, false),
                        skill("Sécurité", SkillLevel.AVANCE, false)),
                tongue("Néerlandais", LanguageLevel.B1, false), null,
                "Emballage et étiquetage des produits finis, contrôle du poids, mise en carton "
                        + "et palettisation avant expédition."),
        new Job("Brancardier", Sector.SANTE, null, false, "14.20", "15.60",
                List.of(skill("Sécurité", SkillLevel.INTERMEDIAIRE, false)),
                tongue("Néerlandais", LanguageLevel.B2, false), null,
                "Transport des patients entre les unités de soins et les services d'examen, "
                        + "désinfection des brancards et suivi des demandes."),
        new Job("Magasinier", Sector.LOGISTIQUE, "2", false, "14.50", "16.30",
                List.of(skill("Gestion de stock", SkillLevel.AVANCE, false),
                        skill("Informatique", SkillLevel.INTERMEDIAIRE, false)),
                null, null,
                "Réception et contrôle des livraisons, encodage des entrées en stock, "
                        + "inventaires tournants et préparation des retours fournisseurs."),
        new Job("Électricien de chantier", Sector.CONSTRUCTION, "5", true, "17.00", "19.50",
                List.of(skill("Électricité", SkillLevel.AVANCE, true)), null, null,
                "Pose de chemins de câbles, tirage et raccordement, montage de tableaux "
                        + "divisionnaires et mise en service des circuits."),
        new Job("Plongeur", Sector.HORECA, null, false, "13.20", "14.10",
                List.of(skill("Nettoyage", SkillLevel.INTERMEDIAIRE, false),
                        skill("Cuisine", SkillLevel.DEBUTANT, false)),
                null, null,
                "Lavage de la vaisselle et du matériel de cuisine, entretien des plans de "
                        + "travail et sortie des déchets en fin de service."),
        new Job("Agent de nettoyage hospitalier", Sector.NETTOYAGE, "1", false, "14.00", "15.30",
                List.of(skill("Nettoyage", SkillLevel.INTERMEDIAIRE, false)),
                tongue("Néerlandais", LanguageLevel.B2, false), null,
                "Bionettoyage des chambres et des couloirs, désinfection des points de contact "
                        + "et respect du circuit du linge."),
        new Job("Réassortisseur en grande surface", Sector.COMMERCE, null, false, "13.70", "15.10",
                List.of(skill("Gestion de stock", SkillLevel.INTERMEDIAIRE, true),
                        skill("Vente", SkillLevel.DEBUTANT, false)),
                null, null,
                "Mise en rayon de la livraison du matin, rotation des dates, facing des "
                        + "linéaires et gestion des retours fournisseurs."),
        new Job("Soudeur MIG-MAG", Sector.INDUSTRIE, "5", true, "17.50", "20.00",
                List.of(skill("Mécanique", SkillLevel.AVANCE, true)), null, null,
                "Assemblage de pièces en acier sur gabarit, soudure semi-automatique, meulage "
                        + "des cordons et contrôle visuel des soudures."),
        new Job("Agent d'accueil en clinique", Sector.SANTE, null, false, "14.00", "15.40",
                List.of(skill("Accueil", SkillLevel.INTERMEDIAIRE, true)),
                tongue("Néerlandais", LanguageLevel.B1, false), null,
                "Accueil des patients et des visiteurs, orientation vers les consultations, "
                        + "tenue du standard et encodage des admissions."),
        new Job("Agent de quai", Sector.LOGISTIQUE, null, true, "14.00", "15.50",
                List.of(skill("Logistique", SkillLevel.INTERMEDIAIRE, false),
                        skill("Sécurité", SkillLevel.AVANCE, false)),
                tongue("Néerlandais", LanguageLevel.B1, false), null,
                "Chargement et déchargement des remorques, tri des colis par tournée, scannage "
                        + "et signalement des marchandises endommagées."),
        new Job("Menuisier d'atelier", Sector.CONSTRUCTION, "3", false, "16.00", "18.00",
                List.of(skill("Menuiserie", SkillLevel.INTERMEDIAIRE, true)), null, null,
                "Débit et usinage des panneaux, assemblage de châssis et de meubles sur plan, "
                        + "ponçage et finition avant livraison."),
        new Job("Chef de partie", Sector.HORECA, "5", false, "16.50", "19.00",
                List.of(skill("Cuisine", SkillLevel.AVANCE, true)), null, null,
                "Production des plats de sa partie, dressage au moment du service, gestion des "
                        + "mises en place et suivi des stocks du poste."),
        new Job("Laveur de vitres", Sector.NETTOYAGE, "2", true, "15.00", "16.80",
                List.of(skill("Nettoyage", SkillLevel.AVANCE, true)), null, null,
                "Nettoyage des vitrages intérieurs et des façades accessibles depuis le sol, "
                        + "usage de la perche télescopique et de la raclette."),
        new Job("Conseiller en téléphonie", Sector.COMMERCE, "2", false, "14.80", "16.60",
                List.of(skill("Vente", SkillLevel.AVANCE, true)),
                tongue("Néerlandais", LanguageLevel.B2, false),
                diploma(DegreeType.BACHELIER, "Commerce", false),
                "Conseil sur les abonnements et les appareils, ouverture de lignes, vente "
                        + "d'accessoires et suivi du service après-vente."),
        new Job("Technicien de maintenance", Sector.INDUSTRIE, "5", true, "18.00", "21.00",
                List.of(skill("Mécanique", SkillLevel.AVANCE, true)), null,
                diploma(DegreeType.BACHELIER, "Électromécanique", false),
                "Entretien préventif des machines, dépannage mécanique et pneumatique, "
                        + "remplacement des pièces d'usure et tenue du carnet d'entretien."),
        new Job("Aide en maison de repos", Sector.SANTE, null, false, "14.30", "15.90",
                List.of(skill("Nettoyage", SkillLevel.DEBUTANT, false),
                        skill("Accueil", SkillLevel.INTERMEDIAIRE, false)),
                tongue("Néerlandais", LanguageLevel.B1, false), null,
                "Service des repas, aide aux déplacements, entretien des chambres et "
                        + "accompagnement des résidents dans les activités.")
    };


    /** Les horaires réellement pratiqués : une offre sans horaire n'est pas une offre. */
    private static final String[] SCHEDULES = {
        "Horaire de jour, de 8 h à 16 h 30, du lundi au vendredi.",
        "Travail en deux pauses, 6 h - 14 h et 14 h - 22 h, une semaine sur deux.",
        "Horaire de jour du lundi au vendredi, avec un samedi sur trois.",
        "Prestations de 7 h 30 à 16 h, avec une pause de midi d'une demi-heure."
    };

    /** La phrase par laquelle une annonce se termine, et qui varie d'un employeur à l'autre. */
    private static final String[] CLOSINGS = {
        "Mission de longue durée, renouvelable en cas de bonne collaboration.",
        "Entrée en fonction rapide, après un entretien dans nos bureaux.",
        "Les premiers jours se font en binôme avec un collègue expérimenté.",
        "Le matériel et les vêtements de travail sont fournis par l'entreprise."
    };

    /**
     * Un candidat de démonstration : identité, contact, domicile et les deux mentions
     * légales sans lesquelles aucun contrat ne peut être établi à son nom.
     *
     * <p>Le registre national suit sa date de naissance et porte sa clé modulo 97, l'IBAN
     * la sienne : ces deux valeurs sont imprimées sur le contrat, et l'application les
     * contrôle à la saisie. Un candidat sans elles ne pouvait pas être détaché en mission,
     * ce qui ne laissait qu'un seul intérimaire à l'écran de l'employeur.
     */
    private record Candidate(
            String firstName,
            String lastName,
            String email,
            String address,
            LocalDate birthdate,
            boolean hasVehicle,
            String nationalNumber,
            String iban) {
    }

    /**
     * Les six candidats de la première offre. Leurs noms viennent d'horizons différents,
     * comme la population qu'une agence liégeoise reçoit réellement : un jeu où tout le
     * monde s'appellerait Dupont ne dirait rien de la façon dont l'application affiche un
     * nom composé ou accentué.
     */
    private static final Candidate[] CANDIDATES = {
        new Candidate("Amélie", "Dubois", "amelie.dubois" + DOMAIN,
                "Rue Saint-Gilles 145, 4000 Liège, Belgique", LocalDate.of(1993, 6, 18), true,
                "93.06.18-424.61", "BE12 4321 0987 6592"),
        new Candidate("Bruno", "Lemaire", "bruno.lemaire" + DOMAIN,
                "Avenue de la Constitution 22, 4020 Liège, Belgique", LocalDate.of(1986, 11, 2), true,
                "86.11.02-157.17", "BE14 0631 2345 6783"),
        new Candidate("Chloé", "Vandeputte", "chloe.vandeputte" + DOMAIN,
                "Rue de Fragnée 78, 4000 Liège, Belgique", LocalDate.of(1998, 3, 27), false,
                "98.03.27-208.91", "BE36 7340 1234 5681"),
        new Candidate("David", "Mercier", "david.mercier" + DOMAIN,
                "Rue de la Station 12, 4032 Chênée, Belgique", LocalDate.of(1979, 9, 14), true,
                "79.09.14-331.01", "BE48 0011 2345 6727"),
        new Candidate("Elena", "Rossi", "elena.rossi" + DOMAIN,
                "Rue du Vinâve 9, 4100 Seraing, Belgique", LocalDate.of(1991, 1, 30), false,
                "91.01.30-116.29", "BE89 6511 2345 9885"),
        new Candidate("Farid", "Benali", "farid.benali" + DOMAIN,
                "Rue Émile Vandervelde 61, 4420 Saint-Nicolas, Belgique", LocalDate.of(1988, 7, 8), true,
                "88.07.08-273.26", "BE02 3630 9876 5440")
    };

    /** Une entreprise qui demande l'accès, et la personne qui la représente. */
    private record Applicant(
            String firstName,
            String lastName,
            String company,
            String email,
            String address,
            String message) {
    }

    /**
     * Neuf demandes d'accès : les trois premières en attente, les six autres déjà
     * traitées. Ce sont des PME wallonnes et bruxelloises de tailles et de secteurs
     * différents, parce que c'est exactement ce qu'une agence reçoit — et que la colonne
     * « société » doit tenir aussi bien un nom court qu'un nom de trente caractères.
     *
     * <p>Deux des trois demandes en attente portent un message de motivation, la
     * troisième non : la file étant courte, c'est la seule façon d'y voir les deux cas.
     */
    private static final Applicant[] APPLICANTS = {
        new Applicant("Marc", "Delvaux", "Boulangerie Delvaux SRL", "m.delvaux" + DOMAIN,
                "Rue de la Station 14, 5000 Namur, Belgique",
                "Nous cherchons un renfort en boulangerie les week-ends et pendant les congés de notre équipe."),
        new Applicant("Sophie", "Grégoire", "Transports Grégoire SA", "s.gregoire" + DOMAIN,
                "Rue du Zoning 8, 6040 Jumet, Belgique", null),
        new Applicant("Ahmed", "Kacem", "Clean Partner SRL", "a.kacem" + DOMAIN,
                "Chaussée de Mons 210, 1070 Anderlecht, Belgique",
                "Nous nettoyons des bureaux à Bruxelles et cherchons des renforts pour les absences "
                        + "de dernière minute."),
        new Applicant("Isabelle", "Docquier", "Résidence Les Tilleuls", "i.docquier" + DOMAIN,
                "Rue du Calvaire 3, 4520 Wanze, Belgique",
                "Nous engageons chaque année une dizaine d'intérimaires pour les remplacements de congés."),
        new Applicant("Pierre", "Deflandre", "Menuiserie Deflandre SRL", "p.deflandre" + DOMAIN,
                "Rue des Ateliers 47, 4800 Verviers, Belgique", null),
        new Applicant("Fatima", "Amrani", "Horeca Service Namur SRL", "f.amrani" + DOMAIN,
                "Place de l'Ange 12, 5000 Namur, Belgique", null),
        new Applicant("Laurent", "Hubin", "Hubin Toitures SRL", "l.hubin" + DOMAIN,
                "Rue de Huy 88, 4300 Waremme, Belgique", null),
        new Applicant("Céline", "Warnier", "Pharma Distri Wallonie", "c.warnier" + DOMAIN,
                "Parc scientifique 5, 1348 Louvain-la-Neuve, Belgique",
                "Notre dépôt tourne en deux pauses ; nous manquons de bras en préparation de commandes."),
        new Applicant("Thomas", "Piette", "Ardenne Métal SA", "t.piette" + DOMAIN,
                "Route de Bastogne 120, 6700 Arlon, Belgique", null)
    };

    /**
     * Les quatre fils de discussion, l'employeur écrivant le premier puis en alternance.
     *
     * <p>Le premier compte trente-cinq messages, seul moyen de voir apparaître « Voir les
     * messages plus anciens ». Il porte sur la première offre — préparateur de commandes
     * à Liège — et reprend ses conditions réelles : l'horaire, la fourchette salariale et
     * le montant des chèques-repas y sont ceux de l'annonce. Une conversation de
     * remplissage aurait donné le même nombre de lignes, mais aurait laissé invérifiable
     * ce à quoi ressemble un fil long dans l'interface.
     */
    private static final String[][] CONVERSATIONS = {
        {
            "Bonjour Amélie, merci pour votre candidature au poste de préparateur de commandes. "
                    + "Votre profil correspond à ce que nous cherchons.",
            "Bonjour Madame, merci pour votre retour. Le poste m'intéresse toujours, je suis "
                    + "disponible pour en discuter.",
            "Parfait. Seriez-vous disponible pour un entretien dans nos bureaux la semaine prochaine ?",
            "Oui, sans problème. Mardi ou jeudi matin me conviendraient le mieux.",
            "Disons mardi à 10 h, rue de l'Île Monsin 44 à Liège. L'entretien dure environ trois "
                    + "quarts d'heure.",
            "C'est noté pour mardi 10 h. Dois-je apporter des documents particuliers ?",
            "Votre carte d'identité et, si vous l'avez, votre brevet cariste. Le reste, nous le "
                    + "remplirons ensemble.",
            "Je n'ai pas encore le brevet cariste, mais j'ai deux ans d'expérience en préparation "
                    + "de commandes au scanner.",
            "Ce n'est pas bloquant pour ce poste : le scanner et le transpalette électrique suffisent.",
            "Très bien. Le transpalette électrique, je l'utilise quotidiennement depuis deux ans.",
            "Noté. L'horaire est de 8 h à 16 h 30, du lundi au vendredi. Est-ce compatible avec vos "
                    + "disponibilités ?",
            "Oui, tout à fait. Je dépose mon fils à l'école à 7 h 45, j'arrive facilement pour 8 h.",
            "Parfait. Le dépôt est desservi par le bus, arrêt Monsin, si vous ne venez pas en voiture.",
            "Je viens en voiture. Y a-t-il un parking sur place ?",
            "Oui, un parking gratuit derrière le bâtiment, à l'entrée du zoning.",
            "Impeccable, merci. À mardi alors.",
            "À mardi. Si un imprévu survient, écrivez-moi ici, je lis les messages dans la journée.",
            "Bonjour, une question avant mardi : la mission est prévue pour combien de temps ?",
            "Trois semaines au départ, avec une prolongation probable jusqu'à la fin de l'année.",
            "Et le salaire annoncé dans l'offre, entre 13,80 et 15,20 euros, dépend de quoi ?",
            "De l'expérience utile au poste. Avec vos deux ans, nous partirions sur 14,40 euros brut "
                    + "de l'heure.",
            "Cela me convient. Des chèques-repas sont-ils prévus ?",
            "Oui, 8 euros par jour presté, dont 1,09 euro à votre charge, comme le prévoit la loi.",
            "Merci pour les précisions. Je prépare mes documents pour mardi.",
            "Bonjour Amélie, l'entretien de mardi est confirmé. Mon collègue du dépôt y assistera "
                    + "également.",
            "Bien reçu, merci. Faut-il prévoir des chaussures de sécurité pour la visite du dépôt ?",
            "Nous en avons en prêt à l'accueil, du 36 au 47. Donnez-moi simplement votre pointure.",
            "Je fais du 39, merci.",
            "C'est noté. Nous ferons le tour de l'entrepôt après l'entretien, comptez une demi-heure "
                    + "de plus.",
            "Parfait, je bloque toute la matinée.",
            "Merci. Dernière chose : avez-vous déjà travaillé en intérim ? Cela change les documents "
                    + "à signer.",
            "Oui, deux missions l'an dernier chez un transporteur. J'ai encore mon numéro de registre "
                    + "national sous la main.",
            "Très bien, cela ira plus vite. L'agence s'occupe du contrat une fois la mission validée.",
            "Entendu. À mardi matin, et merci pour votre disponibilité.",
            "À mardi, Amélie. Bonne fin de journée."
        },
        {
            "Bonjour Bruno, votre candidature nous est bien parvenue. Seriez-vous disponible dès la "
                    + "semaine prochaine ?",
            "Bonjour, oui, je suis libre à partir de lundi.",
            "Parfait, je reviens vers vous dès que l'horaire est arrêté avec le responsable du dépôt."
        },
        {
            "Bonjour Chloé, merci pour votre candidature. Avez-vous déjà travaillé en entrepôt ?",
            "Bonjour, oui, un an chez un distributeur alimentaire, principalement au picking.",
            "Très bien. Le poste demande de porter des charges jusqu'à 15 kg, cela vous convient-il ?",
            "Aucun souci, c'était déjà le cas dans mon poste précédent."
        },
        {
            "Bonjour David, votre profil nous intéresse. Quelles sont vos disponibilités ce mois-ci ?",
            "Bonjour, je termine une mission le 15 et je suis disponible ensuite."
        }
    };

    /**
     * Les motifs par lesquels l'agence refuse une mission. Ils sont tous tirés d'une règle
     * réelle du travail intérimaire : c'est ce qui distingue un refus d'un simple rejet.
     */
    private static final String[] REFUSALS = {
        "Le salaire proposé n'atteint pas celui d'un travailleur permanent occupant la "
                + "même fonction dans l'entreprise.",
        "Le motif de recours n'est pas établi : aucun surcroît de travail n'est documenté.",
        "La durée demandée dépasse la limite admise pour ce motif de recours.",
        "La fonction décrite ne relève pas de la commission paritaire annoncée.",
        "Le salaire horaire proposé est inférieur au minimum de la commission paritaire "
                + "dont relève l'entreprise utilisatrice.",
        "L'entreprise utilisatrice a déjà atteint le nombre d'intérimaires autorisé pour ce motif."
    };

    /** Les travailleurs remplacés, mention légale obligatoire quand le motif est un remplacement. */
    private static final String[] REPLACED = {
        "Sabrina Kaya", "Thierry Colinet", "Murielle Jacobs",
        "Damien Poncelet", "Nathalie Wilmots", "José Da Silva"
    };

    /** Les « conditions particulières » du contrat : celles qu'un employeur écrit vraiment. */
    private static final String[] NOTES = {
        "Chaussures de sécurité et gilet fluorescent fournis par l'entreprise utilisatrice.",
        "Se présenter à l'accueil du dépôt le premier jour à 7 h 45 pour la remise du badge.",
        "Le port de charges est limité à 15 kg ; un transpalette électrique est mis à disposition.",
        "Parking gratuit derrière le bâtiment, à l'entrée du zoning.",
        "Vêtements de travail fournis, leur entretien restant à charge de l'entreprise utilisatrice."
    };

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventRepository auditEventRepository;
    private final EmployerAccessRequestRepository accessRequestRepository;
    private final JobOfferRepository jobOfferRepository;
    private final ApplicationRepository applicationRepository;
    private final MissionRepository missionRepository;
    private final DailyScheduleRepository dailyScheduleRepository;
    private final ContractRepository contractRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SkillRepository skillRepository;
    private final SkillUserRepository skillUserRepository;
    private final SkillJobOfferRepository skillJobOfferRepository;
    private final DegreeRepository degreeRepository;
    private final DegreeJobOfferRepository degreeJobOfferRepository;
    private final DegreeUserRepository degreeUserRepository;
    private final LanguageRepository languageRepository;
    private final LanguageUserRepository languageUserRepository;
    private final LanguageJobOfferRepository languageJobOfferRepository;
    private final ExperienceRepository experienceRepository;
    private final FormationRepository formationRepository;
    private final ContractService contractService;

    public DemoDataSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditEventRepository auditEventRepository,
            EmployerAccessRequestRepository accessRequestRepository,
            JobOfferRepository jobOfferRepository,
            ApplicationRepository applicationRepository,
            MissionRepository missionRepository,
            DailyScheduleRepository dailyScheduleRepository,
            ContractRepository contractRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            SkillRepository skillRepository,
            SkillUserRepository skillUserRepository,
            SkillJobOfferRepository skillJobOfferRepository,
            DegreeRepository degreeRepository,
            DegreeJobOfferRepository degreeJobOfferRepository,
            DegreeUserRepository degreeUserRepository,
            LanguageRepository languageRepository,
            LanguageUserRepository languageUserRepository,
            LanguageJobOfferRepository languageJobOfferRepository,
            ExperienceRepository experienceRepository,
            FormationRepository formationRepository,
            ContractService contractService,
            @Value("${app.demo-data.password}") String demoPassword,
            @Value("${app.demo-data.employer-email:}") String employerEmail,
            @Value("${app.demo-data.jobseeker-email:}") String jobSeekerEmail,
            @Value("${app.mail.from}") String mailbox,
            @Value("${app.admin.email:}") String adminEmail) {
        this.adminEmail = adminEmail;
        this.demoPassword = demoPassword;
        this.employerEmail = demoAddress(employerEmail, "employer", mailbox);
        this.jobSeekerEmail = demoAddress(jobSeekerEmail, "jobseeker", mailbox);
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditEventRepository = auditEventRepository;
        this.accessRequestRepository = accessRequestRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.applicationRepository = applicationRepository;
        this.missionRepository = missionRepository;
        this.dailyScheduleRepository = dailyScheduleRepository;
        this.contractRepository = contractRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.skillRepository = skillRepository;
        this.skillUserRepository = skillUserRepository;
        this.skillJobOfferRepository = skillJobOfferRepository;
        this.degreeRepository = degreeRepository;
        this.degreeJobOfferRepository = degreeJobOfferRepository;
        this.degreeUserRepository = degreeUserRepository;
        this.languageRepository = languageRepository;
        this.languageUserRepository = languageUserRepository;
        this.languageJobOfferRepository = languageJobOfferRepository;
        this.experienceRepository = experienceRepository;
        this.formationRepository = formationRepository;
        this.contractService = contractService;
    }

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        // Toute la pose se fait dans une transaction : interrompue, elle ne laisse pas
        // un jeu de données à moitié construit que le garde-fou empêcherait de refaire.
        if (userRepository.findByEmail(CANDIDATES[0].email()).isPresent()) {
            return;
        }

        User employer = employerAccount();
        User jobSeeker = jobSeekerAccount();
        Map<Sector, Owner> owners = owners(employer);
        List<JobOffer> openOffers = createBrowsableOffers(owners);
        createApplications(jobSeeker, openOffers);
        // Les candidats viennent avant les missions : ce sont eux qui les remplissent.
        List<User> workers = createOtherCandidates(employer, openOffers.get(0));
        List<Mission> missions = createMissions(employer, jobSeeker, workers);
        List<EmployerAccessRequest> decided = createPendingEmployerRequests();
        createAuditTrail(jobSeeker, missions, decided);
    }

    // ----------------------------------------------------------------- comptes

    /**
     * L'adresse d'un compte de démonstration : celle qui est configurée, ou à défaut la
     * boîte d'envoi elle-même, sous-adressée.
     *
     * <p>Les valeurs par défaut étaient {@code test@employer.com} et
     * {@code test@jobseeker.com}, deux domaines qui n'appartiennent à personne. Or c'est
     * sur ces deux comptes que tout le parcours se joue : le code de signature du
     * contrat, la proposition de mission, la réinitialisation du mot de passe leur sont
     * adressés. Une installation qui ne renseignait pas les deux variables envoyait donc
     * tout cela dans le vide, sans que rien ne le signale.
     *
     * <p>La boîte d'envoi, elle, est nécessairement relevée : sans elle, aucun message ne
     * partirait. La déduire donne une adresse distincte par compte et une seule boîte à
     * l'arrivée — {@code tfeagence@gmail.com} devient {@code tfeagence+employer@gmail.com}
     * — le sous-adressage étant accepté par tous les grands fournisseurs. Une étiquette
     * déjà présente est remplacée, faute de quoi les deux comptes finiraient sur la même
     * adresse et le second refuserait de s'enregistrer.
     */
    private static String demoAddress(String configured, String alias, String mailbox) {
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        int at = mailbox.lastIndexOf('@');
        if (at <= 0) {
            throw new IllegalStateException(
                    "Le jeu de démonstration déduit ses deux adresses de app.mail.from, qui doit être "
                            + "une adresse email : « " + mailbox + " » n'en est pas une.");
        }
        String local = mailbox.substring(0, at);
        int tag = local.indexOf('+');
        String address = (tag < 0 ? local : local.substring(0, tag)) + "+" + alias + mailbox.substring(at);
        if (address.length() > User.EMAIL_MAX_LENGTH) {
            throw new IllegalStateException(
                    "L'adresse déduite « " + address + " » dépasse les "
                            + User.EMAIL_MAX_LENGTH + " caractères de la colonne. Renseignez "
                            + "app.demo-data.employer-email et app.demo-data.jobseeker-email, ou "
                            + "utilisez une boîte d'envoi au nom plus court.");
        }
        return address;
    }

    /**
     * Le compte employeur de démonstration, créé s'il n'existe pas. S'il existe, seules
     * les mentions légales absentes sont complétées : sans elles, l'application refuse
     * la création d'une mission.
     */
    private User employerAccount() {
        User employer = userRepository.findByEmail(employerEmail).orElseGet(() -> {
            User created = newUser(employerEmail, "Céline", "Vandenberghe", Role.EMPLOYER);
            created.setCompanyName("Logistique Meuse SRL");
            User saved = userRepository.save(created);

            EmployerAccessRequest accepted = new EmployerAccessRequest();
            accepted.setUser(saved);
            accepted.setRequestDate(LocalDate.now().minusDays(60));
            accepted.setStatus(EmployerAccessStatus.ACCEPTED);
            accessRequestRepository.save(accepted);
            return saved;
        });

        employer.setRole(Role.EMPLOYER);
        fillIfBlank(employer.getCompanyName(), employer::setCompanyName, "Logistique Meuse SRL");
        fillIfBlank(employer.getAddress(), employer::setAddress, "Rue de l'Île Monsin 44, 4020 Liège, Belgique");
        // Numéro BCE valide (clé modulo 97), sinon la fiche entreprise reste incomplète.
        fillIfBlank(employer.getCompanyNumber(), employer::setCompanyNumber, "0987.654.394");
        // 140.03 : transport et logistique pour compte de tiers, la commission dont
        // relèverait réellement une entreprise de ce type.
        fillIfBlank(employer.getJointCommittee(), employer::setJointCommittee, "140.03");
        return userRepository.save(employer);
    }

    /** Le compte intérimaire de démonstration, avec un profil assez fourni pour le matching. */
    private User jobSeekerAccount() {
        User jobSeeker = userRepository.findByEmail(jobSeekerEmail).orElseGet(() -> {
            User created = newUser(jobSeekerEmail, "Nicolas", "Lambert", Role.JOBSEEKER);
            created.setBirthdate(LocalDate.of(1995, 4, 12));
            created.setHasVehicle(true);
            return userRepository.save(created);
        });

        if (jobSeeker.getHasVehicle() == null) {
            jobSeeker.setHasVehicle(true);
        }
        fillIfBlank(jobSeeker.getAddress(), jobSeeker::setAddress, "Rue Saint-Léonard 128, 4000 Liège, Belgique");
        // Registre national et IBAN valides : sans eux, aucune mission ne peut être acceptée.
        // Le premier suit la date de naissance ci-dessus, comme un vrai numéro le fait.
        fillIfBlank(jobSeeker.getNationalNumber(), jobSeeker::setNationalNumber, "95.04.12-123.85");
        fillIfBlank(jobSeeker.getIban(), jobSeeker::setIban, "BE68 5390 0754 7034");
        User saved = userRepository.save(jobSeeker);

        addSkills(saved);
        addDegree(saved);
        addLanguages(saved);
        addExperiences(saved);
        addFormations(saved);
        return saved;
    }

    /** N'écrit la valeur de démonstration que si l'utilisateur n'a rien saisi. */
    private void fillIfBlank(String current, Consumer<String> setter, String fallback) {
        if (current == null || current.isBlank()) {
            setter.accept(fallback);
        }
    }

    private User newUser(String email, String firstName, String lastName, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(demoPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        return user;
    }

    // ------------------------------------------------------------- profil type

    private void addSkills(User jobSeeker) {
        record Owned(String name, SkillLevel level) {
        }
        List<Owned> owned = List.of(
                new Owned("Cariste", SkillLevel.AVANCE),
                new Owned("Logistique", SkillLevel.AVANCE),
                new Owned("Gestion de stock", SkillLevel.INTERMEDIAIRE),
                new Owned("Sécurité", SkillLevel.INTERMEDIAIRE),
                new Owned("Nettoyage", SkillLevel.DEBUTANT),
                new Owned("Informatique", SkillLevel.DEBUTANT));
        for (Owned entry : owned) {
            skillRepository.findFirstByNameIgnoreCaseAndIsGlobalTrue(entry.name()).ifPresent(skill -> {
                if (skillUserRepository.existsByUserIdAndSkillId(jobSeeker.getId(), skill.getId())) {
                    return;
                }
                SkillUser link = new SkillUser();
                link.setUser(jobSeeker);
                link.setSkill(skill);
                link.setLevel(entry.level());
                skillUserRepository.save(link);
            });
        }
    }

    private void addDegree(User jobSeeker) {
        degreeRepository.findFirstByTypeAndSectionIgnoreCaseAndIsGlobalTrue(DegreeType.BACHELIER, "Commerce")
                .ifPresent(degree -> {
                    if (degreeUserRepository.existsByUserIdAndDegreeId(jobSeeker.getId(), degree.getId())) {
                        return;
                    }
                    DegreeUser link = new DegreeUser();
                    link.setUser(jobSeeker);
                    link.setDegree(degree);
                    link.setInstitution("Haute École de la Province de Liège");
                    link.setGraduationYear(2016);
                    degreeUserRepository.save(link);
                });
    }

    private void addLanguages(User jobSeeker) {
        record Spoken(String name, LanguageLevel level) {
        }
        List<Spoken> spoken = List.of(
                new Spoken("Français", LanguageLevel.C2),
                new Spoken("Anglais", LanguageLevel.B2),
                new Spoken("Néerlandais", LanguageLevel.A2));
        for (Spoken entry : spoken) {
            language(entry.name()).ifPresent(language -> {
                if (languageUserRepository.existsByUserIdAndLanguageId(jobSeeker.getId(), language.getId())) {
                    return;
                }
                LanguageUser link = new LanguageUser();
                link.setUser(jobSeeker);
                link.setLanguage(language);
                link.setLevel(entry.level());
                languageUserRepository.save(link);
            });
        }
    }

    /** La liste des langues est fixe et courte : une recherche par nom en mémoire suffit. */
    private Optional<Language> language(String name) {
        return languageRepository.findAllByOrderByNameAsc().stream()
                .filter(entry -> entry.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    private void addExperiences(User jobSeeker) {
        if (!experienceRepository.findByUserIdOrderByStartDateDesc(jobSeeker.getId()).isEmpty()) {
            return;
        }
        experienceRepository.save(experience(
                jobSeeker, "Transports Delvaux", "Cariste",
                LocalDate.now().minusYears(6), LocalDate.now().minusYears(3)));
        experienceRepository.save(experience(
                jobSeeker, "Entrepôts du Sart", "Magasinier",
                LocalDate.now().minusYears(3), null));
    }

    private Experience experience(User user, String company, String position, LocalDate from, LocalDate to) {
        Experience experience = new Experience();
        experience.setUser(user);
        experience.setCompanyName(company);
        experience.setPosition(position);
        experience.setStartDate(from);
        experience.setEndDate(to);
        return experience;
    }

    private void addFormations(User jobSeeker) {
        if (!formationRepository.findByUserIdOrderByStartDateDesc(jobSeeker.getId()).isEmpty()) {
            return;
        }
        Formation certificate = new Formation();
        certificate.setUser(jobSeeker);
        certificate.setTitle("Brevet cariste (chariot élévateur)");
        certificate.setInstitution("Forem");
        certificate.setStartDate(LocalDate.now().minusYears(7));
        certificate.setEndDate(LocalDate.now().minusYears(7).plusMonths(2));
        certificate.setStatus(FormationStatus.TERMINE);
        formationRepository.save(certificate);

        Formation ongoing = new Formation();
        ongoing.setUser(jobSeeker);
        ongoing.setTitle("Néerlandais professionnel");
        ongoing.setInstitution("Enseignement de promotion sociale de Liège");
        ongoing.setStartDate(LocalDate.now().minusMonths(4));
        ongoing.setStatus(FormationStatus.EN_COURS);
        formationRepository.save(ongoing);
    }

    // ------------------------------------------------------------------ offres

    /**
     * Les comptes des six autres entreprises, chacun avec sa demande d'accès acceptée :
     * un employeur qui publie sans que l'agence l'ait jamais agréé n'existe pas dans le
     * parcours de l'application.
     */
    private Map<Sector, Owner> owners(User host) {
        Map<Sector, Owner> owners = new EnumMap<>(Sector.class);
        owners.put(Sector.LOGISTIQUE, new Owner(host, HOST_PLACE));
        for (int index = 0; index < COMPANIES.length; index += 1) {
            Company company = COMPANIES[index];
            owners.put(company.sector(), new Owner(sectorEmployer(company, 40L + index * 9L), company.place()));
        }
        return owners;
    }

    private User sectorEmployer(Company company, long acceptedDaysAgo) {
        User account = newUser(company.email(), company.firstName(), company.lastName(), Role.EMPLOYER);
        account.setCompanyName(company.companyName());
        account.setAddress(company.address());
        account.setCompanyNumber(company.companyNumber());
        account.setJointCommittee(company.jointCommittee());
        User saved = userRepository.save(account);

        EmployerAccessRequest accepted = new EmployerAccessRequest();
        accepted.setUser(saved);
        accepted.setRequestDate(LocalDate.now().minusDays(acceptedDaysAgo));
        accepted.setStatus(EmployerAccessStatus.ACCEPTED);
        accessRequestRepository.save(accepted);
        return saved;
    }

    /** Le catalogue entier, soit 28 offres ouvertes : trois pages côté intérimaire. */
    private List<JobOffer> createBrowsableOffers(Map<Sector, Owner> owners) {
        List<JobOffer> offers = new ArrayList<>();
        for (int index = 0; index < JOBS.length; index += 1) {
            Job job = JOBS[index];
            Owner owner = owners.get(job.sector());
            offers.add(publish(owner.account(), job, owner.place(), index, JobOfferStatus.OPEN));
        }
        return offers;
    }

    /**
     * Enregistre l'offre avec les exigences de son métier.
     *
     * <p>Les deux chemins qui créent une offre passent par ici, et c'est le but : les
     * offres adossées aux missions restent ouvertes tant que la mission n'est pas active,
     * donc visibles dans « Pour moi ». Créées sans la moindre exigence, elles y
     * affichaient toutes 100 %.
     */
    private JobOffer publish(User employer, Job job, Place place, int index, JobOfferStatus status) {
        JobOffer saved = jobOfferRepository.save(offer(employer, job, place, index, status));
        for (SkillNeed need : job.skills()) {
            requireSkill(saved, need);
        }
        if (job.language() != null) {
            requireLanguage(saved, job.language());
        }
        if (job.degree() != null) {
            requireDegree(saved, job.degree());
        }
        return saved;
    }

    private JobOffer offer(User employer, Job job, Place place, int index, JobOfferStatus status) {
        JobOffer offer = new JobOffer();
        offer.setEmployer(employer);
        offer.setTitle(job.title());
        offer.setSector(job.sector());
        offer.setCity(place.city());
        offer.setProvince(place.province());
        // Les tâches, l'horaire et la phrase de clôture : les trois choses qu'une annonce
        // dit toujours, et sans lesquelles une offre de démonstration reste un gabarit.
        offer.setDescription(job.tasks()
                + " " + SCHEDULES[index % SCHEDULES.length]
                + " " + CLOSINGS[(index / 2) % CLOSINGS.length]);
        // Publications échelonnées : la liste est triée de la plus récente à la plus ancienne.
        offer.setPublishedAt(LocalDateTime.now().minusDays(index).minusHours(index));
        offer.setSalaryMin(new BigDecimal(job.salaryMin()));
        offer.setSalaryMax(new BigDecimal(job.salaryMax()));
        offer.setExperienceTime(job.experienceTime());
        offer.setVehicleMandatory(job.vehicleMandatory());
        // Deux offres sur trois accordent des chèques-repas. 8 euros est le maximum légal,
        // 6,50 une valeur courante : au-delà, le montant ne serait plus exonéré.
        offer.setMealVoucherAmount(switch (index % 3) {
            case 0 -> new BigDecimal("8.00");
            case 1 -> new BigDecimal("6.50");
            default -> null;
        });
        offer.setStatus(status);
        return offer;
    }

    private void requireSkill(JobOffer offer, SkillNeed need) {
        skillRepository.findFirstByNameIgnoreCaseAndIsGlobalTrue(need.name()).ifPresent(skill -> {
            SkillJobOffer requirement = new SkillJobOffer();
            requirement.setJobOffer(offer);
            requirement.setSkill(skill);
            requirement.setIsMandatory(need.mandatory());
            requirement.setRequiredLevel(need.level());
            skillJobOfferRepository.save(requirement);
        });
    }

    private void requireLanguage(JobOffer offer, LanguageNeed need) {
        language(need.name()).ifPresent(language -> {
            LanguageJobOffer requirement = new LanguageJobOffer();
            requirement.setJobOffer(offer);
            requirement.setLanguage(language);
            requirement.setIsMandatory(need.mandatory());
            requirement.setRequiredLevel(need.level());
            languageJobOfferRepository.save(requirement);
        });
    }

    /**
     * Le diplôme exigé se rattache à une entrée du référentiel global, et le type comme la
     * section sont recopiés sur l'exigence : c'est ce que fait le service quand un
     * employeur en ajoute une, et le contrôle de correspondance compare l'identifiant.
     */
    private void requireDegree(JobOffer offer, DegreeNeed need) {
        degreeRepository.findFirstByTypeAndSectionIgnoreCaseAndIsGlobalTrue(need.type(), need.section())
                .ifPresent(degree -> {
                    DegreeJobOffer requirement = new DegreeJobOffer();
                    requirement.setJobOffer(offer);
                    requirement.setDegree(degree);
                    requirement.setIsMandatory(need.mandatory());
                    requirement.setRequiredType(need.type());
                    requirement.setRequiredSection(need.section());
                    degreeJobOfferRepository.save(requirement);
                });
    }

    // ------------------------------------------------- candidatures du profil type

    /**
     * Les candidatures libres du compte de démonstration.
     *
     * <p>Aucune offre n'est mise de côté. Le seeder en marquait quatre, ce qui présentait
     * le compte avec une liste déjà garnie sans qu'on sache qui l'avait garnie. Mettre une
     * offre de côté est un geste de l'utilisateur : il se montre mieux fait en direct que
     * trouvé tout fait.
     */
    private void createApplications(User jobSeeker, List<JobOffer> offers) {
        // Quatre candidatures libres, dont une annulée : les deux états du suivi sont
        // montrés. Il y en avait seize, auxquelles s'ajoutait la candidature que chaque
        // mission porte — « Mes candidatures » en comptait alors soixante-sept, ce qu'un
        // intérimaire n'a jamais.
        for (int index = 0; index < 4; index += 1) {
            JobOffer offer = offers.get(index);
            applicationRepository.save(application(
                    jobSeeker, offer,
                    LocalDateTime.now().minusDays(index).minusHours(2),
                    index == 3 ? ApplicationStatus.CANCELED : ApplicationStatus.PENDING));
        }
    }

    private Application application(User jobSeeker, JobOffer offer, LocalDateTime when, ApplicationStatus status) {
        Application application = new Application();
        application.setJobSeeker(jobSeeker);
        application.setJobOffer(offer);
        application.setApplicationTime(when);
        application.setStatus(status);
        return application;
    }

    // ---------------------------------------------------------------- missions

    /** Une annonce de l'employeur et le métier dont elle relève. */
    private record Posting(JobOffer offer, Job job) {
    }

    /**
     * Le contrat d'une mission : aucun tant que l'intérimaire n'a pas accepté, puis en
     * attente des deux signatures, puis signé par les deux parties.
     */
    private enum ContractState { NONE, PENDING, SIGNED }

    /**
     * Douze missions, sur trois annonces et sept intérimaires.
     *
     * <p>Chaque mission avait son offre et son intérimaire : « Mes offres » comptait donc
     * seize lignes pour douze missions, et les écrans de l'employeur — missions en cours,
     * missions à valider — n'affichaient qu'un seul nom, celui du compte de démonstration.
     * Ce n'est pas ainsi qu'une entreprise recrute : elle publie une annonce et y détache
     * plusieurs personnes, successivement ou en même temps. Les trois annonces portent
     * donc chacune plusieurs missions, et le compte de démonstration ne tient plus que
     * quatre des douze — assez pour que ses propres écrans aient de quoi montrer.
     *
     * <p>Deux règles fixent le calendrier. Une même candidature ne porte qu'une mission en
     * cours, d'où une annonce distincte par mission d'un même intérimaire ; et deux
     * missions retenues ne peuvent se chevaucher pour la même personne, sans quoi la
     * validation par l'agence ou l'acceptation par l'intérimaire échouerait en pleine
     * démonstration. Les périodes se suivent donc sans se toucher, intérimaire par
     * intérimaire, dans le mois qui vient.
     */
    private List<Mission> createMissions(User employer, User jobSeeker, List<User> workers) {
        // Trois annonces déjà anciennes : ce sont elles qui ont amené les intérimaires.
        // Les deux premières sont clôturées, un poste y ayant été pourvu.
        Posting picking = missionOffer(employer, 0, JobOfferStatus.CLOSED, 120);
        Posting forklift = missionOffer(employer, 7, JobOfferStatus.CLOSED, 45);
        Posting dock = missionOffer(employer, 21, JobOfferStatus.OPEN, 20);

        List<Mission> missions = new ArrayList<>();
        int variant = 0;
        // Missions terminées : « Historique », côté intérimaire comme côté employeur.
        // Celle du compte de démonstration a son contrat signé des deux côtés, comme toute
        // mission qui a eu lieu ; les autres l'ont encore en attente, ce qui laisse à
        // l'employeur de démonstration de quoi jouer la signature.
        missions.add(createMission(picking, jobSeeker, MissionStatus.ACTIVE,
                day(-95), day(-88), ContractState.SIGNED, variant++));
        missions.add(createMission(picking, workers.get(0), MissionStatus.ACTIVE,
                day(-60), day(-53), ContractState.PENDING, variant++));
        missions.add(createMission(forklift, workers.get(1), MissionStatus.ACTIVE,
                day(-27), day(-20), ContractState.PENDING, variant++));

        // Missions confirmées, contrat à la clé : « Missions confirmées » / « en cours ».
        // Celle du compte de démonstration commence après-demain : son contrat est signé.
        Mission ongoing = createMission(forklift, jobSeeker, MissionStatus.ACTIVE,
                day(2), day(6), ContractState.SIGNED, variant++);
        missions.add(ongoing);
        missions.add(createMission(forklift, workers.get(2), MissionStatus.ACTIVE,
                day(9), day(16), ContractState.PENDING, variant++));

        // Le renouvellement prolonge la mission en cours : même candidature, même annonce,
        // période à la suite. C'est exactement ce que fait MissionService.renew.
        missions.add(renewMission(forklift, ongoing, day(7), day(12), variant++));

        // Propositions qui attendent la réponse de l'intérimaire.
        missions.add(createMission(dock, jobSeeker, MissionStatus.APPROVED,
                day(17), day(20), ContractState.NONE, variant++));
        missions.add(createMission(dock, workers.get(5), MissionStatus.APPROVED,
                day(14), day(18), ContractState.NONE, variant++));

        // Demandes de l'employeur en attente de validation de l'agence.
        missions.add(createMission(dock, workers.get(3), MissionStatus.PENDING,
                day(21), day(24), ContractState.NONE, variant++));
        missions.add(createMission(dock, workers.get(4), MissionStatus.PENDING,
                day(23), day(27), ContractState.NONE, variant++));

        // Une refusée par l'agence, une refusée par l'intérimaire : « Missions refusées »
        // montre les deux origines du refus, qui ne se ressemblent pas. Elles seules
        // peuvent recouvrir une autre période, puisqu'elles n'auront pas lieu.
        missions.add(createMission(picking, workers.get(1), MissionStatus.REFUSED,
                day(9), day(13), ContractState.NONE, variant++));
        missions.add(createMission(dock, workers.get(0), MissionStatus.DECLINED,
                day(23), day(26), ContractState.NONE, variant));
        return missions;
    }

    /** Une date relative au jour de l'amorçage, en clair au point d'appel. */
    private static LocalDate day(long offset) {
        return LocalDate.now().plusDays(offset);
    }

    /**
     * Une annonce destinée à porter des missions, publiée il y a le nombre de jours donné :
     * elle doit précéder les candidatures qu'elle a suscitées, y compris celle d'une
     * mission terminée il y a trois mois.
     */
    private Posting missionOffer(User employer, int jobIndex, JobOfferStatus status, long publishedDaysAgo) {
        Job job = JOBS[jobIndex];
        JobOffer offer = publish(employer, job, HOST_PLACE, jobIndex, status);
        offer.setPublishedAt(LocalDateTime.now().minusDays(publishedDaysAgo));
        return new Posting(jobOfferRepository.save(offer), job);
    }

    private Mission createMission(
            Posting posting,
            User worker,
            MissionStatus status,
            LocalDate start,
            LocalDate end,
            ContractState contract,
            int variant) {
        Application application = applicationRepository.save(application(
                worker, posting.offer(), applicationTime(start, variant), ApplicationStatus.PENDING));
        return saveMission(posting, application, null, status, start, end, contract, variant);
    }

    /** Un renouvellement reprend la candidature de la mission qu'il prolonge, et la désigne. */
    private Mission renewMission(Posting posting, Mission source, LocalDate start, LocalDate end, int variant) {
        return saveMission(posting, source.getApplication(), source, MissionStatus.RENEWAL,
                start, end, ContractState.NONE, variant);
    }

    /**
     * La candidature précède toujours la mission, et ne peut pas être datée de demain :
     * pour une mission passée elle remonte à la semaine qui l'a précédée, pour une mission
     * à venir aux jours qui viennent de s'écouler.
     */
    private LocalDateTime applicationTime(LocalDate start, int variant) {
        LocalDate before = start.minusDays(7);
        LocalDate recent = LocalDate.now().minusDays(2L + variant % 6);
        return (before.isBefore(recent) ? before : recent).atTime(9, 30);
    }

    private Mission saveMission(
            Posting posting,
            Application application,
            Mission previous,
            MissionStatus status,
            LocalDate start,
            LocalDate end,
            ContractState contract,
            int variant) {
        Job job = posting.job();
        User employer = posting.offer().getEmployer();

        Mission mission = new Mission();
        mission.setApplication(application);
        mission.setPreviousMission(previous);
        mission.setStatus(status);
        mission.setStartDate(start);
        mission.setEndDate(end);
        mission.setPosition(job.title());
        mission.setWorkplace(employer.getAddress());
        mission.setMealVoucherAmount(posting.offer().getMealVoucherAmount());
        mission.setDescription(job.tasks());
        mission.setJointCommittee(employer.getJointCommittee());
        // Le salaire convenu doit rester dans la fourchette de l'offre : le barème de
        // départ du métier la respecte par construction.
        mission.setHourlyWage(new BigDecimal(job.salaryMin()));
        // Un remplacement sur trois, et le nom du remplacé avec lui : la mention est
        // obligatoire sur le contrat, et son absence rendrait le motif invalide.
        if (variant % 3 == 0) {
            mission.setWorkReason(WorkReason.REPLACEMENT);
            mission.setReplacedWorker(REPLACED[variant % REPLACED.length]);
        } else {
            mission.setWorkReason(WorkReason.OVERLOAD);
        }
        // Deux contrats sur trois portent une condition particulière, le troisième aucune :
        // les deux formes du paragraphe 5 du contrat sont ainsi visibles.
        if (variant % 3 != 2) {
            mission.setNotes(NOTES[variant % NOTES.length]);
        }
        if (status == MissionStatus.REFUSED) {
            mission.setRefusalReason(REFUSALS[variant % REFUSALS.length]);
        }
        Mission saved = missionRepository.save(mission);

        // Les journées sont enregistrées d'abord : le contrat les reprend.
        List<DailySchedule> slots = dailyScheduleRepository.saveAll(workingDays(saved, start, end));
        // Le contrat passe par le service de production : la ligne en base ET le PDF
        // sur disque sont créés ensemble, sinon le bouton « Contrat » renverrait 404.
        if (contract == ContractState.PENDING) {
            contractService.generate(saved, slots);
        } else if (contract == ContractState.SIGNED) {
            signedContract(saved, slots, start, variant);
        }
        return saved;
    }

    /**
     * Un contrat conclu avant l'amorçage. Établi le lendemain de la validation par
     * l'agence, signé le surlendemain par l'intérimaire puis par l'employeur — dans les
     * deux jours que le contrat lui-même accorde. Si la mission commence trop tôt pour
     * cet enchaînement, il se replie sur les derniers jours écoulés : un contrat signé
     * demain n'existe pas.
     */
    private void signedContract(Mission mission, List<DailySchedule> slots, LocalDate start, int variant) {
        LocalDate wanted = start.minusDays(4);
        LocalDate latest = LocalDate.now().minusDays(2);
        LocalDate established = wanted.isBefore(latest) ? wanted : latest;
        int minute = 7 + variant % 50;
        contractService.generateSigned(
                mission,
                slots,
                established.atTime(10, minute),
                established.plusDays(1).atTime(16, minute),
                established.plusDays(1).atTime(14, minute));
    }

    /** Journées ouvrées de la période, 08:00–16:30 avec une pause de midi non payée. */
    private List<DailySchedule> workingDays(Mission mission, LocalDate start, LocalDate end) {
        List<DailySchedule> slots = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            if (day.getDayOfWeek().getValue() >= 6) {
                continue;
            }
            DailySchedule slot = new DailySchedule();
            slot.setMission(mission);
            slot.setDate(day);
            slot.setStartTime(LocalTime.of(8, 0));
            slot.setEndTime(LocalTime.of(16, 30));
            slot.setBreakStart(LocalTime.of(12, 0));
            slot.setBreakEnd(LocalTime.of(12, 30));
            slots.add(slot);
        }
        return slots;
    }

    // ------------------------------------------- autres candidats et conversations

    /**
     * Six autres candidats sur la première offre, dont quatre ont une conversation
     * ouverte avec l'employeur. Ils étaient quatorze : de quoi paginer la page
     * « Candidatures », mais pas de quoi ressembler à une offre de manutention en
     * province.
     */
    private List<User> createOtherCandidates(User employer, JobOffer offer) {
        List<User> saved = new ArrayList<>();
        for (int index = 0; index < CANDIDATES.length; index += 1) {
            Candidate profile = CANDIDATES[index];
            User candidate = newUser(profile.email(), profile.firstName(), profile.lastName(), Role.JOBSEEKER);
            candidate.setAddress(profile.address());
            candidate.setBirthdate(profile.birthdate());
            candidate.setHasVehicle(profile.hasVehicle());
            candidate.setNationalNumber(profile.nationalNumber());
            candidate.setIban(profile.iban());
            saved.add(userRepository.save(candidate));

            User candidateAccount = saved.get(index);
            Application application = applicationRepository.save(application(
                    candidateAccount, offer, LocalDateTime.now().minusDays(index).minusMinutes(30),
                    ApplicationStatus.PENDING));
            // Une candidature sur trois est notée : le tri par note a de quoi classer.
            if (index % 3 == 0) {
                application.setRating(5 - (index % 5));
                applicationRepository.save(application);
            }
            if (index < CONVERSATIONS.length) {
                createConversation(employer, candidateAccount, application, index);
            }
        }
        return saved;
    }

    /** Le premier fil compte 35 messages : de quoi voir « Voir les messages plus anciens ». */
    private void createConversation(User employer, User candidate, Application application, int index) {
        Conversation conversation = new Conversation();
        conversation.setApplication(application);
        conversation.setSender(employer);
        conversation.setReceiver(candidate);
        Conversation saved = conversationRepository.save(conversation);

        String[] script = CONVERSATIONS[index];
        for (int number = 0; number < script.length; number += 1) {
            boolean fromEmployer = number % 2 == 0;
            Message message = new Message();
            message.setConversation(saved);
            message.setUser(fromEmployer ? employer : candidate);
            message.setContent(script[number]);
            message.setSentTime(LocalDateTime.now().minusDays(index).minusMinutes(script.length - number));
            // Les messages du candidat restent non lus : le badge de l'employeur est visible.
            message.setRead(fromEmployer);
            messageRepository.save(message);
        }
    }

    // -------------------------------------------- demandes d'accès employeur

    /**
     * 3 demandes en attente + 6 traitées.
     *
     * <p>La file d'attente est courte à dessein : une agence qui laisse douze demandes en
     * souffrance ne ressemble à rien. L'historique en garde le double, réparti entre
     * acceptations et refus — assez pour que les deux issues se voient, pas assez pour
     * qu'il faille dérouler.
     */
    private List<EmployerAccessRequest> createPendingEmployerRequests() {
        List<EmployerAccessRequest> decided = new ArrayList<>();
        for (int index = 0; index < APPLICANTS.length; index += 1) {
            Applicant applicant = APPLICANTS[index];
            User account = newUser(
                    applicant.email(), applicant.firstName(), applicant.lastName(), Role.EMPLOYER_PENDING);
            account.setCompanyName(applicant.company());
            account.setAddress(applicant.address());
            User saved = userRepository.save(account);

            EmployerAccessRequest request = new EmployerAccessRequest();
            request.setUser(saved);
            request.setRequestDate(LocalDate.now().minusDays(index + 1L));
            request.setMessage(applicant.message());
            if (index < 3) {
                request.setStatus(EmployerAccessStatus.PENDING);
                accessRequestRepository.save(request);
            } else {
                request.setStatus(index % 2 == 0
                        ? EmployerAccessStatus.ACCEPTED
                        : EmployerAccessStatus.REFUSED);
                saved.setRole(index % 2 == 0 ? Role.EMPLOYER : Role.EMPLOYER_PENDING);
                userRepository.save(saved);
                decided.add(accessRequestRepository.save(request));
            }
        }
        return decided;
    }

    // ------------------------------------------------------------ journal d'audit

    /**
     * Le journal d'audit de l'agence, garni de ce que le jeu de données raconte : les six
     * demandes d'accès tranchées, les missions validées ou refusées, les deux contrats
     * signés par leurs deux parties, et deux actes de l'intérimaire sur son propre compte.
     *
     * <p>Il était vide, et un journal vide ne se démontre pas : ni la pagination, ni le
     * filtre par type d'acte, ni surtout ce qu'il sert à établir — qui a décidé quoi,
     * quand. Les traces reprennent les mêmes libellés que les services qui les écrivent
     * en exploitation, pour qu'aucune ligne du jeu ne diffère d'une ligne réelle.
     *
     * <p>Elles sont écrites directement, et non par {@link be.agence_interim.service.AuditService} :
     * ce service horodate à l'instant de l'appel, ce qui empilerait vingt lignes à la même
     * seconde pour des faits étalés sur trois mois. Un journal ne vaut que par ses dates.
     */
    private void createAuditTrail(
            User jobSeeker, List<Mission> missions, List<EmployerAccessRequest> decided) {
        User admin = adminEmail.isBlank()
                ? null
                : userRepository.findByEmail(Strings.normalizeEmail(adminEmail)).orElse(null);
        if (admin == null) {
            return;
        }

        int variant = 0;
        for (EmployerAccessRequest request : decided) {
            boolean granted = request.getStatus() == EmployerAccessStatus.ACCEPTED;
            // L'agence tranche le lendemain de la demande : c'est le délai qu'annonce
            // l'email d'accusé de réception.
            trace(admin, moment(request.getRequestDate().plusDays(1), variant, 10),
                    granted ? AuditAction.EMPLOYER_ACCESS_GRANTED : AuditAction.EMPLOYER_ACCESS_REFUSED,
                    "USER", request.getUser().getId(),
                    granted ? null : "Refus, nouvelle demande possible");
            variant += 1;
        }

        for (Mission mission : missions) {
            // Une mission retenue ou proposée est passée par la validation de l'agence ;
            // une mission refusée porte le motif du refus, comme le fait le service.
            if (mission.getStatus() == MissionStatus.ACTIVE || mission.getStatus() == MissionStatus.APPROVED) {
                trace(admin, moment(mission.getStartDate().minusDays(5), variant, 9),
                        AuditAction.MISSION_VALIDATED, "MISSION", mission.getId(), null);
            } else if (mission.getStatus() == MissionStatus.REFUSED) {
                trace(admin, moment(mission.getStartDate().minusDays(4), variant, 11),
                        AuditAction.MISSION_REFUSED, "MISSION", mission.getId(), mission.getRefusalReason());
            }
            variant += 1;
        }

        // Un contrat signé a laissé deux traces, comme en laisse ContractService à chaque
        // signature : le signataire, et la date que porte le document.
        for (Mission mission : missions) {
            contractRepository.findByMissionId(mission.getId()).ifPresent(contract -> {
                Application application = mission.getApplication();
                if (contract.getStatusWorker() == SignatureStatus.SIGNED) {
                    trace(application.getJobSeeker(), contract.getWorkerSignedAt(),
                            AuditAction.CONTRACT_SIGNED, "CONTRACT", contract.getId(), "Signature intérimaire");
                }
                if (contract.getStatusEmployer() == SignatureStatus.SIGNED) {
                    trace(application.getJobOffer().getEmployer(), contract.getEmployerSignedAt(),
                            AuditAction.CONTRACT_SIGNED, "CONTRACT", contract.getId(), "Signature employeur");
                }
            });
        }

        // Deux actes de l'intérimaire sur son propre compte : le journal ne consigne pas
        // que les décisions de l'agence, et le filtre a de quoi filtrer.
        trace(jobSeeker, day(-40).atTime(8, 52), AuditAction.PASSWORD_CHANGED, "USER", jobSeeker.getId(), null);
        trace(jobSeeker, day(-12).atTime(19, 3), AuditAction.DATA_EXPORTED, "USER", jobSeeker.getId(), null);
    }

    /**
     * Un instant révolu : la date voulue si elle est déjà passée, sinon ces derniers jours.
     * Une mission qui commence dans deux semaines a bien été validée, mais hier.
     */
    private LocalDateTime moment(LocalDate wanted, int variant, int hour) {
        LocalDate latest = LocalDate.now().minusDays(1L + variant % 5);
        return (wanted.isBefore(latest) ? wanted : latest).atTime(hour, 7 + variant % 50);
    }

    private void trace(
            User actor, LocalDateTime when, AuditAction action, String targetType, int targetId, String detail) {
        AuditEvent event = new AuditEvent();
        event.setOccurredAt(when);
        event.setAction(action);
        event.setActorId(actor.getId());
        // L'adresse est recopiée, et non lue par jointure : la trace doit survivre à la
        // clôture du compte de son auteur.
        event.setActorEmail(actor.getEmail());
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        event.setDetail(detail);
        auditEventRepository.save(event);
    }
}
