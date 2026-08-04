package be.agence_interim.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import be.agence_interim.dto.MissionResponse;
import be.agence_interim.dto.ScheduleEntryResponse;
import be.agence_interim.security.CurrentUser;
import be.agence_interim.service.MissionService;

/** Missions de l'intérimaire authentifié (routes /api/missions/** = rôle JOBSEEKER). */
@RestController
@RequestMapping("/api/missions")
public class JobSeekerMissionController {

    private final MissionService missionService;

    public JobSeekerMissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    @GetMapping
    public List<MissionResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return missionService.listForJobSeeker(CurrentUser.id(jwt));
    }

    /** Nombre de missions attendant une réponse (badge du portail). */
    @GetMapping("/decision-count")
    public Map<String, Long> decisionCount(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("count", missionService.decisionCount(CurrentUser.id(jwt)));
    }

    /** Journées de travail sur une période, pour le planning (missions confirmées). */
    @GetMapping("/schedule")
    public List<ScheduleEntryResponse> schedule(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return missionService.schedule(CurrentUser.id(jwt), from, to);
    }

    @GetMapping("/{id}")
    public MissionResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        return missionService.getForJobSeeker(CurrentUser.id(jwt), id);
    }

    @PostMapping("/{id}/accept")
    public MissionResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        return missionService.accept(CurrentUser.id(jwt), id);
    }

    @PostMapping("/{id}/decline")
    public MissionResponse decline(@AuthenticationPrincipal Jwt jwt, @PathVariable int id) {
        return missionService.decline(CurrentUser.id(jwt), id);
    }
}
