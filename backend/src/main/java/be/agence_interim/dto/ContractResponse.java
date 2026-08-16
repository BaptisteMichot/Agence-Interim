package be.agence_interim.dto;

import java.time.LocalDateTime;

import be.agence_interim.model.Contract;
import be.agence_interim.model.SignatureStatus;

/** Contrat d'une mission : état et date de signature de chaque partie. */
public record ContractResponse(
        int id,
        int missionId,
        LocalDateTime generationTime,
        SignatureStatus statusEmployer,
        SignatureStatus statusWorker,
        String fileName,
        LocalDateTime employerSignedAt,
        LocalDateTime workerSignedAt) {

    public static ContractResponse fromEntity(Contract contract) {
        return new ContractResponse(
                contract.getId(),
                contract.getMission().getId(),
                contract.getGenerationTime(),
                contract.getStatusEmployer(),
                contract.getStatusWorker(),
                contract.getContractFilePath(),
                contract.getEmployerSignedAt(),
                contract.getWorkerSignedAt());
    }
}
