package com.mentorai.services;

import com.mentorai.models.Feedback;
import com.mentorai.models.Traitement;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.util.List;

public class ExcelExportService {

    private TraitementService traitementService = new TraitementService();

    public void exporterFeedbacks(List<Feedback> feedbacks, String cheminFichier) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Feedbacks");

            // ===== STYLES =====
            // Style header
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);

            // Style ligne paire
            CellStyle pairStyle = workbook.createCellStyle();
            pairStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            pairStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            pairStyle.setAlignment(HorizontalAlignment.CENTER);
            pairStyle.setBorderBottom(BorderStyle.THIN);
            pairStyle.setBorderRight(BorderStyle.THIN);

            // Style ligne impaire
            CellStyle impairStyle = workbook.createCellStyle();
            impairStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
            impairStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            impairStyle.setAlignment(HorizontalAlignment.CENTER);
            impairStyle.setBorderBottom(BorderStyle.THIN);
            impairStyle.setBorderRight(BorderStyle.THIN);

            // Style texte gauche
            CellStyle textePairStyle = workbook.createCellStyle();
            textePairStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            textePairStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            textePairStyle.setAlignment(HorizontalAlignment.LEFT);
            textePairStyle.setBorderBottom(BorderStyle.THIN);
            textePairStyle.setWrapText(true);

            CellStyle texteImpairStyle = workbook.createCellStyle();
            texteImpairStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
            texteImpairStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            texteImpairStyle.setAlignment(HorizontalAlignment.LEFT);
            texteImpairStyle.setBorderBottom(BorderStyle.THIN);
            texteImpairStyle.setWrapText(true);

            // ===== EN-TÊTE =====
            Row headerRow = sheet.createRow(0);
            String[] colonnes = {"#", "Type", "Date", "Note", "Message", "Etat", "Traitement", "Description traitement"};
            for (int i = 0; i < colonnes.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(colonnes[i]);
                cell.setCellStyle(headerStyle);
            }
            headerRow.setHeight((short) 500);

            // ===== DONNÉES =====
            int rowNum = 1;
            for (Feedback f : feedbacks) {
                Row row = sheet.createRow(rowNum);
                row.setHeight((short) 450);

                boolean pair = rowNum % 2 == 0;
                CellStyle cs  = pair ? pairStyle  : impairStyle;
                CellStyle cst = pair ? textePairStyle : texteImpairStyle;

                // #
                Cell c0 = row.createCell(0);
                c0.setCellValue(f.getId());
                c0.setCellStyle(cs);

                // Type
                Cell c1 = row.createCell(1);
                c1.setCellValue(f.getTypefeedback());
                c1.setCellStyle(cs);

                // Date
                Cell c2 = row.createCell(2);
                c2.setCellValue(f.getDatefeedback().toString());
                c2.setCellStyle(cs);

                // Note
                Cell c3 = row.createCell(3);
                c3.setCellValue(f.getNote() + "/5");
                c3.setCellStyle(cs);

                // Message
                Cell c4 = row.createCell(4);
                c4.setCellValue(f.getContenu());
                c4.setCellStyle(cst);

                // Etat
                Cell c5 = row.createCell(5);
                c5.setCellValue(f.getEtatfeedback().equals("traite") ? "Traite" : "En attente");
                c5.setCellStyle(cs);

                // Traitement type
                Cell c6 = row.createCell(6);
                Cell c7 = row.createCell(7);
                if (f.getTraitementId() != 0) {
                    Traitement t = traitementService.getById(f.getTraitementId());
                    if (t != null) {
                        c6.setCellValue(t.getTypetraitement());
                        c7.setCellValue(t.getDescription());
                    } else {
                        c6.setCellValue("-");
                        c7.setCellValue("-");
                    }
                } else {
                    c6.setCellValue("-");
                    c7.setCellValue("-");
                }
                c6.setCellStyle(cs);
                c7.setCellStyle(cst);

                rowNum++;
            }

            // ===== LARGEUR COLONNES =====
            sheet.setColumnWidth(0, 1500);
            sheet.setColumnWidth(1, 4000);
            sheet.setColumnWidth(2, 4000);
            sheet.setColumnWidth(3, 2000);
            sheet.setColumnWidth(4, 10000);
            sheet.setColumnWidth(5, 4000);
            sheet.setColumnWidth(6, 6000);
            sheet.setColumnWidth(7, 10000);

            // ===== SAUVEGARDER =====
            try (FileOutputStream fos = new FileOutputStream(cheminFichier)) {
                workbook.write(fos);
            }

            System.out.println("✅ Excel exporté : " + cheminFichier);

        } catch (Exception e) {
            System.out.println("❌ Erreur export Excel : " + e.getMessage());
            e.printStackTrace();
        }
    }
}