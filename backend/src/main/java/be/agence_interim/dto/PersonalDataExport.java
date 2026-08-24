package be.agence_interim.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Données personnelles d'un compte (droit d'accès, RGPD article 15).
 *
 * <p>Le pendant de la clôture de compte : l'un permet de partir, l'autre d'emporter ses
 * données. Cette structure est le recueil ; sa mise en forme lisible est le travail de
 * {@code PersonalDataDocument}.
 *
 * <p>Les sections ne concernent pas tous les rôles : un employeur n'a ni compétences ni
 * candidatures, un intérimaire n'a pas d'offres publiées. Elles sont alors vides, et le
 * document n'en parle pas — un export qui aligne huit rubriques « néant » se lit mal et
 * donne l'impression que la plateforme en sait plus qu'elle n'en dit.
 *
 * <p><strong>Aucun identifiant technique n'y figure.</strong> Les clés primaires sont un
 * détail d'implémentation : « contrat n° 34 » ne dit rien à la personne qui lit son
 * export, et publier la numérotation interne renseigne au passage sur le volume d'activité
 * de la plateforme. Chaque élément est donc désigné par ce qui le distingue aux yeux de
 * son destinataire — un poste et une date, un interlocuteur et une offre.
 *
 * <p>Ce qui n'y figure pas est aussi un choix. Le haché du mot de passe est absent : ce
 * n'est pas une donnée que l'intéressé a fournie, et la republier n'aiderait que celui
 * qui mettrait la main sur l'export. Les documents PDF ne sont pas embarqués non plus :
 * le CV et les contrats se téléchargent par leurs propres routes, déjà authentifiées.
 */
public record PersonalDataExport(
        LocalDateTime exportedAt,
        Identity identity,
        List<UserSkillResponse> skills,
        List<UserDegreeResponse> degrees,
        List<UserLanguageResponse> languages,
        List<ExperienceResponse> experiences,
        List<FormationResponse> formations,
        List<ApplicationLine> applications,
        List<OfferLine> offers,
        List<ContractLine> contracts,
        List<MessageLine> messages) {

    /** Identité et coordonnées telles qu'elles figurent dans la base. */
    public record Identity(
            String role,
            String lastName,
            String firstName,
            String email,
            LocalDate birthdate,
            Boolean hasVehicle,
            String address,
            String nationalNumber,
            String iban,
            String companyName,
            String companyNumber,
            String jointCommittee,
            String cvFileName,
            LocalDateTime lastLoginAt) {
    }

    /** Une candidature déposée par le compte (intérimaire). */
    public record ApplicationLine(
            String offerTitle,
            String company,
            LocalDateTime appliedAt,
            String status,
            Integer rating) {
    }

    /** Une offre publiée par le compte (employeur). */
    public record OfferLine(
            String title,
            String city,
            LocalDateTime publishedAt,
            String status) {
    }

    /** Un contrat auquel le compte est partie, désigné par son poste et sa date. */
    public record ContractLine(
            String position,
            LocalDateTime generatedAt,
            String employerSignature,
            LocalDateTime employerSignedAt,
            String workerSignature,
            LocalDateTime workerSignedAt) {
    }

    /**
     * Un message écrit par le compte.
     *
     * <p>{@code conversation} nomme l'échange par son interlocuteur et l'offre qui l'a
     * fait naître, plutôt que par l'identifiant de la conversation. C'est aussi ce qui
     * permet au document de regrouper les messages par fil.
     */
    public record MessageLine(String conversation, LocalDateTime sentTime, String content) {
    }
}
