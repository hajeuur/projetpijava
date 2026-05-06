package com.mentorai.controllers;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.mentorai.services.ApprentissageService;
import com.mentorai.services.GeminiServiceCustom;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class AnalyseController {

    @FXML private VBox loadingBox, resultBox;
    @FXML private Label statusLabel, moodVal, moodLabel, perfVal, perfLabel, coachSummary;
    @FXML private Label step1, step2, step3, step4;
    @FXML private Button downloadBtn;

    private final ApprentissageService analysisService = new ApprentissageService();
    private final GeminiServiceCustom geminiService = new GeminiServiceCustom();
    private ApprentissageService.RapportData lastReportData;

    private int currentUserId = 1; // Default for context

    public void setUserId(int userId) {
        this.currentUserId = userId;
        startAnalysis();
    }

    @FXML
    public void initialize() {
        // Platform.runLater to ensure userId is set if called from ApprentissageController
        Platform.runLater(() -> {
            if (lastReportData == null) startAnalysis();
        });
    }

    private void startAnalysis() {
        Task<ApprentissageService.RapportData> analysisTask = new Task<>() {
            @Override
            protected ApprentissageService.RapportData call() throws Exception {
                updateMessage("Initialisation...");
                Thread.sleep(500); // Visual feedback

                updateProgress(1, 4);
                Platform.runLater(() -> step1.setText("● Lecture de l'historique d'études (OK)"));
                
                // 1. Core Logic (Java)
                ApprentissageService.RapportData data = analysisService.analyserPerformances(currentUserId);
                
                updateProgress(2, 4);
                Platform.runLater(() -> step2.setText("● Analyse de l'état mental (OK)"));
                Thread.sleep(500);

                updateProgress(3, 4);
                Platform.runLater(() -> step3.setText("● Identification des forces et faiblesses (OK)"));
                
                // 2. AI Enrichment
                updateMessage("Consultation de l'intelligence artificielle...");
                geminiService.genererInsightsRapport(data);
                
                updateProgress(4, 4);
                Platform.runLater(() -> step4.setText("● Génération des conseils IA (OK)"));
                Thread.sleep(500);

                return data;
            }
        };

        analysisTask.setOnSucceeded(e -> {
            this.lastReportData = analysisTask.getValue();
            showResults();
        });

        analysisTask.setOnFailed(e -> {
            statusLabel.setText("Erreur lors de l'analyse : " + analysisTask.getException().getMessage());
            analysisTask.getException().printStackTrace();
        });

        new Thread(analysisTask).start();
    }

    private void showResults() {
        loadingBox.setVisible(false);
        resultBox.setVisible(true);

        moodVal.setText(String.format("%.1f/5", lastReportData.current.avgMood));
        moodLabel.setText(lastReportData.current.mentalState);
        
        perfVal.setText(lastReportData.current.completionRate + "%");
        perfLabel.setText(lastReportData.current.behaviorProfile);
        
        coachSummary.setText(lastReportData.resumeGlobal);
    }

    @FXML
    private void handleDownload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport de coaching");
        fileChooser.setInitialFileName("MentorAI_Rapport_" + System.currentTimeMillis() + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        
        File file = fileChooser.showSaveDialog(downloadBtn.getScene().getWindow());
        if (file != null) {
            try {
                generatePDF(file.getAbsolutePath());
                statusLabel.setText("Rapport enregistré avec succès !");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void generatePDF(String dest) throws Exception {
        PdfWriter writer = new PdfWriter(new FileOutputStream(dest));
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(50, 50, 50, 50);

        DeviceRgb teal = new DeviceRgb(45, 212, 191);
        DeviceRgb darkBlue = new DeviceRgb(15, 23, 42);

        // PAGE 1 : ANALYSE & INSIGHTS
        document.add(new Paragraph("MENTORAI — RAPPORT D'EXCELLENCE").setFontColor(teal).setBold().setFontSize(10).setCharacterSpacing(1));
        document.add(new Paragraph("Votre Diagnostic de Performance").setFontSize(26).setBold().setFontColor(darkBlue).setMarginBottom(15));
        
        // 1. Résumé
        addSectionTitle(document, "1. RÉSUMÉ STRATÉGIQUE", teal);
        document.add(new Paragraph(lastReportData.resumeGlobal).setItalic().setMarginBottom(10));

        // 2. Comportement Réel
        addSectionTitle(document, "2. ANALYSE DU COMPORTEMENT RÉCENT", teal);
        document.add(new Paragraph(lastReportData.analyseComportement).setMarginBottom(10).setTextAlignment(TextAlignment.JUSTIFIED));

        // 3. État Mental
        addSectionTitle(document, "3. ÉTAT MENTAL & DISPONIBILITÉ", teal);
        document.add(new Paragraph(lastReportData.analyseMentale).setMarginBottom(10).setTextAlignment(TextAlignment.JUSTIFIED));

        // 4. Insights
        addSectionTitle(document, "4. OBSERVATIONS CLÉS", teal);
        com.itextpdf.layout.element.List list = new com.itextpdf.layout.element.List().setSymbolIndent(12).setListSymbol("—");
        for (String insight : lastReportData.insights) {
            list.add(insight);
        }
        document.add(list);

        // PAGE 2 : DIAGNOSTIC & RÉPONSE
        document.add(new AreaBreak());
        
        // 5. Matières
        addSectionTitle(document, "5. DIAGNOSTIC PAR MATIÈRE", teal);
        document.add(new Paragraph(lastReportData.diagnosticMatieres).setMarginBottom(10));

        // 6. Réponse du Profil (Output)
        addSectionTitle(document, "6. PROFIL D'APPRENTISSAGE DÉDUIT", teal);
        document.add(new Paragraph(lastReportData.profil).setBold());
        document.add(new Paragraph(lastReportData.planAction));

        // 7. Planning Recommandé
        addSectionTitle(document, "7. PLANNING RECOMMANDÉ", teal);
        document.add(new Paragraph(lastReportData.planning).setFontSize(10).setMarginBottom(20));

        // Message Final
        document.add(new Paragraph(lastReportData.messageFinal)
                .setTextAlignment(TextAlignment.CENTER).setItalic().setFontColor(ColorConstants.GRAY).setMarginTop(20));

        document.close();
    }

    private void addSectionTitle(Document doc, String title, DeviceRgb color) {
        doc.add(new Paragraph(title)
                .setFontColor(color).setBold().setMarginTop(15).setFontSize(11));
    }

    @FXML
    private void handleBack() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Apprentissage.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) resultBox.getScene().getWindow();
        stage.getScene().setRoot(root);
    }
}
