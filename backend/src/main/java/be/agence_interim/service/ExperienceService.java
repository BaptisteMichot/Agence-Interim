package be.agence_interim.service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import be.agence_interim.dto.ExperienceRequest;
import be.agence_interim.model.Experience;
import be.agence_interim.repository.ExperienceRepository;
import be.agence_interim.repository.UserRepository;

/** Gestion des expériences professionnelles de l'utilisateur courant. */
@Service
public class ExperienceService {

    private final ExperienceRepository experienceRepository;
    private final UserRepository userRepository;

    public ExperienceService(ExperienceRepository experienceRepository, UserRepository userRepository) {
        this.experienceRepository = experienceRepository;
        this.userRepository = userRepository;
    }

    public List<Experience> list(int userId) {
        return experienceRepository.findByUserIdOrderByStartDateDesc(userId);
    }

    public Experience add(int userId, ExperienceRequest request) {
        validateDates(request.startDate(), request.endDate());
        Experience experience = new Experience();
        experience.setUser(userRepository.getReferenceById(userId));
        apply(experience, request);
        return experienceRepository.save(experience);
    }

    public Experience update(int userId, int id, ExperienceRequest request) {
        validateDates(request.startDate(), request.endDate());
        Experience experience = experienceRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Expérience introuvable."));
        apply(experience, request);
        return experienceRepository.save(experience);
    }

    public void delete(int userId, int id) {
        Experience experience = experienceRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Expérience introuvable."));
        experienceRepository.delete(experience);
    }

    private void apply(Experience experience, ExperienceRequest request) {
        experience.setCompanyName(request.companyName());
        experience.setPosition(request.position());
        experience.setStartDate(request.startDate());
        experience.setEndDate(request.endDate());
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("La date de fin doit etre apres la date de debut.");
        }
    }
}
