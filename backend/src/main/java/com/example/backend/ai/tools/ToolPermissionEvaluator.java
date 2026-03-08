package com.example.backend.ai.tools;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class ToolPermissionEvaluator {

        private static final Map<String, Set<AiTool>> ROLE_TOOLS = Map.of(
            "ROLE_ADMIN", EnumSet.allOf(AiTool.class),
            "ROLE_COMPTABLE", EnumSet.of(
                AiTool.GET_MONTH_REVENUE,
                AiTool.GET_REVENUE_TREND,
                AiTool.GET_UNPAID_INVOICES,
                AiTool.GET_OVERDUE_INVOICES,
                AiTool.PREDICT_REVENUE,
                AiTool.PREDICT_CASH_FLOW,
                AiTool.CALCULATE_PAYMENT_RISK,
                AiTool.GET_SUPPLIER_ANALYTICS,
                AiTool.GET_CLIENT_LIST,
                AiTool.GET_CLIENT_COUNT,
                AiTool.GET_CLIENT_ANALYTICS,
                AiTool.GET_CLIENT_REVENUE_SUMMARY,
                AiTool.GET_FACTURE_LIST,
                AiTool.GET_FACTURE_DETAILS,
                AiTool.GET_INVOICE_STATISTICS,
                AiTool.GET_YEARLY_REVENUE,
                AiTool.GET_REVENUE_BY_CLIENT,
                AiTool.GET_EXPENSE_SUMMARY,
                AiTool.GET_PROFIT_ANALYSIS,
                AiTool.GET_MONTHLY_PROFIT,
                AiTool.GET_GLOBAL_KPIS,
                AiTool.GET_ENTERPRISE_OVERVIEW,
                AiTool.GET_TASK_STATISTICS,
                AiTool.GET_COMPLETED_TASKS,
                AiTool.GET_OVERDUE_TASKS,
                AiTool.GET_TASK_BY_DATE_RANGE,
                AiTool.GET_TASK_LOAD_PER_EMPLOYEE
            ),
            "ROLE_SECRETAIRE", EnumSet.of(
                AiTool.GET_APPOINTMENT_SUMMARY,
                AiTool.GET_TASK_LOAD_PER_EMPLOYEE,
                AiTool.GET_CLIENT_LIST,
                AiTool.GET_CLIENT_COUNT,
                AiTool.GET_CLIENT_ANALYTICS
            ),
            "ROLE_EMPLOYEE", EnumSet.of(
                AiTool.GET_TASK_LOAD_PER_EMPLOYEE,
                AiTool.GET_TASK_STATISTICS,
                AiTool.GET_COMPLETED_TASKS,
                AiTool.GET_OVERDUE_TASKS
            )
        );

    public void assertAllowed(String role, AiTool tool) {
        var allowed = ROLE_TOOLS.getOrDefault(role, Set.of());
        if (!allowed.contains(tool)) {
            throw new AccessDeniedException("Tool not permitted for role " + role);
        }
    }
}
