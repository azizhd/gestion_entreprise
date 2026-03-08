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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueForecastService {

    private final FactureRepository factureRepository;

    public List<MonthlyAmount> lastSixMonthsRevenue(Long enterpriseId) {
        requireEnterprise(enterpriseId);
        var yearMonths = buildRecentMonths(6);
        List<Facture> factures = factureRepository
                .findByDevis_Client_Entreprise_Id(enterpriseId.intValue(), Pageable.unpaged())
                .getContent();

        Map<YearMonth, BigDecimal> grouped = factures.stream()
                .filter(f -> f.getStatututFacture() == StatutFacture.PAYEE)
                .filter(f -> f.getDate() != null || f.getDueDate() != null)
                .collect(Collectors.groupingBy(
                        f -> YearMonth.from(f.getDate() != null ? f.getDate() : f.getDueDate()),
                        Collectors.reducing(BigDecimal.ZERO, f -> BigDecimal.valueOf(f.getTotalTtc() != null ? f.getTotalTtc() : 0), BigDecimal::add)
                ));

        List<MonthlyAmount> result = new ArrayList<>();
        for (YearMonth ym : yearMonths) {
            result.add(new MonthlyAmount(ym, grouped.getOrDefault(ym, BigDecimal.ZERO)));
        }
        return result;
    }

    public BigDecimal predictNextMonth(Long enterpriseId) {
        requireEnterprise(enterpriseId);
        List<MonthlyAmount> last = lastSixMonthsRevenue(enterpriseId);
        if (last.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (last.size() == 1) {
            return last.get(0).amount();
        }
        BigDecimal first = last.get(0).amount();
        BigDecimal lastVal = last.get(last.size() - 1).amount();
        BigDecimal growth = lastVal.subtract(first)
                .divide(BigDecimal.valueOf(Math.max(last.size() - 1, 1)), RoundingMode.HALF_UP);
        return lastVal.add(growth).max(BigDecimal.ZERO);
    }

    private void requireEnterprise(Long enterpriseId) {
        if (enterpriseId == null || enterpriseId <= 0) {
            throw new IllegalArgumentException("enterpriseId required");
        }
    }

    private List<YearMonth> buildRecentMonths(int count) {
        List<YearMonth> months = new ArrayList<>(count);
        YearMonth current = YearMonth.from(LocalDate.now());
        for (int i = count - 1; i >= 0; i--) {
            months.add(current.minusMonths(i));
        }
        return months;
    }
}
