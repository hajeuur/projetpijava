package com.mentorai;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class MainFX extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showMainMenu();
    }

    private void showMainMenu() {

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 50; -fx-background-color: #f4f6f9;");

        // 🔵 Planning Button
        Button btnPlanning = new Button("Ouvrir Planning");
        btnPlanning.setStyle("-fx-font-size: 16px; -fx-padding: 10 20; -fx-cursor: hand;");
        btnPlanning.setOnAction(e -> loadView("/views/planning.fxml", "MentorAI - Planning"));

        // 🟣 Humeur Button
        Button btnHumeur = new Button("Ouvrir Humeur");
        btnHumeur.setStyle("-fx-font-size: 16px; -fx-padding: 10 20; -fx-cursor: hand;");
        btnHumeur.setOnAction(e -> loadView("/views/humeur.fxml", "MentorAI - Humeur"));

        // 🟢 Carnet Button (NEW)
        Button btnCarnet = new Button("Ouvrir Carnet");
        btnCarnet.setStyle("-fx-font-size: 16px; -fx-padding: 10 20; -fx-cursor: hand;");
        btnCarnet.setOnAction(e -> loadView("/views/carnet.fxml", "MentorAI - Carnet"));

        // Add all buttons
        root.getChildren().addAll(btnPlanning, btnHumeur, btnCarnet);

        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("MentorAI - Test Menu");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadView(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1280, 800);
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("❌ Failed to load view: " + fxmlPath);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}