package com.example.backend.report;

import java.util.List;

public record ReportTable(String title, List<String> headers, List<List<String>> rows) {}
