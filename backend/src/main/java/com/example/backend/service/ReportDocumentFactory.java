package com.example.backend.service;

import com.example.backend.report.ReportDocument;
import com.example.backend.report.ReportSummaryItem;
import com.example.backend.report.ReportTable;
import com.example.backend.entitie.Entreprise;
import com.example.backend.entitie.Utilisateur;
import com.example.backend.entitie.Facture;
import com.example.backend.entitie.enumuration.StatutFacture;
import com.example.backend.entitie.enumuration.StatusDepense;
import com.example.backend.repository.DepenseRepository;
import com.example.backend.repository.EntrepriseRepository;
import com.example.backend.repository.FactureRepository;
import com.example.backend.repository.UtilisateurRepository;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Map;
import java.util.TreeMap;

@Component
public class ReportDocumentFactory {

    private final EntrepriseRepository entrepriseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final FactureRepository factureRepository;
    private final DepenseRepository depenseRepository;

    public ReportDocumentFactory(EntrepriseRepository entrepriseRepository,
                                 UtilisateurRepository utilisateurRepository,
                                 FactureRepository factureRepository,
                                 DepenseRepository depenseRepository) {
        this.entrepriseRepository = entrepriseRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.factureRepository = factureRepository;
        this.depenseRepository = depenseRepository;
    }

    public ReportDocument build(String key, LocalDate start, LocalDate end) {
        String dateRange = formatRange(start, end);
        Entreprise company = resolveEntreprise();
        Integer entrepriseId = company != null ? company.getId() : null;
        LocalDateTime startDateTime = toStartOfDay(start);
        LocalDateTime endDateTime = toEndOfDay(end);

        return switch (key) {
            case "monthly-revenue" -> revenueReport(company, entrepriseId, dateRange, startDateTime, endDateTime);
            case "expenses" -> expenseReport(company, entrepriseId, dateRange, start, end);
            case "profit" -> profitReport(company, entrepriseId, dateRange, startDateTime, endDateTime, start, end);
            case "client-payments" -> clientPaymentsReport(company, entrepriseId, dateRange, startDateTime, endDateTime);
            default -> revenueReport(company, entrepriseId, dateRange, startDateTime, endDateTime);
        };
    }

    private ReportDocument revenueReport(Entreprise company, Integer entrepriseId, String dateRange,
                                         LocalDateTime start, LocalDateTime end) {
        if (entrepriseId == null) {
            return emptyDocument(company, "Rapport - Revenus mensuels", dateRange);
        }

        double total = factureRepository.sumTotalTtcByEntrepriseAndDate(entrepriseId, start, end);
        double paidAmount = factureRepository.sumTotalTtcByEntrepriseStatusAndDate(entrepriseId, StatutFacture.PAYEE, start, end);
        long paidCount = factureRepository.countByEntrepriseStatusAndDate(entrepriseId, StatutFacture.PAYEE, start, end);
        List<Facture> overdue = factureRepository.findOverdueByEntrepriseAndDate(entrepriseId, LocalDate.now(), start, end);
        double overdueAmount = overdue.stream().mapToDouble(f -> safeNumber(f.getTotalTtc())).sum();
        long overdueCount = overdue.size();

        List<List<String>> monthlyRows = toMonthlyRows(factureRepository.monthlyTotalsByDate(entrepriseId, start, end));
        List<List<String>> clientRows = topClients(entrepriseId, start, end);

        return new ReportDocument(
                companyName(company),
            companyLogo(company),
                companyLocation(company),
                companyEmail(company),
                companyPhone(company),
                "Rapport - Revenus mensuels",
                dateRange,
                LocalDate.now(),
                List.of(
                        new ReportSummaryItem("Revenus", money(total), "Total période"),
                        new ReportSummaryItem("Payé", money(paidAmount), paidCount + " factures"),
                        new ReportSummaryItem("En retard", money(overdueAmount), overdueCount + " factures")
                ),
                List.of(
                        new ReportTable(
                                "Revenus par mois",
                                List.of("Mois", "Montant"),
                                monthlyRows
                        ),
                        new ReportTable(
                                "Top clients",
                                List.of("Client", "Montant"),
                                clientRows
                        )
                ),
                List.of(new ReportSummaryItem("Total", money(total), "")),
                "Généré automatiquement"
        );
    }

