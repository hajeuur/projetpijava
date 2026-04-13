package edu.connection3a36.Controller;

import edu.connection3a36.entities.Projet;
import edu.connection3a36.entities.Ressource;
import edu.connection3a36.services.RessourceService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.sql.SQLException;
import java.util.List;

public class ProjetCardController {

    @FXML
    private Label lblTypeTag;
    @FXML
    private Label lblDate;
    @FXML
    private Label lblTitre;
    @FXML
    private Label lblDescription;
    @FXML
    private FlowPane flowPaneTech;
    @FXML
    private VBox vboxRessources;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;

    private Projet projet;
    private AfficherProjetsGlobalController mainController;
    private final RessourceService ressourceService = new RessourceService();

    public void setData(Projet p, AfficherProjetsGlobalController mainController) {
        this.projet = p;
        this.mainController = mainController;

        lblTitre.setText(p.getTitre());
        lblTypeTag.setText(p.getType() != null ? p.getType().toUpperCase() : "PROJET");
        lblDate.setText(p.getDateDebut() != null ? p.getDateDebut().toString() : "??");
        lblDescription.setText(p.getDescription());

        // Tech tags
        flowPaneTech.getChildren().clear();
        if (p.getTechnologies() != null && !p.getTechnologies().isEmpty()) {
            String[] techs = p.getTechnologies().split(",");
            for (String s : techs) {
                Label tag = new Label(s.trim());
                tag.setStyle(
                        "-fx-background-color: #f1f2f6; -fx-text-fill: #102c59; -fx-font-size: 10px; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-weight: bold;");
                flowPaneTech.getChildren().add(tag);
            }
        }

        // Resources
        try {
            List<Ressource> res = ressourceService.getByProjetId(p.getId());
            vboxRessources.getChildren().clear();
            if (res.isEmpty()) {
                vboxRessources.getChildren().add(new Label("Aucune ressource"));
            } else {
                for (Ressource r : res) {
                    HBox row = new HBox(10);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    row.setStyle("-fx-background-color: white; -fx-padding: 8; -fx-background-radius: 5;");

                    Label name = new Label(r.getNom());
                    name.setStyle("-fx-font-size: 12px; -fx-text-fill: #102c59;");

                    Region sp = new Region();
                    HBox.setHgrow(sp, javafx.scene.layout.Priority.ALWAYS);

                    Label typeTag = new Label(r.getTypeRessource());
                    typeTag.setStyle(
                            "-fx-background-color: #e8f0fe; -fx-text-fill: #1a73e8; -fx-font-size: 9px; -fx-padding: 2 6; -fx-background-radius: 3; -fx-font-weight: bold;");

                    row.getChildren().addAll(name, sp, typeTag);
                    vboxRessources.getChildren().add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void modifierProjet() {
        mainController.modifierProjetSpecific(projet);
    }

    @FXML
    private void supprimerProjet() {
        mainController.supprimerProjetSpecific(projet);
    }
}
