package com.example.backend.controller;

import com.example.backend.report.ReportDocument;
import com.example.backend.service.ReportDocumentFactory;
import com.example.backend.service.ReportPdfService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportPdfService pdfService;
    private final ReportDocumentFactory documentFactory;

    public ReportController(ReportPdfService pdfService, ReportDocumentFactory documentFactory) {
        this.pdfService = pdfService;
        this.documentFactory = documentFactory;
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasAnyRole('ADMIN','COMPTABLE')")
    public ResponseEntity<byte[]> export(@PathVariable String key,
                                         @RequestParam(defaultValue = "pdf") String format,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        ReportDocument document = documentFactory.build(key, startDate, endDate);

        if ("pdf".equalsIgnoreCase(format)) {
            byte[] bytes = pdfService.generate(document);
            String filename = key + "-" + LocalDate.now() + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(bytes);
        }

        return ResponseEntity.status(415).build();
    }
}