    private ReportDocument expenseReport(Entreprise company, Integer entrepriseId, String dateRange,
                                         LocalDate start, LocalDate end) {
        if (entrepriseId == null) {
            return emptyDocument(company, "Rapport - Dépenses", dateRange);
        }

        double total = depenseRepository.sumByEntrepriseAndDate(entrepriseId, start, end);
        double approved = depenseRepository.sumByEntrepriseStatusAndDate(entrepriseId, StatusDepense.APPROVED, start, end);
        double pending = depenseRepository.sumByEntrepriseStatusAndDate(entrepriseId, StatusDepense.PENDING, start, end);
        List<List<String>> monthlyRows = toMonthlyRows(depenseRepository.monthlyTotalsByEntrepriseAndDate(entrepriseId, start, end));

        return new ReportDocument(
                companyName(company),
            companyLogo(company),
                companyLocation(company),
                companyEmail(company),
                companyPhone(company),
                "Rapport - Dépenses",
                dateRange,
                LocalDate.now(),
                List.of(
                        new ReportSummaryItem("Dépenses", money(total), "Total période"),
                        new ReportSummaryItem("Approuvées", money(approved), ""),
                        new ReportSummaryItem("En attente", money(pending), "")
                ),
                List.of(
                        new ReportTable(
                                "Dépenses par mois",
                                List.of("Mois", "Montant"),
                                monthlyRows
                        )
                ),
                List.of(new ReportSummaryItem("Total", money(total), "")),
                "Généré automatiquement"
        );
    }

    private ReportDocument profitReport(Entreprise company, Integer entrepriseId, String dateRange,
                                        LocalDateTime startDateTime, LocalDateTime endDateTime,
                                        LocalDate startDate, LocalDate endDate) {
        if (entrepriseId == null) {
            return emptyDocument(company, "Rapport - Profit & Marge", dateRange);
        }

        double revenue = factureRepository.sumTotalTtcByEntrepriseAndDate(entrepriseId, startDateTime, endDateTime);
        double expenses = depenseRepository.sumByEntrepriseAndDate(entrepriseId, startDate, endDate);
        double profit = revenue - expenses;
        double margin = revenue <= 0 ? 0 : (profit / revenue) * 100;

        List<List<String>> profitRows = monthlyProfitRows(entrepriseId, startDateTime, endDateTime, startDate, endDate);

        return new ReportDocument(
                companyName(company),
            companyLogo(company),
                companyLocation(company),
                companyEmail(company),
                companyPhone(company),
                "Rapport - Profit & Marge",
                dateRange,
                LocalDate.now(),
                List.of(
                        new ReportSummaryItem("Revenus", money(revenue), "Brut"),
                        new ReportSummaryItem("Dépenses", money(expenses), "Charges"),
                        new ReportSummaryItem("Profit", money(profit), "Net"),
                        new ReportSummaryItem("Marge", String.format(Locale.FRENCH, "%.1f%%", margin), "")
                ),
                List.of(
                        new ReportTable(
                                "Profit par mois",
                                List.of("Mois", "Revenus", "Dépenses", "Profit"),
                                profitRows
                        )
                ),
                List.of(new ReportSummaryItem("Profit net", money(profit), "")),
                "Généré automatiquement"
        );
    }

    private ReportDocument clientPaymentsReport(Entreprise company, Integer entrepriseId, String dateRange,
                                                LocalDateTime start, LocalDateTime end) {
        if (entrepriseId == null) {
            return emptyDocument(company, "Rapport - Paiements clients", dateRange);
        }

        double total = factureRepository.sumTotalTtcByEntrepriseAndDate(entrepriseId, start, end);
        double paid = factureRepository.sumTotalTtcByEntrepriseStatusAndDate(entrepriseId, StatutFacture.PAYEE, start, end);
        double outstanding = Math.max(total - paid, 0);
        List<Facture> overdue = factureRepository.findOverdueByEntrepriseAndDate(entrepriseId, LocalDate.now(), start, end);
        double overdueAmount = overdue.stream().mapToDouble(f -> safeNumber(f.getTotalTtc())).sum();
        long overdueCount = overdue.size();

        List<List<String>> clientRows = topClients(entrepriseId, start, end);

        return new ReportDocument(
                companyName(company),
            companyLogo(company),
                companyLocation(company),
                companyEmail(company),
                companyPhone(company),
                "Rapport - Paiements clients",
                dateRange,
                LocalDate.now(),
                List.of(
                        new ReportSummaryItem("Paiements reçus", money(paid), ""),
                        new ReportSummaryItem("Montant en retard", money(overdueAmount), overdueCount + " factures"),
                        new ReportSummaryItem("Solde dû", money(outstanding), "Total dû")
                ),
                List.of(
                        new ReportTable(
                                "Top clients",
                                List.of("Client", "Montant"),
                                clientRows
                        )
                ),
                List.of(new ReportSummaryItem("Total facturé", money(total), "")),
                "Généré automatiquement"
        );
    }

