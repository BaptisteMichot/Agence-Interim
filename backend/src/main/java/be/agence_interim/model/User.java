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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    public static final int ROLE_MAX_LENGHT = 16;
    public static final int LAST_NAME_MAX_LENGTH = 25;
    public static final int FIRST_NAME_MAX_LENGTH = 25;
    public static final int EMAIL_MAX_LENGTH = 35;
    public static final int COMPANY_NAME_MAX_LENGTH = 30;
    public static final int PASSWORD_MIN_LENGTH = 14;
    public static final int ADDRESS_MAX_LENGTH = 100;
    public static final int NATIONAL_NUMBER_MAX_LENGTH = 15;
    public static final int COMPANY_NUMBER_MAX_LENGTH = 15;
    public static final int JOINT_COMMITTEE_MAX_LENGTH = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = ROLE_MAX_LENGHT)
    private Role role = Role.JOBSEEKER; // rôle par défaut

    @Column(nullable = false, length = LAST_NAME_MAX_LENGTH)
    private String lastName;

    @Column(nullable = false, length = FIRST_NAME_MAX_LENGTH)
    private String firstName;

    @Column(nullable = false, unique = true, length = EMAIL_MAX_LENGTH)
    private String email;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String password;

    @Column(nullable = true)
    private Boolean hasVehicle;

    @Column(nullable = true)
    private LocalDate birthdate;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String cvFilePath;

    @Column(nullable = true, length = COMPANY_NAME_MAX_LENGTH)
    private String companyName;

    /** Domicile de l'intérimaire ou siège de l'entreprise utilisatrice, repris sur le contrat. */
    @Column(nullable = true, length = ADDRESS_MAX_LENGTH)
    private String address;

    /** Numéro de registre national de l'intérimaire (mention légale du contrat). */
    @Column(nullable = true, length = NATIONAL_NUMBER_MAX_LENGTH)
    private String nationalNumber;

    /** Numéro d'entreprise (BCE) de l'entreprise utilisatrice. */
    @Column(nullable = true, length = COMPANY_NUMBER_MAX_LENGTH)
    private String companyNumber;

    /** Commission paritaire dont relève l'entreprise utilisatrice. */
    @Column(nullable = true, length = JOINT_COMMITTEE_MAX_LENGTH)
    private String jointCommittee;

    // Ajouter les listes de skills, diplomes et langues.

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
