package com.example.backend.ai.analytics;

import com.example.backend.ai.tools.AiTool;
import com.example.backend.entitie.Client;
import com.example.backend.entitie.Facture;
import com.example.backend.entitie.Fournisseur;
import com.example.backend.entitie.RendezVous;
import com.example.backend.entitie.Tache;
import com.example.backend.entitie.Utilisateur;
import com.example.backend.entitie.enumuration.StatutFacture;
import com.example.backend.entitie.enumuration.StatusDepense;
import com.example.backend.entitie.enumuration.StatusTache;
import com.example.backend.repository.ClientRepository;
import com.example.backend.repository.DepenseRepository;
import com.example.backend.repository.FactureRepository;
import com.example.backend.repository.FournisseurRepository;
import com.example.backend.repository.RendezVousRepository;
import com.example.backend.repository.TacheRepository;
import com.example.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessAnalyticsService {

        private final RevenueForecastService revenueForecastService;
        private final PaymentRiskService paymentRiskService;
        private final CashFlowForecastService cashFlowForecastService;
        private final TaskAnalyticsService taskAnalyticsService;
        private final FactureRepository factureRepository;
        private final RendezVousRepository rendezVousRepository;
        private final FournisseurRepository fournisseurRepository;
        private final DepenseRepository depenseRepository;
        private final ClientRepository clientRepository;
        private final UtilisateurRepository utilisateurRepository;
        private final TacheRepository tacheRepository;

        public Object execute(AiTool tool, Long enterpriseId, Map<String, Object> args) {
                requireEnterprise(enterpriseId);
                return switch (tool) {
                        case GET_MONTH_REVENUE, GET_REVENUE_TREND -> revenueForecastService.lastSixMonthsRevenue(enterpriseId);
                        case GET_UNPAID_INVOICES -> unpaidInvoices(enterpriseId);
                        case GET_OVERDUE_INVOICES -> overdueInvoices(enterpriseId);
                        case GET_TASK_LOAD_PER_EMPLOYEE -> taskAnalyticsService.summarize(enterpriseId);
                        case PREDICT_REVENUE -> Map.<String, Object>of("forecast", revenueForecastService.predictNextMonth(enterpriseId));
                        case PREDICT_CASH_FLOW -> cashFlowForecastService.forecast(enterpriseId);
                        case CALCULATE_PAYMENT_RISK -> paymentRiskService.calculate(enterpriseId);
                        case GET_APPOINTMENT_SUMMARY -> appointmentSummary(enterpriseId);
                        case GET_SUPPLIER_ANALYTICS -> supplierAnalytics(enterpriseId);

                        case GET_CLIENT_LIST -> clientList(enterpriseId);
                        case GET_CLIENT_COUNT -> clientCount(enterpriseId);
                        case GET_CLIENT_ANALYTICS -> clientAnalytics(enterpriseId);
                        case GET_CLIENT_REVENUE_SUMMARY, GET_REVENUE_BY_CLIENT -> clientRevenueSummary(enterpriseId);

                        case GET_EMPLOYEE_LIST -> employeeList(enterpriseId);
                        case GET_EMPLOYEE_DETAILS -> employeeDetails(enterpriseId, args);
                        case GET_EMPLOYEE_PERFORMANCE -> employeePerformance(enterpriseId, args);
                        case GET_EMPLOYEE_TASK_HISTORY -> employeeTaskHistory(enterpriseId, args);

                        case GET_FACTURE_LIST -> factureList(enterpriseId);
                        case GET_FACTURE_DETAILS -> factureDetails(enterpriseId, args);
                        case GET_INVOICE_STATISTICS -> invoiceStatistics(enterpriseId);

                        case GET_YEARLY_REVENUE -> yearlyRevenue(enterpriseId);
                        case GET_EXPENSE_SUMMARY -> expenseSummary(enterpriseId);
                        case GET_PROFIT_ANALYSIS -> profitAnalysis(enterpriseId);
                        case GET_MONTHLY_PROFIT -> monthlyProfit(enterpriseId);

                        case GET_TASK_STATISTICS -> taskStatistics(enterpriseId);
                        case GET_COMPLETED_TASKS -> completedTasks(enterpriseId);
                        case GET_OVERDUE_TASKS -> overdueTasks(enterpriseId);
                        case GET_TASK_BY_DATE_RANGE -> taskByDateRange(enterpriseId, args);

                        case GET_GLOBAL_KPIS -> globalKpis(enterpriseId);
                        case GET_ENTERPRISE_OVERVIEW -> enterpriseOverview(enterpriseId);
                };
        }

    private List<Map<String, Object>> unpaidInvoices(Long enterpriseId) {
                requireEnterprise(enterpriseId);
        List<Facture> factures = factureRepository
                .findByDevis_Client_Entreprise_Id(enterpriseId.intValue(), Pageable.unpaged())
                .getContent();
        return factures.stream()
                .filter(f -> f.getStatututFacture() != StatutFacture.PAYEE && f.getStatututFacture() != StatutFacture.ANNULEE)
                .map(f -> Map.<String, Object>of(
                        "id", f.getId(),
                        "amount", f.getTotalTtc(),
                        "dueDate", f.getDueDate(),
                        "status", f.getStatututFacture()
                ))
                .toList();
    }

    private List<Map<String, Object>> overdueInvoices(Long enterpriseId) {
                requireEnterprise(enterpriseId);
        LocalDate today = LocalDate.now();
        List<Facture> factures = factureRepository
                .findByDevis_Client_Entreprise_Id(enterpriseId.intValue(), Pageable.unpaged())
                .getContent();
        return factures.stream()
                .filter(f -> f.getDueDate() != null && f.getDueDate().isBefore(today))
                .filter(f -> f.getStatututFacture() != StatutFacture.PAYEE && f.getStatututFacture() != StatutFacture.ANNULEE)
                .map(f -> Map.<String, Object>of(
                        "id", f.getId(),
                        "amount", f.getTotalTtc(),
                        "dueDate", f.getDueDate(),
                        "status", f.getStatututFacture()
                ))
                .toList();
    }

    private Map<String, Object> appointmentSummary(Long enterpriseId) {
                requireEnterprise(enterpriseId);
        List<RendezVous> rdvs = rendezVousRepository.findByEntreprise_Id(enterpriseId.intValue(), Pageable.unpaged()).getContent();
        List<Map<String, Object>> upcoming = rdvs.stream()
                .filter(r -> r.getStartDateTime() != null && r.getStartDateTime().isAfter(java.time.LocalDateTime.now()))
                .sorted(java.util.Comparator.comparing(RendezVous::getStartDateTime))
                .limit(10)
                .map(r -> Map.<String, Object>of(
                        "id", r.getId(),
                        "title", r.getTitre(),
                        "start", r.getStartDateTime(),
                        "end", r.getEndDateTime(),
                        "status", r.getStatus(),
                        "client", r.getClient() != null ? r.getClient().getNom() : r.getContactName()
                ))
                .toList();
        return Map.<String, Object>of(
                "count", rdvs.size(),
                "upcoming", upcoming
        );
    }

    private Map<String, Object> supplierAnalytics(Long enterpriseId) {
                requireEnterprise(enterpriseId);
        List<Fournisseur> fournisseurs = fournisseurRepository.findByEntreprise_Id(enterpriseId.intValue(), Pageable.unpaged()).getContent();
        long active = fournisseurs.stream().filter(f -> Boolean.TRUE.equals(f.getActif())).count();
        List<Object[]> topSpent = depenseRepository.topFournisseursByDepense(enterpriseId.intValue());
        List<Map<String, Object>> top = topSpent.stream()
                .limit(5)
                .map(row -> Map.<String, Object>of(
                        "fournisseurId", row[0],
                        "amount", row[1]
                ))
                .toList();
        return Map.<String, Object>of(
                "total", fournisseurs.size(),
                "active", active,
                "topBySpend", top
        );
    }

    private Map<String, Object> clientList(Long enterpriseId) {
        Page<Client> clients = clientRepository.findByEntreprise_Id(enterpriseId.intValue(), Pageable.ofSize(100));
        List<Map<String, Object>> items = clients.getContent().stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "name", c.getNom(),
                        "email", c.getEmail(),
                        "phone", c.getTelephone()))
                .toList();
        return Map.<String, Object>of("items", items, "total", Long.valueOf(clients.getTotalElements()));
    }

    private Map<String, Object> clientCount(Long enterpriseId) {
        long total = clientRepository.findByEntreprise_Id(enterpriseId.intValue(), Pageable.unpaged()).getTotalElements();
        return Map.<String, Object>of("total", total);
    }

    private Map<String, Object> clientAnalytics(Long enterpriseId) {
        List<Client> clients = clientRepository.findByEntreprise_Id(enterpriseId.intValue(), Pageable.unpaged()).getContent();
        double avgPaymentTerms = clients.stream()
                .map(Client::getPaymentTermsDays)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);
        long withEmail = clients.stream().filter(c -> c.getEmail() != null && !c.getEmail().isBlank()).count();
        return Map.<String, Object>of(
                "total", clients.size(),
                "withEmail", withEmail,
                "avgPaymentTermsDays", avgPaymentTerms
        );
    }

    private Map<String, Object> clientRevenueSummary(Long enterpriseId) {
        List<Object[]> rows = factureRepository.revenueByClient(enterpriseId.intValue());
        List<Map<String, Object>> perClient = rows.stream()
                .map(r -> Map.<String, Object>of("clientId", r[0], "amount", r[1]))
                .toList();
        double total = perClient.stream().mapToDouble(e -> ((Number) e.get("amount")).doubleValue()).sum();
        return Map.<String, Object>of("total", total, "byClient", perClient);
    }

    private Map<String, Object> employeeList(Long enterpriseId) {
        List<Utilisateur> users = utilisateurRepository.findByEntreprise_Id(enterpriseId.intValue());
        List<Map<String, Object>> items = users.stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "name", u.getPrenom() + " " + u.getNom(),
                        "email", u.getEmail(),
                        "role", u.getRole(),
                        "phone", u.getTelephone()))
                .toList();
        return Map.<String, Object>of("total", items.size(), "items", items);
    }

    private Map<String, Object> employeeDetails(Long enterpriseId, Map<String, Object> args) {
        Long employeeId = requireLong(args, "employeeId");
        Utilisateur user = utilisateurRepository.findById(employeeId)
                .filter(u -> u.getEntreprise() != null && Objects.equals(u.getEntreprise().getId(), enterpriseId.intValue()))
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        return Map.<String, Object>of(
                "id", user.getId(),
                "name", user.getPrenom() + " " + user.getNom(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "phone", user.getTelephone()
        );
    }

    private Map<String, Object> employeePerformance(Long enterpriseId, Map<String, Object> args) {
        Long employeeId = requireLong(args, "employeeId");
        Utilisateur user = utilisateurRepository.findById(employeeId)
                .filter(u -> u.getEntreprise() != null && Objects.equals(u.getEntreprise().getId(), enterpriseId.intValue()))
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        long total = tacheRepository.countByAssignee(employeeId, enterpriseId.intValue());
        long completed = tacheRepository.countByAssigneeAndStatus(employeeId, enterpriseId.intValue(), StatusTache.TERMINE);
        long overdue = tacheRepository.countByAssigneeAndStatus(employeeId, enterpriseId.intValue(), StatusTache.EN_RETARD);
        return Map.<String, Object>of(
                "employeeId", user.getId(),
                "tasksTotal", total,
                "tasksCompleted", completed,
                "tasksOverdue", overdue
        );
    }

    private Map<String, Object> employeeTaskHistory(Long enterpriseId, Map<String, Object> args) {
        Long employeeId = requireLong(args, "employeeId");
        Page<Tache> tasks = tacheRepository.findByUtilisateurs_IdAndEntreprise_Id(employeeId, enterpriseId.intValue(), Pageable.ofSize(20));
        List<Map<String, Object>> items = tasks.getContent().stream()
                .sorted(Comparator.comparing(Tache::getDateFin, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(t -> Map.<String, Object>of(
                        "id", t.getId(),
                        "status", t.getStatus(),
                        "start", t.getDateDebut(),
                        "end", t.getDateFin(),
                        "progress", t.getProgress()))
                .toList();
        return Map.<String, Object>of("total", Long.valueOf(tasks.getTotalElements()), "items", items);
    }

    private Map<String, Object> factureList(Long enterpriseId) {
        Page<Facture> factures = factureRepository.findByDevis_Client_Entreprise_Id(enterpriseId.intValue(), Pageable.ofSize(50));
        List<Map<String, Object>> items = factures.getContent().stream()
                .map(f -> Map.<String, Object>of(
                        "id", f.getId(),
                        "number", f.getNumeroFacture(),
                        "amount", f.getTotalTtc(),
                        "status", f.getStatututFacture(),
                        "dueDate", f.getDueDate()))
                .toList();
        return Map.<String, Object>of("total", Long.valueOf(factures.getTotalElements()), "items", items);
    }

    private Map<String, Object> factureDetails(Long enterpriseId, Map<String, Object> args) {
        Long factureId = requireLong(args, "factureId");
        Facture facture = factureRepository.findByIdAndDevis_Client_Entreprise_Id(factureId, enterpriseId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        return Map.<String, Object>of(
                "id", facture.getId(),
                "number", facture.getNumeroFacture(),
                "amount", facture.getTotalTtc(),
                "status", facture.getStatututFacture(),
                "dueDate", facture.getDueDate(),
                "clientId", facture.getDevis() != null && facture.getDevis().getClient() != null ? facture.getDevis().getClient().getId() : null
        );
    }

    private Map<String, Object> invoiceStatistics(Long enterpriseId) {
        List<Facture> factures = factureRepository.findByDevis_Client_Entreprise_Id(enterpriseId.intValue(), Pageable.unpaged()).getContent();
        long total = factures.size();
        long paid = factures.stream().filter(f -> f.getStatututFacture() == StatutFacture.PAYEE).count();
        long unpaid = factures.stream().filter(f -> f.getStatututFacture() != StatutFacture.PAYEE && f.getStatututFacture() != StatutFacture.ANNULEE).count();
        long overdue = factures.stream()
                .filter(f -> f.getDueDate() != null && f.getDueDate().isBefore(LocalDate.now()))
                .filter(f -> f.getStatututFacture() != StatutFacture.PAYEE && f.getStatututFacture() != StatutFacture.ANNULEE)
                .count();
        double totalAmount = factures.stream().map(Facture::getTotalTtc).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();
        double paidAmount = factureRepository.sumTotalTtcByEntrepriseAndStatus(enterpriseId.intValue(), StatutFacture.PAYEE);
        return Map.<String, Object>of(
                "total", total,
                "paid", paid,
                "unpaid", unpaid,
                "overdue", overdue,
                "amountTotal", totalAmount,
                "amountPaid", paidAmount
        );
    }

    private Map<String, Object> yearlyRevenue(Long enterpriseId) {
        List<Object[]> rows = factureRepository.yearlyTotals(enterpriseId.intValue());
        List<Map<String, Object>> items = rows.stream()
                .map(r -> Map.<String, Object>of("year", r[0], "amount", r[1]))
                .toList();
        double total = items.stream().mapToDouble(e -> ((Number) e.get("amount")).doubleValue()).sum();
        return Map.<String, Object>of("total", total, "years", items);
    }

    private Map<String, Object> expenseSummary(Long enterpriseId) {
        double total = depenseRepository.sumByEntreprise(enterpriseId.intValue());
        double approved = depenseRepository.sumByEntrepriseAndStatus(enterpriseId.intValue(), StatusDepense.APPROVED);
        double pending = depenseRepository.sumByEntrepriseAndStatus(enterpriseId.intValue(), StatusDepense.PENDING);
        double declined = depenseRepository.sumByEntrepriseAndStatus(enterpriseId.intValue(), StatusDepense.DECLINED);
        return Map.<String, Object>of(
                "total", total,
                "approved", approved,
                "pending", pending,
                "declined", declined
        );
    }

    private Map<String, Object> profitAnalysis(Long enterpriseId) {
        double revenue = factureRepository.sumTotalTtcByEntreprise(enterpriseId.intValue());
        double expenses = depenseRepository.sumByEntreprise(enterpriseId.intValue());
        return Map.<String, Object>of(
                "revenue", revenue,
                "expenses", expenses,
                "profit", revenue - expenses
        );
    }

    private Map<String, Object> monthlyProfit(Long enterpriseId) {
        var revenueRows = factureRepository.monthlyTotals(enterpriseId.intValue());
        var expenseRows = depenseRepository.monthlyTotalsByEntreprise(enterpriseId.intValue());
        Map<String, Double> revenueMap = revenueRows.stream()
                .collect(Collectors.toMap(r -> String.valueOf(r[0]), r -> ((Number) r[1]).doubleValue()));
        Map<String, Double> expenseMap = expenseRows.stream()
                .collect(Collectors.toMap(r -> String.valueOf(r[0]), r -> ((Number) r[1]).doubleValue()));
        var months = revenueMap.keySet();
        var allMonths = new java.util.HashSet<>(months);
        allMonths.addAll(expenseMap.keySet());
        List<Map<String, Object>> items = allMonths.stream()
                .sorted()
                .map(m -> Map.<String, Object>of(
                        "month", m,
                        "revenue", revenueMap.getOrDefault(m, 0.0),
                        "expenses", expenseMap.getOrDefault(m, 0.0),
                        "profit", revenueMap.getOrDefault(m, 0.0) - expenseMap.getOrDefault(m, 0.0)
                ))
                .toList();
        return Map.<String, Object>of("items", items);
    }

    private Map<String, Object> taskStatistics(Long enterpriseId) {
        return Map.<String, Object>of(
                "toDo", tacheRepository.countByEntrepriseAndStatus(enterpriseId.intValue(), StatusTache.A_FAIRE),
                "inProgress", tacheRepository.countByEntrepriseAndStatus(enterpriseId.intValue(), StatusTache.EN_COURS),
                "completed", tacheRepository.countByEntrepriseAndStatus(enterpriseId.intValue(), StatusTache.TERMINE),
                "overdue", tacheRepository.countByEntrepriseAndStatus(enterpriseId.intValue(), StatusTache.EN_RETARD)
        );
    }

    private Map<String, Object> completedTasks(Long enterpriseId) {
        List<Tache> tasks = tacheRepository.findTop10ByEntreprise_IdAndStatusOrderByDateFinDesc(enterpriseId.intValue(), StatusTache.TERMINE);
        List<Map<String, Object>> items = tasks.stream()
                .map(t -> Map.<String, Object>of(
                        "id", t.getId(),
                        "end", t.getDateFin(),
                        "progress", t.getProgress()))
                .toList();
        return Map.<String, Object>of("items", items);
    }

    private Map<String, Object> overdueTasks(Long enterpriseId) {
        LocalDate today = LocalDate.now();
        List<Tache> tasks = tacheRepository.findByEntreprise_Id(enterpriseId.intValue());
        List<Map<String, Object>> items = tasks.stream()
                .filter(t -> (t.getDateFin() != null && t.getDateFin().isBefore(today)) || t.getStatus() == StatusTache.EN_RETARD)
                .sorted(Comparator.comparing(Tache::getDateFin, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(20)
                .map(t -> Map.<String, Object>of(
                        "id", t.getId(),
                        "end", t.getDateFin(),
                        "status", t.getStatus()))
                .toList();
        return Map.<String, Object>of("items", items, "count", items.size());
    }

    private Map<String, Object> taskByDateRange(Long enterpriseId, Map<String, Object> args) {
        LocalDate start = requireDate(args, "startDate");
        LocalDate end = requireDate(args, "endDate");
        List<Tache> tasks = tacheRepository.findByEntreprise_IdAndDateFinBetween(enterpriseId.intValue(), start, end);
        List<Map<String, Object>> items = tasks.stream()
                .map(t -> Map.<String, Object>of(
                        "id", t.getId(),
                        "start", t.getDateDebut(),
                        "end", t.getDateFin(),
                        "status", t.getStatus()))
                .toList();
        return Map.<String, Object>of("items", items, "total", items.size());
    }

    private Map<String, Object> globalKpis(Long enterpriseId) {
        long clients = clientRepository.findByEntreprise_Id(enterpriseId.intValue(), Pageable.unpaged()).getTotalElements();
        long employees = utilisateurRepository.findByEntreprise_Id(enterpriseId.intValue()).size();
        double revenue = factureRepository.sumTotalTtcByEntreprise(enterpriseId.intValue());
        double expenses = depenseRepository.sumByEntreprise(enterpriseId.intValue());
        long overdueInvoices = overdueInvoices(enterpriseId).size();
        long overdueTasks = tacheRepository.countByEntrepriseAndStatus(enterpriseId.intValue(), StatusTache.EN_RETARD);
        return Map.<String, Object>of(
                "clients", clients,
                "employees", employees,
                "revenue", revenue,
                "expenses", expenses,
                "overdueInvoices", overdueInvoices,
                "overdueTasks", overdueTasks
        );
    }

    private Map<String, Object> enterpriseOverview(Long enterpriseId) {
        return Map.<String, Object>of(
                "clients", clientAnalytics(enterpriseId),
                "employees", employeeList(enterpriseId),
                "invoices", invoiceStatistics(enterpriseId),
                "profit", profitAnalysis(enterpriseId),
                "tasks", taskStatistics(enterpriseId)
        );
    }

    private Long requireLong(Map<String, Object> args, String key) {
        if (args == null || !args.containsKey(key)) {
            throw new IllegalArgumentException("Missing required argument: " + key);
        }
        Object value = args.get(key);
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number for " + key);
        }
    }

    private LocalDate requireDate(Map<String, Object> args, String key) {
        if (args == null || !args.containsKey(key)) {
            throw new IllegalArgumentException("Missing required argument: " + key);
        }
        try {
            return LocalDate.parse(String.valueOf(args.get(key)));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date for " + key);
        }
    }

        private void requireEnterprise(Long enterpriseId) {
                if (enterpriseId == null || enterpriseId <= 0) {
                        throw new IllegalArgumentException("enterpriseId required");
                }
        }
}
