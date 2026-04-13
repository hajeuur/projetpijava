package edu.connection3a36.Controller;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.entities.Projet;
import edu.connection3a36.services.ProjetService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AfficherProjetsController implements Initializable {

    @FXML
    private ListView<Projet> listProjets;
    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnSupprimer;
    @FXML
    private Button btnEnregistrer;

    @FXML
    private Label lblFormTitle;
    @FXML
    private Label lblTabRessources;
    @FXML
    private Label lblErreur;

    @FXML
    private TextField txtTitre;
    @FXML
    private TextField txtType;
    @FXML
    private TextArea taDescription;
    @FXML
    private TextField txtTechnologies;
    @FXML
    private DatePicker dpDateDebut;
    @FXML
    private DatePicker dpDateFin;
    @FXML
    private ComboBox<Parcours> cbParcours;

    private final ProjetService projetService = new ProjetService();
    private final edu.connection3a36.services.ParcoursService parcoursService = new edu.connection3a36.services.ParcoursService();
    private Parcours parcoursActuel;
    private ObservableList<Projet> projetsData = FXCollections.observableArrayList();
    private Projet selectedProjet = null; // null = mode ajout, non-null = mode édition

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        listProjets.setCellFactory(new Callback<ListView<Projet>, ListCell<Projet>>() {
            @Override
            public ListCell<Projet> call(ListView<Projet> param) {
                return new ListCell<Projet>() {
                    @Override
                    protected void updateItem(Projet item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("-fx-background-color: transparent;");
                        } else {
                            setText(item.getTitre() + "\n" + (item.getType() != null ? item.getType() : "Autre"));
                            setStyle(
                                    "-fx-padding: 10; -fx-border-color: transparent transparent #eee transparent; -fx-border-width: 1; -fx-cursor: hand;");
                            if (isSelected()) {
                                setStyle(getStyle()
                                        + "-fx-background-color: #f0f4f8; -fx-text-fill: #102c59; -fx-font-weight: bold;");
                            }
                        }
                    }
                };
            }
        });

        listProjets.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                remplirFormulaire(newSel);
            }
        });

        /* Role-based hiding removed: CRUD now enabled in Front Office as requested */
        /*
         * if (edu.connection3a36.tools.SessionManager.getInstance().getCurrentUser() !=
         * null) {
         * String role =
         * edu.connection3a36.tools.SessionManager.getInstance().getCurrentUser().
         * getRole();
         * boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(role) ||
         * "ADMIN".equalsIgnoreCase(role);
         * if (!isAdmin) {
         * if (btnAjouter != null) {
         * btnAjouter.setVisible(false);
         * btnAjouter.setManaged(false);
         * }
         * if (btnSupprimer != null) {
         * btnSupprimer.setVisible(false);
         * btnSupprimer.setManaged(false);
         * }
         * if (btnEnregistrer != null) {
         * btnEnregistrer.setVisible(false);
         * btnEnregistrer.setManaged(false);
         * }
         * txtTitre.setEditable(false);
         * txtType.setEditable(false);
         * taDescription.setEditable(false);
         * txtTechnologies.setEditable(false);
         * dpDateDebut.setDisable(true);
         * dpDateFin.setDisable(true);
         * }
         * }
         */
    }

    public void initData(Parcours parcours) {
        this.parcoursActuel = parcours;
        chargerDonnees();
        preparerAjout(); // Par défaut on affiche un formulaire vide
    }

    private void chargerDonnees() {
        try {
            List<Projet> liste;
            if (parcoursActuel != null) {
                liste = projetService.getByParcoursId(parcoursActuel.getId());
            } else {
                liste = projetService.getData(); // Global mode: all projects
            }
            projetsData = FXCollections.observableArrayList(liste);
            listProjets.setItems(projetsData);

            // Charger les parcours disponibles pour le lien
            List<Parcours> allParcours = parcoursService.getData();
            cbParcours.setItems(FXCollections.observableArrayList(allParcours));

            // Custom cell factory for ComboBox to show Title
            cbParcours.setCellFactory(lv -> new ListCell<Parcours>() {
                @Override
                protected void updateItem(Parcours item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getTitre());
                }
            });
            cbParcours.setButtonCell(new ListCell<Parcours>() {
                @Override
                protected void updateItem(Parcours item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getTitre());
                }
            });

        } catch (SQLException e) {
            lblErreur.setText("Erreur de chargement: " + e.getMessage());
        }
    }

    @FXML
    private void preparerAjout() {
        selectedProjet = null;
        listProjets.getSelectionModel().clearSelection();
        lblFormTitle.setText("Créer un projet");
        btnEnregistrer.setText("💾 Enregistrer le projet");
        txtTitre.clear();
        txtType.clear();
        taDescription.clear();
        txtTechnologies.clear();
        dpDateDebut.setValue(null);
        dpDateFin.setValue(null);
        cbParcours.setValue(parcoursActuel); // Default to current if we came from a parcours
        lblErreur.setText("");
        lblTabRessources.setVisible(false);
    }

    private void remplirFormulaire(Projet projet) {
        selectedProjet = projet;
        lblFormTitle.setText("Détails du projet");
        btnEnregistrer.setText("✏️ Modifier le projet");
        txtTitre.setText(projet.getTitre());
        txtType.setText(projet.getType());
        taDescription.setText(projet.getDescription());
        txtTechnologies.setText(projet.getTechnologies());
        dpDateDebut.setValue(projet.getDateDebut());
        dpDateFin.setValue(projet.getDateFin());

        // Trouver le parcours correspondant dans la combo
        if (projet.getParcoursId() > 0) {
            cbParcours.getItems().stream()
                    .filter(p -> p.getId() == projet.getParcoursId())
                    .findFirst()
                    .ifPresent(cbParcours::setValue);
        } else {
            cbParcours.setValue(null);
        }

        lblErreur.setText("");
        lblTabRessources.setVisible(true);
    }

    @FXML
    private void enregistrerProjet() {
        String titre = txtTitre.getText().trim();
        String type = txtType.getText().trim();
        String desc = taDescription.getText().trim();
        String tech = txtTechnologies.getText().trim();
        LocalDate dd = dpDateDebut.getValue();
        LocalDate df = dpDateFin.getValue();

        if (titre.isEmpty() || type.isEmpty() || desc.isEmpty() || dd == null) {
            lblErreur.setText("Veuillez remplir tous les champs obligatoires (*).");
            return;
        }
        if (titre.length() < 3) {
            lblErreur.setText("Le titre doit contenir au moins 3 caractères.");
            return;
        }

        try {
            Parcours linkedP = cbParcours.getValue();

            if (selectedProjet == null) {
                // Ajouter
                Projet p = new Projet();
                p.setTitre(titre);
                p.setType(type);
                p.setDescription(desc);
                p.setTechnologies(tech);
                p.setDateDebut(dd);
                p.setDateFin(df);
                p.setParcoursId(linkedP != null ? linkedP.getId() : 0);
                projetService.addEntity(p);
                afficherInfo("Succès", "Le projet a été créé.");
            } else {
                // Modifier
                selectedProjet.setTitre(titre);
                selectedProjet.setType(type);
                selectedProjet.setDescription(desc);
                selectedProjet.setTechnologies(tech);
                selectedProjet.setDateDebut(dd);
                selectedProjet.setDateFin(df);
                selectedProjet.setParcoursId(linkedP != null ? linkedP.getId() : 0);
                projetService.updateEntity(selectedProjet.getId(), selectedProjet);
                afficherInfo("Succès", "Le projet a été mis à jour.");
            }
            chargerDonnees();
            preparerAjout();
        } catch (SQLException e) {
            lblErreur.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void supprimerProjet() {
        Projet selected = listProjets.getSelectionModel().getSelectedItem();
        if (selected == null) {
            afficherAvertissement("Sélection requise", "Veuillez sélectionner un projet dans la liste.");
            return;
        }
        Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION,
                "Voulez-vous supprimer ce projet et toutes ses ressources ?",
                ButtonType.YES, ButtonType.NO).showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                projetService.deleteEntity(selected);
                chargerDonnees();
                preparerAjout();
            } catch (SQLException e) {
                lblErreur.setText("Erreur suppression: " + e.getMessage());
            }
        }
    }

    @FXML
    private void ouvrirRessources() {
        if (selectedProjet == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherRessources.fxml"));
            Parent root = loader.load();
            AfficherRessourcesController controller = loader.getController();
            controller.initData(selectedProjet);
            Stage stage = new Stage();
            stage.setTitle("Ressources du projet : " + selectedProjet.getTitre());
            stage.setScene(new Scene(root, 950, 650));
            stage.setMaximized(true);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            lblErreur.setText("Erreur ouverture Ressources: " + e.getMessage());
        }
    }

    private void afficherAvertissement(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void afficherInfo(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
