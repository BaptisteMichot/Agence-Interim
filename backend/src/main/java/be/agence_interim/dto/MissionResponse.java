package be.agence_interim.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import be.agence_interim.model.Application;
import be.agence_interim.model.Contract;
import be.agence_interim.model.DailySchedule;
import be.agence_interim.model.JobOffer;
import be.agence_interim.model.Mission;
import be.agence_interim.model.MissionStatus;
import be.agence_interim.model.User;
import be.agence_interim.model.WorkReason;

/**
 * Mission complète, servie aux trois portails (employeur, agence, intérimaire) :
 * conditions contractuelles, journées travaillées, parties concernées et contrat.
 */
public record MissionResponse(
        int id,
        MissionStatus status,
        LocalDate startDate,
        LocalDate endDate,
        String position,
        String workplace,
        BigDecimal hourlyWage,
        WorkReason workReason,
        String notes,
        String refusalReason,
        /** Vrai si la mission renouvelle une mission précédente. */
        boolean renewal,
        LocalDate previousStartDate,
        LocalDate previousEndDate,
        int applicationId,
        int offerId,
        String offerTitle,
        int candidateId,
        String candidateFirstName,
        String candidateLastName,
        String candidateEmail,
        int employerId,
        String employerCompanyName,
        String employerFirstName,
        String employerLastName,
        String employerEmail,
        List<DailySlotResponse> slots,
        ContractResponse contract) {

    public static MissionResponse of(Mission mission, List<DailySchedule> slots, Contract contract) {
        Application application = mission.getApplication();
        JobOffer offer = application.getJobOffer();
        User candidate = application.getJobSeeker();
        User employer = offer.getEmployer();
        Mission previous = mission.getPreviousMission();
        return new MissionResponse(
                mission.getId(),
                mission.getStatus(),
                mission.getStartDate(),
                mission.getEndDate(),
                mission.getPosition(),
                mission.getWorkplace(),
                mission.getHourlyWage(),
                mission.getWorkReason(),
                mission.getNotes(),
                mission.getRefusalReason(),
                previous != null,
                previous == null ? null : previous.getStartDate(),
                previous == null ? null : previous.getEndDate(),
                application.getId(),
                offer.getId(),
                offer.getTitle(),
                candidate.getId(),
                candidate.getFirstName(),
                candidate.getLastName(),
                candidate.getEmail(),
                employer.getId(),
                employer.getCompanyName(),
                employer.getFirstName(),
                employer.getLastName(),
                employer.getEmail(),
                slots.stream().map(DailySlotResponse::fromEntity).toList(),
                contract == null ? null : ContractResponse.fromEntity(contract));
    }
}
