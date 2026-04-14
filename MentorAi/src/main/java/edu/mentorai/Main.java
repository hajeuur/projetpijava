package edu.mentorai;

import edu.mentorai.Controller.ObjectifListController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/objectif_list.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 750);
        ObjectifListController ctrl = (ObjectifListController) loader.getController();
        ctrl.setUtilisateurId(1);
        ctrl.loadData();
        stage.setTitle("MentorAI - Mes Objectifs");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}