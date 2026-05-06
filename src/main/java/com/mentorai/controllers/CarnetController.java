package com.mentorai.controllers;

import com.mentorai.entities.Carnet;
import com.mentorai.entities.PlanningEtude;
import com.mentorai.services.CarnetService;
import com.mentorai.services.PlanningService;
import edu.connection3a36.controllers.MainController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.net.URL;
import java.sql.SQLException;
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
    @FXML private Button btnLierPlanning;

    @FXML private ColorPicker colorPicker;
    @FXML private ComboBox<String> visibiliteCombo;
    @FXML private ComboBox<String> filterCombo;

    // 🔴 VALIDATION LABELS
    @FXML private Label titreError;
    @FXML private Label contenuError;
    @FXML private Label visibiliteError;

    private final CarnetService   carnetService  = new CarnetService();
    private final PlanningService planningService = new PlanningService();
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
        filterCombo.setValue("all");

        filterCombo.setOnAction(e -> loadNotes());

        // 🔴 REAL-TIME VALIDATION
        titreField.textProperty().addListener((obs, o, n) -> validateTitre());
        contenuEditor.textProperty().addListener((obs, o, n) -> validateContenu());
        visibiliteCombo.valueProperty().addListener((obs, o, n) -> validateVisibilite());
    }

    // ── PUBLIC API: select a note externally (called from MainController after navigation) ──

    /**
     * Pre-selects a note and displays it in read mode.
     * Called by MainController.openCarnetAndSelect() when navigating from Planning.
     */
    public void selectNote(Carnet note) {
        if (note == null) return;

        // First make sure the note is in the visible list; reload all if needed
        boolean found = notesList.stream().anyMatch(n -> n.getId() == note.getId());
        if (!found) {
            // Temporarily switch filter to "all" so the note is visible
            filterCombo.setValue("all");
            loadNotes();
        }

        // Find the matching item in the list (use fresh data if not found)
        Carnet target = notesList.stream()
                .filter(n -> n.getId() == note.getId())
                .findFirst()
                .orElse(note);

        notesListView.getSelectionModel().select(target);
        notesListView.scrollTo(target);
        // handleSelectNote will be called by the selection listener
    }

    private void loadNotes() {
        loadNotes(carnetService.findAll());
    }

    private void loadNotes(List<Carnet> notes) {
        String filter = filterCombo.getValue();

        notesList.setAll(
                notes.stream().filter(n -> {
                    String vis = n.getVisibilite();
                    if (vis == null || vis.isBlank()) vis = "visible";

                    if ("visible".equals(filter)) return !"hidden".equalsIgnoreCase(vis);
                    if ("hidden".equals(filter)) return "hidden".equalsIgnoreCase(vis);
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
        showLinkedPlanning(carnet);
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

    // ── "Lier à un planning" ──────────────────────────────────────────────────

    @FXML
    private void handleLierPlanning() {
        if (selectedCarnet == null) {
            showError("Veuillez sélectionner une note d'abord.");
            return;
        }

        List<PlanningEtude> plannings;
        try {
            // Load all plannings so user can pick one
            plannings = planningService.findByDateRange(
                    java.time.LocalDate.of(2000, 1, 1),
                    java.time.LocalDate.of(2100, 12, 31));
        } catch (java.sql.SQLException e) {
            showError("Impossible de charger le planning : " + e.getMessage());
            return;
        }

        if (plannings.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Aucune activité de planning disponible.").showAndWait();
            return;
        }

        // Filter out plannings already linked
        List<Integer> alreadyLinked = carnetService.getLinkedPlanningIds(selectedCarnet.getId());
        List<PlanningEtude> available = plannings.stream()
                .filter(p -> !alreadyLinked.contains(p.getId()))
                .collect(java.util.stream.Collectors.toList());

        if (available.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Cette note est déjà liée à toutes les activités disponibles.").showAndWait();
            return;
        }

        ChoiceDialog<PlanningEtude> pick = new ChoiceDialog<>(available.get(0), available);
        pick.setTitle("Lier à un planning");
        pick.setHeaderText("Choisissez l'activité à lier à cette note :");
        pick.setContentText("Activité :");

        pick.showAndWait().ifPresent(chosen -> {
            boolean ok = carnetService.linkNoteToPlanning(selectedCarnet.getId(), chosen.getId());
            if (ok) {
                editorStatusLabel.setText("📅 Lié à : " + chosen.getTitreP()
                        + " (" + chosen.getDateSeance() + ")");
            } else {
                showError("Impossible de créer le lien.");
            }
        });
    }

    // 🔴 VALIDATION METHODS

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

        // "Lier à un planning" — only visible in read mode when a note is selected
        boolean showLier = !editing && selectedCarnet != null;
        if (btnLierPlanning != null) {
            btnLierPlanning.setVisible(showLier);
            btnLierPlanning.setManaged(showLier);
        }
    }

    private void clearEditor() {
        titreField.clear();
        contenuEditor.setText("");
        selectedCarnet = null;
        editorStatusLabel.setText("");
        if (btnLierPlanning != null) {
            btnLierPlanning.setVisible(false);
            btnLierPlanning.setManaged(false);
        }
    }

    /** Shows which planning (if any) is linked to the given note in the status label. */
    private void showLinkedPlanning(Carnet carnet) {
        if (carnet == null) {
            editorStatusLabel.setText("");
            return;
        }
        PlanningEtude linked = carnetService.getPlanningByNote(carnet.getId());
        if (linked != null) {
            editorStatusLabel.setText(
                    "📅 Lié à : " + linked.getTitreP() + " (" + linked.getDateSeance() + ")");
        } else {
            editorStatusLabel.setText("");
        }
    }

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