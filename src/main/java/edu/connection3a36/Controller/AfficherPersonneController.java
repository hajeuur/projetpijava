package edu.connection3a36.Controller;
import edu.connection3a36.entities.Personne;
import edu.connection3a36.services.PersonneService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;

public class AfficherPersonneController {


    @FXML
    private TableColumn<Personne, String> nomCol;

    @FXML
    private TableColumn<Personne, String> prenomCol;

    @FXML
    private TableView<Personne> tabTF;

    @FXML
    private Label testLabel;

    public AfficherPersonneController() throws SQLException {
    }

    public void initialize() throws Exception {
        PersonneService sc = new PersonneService();
        ObservableList<Personne> obs = FXCollections.observableArrayList(sc.getData());
        tabTF.setItems(obs);
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom") );
        prenomCol.setCellValueFactory(new PropertyValueFactory<>("prenom") );
        testLabel.setText("hello");

    }

}


