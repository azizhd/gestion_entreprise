package com.example.backend.report;

import java.time.LocalDate;
import java.util.List;

public record ReportDocument(
        String companyName,
        String companyLogo,
        String companyAddress,
        String companyEmail,
        String companyPhone,
        String reportTitle,
        String dateRange,
        LocalDate generatedAt,
        List<ReportSummaryItem> summaries,
        List<ReportTable> tables,
        List<ReportSummaryItem> totals,
        String notes
) {}
