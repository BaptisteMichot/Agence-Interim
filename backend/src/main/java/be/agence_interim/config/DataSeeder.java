package be.agence_interim.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import be.agence_interim.model.Degree;
import be.agence_interim.model.DegreeType;
import be.agence_interim.model.Language;
import be.agence_interim.model.Skill;
import be.agence_interim.repository.DegreeRepository;
import be.agence_interim.repository.LanguageRepository;
import be.agence_interim.repository.SkillRepository;

/**
 * Insère au démarrage les listes de base (référentiels globaux) si elles sont absentes.
 * Idempotent : chaque entrée n'est ajoutée que si elle n'existe pas déjà.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final List<String> LANGUAGES = List.of(
            "Français", "Néerlandais", "Anglais", "Allemand", "Espagnol", "Italien",
            "Portugais", "Arabe", "Turc", "Polonais", "Russe", "Chinois");

    private static final List<String> GLOBAL_SKILLS = List.of(
            "Cariste", "Manutention", "Soudure", "Peinture", "Maçonnerie", "Électricité",
            "Plomberie", "Menuiserie", "Nettoyage", "Accueil", "Vente", "Caisse",
            "Cuisine", "Service en salle", "Secrétariat", "Comptabilité", "Informatique",
            "Bureautique", "Gestion de stock", "Conduite PL", "Jardinage", "Mécanique",
            "Logistique", "Sécurité");

    /** Sections de diplôme par type. */
    private static final Map<DegreeType, List<String>> GLOBAL_DEGREES = Map.of(
            DegreeType.BACHELIER, List.of(
                    "Informatique", "Comptabilité", "Marketing", "Droit",
                    "Sciences infirmières", "Électromécanique", "Construction",
                    "Commerce", "Communication", "Éducation"),
            DegreeType.MASTER, List.of(
                    "Informatique", "Ingénierie", "Droit", "Économie",
                    "Psychologie", "Gestion"));

    private final LanguageRepository languageRepository;
    private final SkillRepository skillRepository;
    private final DegreeRepository degreeRepository;

    public DataSeeder(
            LanguageRepository languageRepository,
            SkillRepository skillRepository,
            DegreeRepository degreeRepository) {
        this.languageRepository = languageRepository;
        this.skillRepository = skillRepository;
        this.degreeRepository = degreeRepository;
    }

    @Override
    public void run(String... args) {
        seedLanguages();
        seedSkills();
        seedDegrees();
    }

    private void seedLanguages() {
        for (String name : LANGUAGES) {
            if (!languageRepository.existsByNameIgnoreCase(name)) {
                Language language = new Language();
                language.setName(name);
                languageRepository.save(language);
            }
        }
    }

    private void seedSkills() {
        for (String name : GLOBAL_SKILLS) {
            if (!skillRepository.existsByNameIgnoreCaseAndIsGlobalTrue(name)) {
                Skill skill = new Skill();
                skill.setName(name);
                skill.setIsGlobal(true);
                skill.setCreatedBy(null);
                skillRepository.save(skill);
            }
        }
    }

    private void seedDegrees() {
        GLOBAL_DEGREES.forEach((type, sections) -> {
            for (String section : sections) {
                if (!degreeRepository.existsByTypeAndSectionIgnoreCaseAndIsGlobalTrue(type, section)) {
                    Degree degree = new Degree();
                    degree.setType(type);
                    degree.setSection(section);
                    degree.setIsGlobal(true);
                    degree.setCreatedBy(null);
                    degreeRepository.save(degree);
                }
            }
        });
    }
}
