package edu.connection3a36.controllers;

import edu.connection3a36.entities.Personne;
import edu.connection3a36.services.PersonneService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class AfficherPersonne {

    @FXML
    private TableColumn<Personne, String> nomcol;

    @FXML
    private TableColumn<Personne, String> prenomcol;

    @FXML
    private TableView<Personne> tabpersonne;
    @FXML
    private Label testlabel;

    public void initialize () throws Exception {
        PersonneService sc = new PersonneService();
        ObservableList<Personne> obs = FXCollections.observableArrayList(sc.getData());
        tabpersonne.setItems(obs);
        nomcol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        prenomcol.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        testlabel.setText("hello");
    }
}
