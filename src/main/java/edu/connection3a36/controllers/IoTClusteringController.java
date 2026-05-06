package edu.connection3a36.controllers;

import edu.connection3a36.services.GroqService;
import edu.connection3a36.tools.AlertUtil;
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

    // Mock Data pour NFC
    private static final String[][] STUDENTS = {
        {"101", "Amine Ben Ali", "Moyenne: 18.5 | Absences: 0"},
        {"102", "Sarah Trabelsi", "Moyenne: 09.2 | Absences: 5"},
        {"103", "Youssef Gharbi", "Moyenne: 11.5 | Absences: 1"},
        {"104", "Mariem Jlassi", "Moyenne: 13.0 | Absences: 2 | Dyslexie"},
        {"107", "Rayen Khemiri", "Moyenne: 08.5 | Absences: 8"}
    };

    @FXML public void initialize() {
        for (String[] s : STUDENTS) comboNFC.getItems().add(s[1] + " (Badge " + s[0] + ")");
        boxNfcResult.setVisible(false);
        clusterContainer.setVisible(false);

        // Simulation capteur PIR : Détecte du mouvement aléatoirement toutes les 5-10 sec
        Timer pirTimer = new Timer(true);
        pirTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                if (Math.random() > 0.5) triggerMotion();
            }
        }, 3000, 8000);
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
        for(String[] s : STUDENTS) { if (s[0].equals(id)) { student = s; break; } }
        
        if (student == null) return;

        // Animer l'apparition
        boxNfcResult.setVisible(true);
        lblNfcStudentName.setText(student[1]);
        lblNfcStudentDetails.setText("ID NFC: " + student[0] + " | " + student[2]);

        // Alerte si absence ou problème
        int abs = Integer.parseInt(student[2].split("Absences: ")[1].split(" ")[0].replace("|","").trim());
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
                              + "Crée 2 ou 3 groupes de travail avec eux.\n"
                              + "Pour chaque groupe, définis un nom créatif, la liste de ses étudiants, son objectif pédagogique, et une action à réaliser.";
                String response = groq.sendSimpleJsonMessage(prompt, "ENSEIGNANT", edu.connection3a36.tools.AIJsonSchemas.CLUSTERS);
                
                Platform.runLater(() -> {
                    parseAndDisplayClusters(response);
                    btnCluster.setText("✨ Regrouper les présents via IA");
                    btnCluster.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    AlertUtil.showError("Erreur IA: " + e.getMessage());
                    btnCluster.setText("✨ Regrouper les présents via IA");
                    btnCluster.setDisable(false);
                });
            }
        }).start();
    }

    private void parseAndDisplayClusters(String jsonText) {
        boxCluster1.setVisible(false); boxCluster1.setManaged(false);
        boxCluster2.setVisible(false); boxCluster2.setManaged(false);
        boxCluster3.setVisible(false); boxCluster3.setManaged(false);
        
        org.json.JSONObject json = edu.connection3a36.tools.AIJsonParser.extractFirstJsonObject(jsonText);
        if (json != null && json.has("groupes")) {
            org.json.JSONArray groupes = json.optJSONArray("groupes");
            if (groupes != null) {
                for (int i = 0; i < groupes.length() && i < 3; i++) {
                    org.json.JSONObject g = groupes.optJSONObject(i);
                    if (g != null) {
                        String nom = g.optString("nom", "Groupe " + (i+1));
                        String etudiants = g.optString("etudiants", "");
                        String obj = g.optString("objectif", "");
                        String action = g.optString("action", "");
                        
                        String desc = "👥 Membres : " + etudiants;
                        if (!obj.isEmpty()) desc += "\n🎯 Objectif : " + obj;
                        if (!action.isEmpty()) desc += "\n🚀 Action : " + action;
                        
                        if (i == 0) {
                            lblGroup1Title.setText(nom); lblGroup1Desc.setText(desc);
                            boxCluster1.setVisible(true); boxCluster1.setManaged(true);
                        } else if (i == 1) {
                            lblGroup2Title.setText(nom); lblGroup2Desc.setText(desc);
                            boxCluster2.setVisible(true); boxCluster2.setManaged(true);
                        } else if (i == 2) {
                            lblGroup3Title.setText(nom); lblGroup3Desc.setText(desc);
                            boxCluster3.setVisible(true); boxCluster3.setManaged(true);
                        }
                    }
                }
                return;
            }
        }
        
        // Fallback si l'IA n'a pas respecté le JSON
        lblGroup1Title.setText("Groupes Suggérés");
        lblGroup1Desc.setText(jsonText.length() > 200 ? jsonText.substring(0, 200) + "..." : jsonText);
        boxCluster1.setVisible(true); boxCluster1.setManaged(true);
    }
}
