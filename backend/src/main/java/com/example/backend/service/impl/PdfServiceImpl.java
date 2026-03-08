package com.example.backend.service.impl;

import com.example.backend.entitie.Client;
import com.example.backend.entitie.Devis;
import com.example.backend.entitie.Entreprise;
import com.example.backend.entitie.LigneDevis;
import com.example.backend.service.PdfService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@Slf4j
public class PdfServiceImpl implements PdfService {

    private static final float MARGIN = 50f;
    private static final float HEADER_HEIGHT = 120f;
    private static final float TABLE_ROW_HEIGHT = 24f;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale LOCALE = Locale.FRANCE;

    @Override
    public byte[] generateDevisPdf(Devis devis) throws Exception {
        Objects.requireNonNull(devis, "devis must not be null");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                PDRectangle box = page.getMediaBox();
                float usableWidth = box.getWidth() - (2 * MARGIN);
                float cursorY = box.getUpperRightY() - MARGIN;

                cursorY = drawHeader(doc, cs, devis, usableWidth, cursorY);
                cursorY = drawClientSection(cs, devis, usableWidth, cursorY - 20);
                cursorY = drawLineItems(cs, devis, usableWidth, cursorY - 30);
                cursorY = drawFinancialSummary(cs, devis, usableWidth, cursorY - 25);
                drawFooter(cs, devis, usableWidth, cursorY - 40);
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                doc.save(baos);
                return baos.toByteArray();
            }
        } catch (Exception ex) {
            log.error("Failed to generate devis PDF (id={}, reference={})", devis.getId(), devis.getReference(), ex);
            throw ex;
        }
    }

    private float drawHeader(PDDocument doc, PDPageContentStream cs, Devis devis, float width, float startY) throws IOException {
        float headerY = startY - HEADER_HEIGHT;
        cs.setNonStrokingColor(new Color(244, 247, 252));
        cs.addRect(MARGIN, headerY, width, HEADER_HEIGHT);
        cs.fill();
        cs.setNonStrokingColor(Color.BLACK);

        float leftX = MARGIN + 24;
        float rightX = MARGIN + width - 200;

        Client client = devis.getClient();
        Entreprise entreprise = client != null ? client.getEntreprise() : null;

        drawLogo(doc, cs, entreprise, MARGIN + width - 100, startY - 30, 70, 70);

        cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
        writeText(cs, safe(companyName(entreprise)), leftX, startY - 35);

        cs.setFont(PDType1Font.HELVETICA, 11);
        writeText(cs, safe(entreprise != null ? entreprise.getLocation() : null), leftX, startY - 55);
        writeText(cs, safe(entreprise != null ? entreprise.getEmail() : null), leftX, startY - 70);
        writeText(cs, safe(entreprise != null ? entreprise.getTelephone() : null), leftX, startY - 85);

        cs.setFont(PDType1Font.HELVETICA_BOLD, 22);
        writeText(cs, "DEVIS", rightX, startY - 35);

        cs.setFont(PDType1Font.HELVETICA, 11);
        writeText(cs, "N° : " + documentNumber(devis), rightX, startY - 60);
        writeText(cs, "Date : " + documentDate(devis), rightX, startY - 75);
        writeText(cs, "Statut : " + safe(devis.getStatut() != null ? devis.getStatut().name() : null), rightX, startY - 90);

        return headerY;
    }

    private float drawClientSection(PDPageContentStream cs, Devis devis, float width, float startY) throws IOException {
        float blockHeight = 90f;
        cs.setStrokingColor(new Color(214, 225, 242));
        cs.setLineWidth(0.8f);
        cs.addRect(MARGIN, startY - blockHeight, width, blockHeight);
        cs.stroke();

        Client client = devis.getClient();
        float textY = startY - 20;
        cs.setFont(PDType1Font.HELVETICA_BOLD, 13);
        writeText(cs, "Informations client", MARGIN + 15, textY);

        cs.setFont(PDType1Font.HELVETICA, 11);
        writeText(cs, safe(client != null ? client.getNom() : "Client"), MARGIN + 15, textY - 18);
        writeText(cs, safe(client != null ? client.getAdresse() : null), MARGIN + 15, textY - 34);
        writeText(cs, "Email : " + safe(client != null ? client.getEmail() : null), MARGIN + 15, textY - 50);
        writeText(cs, "Téléphone : " + safe(client != null ? client.getTelephone() : null), MARGIN + 15, textY - 66);

        return startY - blockHeight;
    }

    private float drawLineItems(PDPageContentStream cs, Devis devis, float width, float startY) throws IOException {
        List<LigneDevis> lignes = sanitizeLines(devis.getLignesdevis());
        float[] colWidths = new float[]{width * 0.46f, width * 0.12f, width * 0.18f, width * 0.24f};
        float tableTop = startY;

        // Header background
        cs.setNonStrokingColor(new Color(232, 238, 249));
        cs.addRect(MARGIN, tableTop - TABLE_ROW_HEIGHT, width, TABLE_ROW_HEIGHT);
        cs.fill();
        cs.setNonStrokingColor(Color.BLACK);

        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        writeTableCell(cs, "Description", MARGIN + 8, tableTop - 16);
        writeTableCell(cs, "Quantité", MARGIN + colWidths[0] + 8, tableTop - 16);
        writeAlignedCell(cs, "Prix unitaire", MARGIN + colWidths[0] + colWidths[1] + colWidths[2] - 8, tableTop - 16, true,
            PDType1Font.HELVETICA_BOLD, 11);
        writeAlignedCell(cs, "Total", MARGIN + width - 8, tableTop - 16, true,
            PDType1Font.HELVETICA_BOLD, 11);

        float rowTop = tableTop - TABLE_ROW_HEIGHT;
        cs.setLineWidth(0.5f);

        if (lignes.isEmpty()) {
            rowTop -= TABLE_ROW_HEIGHT;
            drawRowLines(cs, width, rowTop + TABLE_ROW_HEIGHT);
            cs.setFont(PDType1Font.HELVETICA, 11);
            writeTableCell(cs, "Aucune ligne de devis", MARGIN + 8, rowTop + TABLE_ROW_HEIGHT - 16);
            return rowTop;
        }

        for (LigneDevis ligne : lignes) {
            rowTop -= TABLE_ROW_HEIGHT;
            drawRowLines(cs, width, rowTop + TABLE_ROW_HEIGHT);

            double qty = safeNumber(ligne.getQuantite());
            double unit = safeNumber(ligne.getPrixUnitaire());
            double total = ligne.getTotal() != null ? ligne.getTotal() : qty * unit;

            cs.setFont(PDType1Font.HELVETICA, 11);
            writeTableCell(cs, safe(ligne.getDescription()), MARGIN + 8, rowTop + TABLE_ROW_HEIGHT - 16);
            writeAlignedCell(cs, formatQty(qty), MARGIN + colWidths[0] + colWidths[1] - 10, rowTop + TABLE_ROW_HEIGHT - 16, true);
            writeAlignedCell(cs, formatAmount(unit), MARGIN + colWidths[0] + colWidths[1] + colWidths[2] - 8, rowTop + TABLE_ROW_HEIGHT - 16, true);
            writeAlignedCell(cs, formatAmount(total), MARGIN + width - 8, rowTop + TABLE_ROW_HEIGHT - 16, true);
        }

        drawRowLines(cs, width, rowTop);

        return rowTop;
    }

    private float drawFinancialSummary(PDPageContentStream cs, Devis devis, float width, float startY) throws IOException {
        List<LigneDevis> lignes = sanitizeLines(devis.getLignesdevis());
        double totalHt = devis.getTotalHt() != null ? devis.getTotalHt() : computeTotalHt(lignes);
        double tvaRate = devis.getTvaRate() != null ? devis.getTvaRate() : 0d;
        double totalTva = devis.getTotalTva() != null ? devis.getTotalTva() : totalHt * tvaRate;
        double totalTtc = devis.getTotalTtc() != null ? devis.getTotalTtc() : totalHt + totalTva;

        float blockWidth = width * 0.42f;
        float blockX = MARGIN + width - blockWidth;
        float blockHeight = 110f;

        cs.setStrokingColor(new Color(214, 225, 242));
        cs.setLineWidth(0.8f);
        cs.addRect(blockX, startY - blockHeight, blockWidth, blockHeight);
        cs.stroke();

        float textY = startY - 20;
        cs.setFont(PDType1Font.HELVETICA_BOLD, 13);
        writeText(cs, "Récapitulatif financier", blockX + 15, textY);

        textY -= 20;
        writeSummaryRow(cs, "Total HT", formatAmount(totalHt), blockX + 15, textY,
            PDType1Font.HELVETICA, 11, PDType1Font.HELVETICA_BOLD, 11);
        textY -= 18;
        writeSummaryRow(cs, "TVA (" + formatRate(tvaRate) + ")", formatAmount(totalTva), blockX + 15, textY,
            PDType1Font.HELVETICA, 11, PDType1Font.HELVETICA_BOLD, 11);
        textY -= 18;
        writeSummaryRow(cs, "Total TTC", formatAmount(totalTtc), blockX + 15, textY,
            PDType1Font.HELVETICA_BOLD, 12, PDType1Font.HELVETICA_BOLD, 12);

        return startY - blockHeight;
    }

    private void drawFooter(PDPageContentStream cs, Devis devis, float width, float startY) throws IOException {
        Client client = devis.getClient();
        Integer terms = client != null ? client.getPaymentTermsDays() : null;
        String paymentTerms = terms != null && terms > 0
                ? "Conditions de paiement : règlement à " + terms + " jours."
                : "Conditions de paiement : règlement à réception.";

        cs.setFont(PDType1Font.HELVETICA, 10);
        writeText(cs, paymentTerms, MARGIN, startY);
        writeText(cs, "Notes : Merci pour votre confiance. N'hésitez pas à nous contacter pour toute question.", MARGIN, startY - 15);

        cs.moveTo(MARGIN, startY - 45);
        cs.lineTo(MARGIN + width / 2.5f, startY - 45);
        cs.stroke();
        writeText(cs, "Signature", MARGIN, startY - 60);
    }

    private void drawLogo(PDDocument doc, PDPageContentStream cs, Entreprise entreprise, float x, float y,
                           float maxWidth, float maxHeight) {
        if (entreprise == null) {
            return;
        }
        String logo = entreprise.getLogo();
        if (logo == null || logo.isBlank()) {
            return;
        }
        try {
            Path logoPath = Paths.get(logo);
            if (!Files.exists(logoPath)) {
                return;
            }
            PDImageXObject image = PDImageXObject.createFromFile(logoPath.toAbsolutePath().toString(), doc);
            float scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight());
            float imgWidth = image.getWidth() * scale;
            float imgHeight = image.getHeight() * scale;
            cs.drawImage(image, x - imgWidth, y - imgHeight, imgWidth, imgHeight);
        } catch (Exception ignored) {
            // Ignore invalid paths or IO errors to keep PDF generation resilient.
        }
    }

    private void drawRowLines(PDPageContentStream cs, float width, float rowTop) throws IOException {
        cs.moveTo(MARGIN, rowTop);
        cs.lineTo(MARGIN + width, rowTop);
        cs.stroke();
    }

    private void writeTableCell(PDPageContentStream cs, String text, float x, float y) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private void writeAlignedCell(PDPageContentStream cs, String text, float x, float y, boolean alignRight) throws IOException {
        writeAlignedCell(cs, text, x, y, alignRight, PDType1Font.HELVETICA, 11);
    }

    private void writeAlignedCell(PDPageContentStream cs, String text, float x, float y, boolean alignRight,
                                   PDType1Font font, float fontSize) throws IOException {
        if (!alignRight) {
            writeTableCell(cs, text, x, y);
            return;
        }
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        cs.beginText();
        cs.newLineAtOffset(x - textWidth, y);
        cs.showText(text);
        cs.endText();
    }

    private void writeSummaryRow(PDPageContentStream cs, String label, String value, float x, float y,
                                  PDType1Font labelFont, float labelSize,
                                  PDType1Font valueFont, float valueSize) throws IOException {
        cs.setFont(labelFont, labelSize);
        writeText(cs, label, x, y);
        float textWidth = valueFont.getStringWidth(value) / 1000 * valueSize;
        cs.setFont(valueFont, valueSize);
        writeText(cs, value, x + 170 - textWidth, y);
    }

    private void writeText(PDPageContentStream cs, String text, float x, float y) throws IOException {
        if (text == null || text.isBlank()) {
            return;
        }
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private String companyName(Entreprise entreprise) {
        if (entreprise == null || entreprise.getNom() == null || entreprise.getNom().isBlank()) {
            return "Votre entreprise";
        }
        return entreprise.getNom();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String documentNumber(Devis devis) {
        if (devis.getNumeroDevis() != null && !devis.getNumeroDevis().isBlank()) {
            return devis.getNumeroDevis();
        }
        if (devis.getReference() != null && !devis.getReference().isBlank()) {
            return devis.getReference();
        }
        return String.valueOf(devis.getId() != null ? devis.getId() : "");
    }

    private String documentDate(Devis devis) {
        if (devis.getDevisDate() == null) {
            return "";
        }
        return DATE_FORMAT.format(devis.getDevisDate());
    }

    private String formatAmount(double value) {
        return String.format(LOCALE, "%,.2f €", value);
    }

    private String formatQty(double value) {
        return String.format(LOCALE, "%,.0f", value);
    }

    private String formatRate(double rate) {
        return String.format(LOCALE, "%,.2f%%", rate * 100d);
    }

    private double safeNumber(Number value) {
        return value == null ? 0d : value.doubleValue();
    }

    private double computeTotalHt(List<LigneDevis> lignes) {
        return sanitizeLines(lignes).stream()
                .mapToDouble(l -> {
                    double qty = safeNumber(l.getQuantite());
                    double unit = safeNumber(l.getPrixUnitaire());
                    return l.getTotal() != null ? l.getTotal() : qty * unit;
                })
                .sum();
    }

    private List<LigneDevis> sanitizeLines(List<LigneDevis> lignes) {
        if (lignes == null) {
            return Collections.emptyList();
        }
        List<LigneDevis> filtered = lignes.stream()
                .filter(Objects::nonNull)
                .toList();
        if (filtered.size() != lignes.size()) {
            log.warn("Ignoring {} null devis lines while generating PDF", lignes.size() - filtered.size());
        }
        return filtered;
    }
}
