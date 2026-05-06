package com.mentorai.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.*;
import java.sql.*;
import edu.connection3a36.entities.Utilisateur;
import edu.connection3a36.tools.SessionManager;
import com.mentorai.utils.MyConnection;

public class PersonalityController implements Initializable {

    // ── FXML injections ──────────────────────────────────────────────────
    @FXML private Label          questionLabel;
    @FXML private RadioButton    optionA;
    @FXML private RadioButton    optionB;
    @FXML private Label          progressLabel;
    @FXML private Button         btnNext;
    @FXML private Button         btnPrevious;
    @FXML private Button         btnSubmit;
    @FXML private Label          resultLabel;
    @FXML private VBox           questionCard;
    @FXML private VBox           resultCard;

    // ── State ─────────────────────────────────────────────────────────────
    private int currentIndex = 0;
    private final Map<Integer, String> answers = new HashMap<>();
    private ToggleGroup toggleGroup;

    // ── Questions  [text, optionA_text, optionA_dim, optionB_text, optionB_dim]
    // Dimensions: E/I, S/N, T/F, J/P
    private static final String[][] QUESTIONS = {
            // E vs I
            {"At a party, you tend to...",
                    "Talk to many different people", "E",
                    "Stay with one or two people you know", "I"},
            {"After a long week, you recharge by...",
                    "Going out with friends", "E",
                    "Staying home alone", "I"},
            {"In a group project, you prefer to...",
                    "Lead and coordinate the team", "E",
                    "Work on your part independently", "I"},
            // S vs N
            {"When solving a problem, you focus on...",
                    "Facts and concrete details", "S",
                    "Patterns and big-picture possibilities", "N"},
            {"You trust more...",
                    "What you can see and experience", "S",
                    "Your intuition and gut feeling", "N"},
            {"When reading, you prefer...",
                    "Practical how-to guides", "S",
                    "Abstract theories and ideas", "N"},
            // T vs F
            {"When making decisions, you rely on...",
                    "Logic and objective analysis", "T",
                    "Personal values and how others feel", "F"},
            {"A friend tells you about their problem. You first...",
                    "Offer a rational solution", "T",
                    "Empathise and listen carefully", "F"},
            {"You find it more important to be...",
                    "Fair and consistent", "T",
                    "Compassionate and harmonious", "F"},
            // J vs P
            {"You prefer your schedule to be...",
                    "Planned and organised in advance", "J",
                    "Flexible and open to change", "P"},
            {"When starting a project, you...",
                    "Make a detailed plan first", "J",
                    "Dive in and figure it out as you go", "P"},
            {"Deadlines make you feel...",
                    "Focused – you work best with clear dates", "J",
                    "Stressed – you prefer to keep options open", "P"}
    };

    // ════════════════════════════════════════════════════════════════════
    // INIT
    // ════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        toggleGroup = new ToggleGroup();
        optionA.setToggleGroup(toggleGroup);
        optionB.setToggleGroup(toggleGroup);

        // Disable Next until an option is chosen
        btnNext.setDisable(true);
        toggleGroup.selectedToggleProperty().addListener((obs, oldT, newT) ->
                btnNext.setDisable(newT == null));

