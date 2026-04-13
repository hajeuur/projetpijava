package edu.connection3a36.tools;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import edu.connection3a36.entities.PlanActions;
import edu.connection3a36.entities.ReferenceArticle;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
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

                Paragraph title = new Paragraph("Liste des Plans d'Actions")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBold()
                        .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(16, 44, 89)) // #102c59
                        .setFontSize(22);
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
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(p.getId()))).setTextAlignment(TextAlignment.CENTER));
                    table.addCell(p.getDecision() != null ? p.getDecision() : "");
                    table.addCell(new Cell().add(new Paragraph(p.getStatut() != null ? p.getStatut().getLabel() : "")).setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(p.getCategorie() != null ? p.getCategorie().getLabel() : "")).setTextAlignment(TextAlignment.CENTER));
                    table.addCell(new Cell().add(new Paragraph(p.getDate() != null ? p.getDate().format(DATE_FMT) : "")).setTextAlignment(TextAlignment.CENTER));
                }

                document.add(table);
                document.add(new Paragraph("\nGénéré par MentorAI").setItalic().setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY));
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

                Paragraph title = new Paragraph("Article : " + article.getTitre())
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBold()
                        .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(16, 44, 89)) // #102c59
                        .setFontSize(20);
                document.add(title);

                document.add(new Paragraph("\n"));
                document.add(new Paragraph("Statut : " + (article.isPublished() ? "Publié" : "Brouillon")).setItalic());
                document.add(new Paragraph("Catégorie : " + (article.getCategorieNom() != null ? article.getCategorieNom() : "")).setItalic());
                document.add(new Paragraph("Créé le : " + (article.getCreatedAt() != null ? article.getCreatedAt().format(DATE_FMT) : "")).setItalic());
                
                document.add(new Paragraph("\nContenu de l'article :\n").setBold().setFontSize(14).setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(157, 187, 206))); // #9dbbce
                document.add(new Paragraph(article.getContenu() != null ? article.getContenu() : "Aucun contenu."));
                
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
