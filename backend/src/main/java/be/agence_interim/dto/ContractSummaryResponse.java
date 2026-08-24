package be.agence_interim.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import be.agence_interim.model.Contract;
import be.agence_interim.model.Mission;
import be.agence_interim.model.SignatureStatus;
import be.agence_interim.model.User;

/**
 * Contrat tel qu'il apparaît dans « Mes documents » : de quoi identifier la mission
 * qu'il couvre, l'état des deux signatures et la date de chacune.
 */
public record ContractSummaryResponse(
        int id,
        int missionId,
        String position,
        String companyName,
        String workerName,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime generationTime,
        SignatureStatus statusEmployer,
        LocalDateTime employerSignedAt,
        SignatureStatus statusWorker,
        LocalDateTime workerSignedAt,
        /** Vrai si c'est au lecteur de signer : la page n'a pas à déduire son rôle. */
        boolean awaitingMySignature) {

    public static ContractSummaryResponse of(Contract contract, int userId) {
        Mission mission = contract.getMission();
        User employer = mission.getApplication().getJobOffer().getEmployer();
        User worker = mission.getApplication().getJobSeeker();
        boolean awaiting =
                (employer.getId() == userId && contract.getStatusEmployer() == SignatureStatus.PENDING)
                        || (worker.getId() == userId && contract.getStatusWorker() == SignatureStatus.PENDING);
        return new ContractSummaryResponse(
                contract.getId(),
                mission.getId(),
                mission.getPosition(),
                employer.getCompanyName(),
                worker.getFirstName() + " " + worker.getLastName(),
                mission.getStartDate(),
                mission.getEndDate(),
                contract.getGenerationTime(),
                contract.getStatusEmployer(),
                contract.getEmployerSignedAt(),
                contract.getStatusWorker(),
                contract.getWorkerSignedAt(),
                awaiting);
    }
}