        String existing = loadExistingType();
        if (existing != null) {
            showResult(existing);
        } else {
            resultCard.setVisible(false);
            resultCard.setManaged(false);
            loadQuestion();
        }
    }

    private String loadExistingType() {
        Utilisateur user = SessionManager.getCurrentUser();
        if (user == null) return null;
        try (Connection conn = MyConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement("SELECT type_pers FROM profil_apprentissage WHERE utilisateur_id = ?")) {
            ps.setInt(1, user.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("type_pers");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // ════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ════════════════════════════════════════════════════════════════════

    @FXML
    private void handleNext() {
        saveCurrentAnswer();
        if (currentIndex < QUESTIONS.length - 1) {
            currentIndex++;
            loadQuestion();
        }
    }

    @FXML
    private void handlePrevious() {
        if (currentIndex > 0) {
            saveCurrentAnswer();
            currentIndex--;
            loadQuestion();
        }
    }

    @FXML
    private void handleSubmit() {
        saveCurrentAnswer();
        String type = calculateResult();
        saveResultToDB(type);
        showResult(type);
    }

    private void saveResultToDB(String type) {
        Utilisateur user = SessionManager.getCurrentUser();
        if (user == null) return;
        try (Connection conn = MyConnection.getInstance()) {
            boolean exists = false;
            try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM profil_apprentissage WHERE utilisateur_id = ?")) {
                ps.setInt(1, user.getId());
                try (ResultSet rs = ps.executeQuery()) { exists = rs.next(); }
            }
            if (exists) {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE profil_apprentissage SET type_pers = ? WHERE utilisateur_id = ?")) {
                    ps.setString(1, type);
                    ps.setInt(2, user.getId());
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO profil_apprentissage (utilisateur_id, type_pers) VALUES (?, ?)")) {
                    ps.setInt(1, user.getId());
                    ps.setString(2, type);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ════════════════════════════════════════════════════════════════════
    // LOGIC
    // ════════════════════════════════════════════════════════════════════

    void loadQuestion() {
        String[] q = QUESTIONS[currentIndex];
        questionLabel.setText(q[0]);
        optionA.setText(q[1]);
        optionB.setText(q[3]);

        // Restore saved answer if any
        toggleGroup.selectToggle(null);
        String saved = answers.get(currentIndex);
        if (saved != null) {
            if (saved.equals(q[2])) toggleGroup.selectToggle(optionA);
            else                    toggleGroup.selectToggle(optionB);
        }
        btnNext.setDisable(toggleGroup.getSelectedToggle() == null);

        int total = QUESTIONS.length;
        progressLabel.setText("Question " + (currentIndex + 1) + " / " + total);

        btnPrevious.setDisable(currentIndex == 0);
        btnNext.setVisible(currentIndex < total - 1);
        btnNext.setManaged(currentIndex < total - 1);
        btnSubmit.setVisible(currentIndex == total - 1);
        btnSubmit.setManaged(currentIndex == total - 1);

        // Disable submit until answered
        if (currentIndex == total - 1) {
            btnSubmit.setDisable(toggleGroup.getSelectedToggle() == null);
            toggleGroup.selectedToggleProperty().addListener((obs, o, n) ->
                    btnSubmit.setDisable(n == null));
        }
    }

    private void saveCurrentAnswer() {
        RadioButton selected = (RadioButton) toggleGroup.getSelectedToggle();
        if (selected == null) return;
        String[] q = QUESTIONS[currentIndex];
        // optionA → q[2], optionB → q[4]
        String dim = selected == optionA ? q[2] : q[4];
        answers.put(currentIndex, dim);
    }

    String calculateResult() {
        Map<String, Integer> score = new HashMap<>();
        for (String d : new String[]{"E","I","S","N","T","F","J","P"})
            score.put(d, 0);
        for (String v : answers.values())
            score.merge(v, 1, Integer::sum);

        String ei = score.get("E") >= score.get("I") ? "E" : "I";
        String sn = score.get("S") >= score.get("N") ? "S" : "N";
        String tf = score.get("T") >= score.get("F") ? "T" : "F";
        String jp = score.get("J") >= score.get("P") ? "J" : "P";
        return ei + sn + tf + jp;
    }

    private void showResult(String type) {
        questionCard.setVisible(false);
        questionCard.setManaged(false);
        resultCard.setVisible(true);
        resultCard.setManaged(true);
        resultLabel.setText(type);
    }

    @FXML
    private void handleRetake() {
        answers.clear();
        currentIndex = 0;
        resultCard.setVisible(false);
        resultCard.setManaged(false);
        questionCard.setVisible(true);
        questionCard.setManaged(true);
        loadQuestion();
    }
}