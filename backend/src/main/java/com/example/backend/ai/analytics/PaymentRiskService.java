package com.example.backend.ai.analytics;

import com.example.backend.entitie.Facture;
import com.example.backend.entitie.enumuration.StatutFacture;
import com.example.backend.repository.FactureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentRiskService {

    private final FactureRepository factureRepository;

    public Map<String, Object> calculate(Long enterpriseId) {
                requireEnterprise(enterpriseId);
        List<Facture> factures = factureRepository
                .findByDevis_Client_Entreprise_Id(enterpriseId.intValue(), Pageable.unpaged())
                .getContent();

        long total = factures.size();
        long overdue = factures.stream()
                .filter(f -> f.getDueDate() != null)
                .filter(f -> f.getDueDate().isBefore(LocalDate.now()))
                .filter(f -> f.getStatututFacture() != StatutFacture.PAYEE && f.getStatututFacture() != StatutFacture.ANNULEE)
                .count();

        double avgDelay = factures.stream()
                .filter(f -> f.getDueDate() != null && f.getDate() != null)
                .mapToLong(f -> java.time.Duration.between(f.getDueDate().atStartOfDay(), f.getDate()).toDays())
                .filter(d -> d > 0)
                .average()
                .orElse(0);

        BigDecimal riskScore = total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(overdue * 100.0 / total).setScale(2, RoundingMode.HALF_UP);

        return Map.of(
                "totalInvoices", total,
                "overdueInvoices", overdue,
                "delayFrequency", total == 0 ? "0%" : riskScore + "%",
                "avgDaysLate", avgDelay,
                "riskScore", riskScore
        );
    }

        private void requireEnterprise(Long enterpriseId) {
                if (enterpriseId == null || enterpriseId <= 0) {
                        throw new IllegalArgumentException("enterpriseId required");
                }
        }
}