    private ReportDocument emptyDocument(Entreprise company, String title, String dateRange) {
        return new ReportDocument(
                companyName(company),
            companyLogo(company),
                companyLocation(company),
                companyEmail(company),
                companyPhone(company),
                title,
                dateRange,
                LocalDate.now(),
                List.of(new ReportSummaryItem("Données indisponibles", "-", "")),
                List.of(),
                List.of(),
                "Généré automatiquement"
        );
    }

    private List<List<String>> toMonthlyRows(List<Object[]> rows) {
        if (rows == null) return List.of();
        return rows.stream()
                .map(r -> List.of(formatYearMonth(r[0]), money(safeNumber(r[1]))))
                .toList();
    }

    private List<List<String>> topClients(Integer entrepriseId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> raw = factureRepository.revenueByClientAndDate(entrepriseId, start, end);
        if (raw == null) return List.of();
        return raw.stream()
                .limit(6)
                .map(r -> List.of(r[0] != null ? r[0].toString() : "Client", money(safeNumber(r[1]))))
                .toList();
    }

    private List<List<String>> monthlyProfitRows(Integer entrepriseId, LocalDateTime startDateTime, LocalDateTime endDateTime,
                                                 LocalDate startDate, LocalDate endDate) {
        List<Object[]> revenue = factureRepository.monthlyTotalsByDate(entrepriseId, startDateTime, endDateTime);
        List<Object[]> expenses = depenseRepository.monthlyTotalsByEntrepriseAndDate(entrepriseId, startDate, endDate);

        Map<String, double[]> grouped = new TreeMap<>();
        if (revenue != null) {
            for (Object[] r : revenue) {
                String key = r[0] != null ? r[0].toString() : "";
                grouped.computeIfAbsent(key, k -> new double[2])[0] = safeNumber(r[1]);
            }
        }
        if (expenses != null) {
            for (Object[] r : expenses) {
                String key = r[0] != null ? r[0].toString() : "";
                grouped.computeIfAbsent(key, k -> new double[2])[1] = safeNumber(r[1]);
            }
        }

        return grouped.entrySet().stream()
                .map(entry -> {
                    double revenueValue = entry.getValue()[0];
                    double expenseValue = entry.getValue()[1];
                    double profit = revenueValue - expenseValue;
                    return List.of(
                            formatYearMonth(entry.getKey()),
                            money(revenueValue),
                            money(expenseValue),
                            money(profit)
                    );
                })
                .toList();
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    private LocalDateTime toEndOfDay(LocalDate date) {
        return date != null ? date.atTime(LocalTime.MAX) : null;
    }

    private double safeNumber(Object value) {
        return value instanceof Number n ? n.doubleValue() : 0d;
    }

    private String formatYearMonth(Object ym) {
        if (ym == null) return "";
        String raw = ym.toString();
        if (raw.contains("-")) {
            String[] parts = raw.split("-");
            if (parts.length >= 2) {
                return parts[1] + "/" + parts[0];
            }
        }
        return raw;
    }

    private String money(double value) {
        return String.format(Locale.FRENCH, "%,.2f €", value)
                .replace('\u00a0', ' ')
                .replace('\u202f', ' ')
                .replace("  ", " ")
                .trim();
    }

    private String formatRange(LocalDate start, LocalDate end) {
        if (start == null && end == null) return "Période non spécifiée";
        if (start == null) return "Jusqu'au " + end;
        if (end == null) return "Depuis le " + start;
        return start + " - " + end;
    }

    private Entreprise resolveEntreprise() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        Optional<Utilisateur> userOpt = utilisateurRepository.findByEmail(auth.getName());
        if (userOpt.isEmpty()) return null;
        Utilisateur user = userOpt.get();
        if (user.getEntreprise() != null) {
            return entrepriseRepository.findById(user.getEntreprise().getId()).orElse(null);
        }
        return entrepriseRepository.findByManager_Id(user.getId()).orElse(null);
    }

    private String companyName(Entreprise e) {
        return e != null && e.getNom() != null ? e.getNom() : "Entreprise";
    }

    private String companyLocation(Entreprise e) {
        return e != null && e.getLocation() != null ? e.getLocation() : "";
    }

    private String companyLogo(Entreprise e) {
        return e != null ? e.getLogo() : null;
    }

    private String companyEmail(Entreprise e) {
        return e != null && e.getEmail() != null ? e.getEmail() : "";
    }

    private String companyPhone(Entreprise e) {
        return e != null && e.getTelephone() != null ? e.getTelephone() : "";
    }
}
