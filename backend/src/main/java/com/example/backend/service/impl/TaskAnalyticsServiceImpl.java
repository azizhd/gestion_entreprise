package com.example.backend.service.impl;

import com.example.backend.dto.EmployeeTaskCountDto;
import com.example.backend.dto.TaskAnalyticsResponse;
import com.example.backend.dto.TaskExpenseTotalDto;
import com.example.backend.entitie.Entreprise;
import com.example.backend.entitie.Tache;
import com.example.backend.entitie.Utilisateur;
import com.example.backend.entitie.enumuration.StatusDepense;
import com.example.backend.entitie.enumuration.StatusTache;
import com.example.backend.repository.DepenseRepository;
import com.example.backend.repository.EntrepriseRepository;
import com.example.backend.repository.TacheRepository;
import com.example.backend.repository.UtilisateurRepository;
import com.example.backend.service.TaskAnalyticsService;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class TaskAnalyticsServiceImpl implements TaskAnalyticsService {

    private final TacheRepository tacheRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final DepenseRepository depenseRepository;

    public TaskAnalyticsServiceImpl(TacheRepository tacheRepository,
                                    UtilisateurRepository utilisateurRepository,
                                    EntrepriseRepository entrepriseRepository,
                                    DepenseRepository depenseRepository) {
        this.tacheRepository = tacheRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.depenseRepository = depenseRepository;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public TaskAnalyticsResponse computeAnalytics() {
        Entreprise entreprise = resolveCurrentEntreprise();
        List<Utilisateur> employees = utilisateurRepository.findByEntreprise_Id(entreprise.getId());
        List<Tache> tasks = tacheRepository.findByEntreprise_Id(entreprise.getId());

        List<EmployeeTaskCountDto> taskCounts = new ArrayList<>();
        for (Utilisateur user : employees) {
            long count = tacheRepository.countByAssignee(user.getId(), entreprise.getId());
            taskCounts.add(new EmployeeTaskCountDto(user.getId(), user.getNom() + " " + user.getPrenom(), count));
        }

        List<TaskExpenseTotalDto> expenseTotals = new ArrayList<>();
        for (Tache task : tasks) {
            Double approved = depenseRepository.sumByTaskAndStatus(task.getId(), entreprise.getId(), StatusDepense.APPROVED);
            Double pending = depenseRepository.sumByTaskAndStatus(task.getId(), entreprise.getId(), StatusDepense.PENDING);
            Double declined = depenseRepository.sumByTaskAndStatus(task.getId(), entreprise.getId(), StatusDepense.DECLINED);
            expenseTotals.add(new TaskExpenseTotalDto(task.getId(), task.getDescription(), approved, pending, declined));
        }

        long overdueCount = tacheRepository.countOverdue(entreprise.getId());
        Double avgCompletion = computeAverageCompletion(tasks);

        return TaskAnalyticsResponse.builder()
                .overdueTasks(overdueCount)
                .averageCompletionDays(avgCompletion)
                .tasksPerEmployee(taskCounts)
                .expensesPerTask(expenseTotals)
                .build();
    }

    private Double computeAverageCompletion(List<Tache> tasks) {
        List<Long> durations = tasks.stream()
                .filter(t -> StatusTache.TERMINE.equals(t.getStatus()))
                .filter(t -> t.getDateDebut() != null && t.getDateFin() != null)
                .map(t -> ChronoUnit.DAYS.between(t.getDateDebut(), t.getDateFin()))
                .toList();
        if (durations.isEmpty()) {
            return null;
        }
        long sum = durations.stream().reduce(0L, Long::sum);
        return sum / (double) durations.size();
    }

    private Entreprise resolveCurrentEntreprise() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new SecurityException("No authenticated user");
        }
        Utilisateur utilisateur = utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new SecurityException("Authenticated user not found"));
        if (utilisateur.getEntreprise() == null) {
            return entrepriseRepository.findByManager_Id(utilisateur.getId())
                    .orElseThrow(() -> new SecurityException("Entreprise introuvable pour l'utilisateur"));
        }
        return entrepriseRepository.findById(utilisateur.getEntreprise().getId())
                .orElseThrow(() -> new SecurityException("Entreprise introuvable pour l'utilisateur"));
    }
}
