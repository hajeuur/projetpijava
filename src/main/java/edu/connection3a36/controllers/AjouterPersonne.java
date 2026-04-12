package edu.connection3a36.controllers;
import edu.connection3a36.entities.Personne;
import edu.connection3a36.services.PersonneService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.SQLException;

public class AjouterPersonne {

        @FXML
        private TextField nomTextField;

        @FXML
        private TextField prenomTextField;

        @FXML
        void Ajouter(ActionEvent event) throws SQLException {
            PersonneService sc = new PersonneService();
            Personne p = new Personne(nomTextField.getText(), prenomTextField.getText());
            sc.addEntity(p);
        }
        @FXML
        void afficher(ActionEvent event) throws IOException {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherPersonne.fxml"));
            Parent root = loader.load();
            nomTextField.getScene().setRoot(root);
    }
    }


