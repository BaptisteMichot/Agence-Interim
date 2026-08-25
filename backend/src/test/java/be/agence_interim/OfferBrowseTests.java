package be.agence_interim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import be.agence_interim.dto.JobOfferRequest;
import be.agence_interim.dto.MatchingOfferResponse;
import be.agence_interim.dto.OfferFilter;
import be.agence_interim.dto.OfferSkillRequirement;
import be.agence_interim.dto.PageResponse;
import be.agence_interim.model.JobOffer;
import be.agence_interim.model.JobOfferStatus;
import be.agence_interim.model.Province;
import be.agence_interim.model.Role;
import be.agence_interim.model.Sector;
import be.agence_interim.model.Skill;
import be.agence_interim.model.SkillLevel;
import be.agence_interim.model.User;
import be.agence_interim.repository.ApplicationRepository;
import be.agence_interim.repository.JobOfferRepository;
import be.agence_interim.repository.UserRepository;
import be.agence_interim.service.ApplicationService;
import be.agence_interim.service.JobOfferService;
import be.agence_interim.service.OfferBrowseService;
import be.agence_interim.service.SkillService;

/**
 * La recherche d'offres par l'intérimaire : critères, favoris, et classement par score.
 *
 * <p>C'est l'écran par lequel un demandeur d'emploi passe le plus de temps, et le seul
 * dont la justesse ne se constate pas : une offre absente d'une liste filtrée ne se
 * remarque pas, elle manque simplement à quelqu'un.
 *
 * <p>Chaque test étiquette ses offres d'un mot inventé et filtre dessus. La base de test
 * est partagée par toute la suite : sans cette étiquette, les offres des autres classes
 * entreraient dans les résultats et aucun décompte ne serait vérifiable.
 */
@SpringBootTest
class OfferBrowseTests {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private OfferBrowseService offerBrowseService;

    @Autowired
    private JobOfferService jobOfferService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private SkillService skillService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private MissionFixtures fixtures;
    private User employer;
    private User worker;

    /** Mot inventé présent dans le titre des seules offres de ce test. */
    private String tag;

    @BeforeEach
    void setUp() {
        fixtures = new MissionFixtures(userRepository, jobOfferRepository, applicationRepository);
        employer = fixtures.employer;
        worker = fixtures.user("chercheur", Role.JOBSEEKER);
        tag = "Zephyrine" + SEQUENCE.incrementAndGet();
    }

    // ----------------------------------------------------------- neutralisation des jokers

    @Test
    @DisplayName("Les jokers contenus dans un mot-clé sont neutralisés")
    void wildcardsInsideAKeywordAreDefused() {
        // « 100% » est un mot-clé plausible. Laissé tel quel, le pour-cent devient le joker
        // du like et la recherche retourne tout le catalogue ; une saisie de la forme
        // « %_%_%_% » forcerait en plus un balayage complet de la table à chaque frappe,
        // la recherche partant à la volée. Le motif reste un paramètre lié : il n'y a
        // jamais eu d'injection possible, seulement un joker glissé dans la valeur.
        assertThat(keywordPattern("100%")).isEqualTo("%100!%%");
        assertThat(keywordPattern("cariste_nuit")).isEqualTo("%cariste!_nuit%");
        assertThat(keywordPattern("attention !")).isEqualTo("%attention !!%");
    }

    @Test
    @DisplayName("Un mot-clé est mis en minuscules, débarrassé de ses espaces, et borné en longueur")
    void akeywordIsNormalisedAndBounded() {
        assertThat(keywordPattern("  Cariste  ")).isEqualTo("%cariste%");
        assertThat(keywordPattern("   ")).as("un mot-clé vide n'est pas un critère").isNull();
        assertThat(keywordPattern(null)).isNull();

        String tooLong = "a".repeat(OfferFilter.KEYWORD_MAX_LENGTH + 50);
        assertThat(keywordPattern(tooLong)).hasSize(OfferFilter.KEYWORD_MAX_LENGTH + 2);
    }

