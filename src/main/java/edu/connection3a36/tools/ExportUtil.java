package edu.connection3a36.tools;

import edu.connection3a36.entities.Etat;
import edu.connection3a36.entities.Objectif;
import edu.connection3a36.entities.Programme;
import edu.connection3a36.entities.Tache;
import edu.connection3a36.services.ScoreService;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class ExportUtil {

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORT EXCEL (.xlsx)
    // ─────────────────────────────────────────────────────────────────────────

    public static void exporterExcel(String filePath, Objectif objectif,
                                     Programme programme, List<Tache> taches) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Objectif");

            // ── Styles ────────────────────────────────────────────────────────
            XSSFCellStyle styleTitle = wb.createCellStyle();
            XSSFFont fontTitle = wb.createFont();
            fontTitle.setBold(true);
            fontTitle.setFontHeightInPoints((short) 16);
            fontTitle.setColor(new XSSFColor(new byte[]{(byte)16, (byte)44, (byte)89}, null));
            styleTitle.setFont(fontTitle);
            styleTitle.setFillForegroundColor(new XSSFColor(new byte[]{(byte)240, (byte)244, (byte)248}, null));
            styleTitle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCellStyle styleHeader = wb.createCellStyle();
            XSSFFont fontHeader = wb.createFont();
            fontHeader.setBold(true);
            fontHeader.setColor(IndexedColors.WHITE.getIndex());
            styleHeader.setFont(fontHeader);
            styleHeader.setFillForegroundColor(new XSSFColor(new byte[]{(byte)16, (byte)44, (byte)89}, null));
            styleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleHeader.setAlignment(HorizontalAlignment.CENTER);
            styleHeader.setBorderBottom(BorderStyle.THIN);

            XSSFCellStyle styleInfo = wb.createCellStyle();
            XSSFFont fontInfo = wb.createFont();
            fontInfo.setBold(true);
            styleInfo.setFont(fontInfo);
            styleInfo.setFillForegroundColor(new XSSFColor(new byte[]{(byte)248, (byte)250, (byte)252}, null));
            styleInfo.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCellStyle styleGreen = wb.createCellStyle();
            styleGreen.setFillForegroundColor(new XSSFColor(new byte[]{(byte)240, (byte)253, (byte)244}, null));
            styleGreen.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCellStyle styleYellow = wb.createCellStyle();
            styleYellow.setFillForegroundColor(new XSSFColor(new byte[]{(byte)255, (byte)251, (byte)235}, null));
            styleYellow.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCellStyle styleRed = wb.createCellStyle();
            styleRed.setFillForegroundColor(new XSSFColor(new byte[]{(byte)255, (byte)245, (byte)245}, null));
            styleRed.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // ── Titre ─────────────────────────────────────────────────────────
            int row = 0;
            Row r0 = sheet.createRow(row++);
            r0.setHeightInPoints(30);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("RAPPORT MENTORAI — " + objectif.getTitre());
            c0.setCellStyle(styleTitle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

            // ── Infos objectif ────────────────────────────────────────────────
            row++;
            creerLigneInfo(sheet, row++, styleInfo, "Objectif", objectif.getTitre());
            creerLigneInfo(sheet, row++, styleInfo, "Description", nvl(objectif.getDescription()));
            creerLigneInfo(sheet, row++, styleInfo, "Date debut", nvl(objectif.getDatedebut()));
            creerLigneInfo(sheet, row++, styleInfo, "Deadline", nvl(objectif.getDatefin()));
            creerLigneInfo(sheet, row++, styleInfo, "Statut", objectif.getStatut() != null ? objectif.getStatut().getValue() : "—");
            creerLigneInfo(sheet, row++, styleInfo, "Score", programme.getScorePourcentage() + "%");
            creerLigneInfo(sheet, row++, styleInfo, "Medaille", ScoreService.emojiMedaille(programme.getMeilleureMedaille()));
            creerLigneInfo(sheet, row++, styleInfo, "Date export", LocalDate.now().toString());

            // ── En-têtes tâches ───────────────────────────────────────────────
            row++;
            Row headerRow = sheet.createRow(row++);
            headerRow.setHeightInPoints(22);
            String[] cols = {"#", "Titre", "Description", "Etat", "Ordre"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(styleHeader);
            }

            // ── Données tâches ────────────────────────────────────────────────
            for (Tache t : taches) {
                Row dataRow = sheet.createRow(row++);
                dataRow.setHeightInPoints(18);

                XSSFCellStyle rowStyle = t.getEtat() == Etat.realisee ? styleGreen
                        : t.getEtat() == Etat.Abandonner ? styleRed : styleYellow;

                Cell cn = dataRow.createCell(0); cn.setCellValue(t.getOrdre()); cn.setCellStyle(rowStyle);
                Cell ct = dataRow.createCell(1); ct.setCellValue(t.getTitre()); ct.setCellStyle(rowStyle);
                Cell cd = dataRow.createCell(2); cd.setCellValue(t.getDescription() != null ? t.getDescription() : ""); cd.setCellStyle(rowStyle);
                Cell ce = dataRow.createCell(3); ce.setCellValue(t.getEtat() != null ? t.getEtat().getValue() : ""); ce.setCellStyle(rowStyle);
                Cell co = dataRow.createCell(4); co.setCellValue(t.getOrdre()); co.setCellStyle(rowStyle);
            }

            // ── Largeurs colonnes ─────────────────────────────────────────────
            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 35 * 256);
            sheet.setColumnWidth(2, 50 * 256);
            sheet.setColumnWidth(3, 15 * 256);
            sheet.setColumnWidth(4, 10 * 256);

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                wb.write(fos);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORT WORD (.docx)
    // ─────────────────────────────────────────────────────────────────────────

    public static void exporterWord(String filePath, Objectif objectif,
                                    Programme programme, List<Tache> taches) throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {

            // ── Titre ─────────────────────────────────────────────────────────
            XWPFParagraph titre = doc.createParagraph();
            titre.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun runTitre = titre.createRun();
            runTitre.setText("RAPPORT MENTORAI");
            runTitre.setBold(true);
            runTitre.setFontSize(20);
            runTitre.setColor("102c59");
            runTitre.addBreak();

            XWPFParagraph sousTitre = doc.createParagraph();
            sousTitre.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun runSous = sousTitre.createRun();
            runSous.setText(objectif.getTitre());
            runSous.setBold(true);
            runSous.setFontSize(14);
            runSous.setColor("3b82f6");
            runSous.addBreak();

            // ── Séparateur ────────────────────────────────────────────────────
            ajouterSeparateur(doc);

            // ── Infos objectif ────────────────────────────────────────────────
            ajouterTitreSection(doc, "INFORMATIONS DE L'OBJECTIF");
            ajouterInfo(doc, "Titre", objectif.getTitre());
            ajouterInfo(doc, "Description", nvl(objectif.getDescription()));
            ajouterInfo(doc, "Date de debut", nvl(objectif.getDatedebut()));
            ajouterInfo(doc, "Deadline", nvl(objectif.getDatefin()));
            ajouterInfo(doc, "Statut", objectif.getStatut() != null ? objectif.getStatut().getValue() : "—");
            ajouterInfo(doc, "Score de progression", programme.getScorePourcentage() + "%");
            ajouterInfo(doc, "Medaille obtenue", ScoreService.emojiMedaille(programme.getMeilleureMedaille()));
            ajouterInfo(doc, "Date d'export", LocalDate.now().toString());

            doc.createParagraph(); // espace

            // ── Tableau des tâches ────────────────────────────────────────────
            ajouterTitreSection(doc, "TACHES DU PROGRAMME (" + taches.size() + ")");

            XWPFTable table = doc.createTable(taches.size() + 1, 4);
            table.setWidth("100%");

            // En-têtes
            String[] headers = {"#", "Titre de la tache", "Description", "Etat"};
            XWPFTableRow headerRow = table.getRow(0);
            for (int i = 0; i < headers.length; i++) {
                XWPFTableCell cell = headerRow.getCell(i);
                cell.setColor("102c59");
                XWPFParagraph p = cell.getParagraphs().get(0);
                XWPFRun run = p.createRun();
                run.setText(headers[i]);
                run.setBold(true);
                run.setColor("FFFFFF");
                run.setFontSize(10);
            }

            // Données
            for (int i = 0; i < taches.size(); i++) {
                Tache t = taches.get(i);
                XWPFTableRow dataRow = table.getRow(i + 1);

                String couleur = t.getEtat() == Etat.realisee ? "f0fdf4"
                        : t.getEtat() == Etat.Abandonner ? "fff5f5" : "fffbeb";

                String[] vals = {
                        String.valueOf(t.getOrdre()),
                        t.getTitre(),
                        t.getDescription() != null ? t.getDescription() : "",
                        t.getEtat() != null ? t.getEtat().getValue() : ""
                };
                for (int j = 0; j < vals.length; j++) {
                    XWPFTableCell cell = dataRow.getCell(j);
                    cell.setColor(couleur);
                    XWPFParagraph p = cell.getParagraphs().get(0);
                    XWPFRun run = p.createRun();
                    run.setText(vals[j]);
                    run.setFontSize(9);
                    if (j == 3) { // colonne état en couleur
                        String etatColor = t.getEtat() == Etat.realisee ? "198754"
                                : t.getEtat() == Etat.Abandonner ? "d52e28" : "d97706";
                        run.setColor(etatColor);
                        run.setBold(true);
                    }
                }
            }

            // ── Pied de page ──────────────────────────────────────────────────
            doc.createParagraph();
            ajouterSeparateur(doc);
            XWPFParagraph footer = doc.createParagraph();
            footer.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun runFooter = footer.createRun();
            runFooter.setText("Genere par MentorAI — " + LocalDate.now());
            runFooter.setColor("9dbbce");
            runFooter.setFontSize(9);

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                doc.write(fos);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private static void creerLigneInfo(org.apache.poi.ss.usermodel.Sheet sheet,
                                       int rowNum, CellStyle style, String label, String value) {
        Row row = sheet.createRow(rowNum);
        Cell c1 = row.createCell(0); c1.setCellValue(label); c1.setCellStyle(style);
        Cell c2 = row.createCell(1); c2.setCellValue(value);
    }

    private static void ajouterTitreSection(XWPFDocument doc, String texte) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(200);
        XWPFRun run = p.createRun();
        run.setText(texte);
        run.setBold(true);
        run.setFontSize(12);
        run.setColor("102c59");
        run.addBreak();
    }

    private static void ajouterInfo(XWPFDocument doc, String label, String value) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(60);
        XWPFRun runLabel = p.createRun();
        runLabel.setText(label + " : ");
        runLabel.setBold(true);
        runLabel.setFontSize(10);
        runLabel.setColor("102c59");
        XWPFRun runValue = p.createRun();
        runValue.setText(value);
        runValue.setFontSize(10);
    }

    private static void ajouterSeparateur(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        p.setBorderBottom(Borders.SINGLE);
        p.setSpacingAfter(100);
    }

    private static String nvl(Object o) { return o != null ? o.toString() : "—"; }
}
