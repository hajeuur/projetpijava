package edu.connection3a36.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import edu.connection3a36.entities.Parcours;
import edu.connection3a36.entities.Projet;
import edu.connection3a36.entities.Utilisateur;
import edu.connection3a36.tools.SessionManager;
import javafx.scene.control.Alert;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class CVService {

    public static void genererEtOuvrirCV(List<Parcours> parcoursList, List<Projet> projetList) {
        try {
            Utilisateur user = SessionManager.getCurrentUser();
            String nameRaw = (user != null ? user.getEmail().split("@")[0] : "Mon Nom");
            String fileName = "CV_" + nameRaw + ".pdf";
            
            // Marges réduites pour tout faire tenir sur une page
            Document document = new Document(PageSize.A4, 40, 40, 30, 30);
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();

            // --- FONTS (Légèrement réduites pour l'espace) ---
            Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.BLACK);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.BLACK);
            Font italicFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, BaseColor.DARK_GRAY);

            // --- HEADER (Compact) ---
            Paragraph pName = new Paragraph(nameRaw.replace(".", " ").toUpperCase(), nameFont);
            pName.setAlignment(Element.ALIGN_CENTER);
            pName.setSpacingAfter(2);
            document.add(pName);

            Paragraph pSub = new Paragraph("Étudiant en Business Intelligence / Développeur Fullstack", subTitleFont);
            pSub.setAlignment(Element.ALIGN_CENTER);
            pSub.setSpacingAfter(2);
            document.add(pSub);

            Paragraph pContact = new Paragraph("Tunis • " + (user != null ? user.getEmail() : "") + " • LinkedIn • GitHub", normalFont);
            pContact.setAlignment(Element.ALIGN_CENTER);
            pContact.setSpacingAfter(10);
            document.add(pContact);
            
            // --- RÉSUMÉ PROFESSIONNEL ---
            addSectionTitle(document, "RÉSUMÉ PROFESSIONNEL", sectionFont);
            Paragraph resume = new Paragraph("Étudiant passionné par l'IA et le développement logiciel, maîtrisant Java, SQL et les technologies modernes. Orienté solutions, je cherche à mettre en pratique mes compétences analytiques et techniques au sein de projets innovants et stimulants.", normalFont);
            resume.setAlignment(Element.ALIGN_JUSTIFIED);
            resume.setSpacingAfter(8);
            document.add(resume);

            // --- EXPÉRIENCE PROFESSIONNELLE ---
            addSectionTitle(document, "EXPÉRIENCE PROFESSIONNELLE", sectionFont);
            for (Parcours p : parcoursList) {
                if ("Stage".equalsIgnoreCase(p.getTypeParcours()) || "Emploi".equalsIgnoreCase(p.getTypeParcours())) {
                    addListHeader(document, p.getTitre() + " - " + p.getEtablissement(), 
                                 p.getDateDebut() != null ? p.getDateDebut().toString() : "", boldFont, italicFont);
                    if (p.getDescription() != null && !p.getDescription().isEmpty()) {
                        Paragraph desc = new Paragraph("• " + p.getDescription(), normalFont);
                        desc.setIndentationLeft(10);
                        document.add(desc);
                    }
                    document.add(new Paragraph(2)); // Petit espacement
                }
            }
            document.add(new Paragraph(5));

            // --- FORMATION ---
            addSectionTitle(document, "FORMATION", sectionFont);
            for (Parcours p : parcoursList) {
                if ("Formation".equalsIgnoreCase(p.getTypeParcours())) {
                    addListHeader(document, (p.getDiplome() != null ? p.getDiplome() : p.getTitre()) + " - " + p.getEtablissement(), 
                                 (p.getDateDebut() != null ? p.getDateDebut().getYear() : "") + " - " + (p.getDateFin() != null ? p.getDateFin().getYear() : "En cours"), 
                                 boldFont, italicFont);
                    if (p.getSpecialite() != null) document.add(new Paragraph("  Spécialité : " + p.getSpecialite(), normalFont));
                }
            }
            document.add(new Paragraph(8));

            // --- COMPÉTENCES TECHNIQUES ---
            addSectionTitle(document, "COMPÉTENCES TECHNIQUES", sectionFont);
            document.add(new Paragraph("• Langages : Java, Python, SQL, C#, JavaScript, PHP", normalFont));
            document.add(new Paragraph("• Outils : SpringBoot, JavaFX, Git, Docker, MySQL, Sage", normalFont));
            document.add(new Paragraph("• Soft Skills : Communication, Travail d'équipe, Résolution de problèmes", normalFont));
            document.add(new Paragraph(8));

            // --- PROJETS ---
            addSectionTitle(document, "PROJETS", sectionFont);
            int count = 0;
            for (Projet pr : projetList) {
                if (count++ >= 3) break; // Limiter aux 3 projets les plus importants pour l'espace
                Paragraph prName = new Paragraph("• " + pr.getTitre() + " : " + pr.getDescription(), normalFont);
                document.add(prName);
                if (pr.getTechnologies() != null) {
                    Paragraph prTech = new Paragraph("  Stack : " + pr.getTechnologies(), italicFont);
                    prTech.setIndentationLeft(10);
                    document.add(prTech);
                }
            }

            document.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("CV 1 Page");
            alert.setContentText("Votre CV a été optimisé pour tenir sur une seule page.");
            alert.show();

            java.awt.Desktop.getDesktop().open(new File(fileName));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addSectionTitle(Document doc, String title, Font font) throws DocumentException {
        Paragraph p = new Paragraph(title, font);
        p.setSpacingBefore(5);
        doc.add(p);
        LineSeparator ls = new LineSeparator();
        ls.setLineWidth(0.5f);
        doc.add(new Chunk(ls));
        doc.add(new Paragraph(2));
    }

    private static void addListHeader(Document doc, String left, String right, Font leftFont, Font rightFont) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(2);
        PdfPCell cellLeft = new PdfPCell(new Phrase(left, leftFont));
        cellLeft.setBorder(Rectangle.NO_BORDER);
        PdfPCell cellRight = new PdfPCell(new Phrase(right, rightFont));
        cellRight.setBorder(Rectangle.NO_BORDER);
        cellRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cellLeft);
        table.addCell(cellRight);
        doc.add(table);
    }
}