    // ---------------------------------------------------------------------- les critères

    @Test
    @DisplayName("Le mot-clé porte sur le titre comme sur la description")
    void thekeywordSearchesTitleAndDescription() {
        int inTitle = publish(offer("Cariste " + tag));
        int inDescription = publish(new JobOfferRequest(
                "Magasinier", Sector.LOGISTIQUE, "Mons", Province.HAINAUT,
                "Poste en entrepôt, mention " + tag + " dans le texte.",
                new BigDecimal("13.00"), new BigDecimal("18.00"), "2", false, null,
                List.of(), List.of(), List.of()));

        assertThat(browse(filter(tag, null, null, null, null, false)))
                .containsExactlyInAnyOrder(inTitle, inDescription);
    }

    @Test
    @DisplayName("Secteur et province filtrent chacun de leur côté")
    void sectorAndProvinceEachFilterOnTheirOwn() {
        int logistique = publish(offer("Cariste " + tag));
        int construction = publish(offerIn(Sector.CONSTRUCTION, Province.LIEGE, "Macon " + tag));

        assertThat(browse(filter(tag, Sector.LOGISTIQUE, null, null, null, false)))
                .containsExactly(logistique);
        assertThat(browse(filter(tag, null, Province.LIEGE, null, null, false)))
                .containsExactly(construction);
        assertThat(browse(filter(tag, Sector.CONSTRUCTION, Province.HAINAUT, null, null, false)))
                .as("les critères se cumulent")
                .isEmpty();
    }

    @Test
    @DisplayName("Un salaire minimum écarte les offres qui n'annoncent aucune fourchette")
    void asalaryFilterDiscardsOffersWithoutAnyRange() {
        // Comparaison sur le haut de la fourchette : une offre « 13 à 18 € » répond à qui
        // cherche 16 €. Mais une offre qui n'annonce rien ne peut pas être comparée — elle
        // sort des résultats dès que ce critère est actif, ce que l'écran annonce.
        int withRange = publish(offer("Cariste " + tag));
        int withoutRange = publish(new JobOfferRequest(
                "Manutentionnaire " + tag, Sector.LOGISTIQUE, "Mons", Province.HAINAUT,
                "Poste en entrepôt.", null, null, "2", false, null,
                List.of(), List.of(), List.of()));

        assertThat(browse(filter(tag, null, null, new BigDecimal("16.00"), null, false)))
                .containsExactly(withRange);
        assertThat(browse(filter(tag, null, null, new BigDecimal("19.00"), null, false)))
                .isEmpty();
        assertThat(browse(filter(tag, null, null, null, null, false)))
                .as("sans le critère, l'offre muette est bien là")
                .contains(withoutRange);
    }

    @Test
    @DisplayName("Une expérience maximale garde les offres qui n'en demandent aucune")
    void anexperienceCeilingKeepsOffersThatAskForNone() {
        // L'asymétrie avec le salaire est voulue : « je n'ai pas plus de deux ans
        // d'expérience » doit faire remonter les offres qui n'en exigent pas, alors que
        // « je veux au moins 16 € » ne peut rien dire d'une offre sans salaire annoncé.
        int demanding = publish(offerRequiring("5", "Cariste confirme " + tag));
        int modest = publish(offerRequiring("1", "Cariste debutant " + tag));
        int silent = publish(offerRequiring(null, "Cariste " + tag));

        assertThat(browse(filter(tag, null, null, null, 2, false)))
                .containsExactlyInAnyOrder(modest, silent)
                .doesNotContain(demanding);
    }

