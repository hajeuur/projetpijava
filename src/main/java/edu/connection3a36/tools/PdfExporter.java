package edu.connection3a36.tools;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import edu.connection3a36.entities.PlanActions;
import edu.connection3a36.entities.ReferenceArticle;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdfExporter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static void exportPlanActionsList(Window window, List<PlanActions> list) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sauvegarder la liste des Plans d'Actions en PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("Liste_Plans_Actions.pdf");
        File file = fileChooser.showSaveDialog(window);

        if (file != null) {
            try {
                PdfWriter writer = new PdfWriter(file.getAbsolutePath());
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);

                // Add Logo and Header Metadata
                Table headerInfo = new Table(2).useAllAvailableWidth();
                headerInfo.setBorder(Border.NO_BORDER);

                // Logo column
                try {
                    URL logoUrl = PdfExporter.class.getResource("/images/123.png");
                    if (logoUrl != null) {
                        Image logo = new Image(ImageDataFactory.create(logoUrl));
                        logo.setWidth(80);
                        headerInfo.addCell(new Cell().add(logo).setBorder(Border.NO_BORDER));
                    } else {
                        headerInfo.addCell(new Cell().add(new Paragraph("MentorAI")).setBorder(Border.NO_BORDER));
                    }
                } catch (Exception ignored) {
                    headerInfo.addCell(new Cell().add(new Paragraph("MentorAI")).setBorder(Border.NO_BORDER));
                }

                // Metadata column
                String user = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getNom() : "Admin";
                Paragraph meta = new Paragraph("MentorAI — Plateforme de Gestion Pédagogique\nExporté le : " + LocalDateTime.now().format(DATE_FMT) + "\nPar : " + user)
                        .setFontSize(9)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY);
                headerInfo.addCell(new Cell().add(meta).setBorder(Border.NO_BORDER));

                document.add(headerInfo);
                document.add(new LineSeparator(new SolidLine()));
                document.add(new Paragraph("\n"));

                Paragraph title = new Paragraph("LISTE DES PLANS D'ACTIONS")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBold()
                        .setFontColor(new DeviceRgb(16, 44, 89))
                        .setFontSize(20);
                document.add(title);
                document.add(new Paragraph("\n"));

                Table table = new Table(new float[]{1, 3, 2, 2, 2});
                table.useAllAvailableWidth();

                com.itextpdf.kernel.colors.Color primaryColor = new com.itextpdf.kernel.colors.DeviceRgb(16, 44, 89); // #102c59
                com.itextpdf.kernel.colors.Color whiteColor = com.itextpdf.kernel.colors.ColorConstants.WHITE;

                // Headers
                table.addHeaderCell(new Cell().add(new Paragraph("ID").setBold().setFontColor(whiteColor)).setBackgroundColor(primaryColor).setTextAlignment(TextAlignment.CENTER));
                table.addHeaderCell(new Cell().add(new Paragraph("Décision").setBold().setFontColor(whiteColor)).setBackgroundColor(primaryColor));
                table.addHeaderCell(new Cell().add(new Paragraph("Statut").setBold().setFontColor(whiteColor)).setBackgroundColor(primaryColor).setTextAlignment(TextAlignment.CENTER));
                table.addHeaderCell(new Cell().add(new Paragraph("Catégorie").setBold().setFontColor(whiteColor)).setBackgroundColor(primaryColor).setTextAlignment(TextAlignment.CENTER));
                table.addHeaderCell(new Cell().add(new Paragraph("Date").setBold().setFontColor(whiteColor)).setBackgroundColor(primaryColor).setTextAlignment(TextAlignment.CENTER));

                for (PlanActions p : list) {
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(p.getId()))).setTextAlignment(TextAlignment.CENTER).setFontSize(10));
                    table.addCell(new Cell().add(new Paragraph(p.getDecision() != null ? p.getDecision() : "")).setFontSize(10));
                    table.addCell(new Cell().add(new Paragraph(p.getStatut() != null ? p.getStatut().getLabel() : "")).setTextAlignment(TextAlignment.CENTER).setFontSize(10));
                    table.addCell(new Cell().add(new Paragraph(p.getCategorie() != null ? p.getCategorie().getLabel() : "")).setTextAlignment(TextAlignment.CENTER).setFontSize(10));
                    table.addCell(new Cell().add(new Paragraph(p.getDate() != null ? p.getDate().format(DATE_FMT) : "")).setTextAlignment(TextAlignment.CENTER).setFontSize(10));
                }

                document.add(table);
                document.add(new Paragraph("\n© MentorAI — Système de gestion académique")
                        .setItalic().setFontSize(9).setTextAlignment(TextAlignment.CENTER).setFontColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));
                document.close();
                AlertUtil.showSuccess("Export des plans d'actions réussi ! Fichier sauvegardé :\n" + file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                AlertUtil.showError("Erreur lors de l'export PDF : " + e.getMessage());
            }
        }
    }

    public static void exportSingleArticle(Window window, ReferenceArticle article) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sauvegarder l'article en PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("Article_" + article.getId() + ".pdf");
        File file = fileChooser.showSaveDialog(window);

        if (file != null) {
            try {
                PdfWriter writer = new PdfWriter(file.getAbsolutePath());
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);

                // Header with Logo
                Table headerInfo = new Table(2).useAllAvailableWidth().setBorder(Border.NO_BORDER);
                try {
                    URL logoUrl = PdfExporter.class.getResource("/images/123.png");
                    if (logoUrl != null) {
                        Image logo = new Image(ImageDataFactory.create(logoUrl)).setWidth(80);
                        headerInfo.addCell(new Cell().add(logo).setBorder(Border.NO_BORDER));
                    } else { headerInfo.addCell(new Cell().add(new Paragraph("MentorAI")).setBorder(Border.NO_BORDER)); }
                } catch (Exception e) { headerInfo.addCell(new Cell().add(new Paragraph("MentorAI")).setBorder(Border.NO_BORDER)); }

                String user = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getNom() : "Admin";
                headerInfo.addCell(new Cell().add(new Paragraph("MentorAI — Plateforme de Gestion Pédagogique\nRapport Officiel\nExporté le : " + LocalDateTime.now().format(DATE_FMT) + "\nPar : " + user)
                        .setFontSize(9).setTextAlignment(TextAlignment.RIGHT).setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY))
                        .setBorder(Border.NO_BORDER));
                document.add(headerInfo);
                document.add(new LineSeparator(new SolidLine()));

                Paragraph title = new Paragraph(article.getTitre().toUpperCase())
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBold()
                        .setFontColor(new DeviceRgb(16, 44, 89))
                        .setMarginTop(20)
                        .setFontSize(18);
                document.add(title);

                Table infoTable = new Table(3).useAllAvailableWidth().setMarginTop(10);
                infoTable.addCell(new Cell().add(new Paragraph("Statut: " + (article.isPublished() ? "Publié" : "Brouillon"))).setBorder(Border.NO_BORDER).setFontSize(10));
                infoTable.addCell(new Cell().add(new Paragraph("Catégorie: " + (article.getCategorieNom() != null ? article.getCategorieNom() : ""))).setBorder(Border.NO_BORDER).setFontSize(10));
                infoTable.addCell(new Cell().add(new Paragraph("Créé le: " + (article.getCreatedAt() != null ? article.getCreatedAt().format(DATE_FMT) : ""))).setBorder(Border.NO_BORDER).setFontSize(10));
                document.add(infoTable);
                
                document.add(new Paragraph("\nCONTENU DE L'ARTICLE\n")
                        .setBold().setFontSize(12).setUnderline().setFontColor(new DeviceRgb(213, 46, 40))); // #d52e28
                document.add(new Paragraph(article.getContenu() != null ? article.getContenu() : "Aucun contenu.")
                        .setFontSize(11).setTextAlignment(TextAlignment.JUSTIFIED).setMultipliedLeading(1.5f));
                
                document.add(new Paragraph("\nDocument généré par MentorAI").setItalic().setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY));

                document.close();
                AlertUtil.showSuccess("Article exporté en PDF avec succès !Fichier :\n" + file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                AlertUtil.showError("Erreur lors de l'export PDF : " + e.getMessage());
            }
        }
    }
}
