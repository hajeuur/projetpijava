package com.esprit.controllers;

import com.esprit.dao.UtilisateurDAO;
import com.esprit.models.Utilisateur;
import com.esprit.utils.OllamaAIService;
import com.esprit.utils.UserRiskAnalyzer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackOfficeController implements Initializable {

    @FXML private TableView<Utilisateur> tableView;
    @FXML private TableColumn<Utilisateur, Integer> colId;
    @FXML private TableColumn<Utilisateur, String>  colNom;
    @FXML private TableColumn<Utilisateur, String>  colPrenom;
    @FXML private TableColumn<Utilisateur, String>  colEmail;
    @FXML private TableColumn<Utilisateur, String>  colRole;
    @FXML private TableColumn<Utilisateur, String>  colStatus;
    @FXML private TableColumn<Utilisateur, Void>    colActions;
    @FXML private TableColumn<Utilisateur, Void>    colScore;
    @FXML private TableColumn<Utilisateur, Void>    colRisk;
    @FXML private TableColumn<Utilisateur, Void>    colDuplicate;
    @FXML private TableColumn<Utilisateur, Void>    colAiVerdict;

    @FXML private Label lblLow;
    @FXML private Label lblMedium;
    @FXML private Label lblHigh;
    @FXML private Label lblDuplicates;

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label            countLabel;
    @FXML private Label            aiStatusLabel;

    private final UtilisateurDAO dao = new UtilisateurDAO();
    private ObservableList<Utilisateur> data = FXCollections.observableArrayList();
    private Utilisateur currentUser;

    public void setUtilisateur(Utilisateur u) { this.currentUser = u; }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        sortCombo.getItems().addAll(
                "Nom (A-Z)", "Nom (Z-A)", "Prénom (A-Z)", "Prénom (Z-A)",
                "Rôle", "Status", "ID croissant", "ID décroissant",
                "Score (croissant)", "Score (décroissant)"
        );

        ajouterColonneScore();
        ajouterColonneRisk();
        ajouterColonneDuplicate();
        ajouterColonneAiVerdict();
        ajouterColonneActions();
        chargerDonnees();
    }

    // ── Données ───────────────────────────────────────────────────────────────

    private void chargerDonnees() {
        data.clear();
        data.addAll(dao.getAll());
        tableView.setItems(data);
        updateCount();
        updateStats();
    }

    private void updateCount() {
        if (countLabel != null)
            countLabel.setText("Affichage de " + data.size() + " utilisateur(s)");
    }

    private void updateStats() {
        long low  = data.stream().filter(u -> "LOW".equalsIgnoreCase(u.getRiskLevel())).count();
        long med  = data.stream().filter(u -> "MEDIUM".equalsIgnoreCase(u.getRiskLevel())).count();
        long high = data.stream().filter(u -> "HIGH".equalsIgnoreCase(u.getRiskLevel())).count();
        long dup  = data.stream().filter(u -> u.getFlaggedDuplicate() == 1).count();
        if (lblLow        != null) lblLow.setText(String.valueOf(low));
        if (lblMedium     != null) lblMedium.setText(String.valueOf(med));
        if (lblHigh       != null) lblHigh.setText(String.valueOf(high));
        if (lblDuplicates != null) lblDuplicates.setText(String.valueOf(dup));
    }

    // ── Colonne Score ─────────────────────────────────────────────────────────

    private void ajouterColonneScore() {
        if (colScore == null) return;
        colScore.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Utilisateur u = getTableView().getItems().get(getIndex());
                double score = u.getTrustScore();
                Rectangle bg  = new Rectangle(100, 12, Color.web("#e0e0e0"));
                bg.setArcWidth(6); bg.setArcHeight(6);
                Rectangle bar = new Rectangle(score, 12, Color.web(UserRiskAnalyzer.getRiskColor(u.getRiskLevel())));
                bar.setArcWidth(6); bar.setArcHeight(6);
                javafx.scene.layout.StackPane stack = new javafx.scene.layout.StackPane(bg, bar);
                stack.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label lbl = new Label(String.format("%.0f%%", score));
                lbl.setStyle("-fx-font-size: 11; -fx-text-fill: #333;");
                VBox box = new VBox(3, stack, lbl);
                box.setAlignment(javafx.geometry.Pos.CENTER);
                setGraphic(box);
            }
        });
    }

    // ── Colonne Risque ────────────────────────────────────────────────────────

    private void ajouterColonneRisk() {
        if (colRisk == null) return;
        colRisk.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Utilisateur u = getTableView().getItems().get(getIndex());
                String level = u.getRiskLevel() != null ? u.getRiskLevel() : "LOW";
                String color = UserRiskAnalyzer.getRiskColor(level);
                Label badge = new Label(UserRiskAnalyzer.getRiskLabel(level));
                badge.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color +
                        "; -fx-font-weight: bold; -fx-font-size: 11; -fx-background-radius: 10; -fx-padding: 3 10;");
                setGraphic(badge);
            }
        });
    }

    // ── Colonne Doublon ───────────────────────────────────────────────────────

    private void ajouterColonneDuplicate() {
        if (colDuplicate == null) return;
        colDuplicate.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Utilisateur u = getTableView().getItems().get(getIndex());
                Label lbl = u.getFlaggedDuplicate() == 1
                        ? new Label("⚠ Oui") : new Label("Non");
                lbl.setStyle(u.getFlaggedDuplicate() == 1
                        ? "-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-font-size: 11;"
                        : "-fx-text-fill: #27ae60; -fx-font-size: 11;");
                setGraphic(lbl);
            }
        });
    }

    // ── Colonne AI Verdict (avec wrapText) ────────────────────────────────────

    private void ajouterColonneAiVerdict() {
        if (colAiVerdict == null) return;
        colAiVerdict.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Utilisateur u = getTableView().getItems().get(getIndex());
                String verdict = u.getAiVerdict();

                if (verdict == null || verdict.isEmpty()) {
                    Label lbl = new Label("Non analysé");
                    lbl.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 10; -fx-font-style: italic;");
                    setGraphic(lbl);
                } else {
                    Label lbl = new Label(verdict);
                    // ✅ wrapText activé pour afficher sur plusieurs lignes
                    lbl.setWrapText(true);
                    lbl.setMaxWidth(280);

                    // Couleur selon FIABLE/NEUTRE/SUSPECT
                    if (verdict.contains("FIABLE")) {
                        lbl.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 10;");
                    } else if (verdict.contains("SUSPECT")) {
                        lbl.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10; -fx-font-weight: bold;");
                    } else {
                        lbl.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 10;");
                    }

                    // Tooltip complet au survol
                    Tooltip tip = new Tooltip(verdict);
                    tip.setWrapText(true);
                    tip.setMaxWidth(450);
                    Tooltip.install(lbl, tip);
                    setGraphic(lbl);
                }
            }
        });
    }

    // ── Colonne Actions ───────────────────────────────────────────────────────

    private void ajouterColonneActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            Button btnModifier   = new Button("Modifier");
            Button btnDesactiver = new Button("Désactiver");
            Button btnVoir       = new Button("Voir");
            Button btnRecalcul   = new Button("⟳");
            Button btnAI         = new Button("🤖 IA");
            HBox hbox = new HBox(3, btnVoir, btnModifier, btnDesactiver, btnRecalcul, btnAI);

            {
                btnModifier.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 10;");
                btnDesactiver.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 10;");
                btnVoir.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 10;");
                btnRecalcul.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 10;");
                btnAI.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 10;");
                btnAI.setTooltip(new Tooltip("Analyser avec Mistral AI"));

                btnModifier.setOnAction(e   -> handleModifier(getTableView().getItems().get(getIndex())));
                btnDesactiver.setOnAction(e -> handleDesactiver(getTableView().getItems().get(getIndex())));
                btnVoir.setOnAction(e       -> handleVoir(getTableView().getItems().get(getIndex())));
                btnRecalcul.setOnAction(e   -> handleRecalcul(getTableView().getItems().get(getIndex())));
                btnAI.setOnAction(e         -> handleAnalyseAI(getTableView().getItems().get(getIndex()), btnAI));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Utilisateur u = getTableView().getItems().get(getIndex());
                btnDesactiver.setText(u.getStatus().equals("actif") ? "Désactiver" : "Activer");
                setGraphic(hbox);
            }
        });
    }

    // ── Analyser UN utilisateur avec Mistral ──────────────────────────────────

    private void handleAnalyseAI(Utilisateur u, Button btn) {
        btn.setDisable(true);
        btn.setText("⏳");
        if (aiStatusLabel != null)
            aiStatusLabel.setText("🤖 Analyse IA en cours pour " + u.getNom() + "...");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                String verdict = OllamaAIService.analyzeUser(u);
                u.setAiVerdict(verdict);
                dao.saveAiVerdict(u);
                Platform.runLater(() -> {
                    chargerDonnees();
                    btn.setDisable(false);
                    btn.setText("🤖 IA");
                    if (aiStatusLabel != null)
                        aiStatusLabel.setText("✅ Analyse terminée pour " + u.getNom());
                    showAlert(Alert.AlertType.INFORMATION,
                            "Verdict IA — " + u.getNom() + " " + u.getPrenom(), verdict);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText("🤖 IA");
                    if (aiStatusLabel != null)
                        aiStatusLabel.setText("❌ Erreur : " + e.getMessage());
                });
            } finally {
                executor.shutdown();
            }
        });
    }

    // ── Analyser TOUS les utilisateurs ───────────────────────────────────────

    @FXML
    public void handleAnalyseAllAI() {
        if (aiStatusLabel != null)
            aiStatusLabel.setText("🤖 Analyse IA de tous les utilisateurs en cours...");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            List<Utilisateur> tous = dao.getAll();
            int total = tous.size();
            int[] count = {0};
            for (Utilisateur u : tous) {
                try {
                    String verdict = OllamaAIService.analyzeUser(u);
                    u.setAiVerdict(verdict);
                    dao.saveAiVerdict(u);
                    count[0]++;
                    int current = count[0];
                    Platform.runLater(() -> {
                        if (aiStatusLabel != null)
                            aiStatusLabel.setText("🤖 Analyse " + current + "/" + total + "...");
                    });
                } catch (Exception e) {
                    System.out.println(">>> Erreur AI pour " + u.getEmail() + ": " + e.getMessage());
                }
            }
            Platform.runLater(() -> {
                chargerDonnees();
                if (aiStatusLabel != null)
                    aiStatusLabel.setText("✅ Analyse terminée pour " + count[0] + "/" + total + " utilisateurs");
                showAlert(Alert.AlertType.INFORMATION, "Analyse IA terminée",
                        count[0] + " utilisateurs analysés avec Mistral AI !");
            });
            executor.shutdown();
        });
    }

    // ── Recalculer risque ─────────────────────────────────────────────────────

    private void handleRecalcul(Utilisateur u) {
        List<Utilisateur> tous = dao.getAll();
        UserRiskAnalyzer.analyze(u, tous);
        dao.updateRisk(u);
        chargerDonnees();
        showAlert(Alert.AlertType.INFORMATION, "Score recalculé",
                u.getNom() + " " + u.getPrenom() +
                        "\nScore : " + String.format("%.0f%%", u.getTrustScore()) +
                        "\nNiveau : " + UserRiskAnalyzer.getRiskLabel(u.getRiskLevel()));
    }

    @FXML
    public void handleRecalculAll() {
        List<Utilisateur> tous = dao.getAll();
        for (Utilisateur u : tous) {
            UserRiskAnalyzer.analyze(u, tous);
            dao.updateRisk(u);
        }
        chargerDonnees();
        showAlert(Alert.AlertType.INFORMATION, "Recalcul terminé",
                "Tous les scores de risque ont été recalculés !");
    }

    // ── Tri / Recherche ───────────────────────────────────────────────────────

    @FXML public void handleSort() {
        String s = sortCombo.getValue();
        if (s == null) return;
        switch (s) {
            case "Nom (A-Z)"           -> data.sort(Comparator.comparing(u -> u.getNom().toLowerCase()));
            case "Nom (Z-A)"           -> data.sort((a, b) -> b.getNom().compareToIgnoreCase(a.getNom()));
            case "Prénom (A-Z)"        -> data.sort(Comparator.comparing(u -> u.getPrenom().toLowerCase()));
            case "Prénom (Z-A)"        -> data.sort((a, b) -> b.getPrenom().compareToIgnoreCase(a.getPrenom()));
            case "Rôle"                -> data.sort(Comparator.comparing(u -> u.getRole().toLowerCase()));
            case "Status"              -> data.sort(Comparator.comparing(u -> u.getStatus().toLowerCase()));
            case "ID croissant"        -> data.sort(Comparator.comparingInt(Utilisateur::getId));
            case "ID décroissant"      -> data.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
            case "Score (croissant)"   -> data.sort(Comparator.comparingDouble(Utilisateur::getTrustScore));
            case "Score (décroissant)" -> data.sort((a, b) -> Double.compare(b.getTrustScore(), a.getTrustScore()));
        }
        tableView.setItems(data);
    }

    @FXML public void handleSearch() {
        String kw = searchField.getText().toLowerCase();
        List<Utilisateur> tous = dao.getAll();
        data.clear();
        for (Utilisateur u : tous) {
            if (u.getNom().toLowerCase().contains(kw) ||
                    u.getPrenom().toLowerCase().contains(kw) ||
                    u.getEmail().toLowerCase().contains(kw) ||
                    u.getRole().toLowerCase().contains(kw)) data.add(u);
        }
        tableView.setItems(data);
        updateCount();
    }

    @FXML public void handleReset() {
        searchField.clear();
        sortCombo.setValue(null);
        chargerDonnees();
    }

    // ── Actions existantes ────────────────────────────────────────────────────

    private void handleModifier(Utilisateur u) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/ModifierUser.fxml"));
            Parent root = loader.load();
            ModifierUserController ctrl = loader.getController();
            ctrl.setUtilisateur(u, this);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier utilisateur");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleVoir(Utilisateur u) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/ProfilUser.fxml"));
            Parent root = loader.load();
            ProfilUserController ctrl = loader.getController();
            ctrl.setUtilisateur(u);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Profil utilisateur");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleDesactiver(Utilisateur u) {
        u.setStatus(u.getStatus().equals("actif") ? "desactiver" : "actif");
        dao.modifier(u);
        chargerDonnees();
    }

    @FXML public void handleAjouter() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/Inscription.fxml"));
            Parent root = loader.load();
            InscriptionController ctrl = loader.getController();
            ctrl.setBackOfficeController(this);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Ajouter utilisateur");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tableView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void refreshTable() { chargerDonnees(); }

    @FXML public void handleExportExcel() {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("utilisateurs.xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File file = fc.showSaveDialog((Stage) tableView.getScene().getWindow());
        if (file != null) {
            try { generateXLSX(file); showAlert(Alert.AlertType.INFORMATION, "Export réussi", "Fichier Excel exporté !"); }
            catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
        }
    }

    @FXML public void handleExportPDF() {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("utilisateurs.pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showSaveDialog((Stage) tableView.getScene().getWindow());
        if (file != null) {
            try { generatePDF(file); showAlert(Alert.AlertType.INFORMATION, "Export réussi", "PDF exporté !"); }
            catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void generateXLSX(File file) throws Exception {
        java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new FileOutputStream(file));
        zos.putNextEntry(new java.util.zip.ZipEntry("[Content_Types].xml"));
        zos.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/></Types>".getBytes("UTF-8")); zos.closeEntry();
        zos.putNextEntry(new java.util.zip.ZipEntry("_rels/.rels"));
        zos.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>".getBytes("UTF-8")); zos.closeEntry();
        zos.putNextEntry(new java.util.zip.ZipEntry("xl/_rels/workbook.xml.rels"));
        zos.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>".getBytes("UTF-8")); zos.closeEntry();
        zos.putNextEntry(new java.util.zip.ZipEntry("xl/workbook.xml"));
        zos.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Utilisateurs\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>".getBytes("UTF-8")); zos.closeEntry();
        zos.putNextEntry(new java.util.zip.ZipEntry("xl/styles.xml"));
        zos.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><fonts><font><b/><sz val=\"11\"/></font><font><sz val=\"10\"/></font></fonts><fills><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1a2a4a\"/></patternFill></fill></fills><borders><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs><cellXfs><xf numFmtId=\"0\" fontId=\"0\" fillId=\"2\" borderId=\"0\" xfId=\"0\"/><xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/></cellXfs></styleSheet>".getBytes("UTF-8")); zos.closeEntry();
        zos.putNextEntry(new java.util.zip.ZipEntry("xl/worksheets/sheet1.xml"));
        StringBuilder sheet = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData><row r=\"1\">");
        String[] h = {"ID","Nom","Prénom","Email","Rôle","Status","Score","Risque","Doublon","Verdict IA"};
        String[] c = {"A","B","C","D","E","F","G","H","I","J"};
        for (int i=0;i<h.length;i++) sheet.append(String.format("<c r=\"%s1\" t=\"inlineStr\" s=\"0\"><is><t>%s</t></is></c>",c[i],h[i]));
        sheet.append("</row>");
        int r=2;
        for (Utilisateur u:data) {
            sheet.append("<row r=\"").append(r).append("\">");
            sheet.append(String.format("<c r=\"A%d\" s=\"1\"><v>%d</v></c>",r,u.getId()));
            sheet.append(xc("B",r,u.getNom())); sheet.append(xc("C",r,u.getPrenom()));
            sheet.append(xc("D",r,u.getEmail())); sheet.append(xc("E",r,u.getRole()));
            sheet.append(xc("F",r,u.getStatus()));
            sheet.append(xc("G",r,String.format("%.0f%%",u.getTrustScore())));
            sheet.append(xc("H",r,UserRiskAnalyzer.getRiskLabel(u.getRiskLevel())));
            sheet.append(xc("I",r,u.getFlaggedDuplicate()==1?"Oui":"Non"));
            sheet.append(xc("J",r,u.getAiVerdict()!=null?u.getAiVerdict().substring(0,Math.min(100,u.getAiVerdict().length())):""));
            sheet.append("</row>"); r++;
        }
        sheet.append("</sheetData></worksheet>");
        zos.write(sheet.toString().getBytes("UTF-8")); zos.closeEntry(); zos.close();
    }

    private String xc(String col,int row,String val) {
        String s=val==null?"":val.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
        return String.format("<c r=\"%s%d\" t=\"inlineStr\" s=\"1\"><is><t>%s</t></is></c>",col,row,s);
    }

    private void generatePDF(File file) throws Exception {
        try (FileOutputStream fos=new FileOutputStream(file)) {
            StringBuilder stream=new StringBuilder();
            stream.append("BT\n/F1 14 Tf\n50 800 Td\n(Liste Utilisateurs - MentorAI AI Risk) Tj\n");
            stream.append("/F1 9 Tf\n0 -20 Td\n(Total : ").append(data.size()).append(" utilisateur(s)) Tj\n0 -20 Td\n");
            stream.append("/F1 10 Tf\n(ID    Nom          Email                  Score  Risque    Verdict IA) Tj\n0 -5 Td\n");
            stream.append("(------------------------------------------------------------------------) Tj\n/F1 9 Tf\n0 -15 Td\n");
            for (Utilisateur u:data) {
                String verdict = u.getAiVerdict()!=null ? u.getAiVerdict().substring(0,Math.min(25,u.getAiVerdict().length())) : "N/A";
                String line=String.format("%-6d %-12s %-22s %-6s %-9s %-25s",
                        u.getId(),truncate(u.getNom(),11),truncate(u.getEmail(),21),
                        String.format("%.0f%%",u.getTrustScore()),
                        UserRiskAnalyzer.getRiskLabel(u.getRiskLevel()),truncate(verdict,24));
                line=line.replaceAll("[^\\x20-\\x7E]","?");
                stream.append("(").append(line).append(") Tj\n0 -13 Td\n");
            }
            stream.append("ET\n");
            byte[] sb=stream.toString().getBytes("ISO-8859-1");
            StringBuilder pdf=new StringBuilder("%PDF-1.4\n");
            int[] off=new int[6];
            off[1]=pdf.length(); pdf.append("1 0 obj\n<</Type /Catalog /Pages 2 0 R>>\nendobj\n");
            off[2]=pdf.length(); pdf.append("2 0 obj\n<</Type /Pages /Kids [3 0 R] /Count 1>>\nendobj\n");
            off[3]=pdf.length(); pdf.append("3 0 obj\n<</Type /Page /Parent 2 0 R\n/MediaBox [0 0 595 842]\n/Contents 4 0 R\n/Resources <</Font <</F1 5 0 R>>>>>>\n>>\nendobj\n");
            off[4]=pdf.length(); pdf.append("4 0 obj\n<</Length ").append(sb.length).append(">>\nstream\n");
            byte[] pb=pdf.toString().getBytes("ISO-8859-1");
            fos.write(pb); fos.write(sb);
            StringBuilder p2=new StringBuilder("\nendstream\nendobj\n");
            off[5]=pb.length+sb.length+p2.length();
            p2.append("5 0 obj\n<</Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding>>\nendobj\n");
            int xo=pb.length+sb.length+p2.length();
            p2.append("xref\n0 6\n0000000000 65535 f \n");
            for (int i=1;i<=5;i++) p2.append(String.format("%010d 00000 n \n",off[i]));
            p2.append("trailer\n<</Size 6 /Root 1 0 R>>\nstartxref\n").append(xo).append("\n%%EOF\n");
            fos.write(p2.toString().getBytes("ISO-8859-1"));
        }
    }

    private String truncate(String s,int max) { if(s==null)return""; return s.length()>max?s.substring(0,max):s; }
}