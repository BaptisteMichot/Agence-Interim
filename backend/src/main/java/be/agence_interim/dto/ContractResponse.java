package be.agence_interim.dto;

import java.time.LocalDateTime;

import be.agence_interim.model.Contract;
import be.agence_interim.model.SignatureStatus;

/** Contrat d'une mission : état de signature des deux parties (signature simulée). */
public record ContractResponse(
        int id,
        int missionId,
        LocalDateTime generationTime,
        SignatureStatus statusEmployer,
        SignatureStatus statusWorker,
        String fileName) {

    public static ContractResponse fromEntity(Contract contract) {
        return new ContractResponse(
                contract.getId(),
                contract.getMission().getId(),
                contract.getGenerationTime(),
                contract.getStatusEmployer(),
                contract.getStatusWorker(),
                contract.getContractFilePath());
    }
}
