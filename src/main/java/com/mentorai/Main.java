package com.mentorai;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    private static final String MODE = "USER";

    @Override
    public void start(Stage stage) throws Exception {
        String fxmlPath;
        String titre;

        if (MODE.equals("ADMIN")) {
            fxmlPath = "/fxml/AdminFeedback.fxml";
            titre    = "MentorAI - Gestion des Feedbacks (Admin)";
        } else {
            fxmlPath = "/fxml/AjouterFeedback.fxml";
            titre    = "MentorAI - Donner un feedback";
        }

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(fxmlPath)
        );
        VBox root = loader.load();
        stage.setTitle(titre);
        Scene scene = new Scene(root, 1100, 750);
        stage.setScene(scene);
        stage.setMaximized(true); // ✅ Ouvre en plein écran
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}