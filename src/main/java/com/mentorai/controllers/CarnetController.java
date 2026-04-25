package com.mentorai.controllers;

import com.mentorai.entities.Carnet;
import com.mentorai.services.CarnetService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class CarnetController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ListView<Carnet> notesListView;
    @FXML private Label noteCountLabel;

    @FXML private TextField titreField;
    @FXML private TextArea contenuEditor;
    @FXML private Label errorLabel;
    @FXML private Label editorStatusLabel;

    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnAnnuler;

    @FXML private ColorPicker colorPicker;
    @FXML private ComboBox<String> visibiliteCombo;
    @FXML private ComboBox<String> filterCombo;

    // 🔴 VALIDATION LABELS
    @FXML private Label titreError;
    @FXML private Label contenuError;
    @FXML private Label visibiliteError;

    private final CarnetService carnetService = new CarnetService();
    private final ObservableList<Carnet> notesList = FXCollections.observableArrayList();
    private Carnet selectedCarnet = null;
    private boolean isEditing = false;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        notesListView.setItems(notesList);
        notesListView.setCellFactory(lv -> new NoteCell());

        notesListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> handleSelectNote(selected));

        searchField.textProperty().addListener(
                (obs, old, text) -> handleSearch(text));

        setEditorMode(false);
        loadNotes();

        visibiliteCombo.setItems(FXCollections.observableArrayList("visible", "hidden"));
        visibiliteCombo.setValue("visible");

        filterCombo.setItems(FXCollections.observableArrayList("visible", "hidden", "all"));
        filterCombo.setValue("visible");

        filterCombo.setOnAction(e -> loadNotes());

        // 🔴 REAL-TIME VALIDATION
        titreField.textProperty().addListener((obs, o, n) -> validateTitre());
        contenuEditor.textProperty().addListener((obs, o, n) -> validateContenu());
        visibiliteCombo.valueProperty().addListener((obs, o, n) -> validateVisibilite());
    }

    private void loadNotes() {
        loadNotes(carnetService.findAll());
    }

    private void loadNotes(List<Carnet> notes) {
        String filter = filterCombo.getValue();

        notesList.setAll(
                notes.stream().filter(n -> {
                    if ("visible".equals(filter)) return !"hidden".equalsIgnoreCase(n.getVisibilite());
                    if ("hidden".equals(filter)) return "hidden".equalsIgnoreCase(n.getVisibilite());
                    return true;
                }).toList()
        );

        noteCountLabel.setText(notesList.size() + " note" + (notesList.size() != 1 ? "s" : ""));
    }

    private void handleSelectNote(Carnet carnet) {
        if (carnet == null) return;
        selectedCarnet = carnet;
        isEditing = false;
        titreField.setText(carnet.getTitre());
        contenuEditor.setText(carnet.getContenu() != null ? carnet.getContenu() : "");

        if (carnet.getCouleur() != null) {
            colorPicker.setValue(Color.web(carnet.getCouleur()));
        }
        visibiliteCombo.setValue(carnet.getVisibilite());

        errorLabel.setVisible(false);
        setEditorMode(false);
        updateStatusLabel();
    }

    private void handleSearch(String keyword) {
        if (keyword == null || keyword.isBlank()) loadNotes();
        else loadNotes(carnetService.search(keyword.trim()));
    }

    @FXML
    private void handleAjouter() {
        selectedCarnet = null;
        isEditing = true;
        titreField.clear();
        contenuEditor.setText("");

        colorPicker.setValue(Color.WHITE);
        visibiliteCombo.setValue("visible");

        errorLabel.setVisible(false);
        setEditorMode(true);
        editorStatusLabel.setText("✏️  Nouvelle note");
        titreField.requestFocus();
        notesListView.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleModifier() {
        if (selectedCarnet == null) {
            showError("Veuillez sélectionner une note à modifier.");
            return;
        }
        isEditing = true;
        setEditorMode(true);
        editorStatusLabel.setText("✏️  Modification : " + selectedCarnet.getTitre());
        titreField.requestFocus();
    }

    @FXML
    private void handleSupprimer() {
        if (selectedCarnet == null) {
            showError("Veuillez sélectionner une note à supprimer.");
            return;
        }

        Alert alert = buildAlert(Alert.AlertType.CONFIRMATION,
                "Supprimer la note",
                "Êtes-vous sûr ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (carnetService.delete(selectedCarnet.getId())) {
                notesList.remove(selectedCarnet);
                selectedCarnet = null;
                clearEditor();
            }
        }
    }

    @FXML
    private void handleEnregistrer() {

        // 🔴 VALIDATION
        if (!validateTitre() || !validateContenu() || !validateVisibilite()) {
            showError("Corrigez les erreurs");
            return;
        }

        String titre = titreField.getText().trim();
        String contenu = contenuEditor.getText();

        // 🔴 DUPLICATE CHECK — title must be unique (CREATE only)
        if (selectedCarnet == null) {
            boolean exists = carnetService.findAll().stream()
                    .anyMatch(n -> n.getTitre().equalsIgnoreCase(titre));
            if (exists) {
                titreError.setText("Ce titre existe déjà. Choisissez un titre unique.");
                titreError.setVisible(true);
                titreError.setManaged(true);
                showError("Titre en double — impossible d'enregistrer.");
                titreField.requestFocus();
                return;
            }
        }

        if (selectedCarnet == null) {
            Carnet carnet = new Carnet(titre, contenu);
            carnet.setCouleur(toHex(colorPicker.getValue()));
            carnet.setVisibilite(visibiliteCombo.getValue());

            if (carnetService.create(carnet)) {
                notesList.add(0, carnet);
            }
        } else {
            selectedCarnet.setTitre(titre);
            selectedCarnet.setContenu(contenu);
            selectedCarnet.setCouleur(toHex(colorPicker.getValue()));
            selectedCarnet.setVisibilite(visibiliteCombo.getValue());
            carnetService.update(selectedCarnet);
        }

        setEditorMode(false);
    }

    @FXML
    private void handleAnnuler() {
        clearEditor();
        setEditorMode(false);
    }

    // 🔴 VALIDATION METHODS (FIXED)

    private boolean validateTitre() {
        String t = titreField.getText().trim();
        if (t.isEmpty()) {
            titreError.setText("Titre obligatoire");
            titreError.setVisible(true);
            titreError.setManaged(true);
            return false;
        }
        if (t.length() > 100) {
            titreError.setText("Max 100 caractères");
            titreError.setVisible(true);
            titreError.setManaged(true);
            return false;
        }
        // Duplicate check: only block on CREATE; on UPDATE allow keeping same title
        int excludeId = (selectedCarnet != null) ? selectedCarnet.getId() : 0;
        if (carnetService.existsByTitre(t, excludeId)) {
            titreError.setText("Ce titre existe déjà. Choisissez un titre unique.");
            titreError.setVisible(true);
            titreError.setManaged(true);
            return false;
        }
        titreError.setVisible(false);
        titreError.setManaged(false);
        return true;
    }

    private boolean validateContenu() {
        String c = contenuEditor.getText().trim();
        if (c.isEmpty()) {
            contenuError.setText("Contenu obligatoire");
            contenuError.setVisible(true);
            contenuError.setManaged(true);
            return false;
        }
        if (c.length() < 5) {
            contenuError.setText("Contenu trop court");
            contenuError.setVisible(true);
            contenuError.setManaged(true);
            return false;
        }
        contenuError.setVisible(false);
        contenuError.setManaged(false);
        return true;
    }

    private boolean validateVisibilite() {
        if (visibiliteCombo.getValue() == null) {
            visibiliteError.setText("Choisir visibilité");
            visibiliteError.setVisible(true);
            visibiliteError.setManaged(true);
            return false;
        }
        visibiliteError.setVisible(false);
        visibiliteError.setManaged(false);
        return true;
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int)(c.getRed()*255),
                (int)(c.getGreen()*255),
                (int)(c.getBlue()*255));
    }

    private void setEditorMode(boolean editing) {
        titreField.setEditable(editing);
        contenuEditor.setDisable(!editing);
        btnModifier.setDisable(editing);
        btnSupprimer.setDisable(editing);
        btnAjouter.setDisable(editing);
        btnAnnuler.setVisible(editing);
        btnAnnuler.setManaged(editing);
    }

    private void clearEditor() {
        titreField.clear();
        contenuEditor.setText("");
    }

    private void updateStatusLabel() {}

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private Alert buildAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        return alert;
    }

    private class NoteCell extends ListCell<Carnet> {
        private final VBox container = new VBox(4);
        private final Label titleLbl = new Label();

        NoteCell() {
            container.getChildren().add(titleLbl);
            container.setPadding(new Insets(8));
        }

        @Override
        protected void updateItem(Carnet item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) setGraphic(null);
            else {
                titleLbl.setText(item.getTitre());
                if (item.getCouleur() != null)
                    container.setStyle("-fx-background-color:" + item.getCouleur());
                setGraphic(container);
            }
        }
    }
}