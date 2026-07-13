package be.agence_interim.dto;

import be.agence_interim.model.EmployerAccessRequest;
import be.agence_interim.model.EmployerAccessStatus;
import be.agence_interim.model.User;
import java.time.LocalDate;

/** Demande d'accès employeur présentée à l'administrateur, avec le demandeur. */
public record AdminEmployerRequestResponse(
        int id,
        int userId,
        String firstName,
        String lastName,
        String email,
        String companyName,
        LocalDate requestDate,
        EmployerAccessStatus status,
        String message,
        boolean resubmission) {

    public static AdminEmployerRequestResponse fromEntity(EmployerAccessRequest request, boolean resubmission) {
        User user = request.getUser();
        return new AdminEmployerRequestResponse(
                request.getId(),
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getCompanyName(),
                request.getRequestDate(),
                request.getStatus(),
                request.getMessage(),
                resubmission);
    }
}
