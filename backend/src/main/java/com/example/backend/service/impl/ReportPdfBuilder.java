package com.example.backend.service.impl;

import com.example.backend.report.ReportDocument;
import com.example.backend.report.ReportSummaryItem;
import com.example.backend.report.ReportTable;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
class ReportPdfBuilder {

    private static final float MARGIN = 50f;
    private static final float HEADER_HEIGHT = 90f;
    private static final float FOOTER_HEIGHT = 40f;
    private static final float TABLE_ROW_HEIGHT = 22f;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Path logoStorageRoot;

    ReportPdfBuilder(@Value("${app.entreprises.logos.storage:uploads/logos}") String logoStoragePath) {
        this.logoStorageRoot = Paths.get(logoStoragePath).toAbsolutePath().normalize();
    }

    byte[] build(ReportDocument doc) throws Exception {
        try (PDDocument pdf = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            pdf.addPage(page);
            PDRectangle box = page.getMediaBox();
            float usableWidth = box.getWidth() - (2 * MARGIN);

            try (PDPageContentStream cs = new PDPageContentStream(pdf, page)) {
                float cursorY = box.getUpperRightY() - MARGIN;
                cursorY = drawHeader(pdf, cs, doc, usableWidth, cursorY);
                cursorY = drawTitle(cs, doc, usableWidth, cursorY - 20);
                cursorY = drawSummaries(cs, doc.summaries(), usableWidth, cursorY - 25);
                cursorY = drawTables(cs, doc.tables(), usableWidth, cursorY - 30);
                cursorY = drawTotals(cs, doc.totals(), usableWidth, cursorY - 25);
                drawFooter(cs, doc, usableWidth, MARGIN);
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                pdf.save(baos);
                return baos.toByteArray();
            }
        }
    }

    private float drawHeader(PDDocument pdf, PDPageContentStream cs, ReportDocument doc, float width, float startY) throws Exception {
        float y = startY;
        cs.setNonStrokingColor(new Color(243, 246, 255));
        cs.addRect(MARGIN, y - HEADER_HEIGHT, width, HEADER_HEIGHT);
        cs.fill();
        cs.setNonStrokingColor(Color.BLACK);

        float rightTextX = MARGIN + width - 110;
        drawLogo(pdf, cs, doc.companyLogo(), MARGIN + width - 20, y - 10, 60, 60);

        cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
        write(cs, safe(doc.companyName()), MARGIN + 10, y - 24);
        cs.setFont(PDType1Font.HELVETICA, 11);
        write(cs, safe(doc.companyAddress()), MARGIN + 10, y - 40);
        write(cs, safe(doc.companyEmail()), MARGIN + 10, y - 55);
        write(cs, safe(doc.companyPhone()), MARGIN + 10, y - 70);

        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        writeRight(cs, "Date : " + (doc.generatedAt() != null ? DATE_FMT.format(doc.generatedAt()) : ""), rightTextX, y - 24);
        writeRight(cs, safe(doc.dateRange()), rightTextX, y - 40);
        return y - HEADER_HEIGHT;
    }

