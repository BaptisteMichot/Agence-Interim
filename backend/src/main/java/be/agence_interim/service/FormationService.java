package be.agence_interim.service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import be.agence_interim.dto.FormationRequest;
import be.agence_interim.model.Formation;
import be.agence_interim.model.FormationStatus;
import be.agence_interim.repository.FormationRepository;
import be.agence_interim.repository.UserRepository;

/** Gestion des formations de l'utilisateur courant. */
@Service
public class FormationService {

    private final FormationRepository formationRepository;
    private final UserRepository userRepository;

    public FormationService(FormationRepository formationRepository, UserRepository userRepository) {
        this.formationRepository = formationRepository;
        this.userRepository = userRepository;
    }

    public List<Formation> list(int userId) {
        return formationRepository.findByUserIdOrderByStartDateDesc(userId);
    }

    public Formation add(int userId, FormationRequest request) {
        validateDates(request.startDate(), request.endDate());
        Formation formation = new Formation();
        formation.setUser(userRepository.getReferenceById(userId));
        apply(formation, request);
        return formationRepository.save(formation);
    }

    public Formation update(int userId, int id, FormationRequest request) {
        validateDates(request.startDate(), request.endDate());
        Formation formation = formationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Formation introuvable."));
        apply(formation, request);
        return formationRepository.save(formation);
    }

    public void delete(int userId, int id) {
        Formation formation = formationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Formation introuvable."));
        formationRepository.delete(formation);
    }

    private void apply(Formation formation, FormationRequest request) {
        formation.setTitle(request.title());
        formation.setInstitution(request.institution());
        formation.setStartDate(request.startDate());
        formation.setEndDate(request.endDate());
        // Statut déduit : pas de date de fin = en cours, date de fin renseignée = terminé.
        formation.setStatus(request.endDate() == null ? FormationStatus.EN_COURS : FormationStatus.TERMINE);
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("La date de fin doit etre apres la date de debut.");
        }
    }
}
