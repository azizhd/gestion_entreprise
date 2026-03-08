package com.example.backend.ai.analytics;

import com.example.backend.entitie.Utilisateur;
import com.example.backend.repository.TacheRepository;
import com.example.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskAnalyticsService {

    private final UtilisateurRepository utilisateurRepository;
    private final TacheRepository tacheRepository;

    public Map<String, Object> summarize(Long enterpriseId) {
        requireEnterprise(enterpriseId);
        List<Utilisateur> users = utilisateurRepository.findByEntreprise_Id(enterpriseId.intValue());
        List<Map<String, Object>> perEmployee = new ArrayList<>();
        for (Utilisateur user : users) {
            long count = tacheRepository.countByAssignee(user.getId(), enterpriseId.intValue());
            perEmployee.add(Map.of(
                    "userId", user.getId(),
                    "name", user.getPrenom() + " " + user.getNom(),
                    "taskCount", count
            ));
        }
        long overdue = tacheRepository.countOverdue(enterpriseId.intValue());
        List<Map<String, Object>> overloadRisk = perEmployee.stream()
                .filter(e -> ((Number) e.get("taskCount")).longValue() > 10)
                .toList();
        return Map.of(
                "tasksPerEmployee", perEmployee,
                "overdue", overdue,
                "overloadRisk", overloadRisk
        );
    }

    private void requireEnterprise(Long enterpriseId) {
        if (enterpriseId == null || enterpriseId <= 0) {
            throw new IllegalArgumentException("enterpriseId required");
        }
    }
}
