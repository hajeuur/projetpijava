package edu.connection3a36.controllers;

import edu.connection3a36.services.GroqService;
import edu.connection3a36.services.MockDataService;
import edu.connection3a36.tools.AlertUtil;
import edu.connection3a36.tools.AIJsonParser;
import edu.connection3a36.tools.AIJsonSchemas;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class IoTClusteringController {

    // --- Indicateur PIR (Présence) ---
    @FXML private Circle indicatorPIR;
    @FXML private Label lblStatusPIR;
    @FXML private Label lblLastMotion;
    private boolean motionDetected = false;

    // --- Badge NFC ---
    @FXML private ComboBox<String> comboNFC;
    @FXML private Label lblNfcStudentName, lblNfcStudentDetails, lblNfcAlert;
    @FXML private VBox boxNfcResult;

    // --- Liste des présents ---
    @FXML private ListView<String> listPresents;
    private final List<String> presentsList = new ArrayList<>();

    // --- IA Clustering ---
    @FXML private Button btnCluster;
    @FXML private VBox boxCluster1, boxCluster2, boxCluster3;
    @FXML private Label lblGroup1Title, lblGroup1Desc;
    @FXML private Label lblGroup2Title, lblGroup2Desc;
    @FXML private Label lblGroup3Title, lblGroup3Desc;
    @FXML private VBox clusterContainer;
    @FXML private Label lblKpiPresentCount, lblKpiRiskCount, lblCoveragePct, lblOperationalHint;
    @FXML private ProgressBar progressCoverage;

    private List<String[]> students;

    @FXML public void initialize() {
        students = MockDataService.getStudentProfiles();
        for (String[] s : students) comboNFC.getItems().add(s[1] + " (Badge " + s[0] + ")");
        boxNfcResult.setVisible(false);
        clusterContainer.setVisible(false);

        // Simulation capteur PIR : Détecte du mouvement aléatoirement toutes les 5-10 sec
        Timer pirTimer = new Timer(true);
        pirTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                if (Math.random() > 0.5) triggerMotion();
            }
        }, 3000, 8000);
        refreshKpisAndHint();
    }

    private void triggerMotion() {
        Platform.runLater(() -> {
            motionDetected = true;
            indicatorPIR.setFill(Color.web("#10b981")); // Vert
            lblStatusPIR.setText("🟢 MOUVEMENT DÉTECTÉ");
            lblLastMotion.setText("Dernière détection : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            
            // Revenir au repos après 3s
            new Timer(true).schedule(new TimerTask() {
                @Override public void run() {
                    Platform.runLater(() -> {
                        motionDetected = false;
                        indicatorPIR.setFill(Color.web("#64748b"));
                        lblStatusPIR.setText("⚪ SALLE VIDE (Attente...)");
                    });
                }
            }, 3000);
        });
    }

    @FXML void handlePIRManual() {
        triggerMotion();
    }

    @FXML void handleSimulateNFC() {
        String sel = comboNFC.getValue();
        if (sel == null) { AlertUtil.showError("Sélectionnez un badge NFC."); return; }
        
        String id = sel.substring(sel.indexOf("Badge ") + 6, sel.indexOf(")"));
        String[] student = null;
        for(String[] s : students) { if (s[0].equals(id)) { student = s; break; } }
        
        if (student == null) return;

        // Animer l'apparition
        boxNfcResult.setVisible(true);
        lblNfcStudentName.setText(student[1]);
        lblNfcStudentDetails.setText("ID NFC: " + student[0] + " | Moyenne: " + student[3] + " | Absences: " + student[4] + " | " + student[2]);

        // Alerte si absence ou problème
        int abs = Integer.parseInt(student[4]);
        if (abs >= 3) {
            lblNfcAlert.setText("⚠️ ALERTE : Étudiant à risque (" + abs + " absences) ! Interpellez-le à la fin du cours.");
            lblNfcAlert.setVisible(true);
            lblNfcAlert.setManaged(true);
        } else {
            lblNfcAlert.setVisible(false);
            lblNfcAlert.setManaged(false);
        }

        // Ajouter aux présents si pas déjà là
        if (!presentsList.contains(student[1])) {
            presentsList.add(student[1]);
            listPresents.getItems().add("✅ " + student[1] + " (" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + ")");
            btnCluster.setDisable(presentsList.size() < 2);
            refreshKpisAndHint();
        }
    }

    @FXML void handleClusteringIA() {
        if (presentsList.isEmpty()) return;
        
        btnCluster.setDisable(true);
        btnCluster.setText("🔄 Analyse IA en cours...");
        clusterContainer.setVisible(true);
        clusterContainer.setManaged(true);
        boxCluster1.setVisible(false); boxCluster2.setVisible(false); boxCluster3.setVisible(false);

        new Thread(() -> {
            try {
                edu.connection3a36.services.GroqService groq = new edu.connection3a36.services.GroqService();
                String prompt = "Tu es MentorAI. Voici la liste des étudiants présents: " + String.join(", ", presentsList) + ".\n"
                              + MockDataService.getEtudiants3A36Context() + "\n"
                              + "Crée 2 ou 3 groupes de travail équilibrés selon profils pédagogiques.\n"
                              + "Pour chaque groupe, donne un objectif concret et une action pédagogique de 15 minutes.";
                String response = groq.sendSimpleJsonMessage(prompt, "ENSEIGNANT", AIJsonSchemas.CLUSTERS);
                
                Platform.runLater(() -> {
                    parseAndDisplayClusters(response);
                    btnCluster.setText("✨ Regrouper les présents via IA");
                    btnCluster.setDisable(false);
                    refreshKpisAndHint();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    AlertUtil.showError("Erreur IA: " + e.getMessage());
                    btnCluster.setText("✨ Regrouper les présents via IA");
                    btnCluster.setDisable(false);
                    refreshKpisAndHint();
                });
            }
        }).start();
    }

    private void parseAndDisplayClusters(String text) {
        org.json.JSONObject json = AIJsonParser.extractFirstJsonObject(text);
        if (json != null && json.has("groupes")) {
            try {
                org.json.JSONArray groups = json.getJSONArray("groupes");
                boxCluster1.setVisible(false); boxCluster1.setManaged(false);
                boxCluster2.setVisible(false); boxCluster2.setManaged(false);
                boxCluster3.setVisible(false); boxCluster3.setManaged(false);
                for (int i = 0; i < Math.min(groups.length(), 3); i++) {
                    org.json.JSONObject g = groups.getJSONObject(i);
                    String title = g.optString("nom", "Groupe " + (i + 1));
                    String desc = "Etudiants: " + g.optString("etudiants", "")
                            + "\nObjectif: " + g.optString("objectif", "")
                            + "\nAction: " + g.optString("action", "");
                    if (i == 0) {
                        lblGroup1Title.setText("Groupe 1 : " + title); lblGroup1Desc.setText(desc);
                        boxCluster1.setVisible(true); boxCluster1.setManaged(true);
                    } else if (i == 1) {
                        lblGroup2Title.setText("Groupe 2 : " + title); lblGroup2Desc.setText(desc);
                        boxCluster2.setVisible(true); boxCluster2.setManaged(true);
                    } else {
                        lblGroup3Title.setText("Groupe 3 : " + title); lblGroup3Desc.setText(desc);
                        boxCluster3.setVisible(true); boxCluster3.setManaged(true);
                    }
                }
                refreshKpisAndHint();
                return;
            } catch (Exception ignored) {}
        }
        String[] lines = text.split("\n");
        int gIdx = 1;
        
        boxCluster1.setVisible(false); boxCluster1.setManaged(false);
        boxCluster2.setVisible(false); boxCluster2.setManaged(false);
        boxCluster3.setVisible(false); boxCluster3.setManaged(false);
        
        for (String l : lines) {
            if (l.toUpperCase().contains("GROUPE")) {
                try {
                    String[] parts = l.split(":", 2);
                    String[] subParts = parts[1].split("\\|", 3);
                    String title = subParts[0].trim();
                    String desc = "";
                    if (subParts.length > 1) {
                        desc = "Étudiants: " + subParts[1].trim();
                    }
                    if (subParts.length > 2) {
                        desc += "\n" + subParts[2].trim();
                    }
                    
                    if (title.isEmpty() && desc.isEmpty()) continue;
                    
                    if (gIdx == 1) {
                        lblGroup1Title.setText("Groupe 1 : " + title); lblGroup1Desc.setText(desc);
                        boxCluster1.setVisible(true); boxCluster1.setManaged(true);
                    } else if (gIdx == 2) {
                        lblGroup2Title.setText("Groupe 2 : " + title); lblGroup2Desc.setText(desc);
                        boxCluster2.setVisible(true); boxCluster2.setManaged(true);
                    } else if (gIdx == 3) {
                        lblGroup3Title.setText("Groupe 3 : " + title); lblGroup3Desc.setText(desc);
                        boxCluster3.setVisible(true); boxCluster3.setManaged(true);
                    }
                    gIdx++;
                } catch (Exception ignored) {}
            }
        }
        if (gIdx == 1) {
            // Fallback si l'IA n'a pas respecté le format
            lblGroup1Title.setText("Groupes Suggérés");
            lblGroup1Desc.setText(text.length() > 200 ? text.substring(0, 200) + "..." : text);
            boxCluster1.setVisible(true); boxCluster1.setManaged(true);
        }
        refreshKpisAndHint();
    }

    private void refreshKpisAndHint() {
        int total = students != null ? students.size() : 10;
        int present = presentsList.size();
        int risk = 0;
        for (String[] s : students) {
            if (presentsList.contains(s[1])) {
                int abs = Integer.parseInt(s[4]);
                double avg = Double.parseDouble(s[3]);
                if (s[2].toLowerCase().contains("risque") || abs >= 3 || avg < 11.0) {
                    risk++;
                }
            }
        }
        double coverage = total > 0 ? (double) present / total : 0.0;
        if (lblKpiPresentCount != null) lblKpiPresentCount.setText(present + " / " + total);
        if (lblKpiRiskCount != null) lblKpiRiskCount.setText(String.valueOf(risk));
        if (progressCoverage != null) progressCoverage.setProgress(coverage);
        if (lblCoveragePct != null) lblCoveragePct.setText(String.format("%.0f%%", coverage * 100));

        if (lblOperationalHint != null) {
            if (present < 3) {
                lblOperationalHint.setText("Ajoutez encore " + (3 - present) + " badge(s) pour un clustering plus fiable.");
            } else if (risk >= 2) {
                lblOperationalHint.setText("Priorité: créez un groupe de remédiation ciblé et un point individuel de 10 min pour chaque étudiant à risque.");
            } else {
                lblOperationalHint.setText("Répartition équilibrée: lancez une activité collaborative de 15 min puis mesurez progrès/participation en fin de séance.");
            }
        }
    }
}
