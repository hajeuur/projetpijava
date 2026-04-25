package edu.connection3a36.controllers;

import edu.connection3a36.entities.Projet;
import edu.connection3a36.entities.Ressource;
import edu.connection3a36.services.RessourceService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

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

                    // Add Edit/Delete buttons for each resource
                    Button btnMod = new Button("✏️");
                    btnMod.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #102c59;");
                    btnMod.setOnAction(e -> modifierRessourceSpecific(r));

                    Button btnSupp = new Button("🗑");
                    btnSupp.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #e74c3c;");
                    btnSupp.setOnAction(e -> supprimerRessourceSpecific(r));

                    row.getChildren().addAll(btnMod, btnSupp);
                    vboxRessources.getChildren().add(row);

                    // YouTube Preview if Video
                    if ("VIDEO".equalsIgnoreCase(r.getTypeRessource()) && r.getUrlRessource() != null) {
                        String videoId = extractYoutubeId(r.getUrlRessource());
                        if (videoId != null) {
                            WebView webView = new WebView();
                            webView.setPrefHeight(200);
                            webView.setPrefWidth(350);
                            String embedUrl = "https://www.youtube.com/embed/" + videoId;
                            webView.getEngine().load(embedUrl);

                            VBox videoBox = new VBox(webView);
                            videoBox.setStyle(
                                    "-fx-padding: 10; -fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-border-color: #dcdde1; -fx-border-radius: 10;");
                            vboxRessources.getChildren().add(videoBox);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String extractYoutubeId(String url) {
        String pattern = "(?<=watch\\?v=|/videos/|embed/|youtu.be/|/v/|/e/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%2F|youtu.be%2F|%2Fv%2F)[^#&?\\n]*";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private void modifierRessourceSpecific(Ressource r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierRessource.fxml"));
            Parent view = loader.load();
            ModifierRessourceController ctrl = loader.getController();
            ctrl.initData(r, projet);
            MainController.getInstance().loadInContentArea(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void supprimerRessourceSpecific(Ressource r) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer la ressource \"" + r.getNom() + "\" ?",
                ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                ressourceService.deleteEntity(r);
                setData(projet, mainController); // Refresh card
            } catch (SQLException e) {
                e.printStackTrace();
            }
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