    @Test
    @DisplayName("« Sans véhicule » ne garde que les offres qui n'en exigent pas")
    void thenoVehicleFilterOnlyKeepsOffersThatDoNotRequireOne() {
        int withoutCar = publish(offer("Cariste " + tag));
        int withCar = publish(new JobOfferRequest(
                "Chauffeur " + tag, Sector.LOGISTIQUE, "Mons", Province.HAINAUT,
                "Tournées régionales.", new BigDecimal("13.00"), new BigDecimal("18.00"),
                "2", true, null, List.of(), List.of(), List.of()));

        assertThat(browse(filter(tag, null, null, null, null, true))).containsExactly(withoutCar);
        assertThat(browse(filter(tag, null, null, null, null, false)))
                .containsExactlyInAnyOrder(withoutCar, withCar);
    }

    // ------------------------------------------------------------ pagination et statut

    @Test
    @DisplayName("Les pages se suivent sans doublon et le total est celui de la recherche")
    void pagesFollowEachOtherWithoutRepeating() {
        // Les dates de publication sont espacées à la main : trois offres créées dans la
        // même milliseconde ne se classeraient dans aucun ordre stable, et une même ligne
        // pourrait alors se retrouver sur deux pages.
        publishedAt(publish(offer("Cariste " + tag)), 3);
        publishedAt(publish(offer("Magasinier " + tag)), 2);
        publishedAt(publish(offer("Preparateur " + tag)), 1);

        PageResponse<?> first = offerBrowseService.browseOpen(
                worker.getId(), filter(tag, null, null, null, null, false), PageRequest.of(0, 2));

        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(browse(PageRequest.of(0, 2))).hasSize(2);
        assertThat(browse(PageRequest.of(1, 2))).hasSize(1);
        assertThat(browse(PageRequest.of(0, 2))).doesNotContainAnyElementsOf(browse(PageRequest.of(1, 2)));
        assertThat(browse(PageRequest.of(5, 2))).as("au-delà de la dernière page").isEmpty();
    }

    @Test
    @DisplayName("Une offre clôturée quitte la recherche mais reste consultable en détail")
    void aclosedOfferLeavesTheSearchButStaysReadable() {
        // Un favori peut se clôturer entre deux visites : renvoyer « introuvable » sur un
        // lien que l'intérimaire a lui-même mis de côté serait incompréhensible. L'offre
        // s'ouvre, et son statut dit qu'elle n'est plus à pourvoir.
        int offerId = publish(offer("Cariste " + tag));
        jobOfferService.close(employer.getId(), offerId);

        assertThat(browse(filter(tag, null, null, null, null, false))).isEmpty();
        assertThat(offerBrowseService.detail(worker.getId(), offerId).status())
                .isEqualTo(JobOfferStatus.CLOSED);
    }

    @Test
    @DisplayName("Le détail d'une offre dit à l'intérimaire où il en est")
    void theofferDetailTellsTheJobSeekerWhereHeStands() {
        // Les deux drapeaux évitent de proposer « postuler » à quelqu'un qui a déjà postulé,
        // ou « ajouter aux favoris » à une offre qui y est déjà.
        int offerId = publish(offer("Cariste " + tag));

        assertThat(offerBrowseService.detail(worker.getId(), offerId))
                .satisfies(detail -> {
                    assertThat(detail.applied()).isFalse();
                    assertThat(detail.favorite()).isFalse();
                });

        applicationService.apply(worker.getId(), offerId);
        offerBrowseService.addFavorite(worker.getId(), offerId);

        assertThat(offerBrowseService.detail(worker.getId(), offerId))
                .satisfies(detail -> {
                    assertThat(detail.applied()).isTrue();
                    assertThat(detail.favorite()).isTrue();
                });
    }

    // ------------------------------------------------------------------------ favoris

    @Test
    @DisplayName("Un favori s'ajoute une fois, se retire une fois")
    void afavouriteIsAddedOnceAndRemovedOnce() {
        int offerId = publish(offer("Cariste " + tag));

        offerBrowseService.addFavorite(worker.getId(), offerId);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> offerBrowseService.addFavorite(worker.getId(), offerId))
                .withMessageContaining("déjà dans vos favoris");

