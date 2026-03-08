package com.example.backend.ai.analytics;

import com.example.backend.entitie.Depense;
import com.example.backend.entitie.Facture;
import com.example.backend.entitie.enumuration.StatutFacture;
import com.example.backend.repository.DepenseRepository;
import com.example.backend.repository.FactureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CashFlowForecastService {

    private final FactureRepository factureRepository;
    private final DepenseRepository depenseRepository;

    public Map<String, Object> forecast(Long enterpriseId) {
                requireEnterprise(enterpriseId);
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusDays(30);

        List<Facture> factures = factureRepository
                .findByDevis_Client_Entreprise_Id(enterpriseId.intValue(), Pageable.unpaged())
                .getContent();

        double incoming = factures.stream()
                .filter(f -> f.getDueDate() != null && !f.getDueDate().isBefore(today) && !f.getDueDate().isAfter(horizon))
                .filter(f -> f.getStatututFacture() != StatutFacture.PAYEE && f.getStatututFacture() != StatutFacture.ANNULEE)
                .mapToDouble(f -> f.getTotalTtc() != null ? f.getTotalTtc() : 0d)
                .sum();

        List<Depense> depenses = depenseRepository.findAll();
        double outgoing = depenses.stream()
                .filter(d -> d.getTache() != null && d.getTache().getEntreprise() != null && enterpriseId.equals(d.getTache().getEntreprise().getId()))
                .filter(d -> d.getDate() != null && !d.getDate().isBefore(today) && !d.getDate().isAfter(horizon))
                .mapToDouble(d -> d.getMontant() != null ? d.getMontant() : 0d)
                .sum();

        double projectedBalance = incoming - outgoing;

        return Map.of(
                "incoming30d", BigDecimal.valueOf(incoming),
                "outgoing30d", BigDecimal.valueOf(outgoing),
                "projectedBalance30d", BigDecimal.valueOf(projectedBalance)
        );
    }

        private void requireEnterprise(Long enterpriseId) {
                if (enterpriseId == null || enterpriseId <= 0) {
                        throw new IllegalArgumentException("enterpriseId required");
                }
        }
}
