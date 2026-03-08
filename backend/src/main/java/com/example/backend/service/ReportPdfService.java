package com.example.backend.service;

import com.example.backend.report.ReportDocument;

public interface ReportPdfService {
    byte[] generate(ReportDocument document) throws Exception;
}