    private void drawLogo(PDDocument pdf, PDPageContentStream cs, String logoName, float x, float y,
                          float maxWidth, float maxHeight) {
        if (logoName == null || logoName.isBlank()) {
            return;
        }
        try {
            if (logoName.contains("://")) {
                return;
            }
            Path logoPath = resolveLogoPath(logoName);
            if (!Files.exists(logoPath)) {
                return;
            }
            PDImageXObject image = PDImageXObject.createFromFile(logoPath.toAbsolutePath().toString(), pdf);
            float scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight());
            float imgWidth = image.getWidth() * scale;
            float imgHeight = image.getHeight() * scale;
            cs.drawImage(image, x - imgWidth, y - imgHeight, imgWidth, imgHeight);
        } catch (Exception ignored) {
            // Ignore invalid paths or IO errors to keep report generation resilient.
        }
    }

    private Path resolveLogoPath(String logoName) {
        Path rawPath = Paths.get(logoName);
        if (rawPath.isAbsolute()) {
            return rawPath.normalize();
        }
        return logoStorageRoot.resolve(logoName).normalize();
    }

    private float drawTitle(PDPageContentStream cs, ReportDocument doc, float width, float startY) throws Exception {
        cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
        float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(doc.reportTitle()) / 1000 * 20;
        float centerX = MARGIN + (width - textWidth) / 2;
        write(cs, doc.reportTitle(), centerX, startY - 5);
        return startY - 18;
    }

    private float drawSummaries(PDPageContentStream cs, List<ReportSummaryItem> summaries, float width, float startY) throws Exception {
        if (summaries == null || summaries.isEmpty()) return startY;
        float cardWidth = (width - 20) / 3f;
        float y = startY;
        int count = 0;
        for (ReportSummaryItem item : summaries) {
            float x = MARGIN + (count % 3) * (cardWidth + 10);
            y = startY - (count / 3) * 70f;
            drawSummaryCard(cs, item, x, y, cardWidth, 60f);
            count++;
        }
        float rows = (float) Math.ceil(summaries.size() / 3.0);
        return startY - rows * 70f;
    }

    private void drawSummaryCard(PDPageContentStream cs, ReportSummaryItem item, float x, float y, float w, float h) throws Exception {
        cs.setStrokingColor(new Color(217, 225, 240));
        cs.setLineWidth(0.8f);
        cs.addRect(x, y - h, w, h);
        cs.stroke();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        write(cs, safe(item.label()), x + 10, y - 16);
        cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
        write(cs, safe(item.value()), x + 10, y - 34);
        cs.setFont(PDType1Font.HELVETICA, 10);
        write(cs, safe(item.hint()), x + 10, y - 50);
    }

    private float drawTables(PDPageContentStream cs, List<ReportTable> tables, float width, float startY) throws Exception {
        float cursor = startY;
        if (tables == null) return cursor;
        for (ReportTable table : tables) {
            cursor -= 10;
            cs.setFont(PDType1Font.HELVETICA_BOLD, 13);
            write(cs, safe(table.title()), MARGIN, cursor);
            cursor -= 14;
            cursor = drawTable(cs, table, width, cursor);
            cursor -= 12;
        }
        return cursor;
    }

    private float drawTable(PDPageContentStream cs, ReportTable table, float width, float startY) throws Exception {
        List<String> headers = table.headers();
        List<List<String>> rows = table.rows();
        if (headers == null || rows == null) return startY;
        float colWidth = width / headers.size();
        float y = startY;

        // Header row
        cs.setNonStrokingColor(new Color(232, 238, 249));
        cs.addRect(MARGIN, y - TABLE_ROW_HEIGHT, width, TABLE_ROW_HEIGHT);
        cs.fill();
        cs.setNonStrokingColor(Color.BLACK);
        cs.setStrokingColor(new Color(210, 220, 235));
        cs.setLineWidth(0.5f);
        cs.addRect(MARGIN, y - TABLE_ROW_HEIGHT, width, TABLE_ROW_HEIGHT);
        cs.stroke();

        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        for (int i = 0; i < headers.size(); i++) {
            write(cs, safe(headers.get(i)), MARGIN + 6 + (i * colWidth), y - 15);
        }

        // Body
        y -= TABLE_ROW_HEIGHT;
        cs.setFont(PDType1Font.HELVETICA, 10);
        for (List<String> row : rows) {
            cs.addRect(MARGIN, y - TABLE_ROW_HEIGHT, width, TABLE_ROW_HEIGHT);
            cs.stroke();
            for (int i = 0; i < headers.size(); i++) {
                String cell = i < row.size() ? row.get(i) : "";
                write(cs, safe(cell), MARGIN + 6 + (i * colWidth), y - 15);
            }
            y -= TABLE_ROW_HEIGHT;
        }
        return y;
    }

    private float drawTotals(PDPageContentStream cs, List<ReportSummaryItem> totals, float width, float startY) throws Exception {
        if (totals == null || totals.isEmpty()) return startY;
        float boxWidth = width * 0.4f;
        float x = MARGIN + width - boxWidth;
        float h = 24f * totals.size() + 24f;

        cs.setStrokingColor(new Color(200, 210, 230));
        cs.setLineWidth(0.9f);
        cs.addRect(x, startY - h, boxWidth, h);
        cs.stroke();

        float y = startY - 18;
        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        write(cs, "Totaux", x + 10, y);
        y -= 12;

        cs.setFont(PDType1Font.HELVETICA, 11);
        for (ReportSummaryItem item : totals) {
            write(cs, safe(item.label()), x + 10, y - 8);
            writeRight(cs, safe(item.value()), x + boxWidth - 10, y - 8);
            y -= 18;
        }
        return startY - h;
    }

    private void drawFooter(PDPageContentStream cs, ReportDocument doc, float width, float margin) throws Exception {
        cs.setFont(PDType1Font.HELVETICA, 9);
        cs.setStrokingColor(new Color(210, 220, 235));
        cs.moveTo(margin, margin + 14);
        cs.lineTo(margin + width, margin + 14);
        cs.stroke();
        write(cs, safe(doc.notes()), margin, margin);
        writeRight(cs, "Page 1", margin + width, margin);
    }

    private void write(PDPageContentStream cs, String text, float x, float y) throws Exception {
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private void writeRight(PDPageContentStream cs, String text, float x, float y) throws Exception {
        float tw = PDType1Font.HELVETICA.getStringWidth(text) / 1000 * 11;
        cs.beginText();
        cs.newLineAtOffset(x - tw, y);
        cs.showText(text);
        cs.endText();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
