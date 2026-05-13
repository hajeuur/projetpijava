package com.mentorai.controllers;

import com.mentorai.models.ApprentissageServiceItem;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ApprentissageController implements Initializable {

    @FXML private TextField searchField;
    @FXML private HBox recommendedContainer;
    @FXML private HBox recentContainer;
    @FXML private FlowPane contentGenGrid;
    @FXML private FlowPane analysisGrid;
    @FXML private FlowPane productivityGrid;
    @FXML private FlowPane resourcesGrid;

    private List<ApprentissageServiceItem> allServices = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadStaticData();
        renderServices(allServices);
    }

    private void loadStaticData() {
        // Génération de contenu
        allServices.add(new ApprentissageServiceItem("Pack d'étude intelligent", "Générez un pack d'étude complet et personnalisé selon votre profil et votre humeur.", "Génération de contenu"));
        allServices.add(new ApprentissageServiceItem("Générer un résumé", "Créez un résumé professionnel de vos notes.", "Génération de contenu"));
        allServices.add(new ApprentissageServiceItem("Générer des flashcards", "Convertissez vos cours en fiches de révision.", "Génération de contenu"));
        allServices.add(new ApprentissageServiceItem("Générer un quiz", "Testez vos connaissances avec des quiz IA.", "Génération de contenu"));
        allServices.add(new ApprentissageServiceItem("Expliquer un sujet", "Obtenez une explication simplifiée des concepts complexes.", "Génération de contenu"));

        // Analyse & Intelligence
        allServices.add(new ApprentissageServiceItem("Que réviser aujourd'hui ?", "Recommandations d'étude personnalisées.", "Analyse & Intelligence"));
        allServices.add(new ApprentissageServiceItem("Analyser mes performances", "Visualisez votre progression d'apprentissage.", "Analyse & Intelligence"));

        // Productivité & Planification
        allServices.add(new ApprentissageServiceItem("Optimiser mon planning", "Trouvez les meilleurs créneaux horaires pour étudier.", "Productivité & Planification"));
        allServices.add(new ApprentissageServiceItem("Durée de session conseillée", "Basé sur la complexité du sujet et votre concentration.", "Productivité & Planification"));

        // Ressources
        allServices.add(new ApprentissageServiceItem("Trouver des vidéos", "Localisez des vidéos éducatives pertinentes.", "Ressources"));
        allServices.add(new ApprentissageServiceItem("Trouver des résumés", "Recherchez des résumés d'articles validés.", "Ressources"));
    }

    private void renderServices(List<ApprentissageServiceItem> services) {
        // Clear all grids
        recommendedContainer.getChildren().clear();
        recentContainer.getChildren().clear();
        contentGenGrid.getChildren().clear();
        analysisGrid.getChildren().clear();
        productivityGrid.getChildren().clear();
        resourcesGrid.getChildren().clear();

        // Recommended (static selection for UI demo)
        if (services.size() >= 2) {
            recommendedContainer.getChildren().add(createServiceCard(services.get(0)));
            recommendedContainer.getChildren().add(createServiceCard(services.get(4)));
        }

        // Recent (static selection for UI demo)
        if (services.size() >= 3) {
            recentContainer.getChildren().add(createServiceCard(services.get(1)));
            recentContainer.getChildren().add(createServiceCard(services.get(6)));
        }

        // Catégorisé
        for (ApprentissageServiceItem item : services) {
            VBox card = createServiceCard(item);
            switch (item.getCategory()) {
                case "Génération de contenu" -> contentGenGrid.getChildren().add(card);
                case "Analyse & Intelligence" -> analysisGrid.getChildren().add(card);
                case "Productivité & Planification" -> productivityGrid.getChildren().add(card);
                case "Ressources" -> resourcesGrid.getChildren().add(card);
            }
        }
    }

    private VBox createServiceCard(ApprentissageServiceItem item) {
        VBox card = new VBox();
        card.getStyleClass().add("service-card");
        card.setPrefWidth(220);
        card.setMinWidth(220);

        Label title = new Label(item.getTitle());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);

        Label desc = new Label(item.getDescription());
        desc.getStyleClass().add("card-description");
        desc.setWrapText(true);
        desc.setMinHeight(50);

        Button openBtn = new Button("Ouvrir");
        openBtn.getStyleClass().add("btn-open");
        openBtn.setOnAction(e -> {
            edu.connection3a36.controllers.MainController main =
                    edu.connection3a36.controllers.MainController.getInstance();
            if ("Analyser mes performances".equals(item.getTitle())) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/analyse.fxml"));
                    Parent root = loader.load();
                    if (main != null) main.loadInContentArea(root);
                    else openBtn.getScene().setRoot(root);
                } catch (IOException ex) { ex.printStackTrace(); }
            } else if ("Pack d'étude intelligent".equals(item.getTitle())) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/pack_etude.fxml"));
                    Parent root = loader.load();
                    if (main != null) main.loadInContentArea(root);
                    else openBtn.getScene().setRoot(root);
                } catch (IOException ex) { ex.printStackTrace(); }
            } else if ("Que réviser aujourd'hui ?".equals(item.getTitle())) {
                showIntelligenceInsights();
            } else {
                System.out.println("Service cliqué : " + item.getTitle());
            }
        });

        card.getChildren().addAll(title, desc, openBtn);
        return card;
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        List<ApprentissageServiceItem> filtered = allServices.stream()
                .filter(s -> s.getTitle().toLowerCase().contains(query) || 
                            s.getDescription().toLowerCase().contains(query))
                .collect(Collectors.toList());
        
        renderServices(filtered);
    }

    private void showIntelligenceInsights() {
        int userId = 1;
        try {
            if (edu.connection3a36.tools.SessionManager.getCurrentUser() != null) {
                userId = edu.connection3a36.tools.SessionManager.getCurrentUser().getId();
            }
        } catch (Exception e) {}

        com.mentorai.services.IntelligenceService engine = new com.mentorai.services.IntelligenceService();
        java.util.List<com.mentorai.services.IntelligenceService.PrioritySubject> subjects = engine.getPrioritySubjects(userId);
        com.mentorai.services.IntelligenceService.UserProfile profile = engine.getUserProfile(userId);

        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Intelligence & Recommandations");
        dialog.setHeaderText(null); // Remove default header to style it custom

        // Profile Section
        VBox profileBox = new VBox(5);
        profileBox.setStyle("-fx-background-color: #112240; -fx-padding: 15; -fx-background-radius: 8;");
        Label profTitle = new Label(profile.type);
        profTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #64ffda; -fx-font-size: 18px;");
        Label profSugg = new Label(profile.suggestion);
        profSugg.setStyle("-fx-text-fill: white; -fx-wrap-text: true; -fx-font-size: 14px;");
        Label profDet = new Label(profile.details);
        profDet.setStyle("-fx-text-fill: #8892b0; -fx-wrap-text: true; -fx-font-size: 12px;");
        profileBox.getChildren().addAll(profTitle, profSugg, profDet);

        // Priority Section
        VBox priorityBox = new VBox(10);
        Label prioTitle = new Label("🎯 Que réviser aujourd'hui ?");
        prioTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10 0 0 0;");
        priorityBox.getChildren().add(prioTitle);

        if (subjects.isEmpty()) {
            Label empty = new Label("Aucune donnée de planification trouvée. Commencez à planifier vos sessions !");
            empty.setStyle("-fx-text-fill: #8892b0; -fx-wrap-text: true;");
            priorityBox.getChildren().add(empty);
        } else {
            for (com.mentorai.services.IntelligenceService.PrioritySubject sub : subjects) {
                VBox subBox = new VBox(2);
                subBox.setStyle("-fx-background-color: #233554; -fx-padding: 10; -fx-background-radius: 5;");
                
                String color = sub.isUrgent ? "#ff6b6b" : (sub.score >= 60 ? "#f0a500" : "#64ffda");
                Label name = new Label(sub.matiere + " (Score: " + sub.score + "/100)");
                name.setStyle("-fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-font-size: 14px;");
                
                Label msg = new Label(sub.message);
                msg.setStyle("-fx-text-fill: #ccd6f6; -fx-wrap-text: true;");
                
                subBox.getChildren().addAll(name, msg);
                priorityBox.getChildren().add(subBox);
            }
        }

        VBox content = new VBox(20);
        content.setStyle("-fx-background-color: #0a192f; -fx-padding: 20;");
        content.getChildren().addAll(profileBox, priorityBox);

        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(450);
        scroll.setPrefViewportWidth(400);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #0a192f;");

        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color: #0a192f;");

        javafx.scene.Node closeBtn = dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CLOSE);
        if (closeBtn != null) {
            closeBtn.setStyle("-fx-background-color: #64ffda; -fx-text-fill: #0a192f; -fx-font-weight: bold;");
        }

        dialog.showAndWait();
    }
}
