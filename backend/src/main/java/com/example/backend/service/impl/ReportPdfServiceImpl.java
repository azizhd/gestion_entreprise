package com.example.backend.service.impl;

import com.example.backend.report.ReportDocument;
import com.example.backend.service.ReportPdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportPdfServiceImpl implements ReportPdfService {

    private final ReportPdfBuilder builder;

    @Override
    public byte[] generate(ReportDocument document) throws Exception {
        try {
            return builder.build(document);
        } catch (Exception ex) {
            log.error("Failed to generate report PDF: {}", document != null ? document.reportTitle() : "unknown", ex);
            throw ex;
        }
    }
}
