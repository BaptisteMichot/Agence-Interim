package be.agence_interim.service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import be.agence_interim.dto.UnavailabilityRequest;
import be.agence_interim.dto.UnavailabilityResponse;
import be.agence_interim.model.DailySchedule;
import be.agence_interim.model.MissionStatus;
import be.agence_interim.model.Unavailability;
import be.agence_interim.repository.DailyScheduleRepository;
import be.agence_interim.repository.UnavailabilityRepository;
import be.agence_interim.repository.UserRepository;

/**
 * Disponibilités de l'intérimaire : les indisponibilités déclarées ne sont
 * modifiables qu'à partir de J+8 (demande fonctionnelle 16), et ne peuvent pas
 * tomber sur une journée de mission confirmée.
 */
@Service
public class AvailabilityService {

    /** Délai de prévenance : on ne touche plus aux 7 prochains jours. */
    public static final int NOTICE_DAYS = 8;

    private final UnavailabilityRepository unavailabilityRepository;
    private final DailyScheduleRepository dailyScheduleRepository;
    private final UserRepository userRepository;

    public AvailabilityService(
            UnavailabilityRepository unavailabilityRepository,
            DailyScheduleRepository dailyScheduleRepository,
            UserRepository userRepository) {
        this.unavailabilityRepository = unavailabilityRepository;
        this.dailyScheduleRepository = dailyScheduleRepository;
        this.userRepository = userRepository;
    }

    /** Première date modifiable (aujourd'hui + 8 jours). */
    public static LocalDate editableFrom() {
        return LocalDate.now().plusDays(NOTICE_DAYS);
    }

    @Transactional(readOnly = true)
    public List<UnavailabilityResponse> list(int userId, LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now();
        LocalDate end = to != null ? to : start.plusYears(1);
        LocalDate editableFrom = editableFrom();
        return unavailabilityRepository
                .findByUserIdAndDateBetweenOrderByDateAscStartTimeAsc(userId, start, end)
                .stream()
                .map(unavailability -> UnavailabilityResponse.of(unavailability, editableFrom))
                .toList();
    }

    @Transactional
    public UnavailabilityResponse add(int userId, UnavailabilityRequest request) {
        requireEditableDate(request.date());
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("L'heure de fin doit être postérieure à l'heure de début.");
        }
        checkNoOverlapWithExisting(userId, request);
        checkNoMissionThatDay(userId, request);

        Unavailability unavailability = new Unavailability();
        unavailability.setUser(userRepository.getReferenceById(userId));
        unavailability.setDate(request.date());
        unavailability.setStartTime(request.startTime());
        unavailability.setEndTime(request.endTime());
        unavailability.setReason(
                request.reason() == null || request.reason().isBlank() ? null : request.reason().trim());
        return UnavailabilityResponse.of(unavailabilityRepository.save(unavailability), editableFrom());
    }

    @Transactional
    public void remove(int userId, int unavailabilityId) {
        Unavailability unavailability = unavailabilityRepository.findById(unavailabilityId)
                .filter(item -> item.getUser().getId() == userId)
                .orElseThrow(() -> new NoSuchElementException("Indisponibilité introuvable."));
        requireEditableDate(unavailability.getDate());
        unavailabilityRepository.delete(unavailability);
    }

    /** FR16 : les disponibilités ne se modifient qu'à partir de J+8. */
    private void requireEditableDate(LocalDate date) {
        LocalDate editableFrom = editableFrom();
        if (date.isBefore(editableFrom)) {
            throw new IllegalArgumentException(
                    "Les disponibilités ne sont modifiables qu'à partir du " + editableFrom
                            + " (délai de " + NOTICE_DAYS + " jours).");
        }
    }

    private void checkNoOverlapWithExisting(int userId, UnavailabilityRequest request) {
        boolean overlap = unavailabilityRepository.findByUserIdAndDate(userId, request.date()).stream()
                .anyMatch(existing -> existing.getStartTime().isBefore(request.endTime())
                        && request.startTime().isBefore(existing.getEndTime()));
        if (overlap) {
            throw new IllegalArgumentException("Une indisponibilité couvre déjà cette plage horaire.");
        }
    }

    private void checkNoMissionThatDay(int userId, UnavailabilityRequest request) {
        List<DailySchedule> missionDays = dailyScheduleRepository.findForJobSeekerBetween(
                userId, MissionStatus.ACTIVE, request.date(), request.date());
        boolean overlap = missionDays.stream()
                .anyMatch(day -> day.getStartTime().isBefore(request.endTime())
                        && request.startTime().isBefore(day.getEndTime()));
        if (overlap) {
            throw new IllegalArgumentException(
                    "Une mission confirmée est déjà planifiée sur cette plage horaire.");
        }
    }
}
