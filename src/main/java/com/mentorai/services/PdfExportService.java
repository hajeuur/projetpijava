package com.mentorai.services;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.mentorai.models.Feedback;
import com.mentorai.models.Traitement;

import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.List;

public class PdfExportService {

    private TraitementService traitementService = new TraitementService();

    private static final DeviceRgb BLEU_HEADER = new DeviceRgb(16, 44, 89);
    private static final DeviceRgb VERT        = new DeviceRgb(40, 167, 69);
    private static final DeviceRgb ORANGE      = new DeviceRgb(240, 165, 0);
    private static final DeviceRgb ROUGE       = new DeviceRgb(213, 46, 40);
    private static final DeviceRgb TEAL        = new DeviceRgb(157, 187, 206);
    private static final DeviceRgb GRIS_CLAIR  = new DeviceRgb(245, 247, 250);

    public void exporterFeedbacks(List<Feedback> feedbacks, String cheminFichier) {
        try {
            PdfWriter writer   = new PdfWriter(new FileOutputStream(cheminFichier));
            PdfDocument pdf    = new PdfDocument(writer);
            Document document  = new Document(pdf, PageSize.A4.rotate());
            document.setMargins(20, 20, 20, 20);

            // ===== TITRE =====
            document.add(new Paragraph("MentorAI — Rapport des Feedbacks")
                    .setFontSize(18).setBold()
                    .setFontColor(BLEU_HEADER)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("Généré le : " + LocalDate.now())
                    .setFontSize(10)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(15));

            // ===== TABLEAU =====
            float[] colonnes = {40f, 80f, 80f, 45f, 200f, 70f, 100f};
            Table table = new Table(UnitValue.createPercentArray(colonnes));
            table.setWidth(UnitValue.createPercentValue(100));

            // En-têtes
            String[] headers = {"#", "Type", "Date", "Note", "Message", "Etat", "Traitement"};
            for (String h : headers) {
                table.addHeaderCell(
                        new Cell()
                                .add(new Paragraph(h).setBold()
                                        .setFontColor(ColorConstants.WHITE).setFontSize(11))
                                .setBackgroundColor(BLEU_HEADER)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setPadding(7)
                );
            }

            // Lignes
            boolean pair = false;
            for (Feedback f : feedbacks) {
                DeviceRgb bg = pair ? GRIS_CLAIR : new DeviceRgb(255, 255, 255);

                // #
                table.addCell(cellule(String.valueOf(f.getId()), bg, TextAlignment.CENTER, 10));

                // Type coloré
                DeviceRgb typeColor = switch (f.getTypefeedback()) {
                    case "probleme"     -> ROUGE;
                    case "satisfaction" -> VERT;
                    default             -> TEAL;
                };
                table.addCell(new Cell()
                        .add(new Paragraph(f.getTypefeedback())
                                .setBold().setFontColor(ColorConstants.WHITE).setFontSize(9))
                        .setBackgroundColor(typeColor)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(6));

                // Date
                table.addCell(cellule(f.getDatefeedback().toString(), bg, TextAlignment.CENTER, 10));

                // Note
                table.addCell(cellule(f.getNote() + "/5", bg, TextAlignment.CENTER, 10));

                // Message tronqué
                String contenu = f.getContenu().length() > 80
                        ? f.getContenu().substring(0, 80) + "..."
                        : f.getContenu();
                table.addCell(cellule(contenu, bg, TextAlignment.LEFT, 9));

                // État coloré
                boolean traite = f.getEtatfeedback().equals("traite");
                table.addCell(new Cell()
                        .add(new Paragraph(traite ? "Traité" : "En attente")
                                .setBold().setFontColor(ColorConstants.WHITE).setFontSize(9))
                        .setBackgroundColor(traite ? VERT : ORANGE)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(6));

                // Traitement
                String traitementText = "-";
                if (f.getTraitementId() != 0) {
                    Traitement t = traitementService.getById(f.getTraitementId());
                    if (t != null) traitementText = t.getTypetraitement();
                }
                table.addCell(cellule(traitementText, bg, TextAlignment.CENTER, 9));

                pair = !pair;
            }

            document.add(table);

            // ===== PIED DE PAGE =====
            document.add(new Paragraph("\nTotal : " + feedbacks.size() + " feedback(s)")
                    .setFontSize(10).setItalic()
                    .setFontColor(ColorConstants.GRAY)
                    .setMarginTop(10));

            document.close();
            System.out.println("✅ PDF exporté : " + cheminFichier);

        } catch (Exception e) {
            System.out.println("❌ Erreur export PDF : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Cell cellule(String texte, DeviceRgb bg, TextAlignment align, float fontSize) {
        return new Cell()
                .add(new Paragraph(texte).setFontSize(fontSize))
                .setBackgroundColor(bg)
                .setTextAlignment(align)
                .setPadding(6);
    }
}