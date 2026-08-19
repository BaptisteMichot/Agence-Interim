package be.agence_interim.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    public static final int ROLE_MAX_LENGTH = 16;
    public static final int LAST_NAME_MAX_LENGTH = 25;
    public static final int FIRST_NAME_MAX_LENGTH = 25;
    public static final int EMAIL_MAX_LENGTH = 35;
    public static final int COMPANY_NAME_MAX_LENGTH = 30;
    public static final int PASSWORD_MIN_LENGTH = 14;
    public static final int ADDRESS_MAX_LENGTH = 100;
    public static final int NATIONAL_NUMBER_MAX_LENGTH = 15;
    public static final int IBAN_MAX_LENGTH = 42; // 34 caracteres + les espaces des groupes de quatre
    public static final int COMPANY_NUMBER_MAX_LENGTH = 15;
    public static final int JOINT_COMMITTEE_MAX_LENGTH = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = ROLE_MAX_LENGTH)
    private Role role = Role.JOBSEEKER; // rôle par défaut

    @Column(nullable = false, length = LAST_NAME_MAX_LENGTH)
    private String lastName;

    @Column(nullable = false, length = FIRST_NAME_MAX_LENGTH)
    private String firstName;

    @Column(nullable = false, unique = true, length = EMAIL_MAX_LENGTH)
    private String email;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String password;

    private Boolean hasVehicle;

    private LocalDate birthdate;

    @Column(columnDefinition = "TEXT")
    private String cvFilePath;

    @Column(length = COMPANY_NAME_MAX_LENGTH)
    private String companyName;

    /** Domicile de l'intérimaire ou siège de l'entreprise utilisatrice, repris sur le contrat. */
    @Column(length = ADDRESS_MAX_LENGTH)
    private String address;

    /** Numéro de registre national de l'intérimaire (mention légale du contrat). */
    @Column(length = NATIONAL_NUMBER_MAX_LENGTH)
    private String nationalNumber;

    /** Compte bancaire sur lequel le salaire de l'intérimaire est versé. */
    @Column(length = IBAN_MAX_LENGTH)
    private String iban;

    /** Numéro d'entreprise (BCE) de l'entreprise utilisatrice. */
    @Column(length = COMPANY_NUMBER_MAX_LENGTH)
    private String companyNumber;

    /** Commission paritaire dont relève l'entreprise utilisatrice. */
    @Column(length = JOINT_COMMITTEE_MAX_LENGTH)
    private String jointCommittee;

    // Ajouter les listes de skills, diplomes et langues.
}
