package com.example.backend.service;

import com.example.backend.entitie.Facture;
import com.example.backend.entitie.Tache;
import com.example.backend.entitie.Utilisateur;
import com.example.backend.entitie.enumuration.NotificationType;
import com.example.backend.entitie.enumuration.StatutFacture;
import com.example.backend.entitie.enumuration.StatusTache;
import com.example.backend.entitie.enumuration.TypeRole;
import com.example.backend.repository.FactureRepository;
import com.example.backend.repository.TacheRepository;
import com.example.backend.repository.UtilisateurRepository;
import com.example.backend.service.NotificationService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AlertScheduler {

    private final TacheRepository tacheRepository;
    private final FactureRepository factureRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;

    public AlertScheduler(TacheRepository tacheRepository,
                          FactureRepository factureRepository,
                          UtilisateurRepository utilisateurRepository,
                          NotificationService notificationService) {
        this.tacheRepository = tacheRepository;
        this.factureRepository = factureRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void detectOverdueItems() {
        LocalDate today = LocalDate.now();
        handleOverdueTasks(today);
        handleOverdueInvoices(today);
    }

    private void handleOverdueTasks(LocalDate today) {
        List<Tache> candidates = tacheRepository.findOverdueCandidates(today);
        for (Tache tache : candidates) {
            if (tache.getEntreprise() == null || tache.getId() == null) {
                continue;
            }
            tache.setStatus(StatusTache.EN_RETARD);
            tacheRepository.save(tache);

            List<Long> recipients = recipientsForEntreprise(tache.getEntreprise().getId());
            if (!recipients.isEmpty()) {
                String title = "Tâche en retard";
                String message = String.format("%s est en retard (échéance %s)",
                        tache.getDescription(),
                        tache.getDateFin());
                notificationService.sendInAppNotificationToUsers(recipients, title, message, NotificationType.TASK, tache.getId(), "TACHE");
            }
        }
    }

    private void handleOverdueInvoices(LocalDate today) {
        List<Facture> factures = factureRepository.findOverdue(today);
        for (Facture facture : factures) {
            if (facture.getId() == null || facture.getDevis() == null || facture.getDevis().getClient() == null || facture.getDevis().getClient().getEntreprise() == null) {
                continue;
            }
            if (facture.getStatututFacture() != StatutFacture.EN_RETARD) {
                facture.setStatututFacture(StatutFacture.EN_RETARD);
                factureRepository.save(facture);
            }

            Integer entrepriseId = facture.getDevis().getClient().getEntreprise().getId();
            List<Long> recipients = recipientsForEntreprise(entrepriseId);
            if (!recipients.isEmpty()) {
                String title = "Facture en retard";
                String message = String.format("Facture %s est en retard (échéance %s)",
                        facture.getReference() != null ? facture.getReference() : facture.getNumeroFacture(),
                        facture.getDueDate());
                notificationService.sendInAppNotificationToUsers(recipients, title, message, NotificationType.INVOICE, facture.getId(), "FACTURE");
            }
        }
    }

    private List<Long> recipientsForEntreprise(Integer entrepriseId) {
        List<Long> adminIds = utilisateurRepository.findByEntreprise_IdAndRole(entrepriseId, TypeRole.ROLE_ADMIN)
                .stream().map(Utilisateur::getId).toList();
        List<Long> secretaireIds = utilisateurRepository.findByEntreprise_IdAndRole(entrepriseId, TypeRole.ROLE_SECRETAIRE)
                .stream().map(Utilisateur::getId).toList();

        List<Long> recipients = new ArrayList<>();
        recipients.addAll(adminIds);
        secretaireIds.stream().filter(id -> !recipients.contains(id)).forEach(recipients::add);
        return recipients;
    }
}