        offerBrowseService.removeFavorite(worker.getId(), offerId);
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> offerBrowseService.removeFavorite(worker.getId(), offerId))
                .withMessageContaining("pas dans vos favoris");
    }

    @Test
    @DisplayName("Une offre clôturée ne se met plus en favori, mais celle qui l'était y reste")
    void aclosedOfferCannotBeStarredButStaysStarred() {
        // Mettre de côté une offre qui n'est plus à pourvoir n'a pas de sens ; retirer
        // d'autorité un favori déjà posé en aurait encore moins, c'est la trace d'une
        // recherche que l'intérimaire a menée.
        int offerId = publish(offer("Cariste " + tag));
        offerBrowseService.addFavorite(worker.getId(), offerId);
        jobOfferService.close(employer.getId(), offerId);

        assertThat(favouriteIds()).contains(offerId);

        int another = publish(offer("Magasinier " + tag));
        jobOfferService.close(employer.getId(), another);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> offerBrowseService.addFavorite(worker.getId(), another))
                .withMessageContaining("clôturée");
    }

    @Test
    @DisplayName("Le favori s'allume dans la liste des offres, pour son seul propriétaire")
    void thefavouriteFlagShowsUpForItsOwnerOnly() {
        int starred = publish(offer("Cariste " + tag));
        publish(offer("Magasinier " + tag));
        offerBrowseService.addFavorite(worker.getId(), starred);
        User someoneElse = fixtures.user("autre-chercheur", Role.JOBSEEKER);

        assertThat(offerBrowseService.browseOpen(
                        worker.getId(), filter(tag, null, null, null, null, false), PageRequest.of(0, 20))
                .content().stream().filter(offer -> offer.favorite()).map(offer -> offer.id()).toList())
                .containsExactly(starred);

        assertThat(offerBrowseService.browseOpen(
                        someoneElse.getId(), filter(tag, null, null, null, null, false), PageRequest.of(0, 20))
                .content().stream().filter(offer -> offer.favorite()).toList())
                .isEmpty();
    }

    // ------------------------------------------------------------------ « Pour moi »

    @Test
    @DisplayName("« Pour moi » classe par score et écarte ceux dont une exigence n'est pas remplie")
    void theforMeListRanksByScoreAndDropsTheIneligible() {
        // Le classement est ce qui distingue cette liste de la recherche : la première
        // offre est celle qui correspond le mieux, pas la plus récente.
        Skill cariste = skillService.resolveSkill(worker.getId(), null, "Cariste " + tag);
        Skill soudure = skillService.resolveSkill(worker.getId(), null, "Soudure " + tag);
        skillService.add(worker.getId(), cariste.getId(), null, SkillLevel.EXPERT);

        int perfect = publish(offerNeeding("Parfaite " + tag, cariste, SkillLevel.AVANCE, true, null, null));
        int partial = publish(offerNeeding(
                "Partielle " + tag, cariste, SkillLevel.AVANCE, true, soudure, SkillLevel.AVANCE));
        int excluded = publish(offerNeeding("Exclue " + tag, soudure, SkillLevel.AVANCE, true, null, null));

        List<MatchingOfferResponse> ranked = offerBrowseService.matching(
                worker.getId(), filter(tag, null, null, null, null, false), 0, 20).content();

        assertThat(ranked).extracting(match -> match.offer().id())
                .containsExactly(perfect, partial)
                .doesNotContain(excluded);
        assertThat(ranked.get(0).score()).isEqualTo(100);
        assertThat(ranked.get(1).score()).isLessThan(100);
    }

    @Test
    @DisplayName("Les critères de recherche s'appliquent aussi à « Pour moi »")
    void thesearchCriteriaAlsoApplyToTheForMeList() {
        // Sans cela, l'intérimaire qui restreint sa recherche à sa province verrait
        // réapparaître dans « Pour moi » les offres qu'il vient d'écarter.
        publish(offer("Cariste " + tag));
        int liege = publish(offerIn(Sector.CONSTRUCTION, Province.LIEGE, "Macon " + tag));

        assertThat(offerBrowseService.matching(
                        worker.getId(), filter(tag, null, Province.LIEGE, null, null, false), 0, 20)
                .content())
                .extracting(match -> match.offer().id())
                .containsExactly(liege);
    }

    // ------------------------------------------------------------------------------ outils

    private static String keywordPattern(String keyword) {
        return new OfferFilter(keyword, null, null, null, null, false).keywordPattern();
    }

    private static OfferFilter filter(
            String keyword, Sector sector, Province province,
            BigDecimal minHourlyWage, Integer maxExperienceYears, boolean noVehicleRequired) {
        return new OfferFilter(keyword, sector, province, minHourlyWage, maxExperienceYears, noVehicleRequired);
    }

    private int publish(JobOfferRequest request) {
        return jobOfferService.create(employer.getId(), request).id();
    }

    /** Recule la date de publication d'une offre, pour que le classement soit sans ambiguïté. */
    private void publishedAt(int offerId, int minutesAgo) {
        JobOffer offer = jobOfferRepository.findById(offerId).orElseThrow();
        offer.setPublishedAt(LocalDateTime.now().minusMinutes(minutesAgo));
        jobOfferRepository.save(offer);
    }

    private List<Integer> browse(OfferFilter filter) {
        return browse(filter, PageRequest.of(0, 20));
    }

    private List<Integer> browse(Pageable pageable) {
        return browse(filter(tag, null, null, null, null, false), pageable);
    }

    private List<Integer> browse(OfferFilter filter, Pageable pageable) {
        return offerBrowseService.browseOpen(worker.getId(), filter, pageable)
                .content().stream().map(offer -> offer.id()).toList();
    }

    private List<Integer> favouriteIds() {
        return offerBrowseService.favorites(worker.getId(), PageRequest.of(0, 20))
                .content().stream().map(offer -> offer.id()).toList();
    }

    private JobOfferRequest offer(String title) {
        return offerIn(Sector.LOGISTIQUE, Province.HAINAUT, title);
    }

    private JobOfferRequest offerIn(Sector sector, Province province, String title) {
        return new JobOfferRequest(
                title, sector, province == Province.LIEGE ? "Liège" : "Mons", province,
                "Poste en entrepôt, horaire de jour.",
                new BigDecimal("13.00"), new BigDecimal("18.00"), "2", false, null,
                List.of(), List.of(), List.of());
    }

    private JobOfferRequest offerRequiring(String experienceYears, String title) {
        return new JobOfferRequest(
                title, Sector.LOGISTIQUE, "Mons", Province.HAINAUT, "Poste en entrepôt.",
                new BigDecimal("13.00"), new BigDecimal("18.00"), experienceYears, false, null,
                List.of(), List.of(), List.of());
    }

    /** Offre portant une ou deux compétences exigées, pour éprouver le classement. */
    private JobOfferRequest offerNeeding(
            String title, Skill first, SkillLevel firstLevel, boolean firstMandatory,
            Skill second, SkillLevel secondLevel) {
        List<OfferSkillRequirement> skills = second == null
                ? List.of(new OfferSkillRequirement(first.getName(), firstMandatory, firstLevel))
                : List.of(
                        new OfferSkillRequirement(first.getName(), firstMandatory, firstLevel),
                        new OfferSkillRequirement(second.getName(), false, secondLevel));
        return new JobOfferRequest(
                title, Sector.LOGISTIQUE, "Mons", Province.HAINAUT, "Poste en entrepôt.",
                new BigDecimal("13.00"), new BigDecimal("18.00"), null, false, null,
                skills, List.of(), List.of());
    }
}
