package com.example.backend.ai.service;

import com.example.backend.ai.analytics.BusinessAnalyticsService;
import com.example.backend.ai.dto.ToolCall;
import com.example.backend.ai.tools.AiTool;
import com.example.backend.ai.tools.ToolPermissionEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToolExecutionService {

    private final ToolPermissionEvaluator permissionEvaluator;
    private final BusinessAnalyticsService analyticsService;

        private static final Map<AiTool, Set<String>> TOOL_ARGUMENTS = Map.ofEntries(
            Map.entry(AiTool.GET_MONTH_REVENUE, Set.of()),
            Map.entry(AiTool.GET_REVENUE_TREND, Set.of()),
            Map.entry(AiTool.GET_UNPAID_INVOICES, Set.of()),
            Map.entry(AiTool.GET_OVERDUE_INVOICES, Set.of()),
            Map.entry(AiTool.GET_TASK_LOAD_PER_EMPLOYEE, Set.of()),
            Map.entry(AiTool.PREDICT_REVENUE, Set.of()),
            Map.entry(AiTool.PREDICT_CASH_FLOW, Set.of()),
            Map.entry(AiTool.CALCULATE_PAYMENT_RISK, Set.of()),
            Map.entry(AiTool.GET_APPOINTMENT_SUMMARY, Set.of()),
            Map.entry(AiTool.GET_SUPPLIER_ANALYTICS, Set.of()),

            Map.entry(AiTool.GET_CLIENT_LIST, Set.of()),
            Map.entry(AiTool.GET_CLIENT_COUNT, Set.of()),
            Map.entry(AiTool.GET_CLIENT_ANALYTICS, Set.of()),
            Map.entry(AiTool.GET_CLIENT_REVENUE_SUMMARY, Set.of()),

            Map.entry(AiTool.GET_EMPLOYEE_LIST, Set.of()),
            Map.entry(AiTool.GET_EMPLOYEE_DETAILS, Set.of("employeeId")),
            Map.entry(AiTool.GET_EMPLOYEE_PERFORMANCE, Set.of("employeeId")),
            Map.entry(AiTool.GET_EMPLOYEE_TASK_HISTORY, Set.of("employeeId")),

            Map.entry(AiTool.GET_FACTURE_LIST, Set.of()),
            Map.entry(AiTool.GET_FACTURE_DETAILS, Set.of("factureId")),
            Map.entry(AiTool.GET_INVOICE_STATISTICS, Set.of()),

            Map.entry(AiTool.GET_YEARLY_REVENUE, Set.of()),
            Map.entry(AiTool.GET_REVENUE_BY_CLIENT, Set.of()),
            Map.entry(AiTool.GET_EXPENSE_SUMMARY, Set.of()),
            Map.entry(AiTool.GET_PROFIT_ANALYSIS, Set.of()),
            Map.entry(AiTool.GET_MONTHLY_PROFIT, Set.of()),

            Map.entry(AiTool.GET_TASK_STATISTICS, Set.of()),
            Map.entry(AiTool.GET_COMPLETED_TASKS, Set.of()),
            Map.entry(AiTool.GET_OVERDUE_TASKS, Set.of()),
            Map.entry(AiTool.GET_TASK_BY_DATE_RANGE, Set.of("startDate", "endDate")),

            Map.entry(AiTool.GET_GLOBAL_KPIS, Set.of()),
            Map.entry(AiTool.GET_ENTERPRISE_OVERVIEW, Set.of())
        );

    public Object execute(String role, Long enterpriseId, ToolCall call) {
        Instant start = Instant.now();
        validateCall(role, enterpriseId, call);
        permissionEvaluator.assertAllowed(role, call.tool());
        log.info("[AI][TOOL] execute role={} enterprise={} tool={}", role, enterpriseId, call.tool());
        Object result = analyticsService.execute(call.tool(), enterpriseId, call.args());
        log.info("[AI][TOOL] success role={} enterprise={} tool={} durationMs={}", role, enterpriseId, call.tool(), java.time.Duration.between(start, Instant.now()).toMillis());
        return result;
    }

    private void validateCall(String role, Long enterpriseId, ToolCall call) {
        Objects.requireNonNull(role, "role required");
        Objects.requireNonNull(enterpriseId, "enterpriseId required");
        if (call == null || call.tool() == null) {
            throw new IllegalArgumentException("Invalid tool call");
        }
        // ensure tool is from enum (defensive against malformed JSON)
        if (!Set.of(com.example.backend.ai.tools.AiTool.values()).contains(call.tool())) {
            throw new IllegalArgumentException("Unknown tool");
        }
        validateArgs(call.tool(), call.args());
    }

    private void validateArgs(AiTool tool, Object args) {
        if (args == null) {
            requireNoArguments(tool);
            return;
        }
        if (!(args instanceof Map<?, ?> argMap)) {
            throw new IllegalArgumentException("Invalid tool arguments type");
        }
        Map<String, Object> safeMap = argMap.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        e -> String.valueOf(e.getKey()),
                        Map.Entry::getValue
                ));
        Set<String> expected = TOOL_ARGUMENTS.getOrDefault(tool, Set.of());
        if (!expected.isEmpty()) {
            // required fields must exist
            for (String key : expected) {
                if (!safeMap.containsKey(key)) {
                    throw new IllegalArgumentException("Missing required argument: " + key);
                }
            }
        }
        if (!safeMap.isEmpty() && expected.isEmpty()) {
            throw new IllegalArgumentException("Unexpected arguments for tool " + tool);
        }
        // if expected had entries, check for unexpected
        if (!expected.isEmpty()) {
            var unexpected = safeMap.keySet().stream()
                    .filter(k -> !expected.contains(k))
                    .toList();
            if (!unexpected.isEmpty()) {
                throw new IllegalArgumentException("Unexpected arguments: " + unexpected);
            }
        }
    }

    private void requireNoArguments(AiTool tool) {
        if (!TOOL_ARGUMENTS.getOrDefault(tool, Set.of()).isEmpty()) {
            throw new IllegalArgumentException("Missing required arguments for tool " + tool);
        }
    }
}
