package edu.connection3a36.Controller;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.entities.Projet;
import edu.connection3a36.services.ProjetService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import java.sql.SQLException;
import java.util.List;

public class ParcoursCardController {

    @FXML
    private Label lblTypeTag;
    @FXML
    private Label lblTitre;
    @FXML
    private Label lblEtablissement;
    @FXML
    private Label lblDates;
    @FXML
    private Label lblDiplome;
    @FXML
    private Label lblDescription;
    @FXML
    private FlowPane flowPaneProjets;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;

    private Parcours parcours;
    private AfficherParcoursController mainController;
    private final ProjetService projetService = new ProjetService();

    public void setData(Parcours p, AfficherParcoursController mainController) {
        this.parcours = p;
        this.mainController = mainController;

        lblTitre.setText(p.getTitre());
        lblTypeTag.setText(p.getTypeParcours() != null ? p.getTypeParcours().toUpperCase() : "FORMATION");
        lblEtablissement.setText(p.getEtablissement());
        lblDiplome.setText(p.getDiplome());

        String start = p.getDateDebut() != null ? p.getDateDebut().toString() : "??";
        String end = p.getDateFin() != null ? p.getDateFin().toString() : "En cours";
        lblDates.setText(start + " - " + end);

        lblDescription.setText(p.getDescription());

        // Load related projects
        try {
            List<Projet> projets = projetService.getByParcoursId(p.getId());
            flowPaneProjets.getChildren().clear();
            if (projets.isEmpty()) {
                Label empty = new Label("Aucun projet lié");
                empty.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6; -fx-font-style: italic;");
                flowPaneProjets.getChildren().add(empty);
            } else {
                for (Projet proj : projets) {
                    Label tag = new Label(proj.getTitre());
                    tag.setStyle(
                            "-fx-background-color: #f0f4f8; -fx-text-fill: #102c59; -fx-font-size: 11px; -fx-padding: 3 8; -fx-background-radius: 4; -fx-border-color: #9dbbce; -fx-border-radius: 4;");
                    flowPaneProjets.getChildren().add(tag);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Hide buttons for non-admins (REMOVED: Enabled for Front Office as requested)
        /*
         * if (edu.connection3a36.tools.SessionManager.getInstance().getCurrentUser() !=
         * null) {
         * String role =
         * edu.connection3a36.tools.SessionManager.getInstance().getCurrentUser().
         * getRole();
         * if (!("ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)))
         * {
         * btnModifier.setVisible(false);
         * btnModifier.setManaged(false);
         * btnSupprimer.setVisible(false);
         * btnSupprimer.setManaged(false);
         * }
         * }
         */
    }

    @FXML
    private void modifierParcours() {
        mainController.modifierParcours(parcours);
    }

    @FXML
    private void supprimerParcours() {
        mainController.supprimerParcours(parcours);
    }

    @FXML
    private void voirProjets() {
        mainController.voirProjets(parcours);
    }
}
