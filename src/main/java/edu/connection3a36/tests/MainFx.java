package edu.connection3a36.tests;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainFx extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Connexion.fxml"));
        Parent root = loader.load();
        Scene sc = new Scene(root);
        primaryStage.setTitle("MentorAI - Connexion");
        primaryStage.setScene(sc);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }
}
