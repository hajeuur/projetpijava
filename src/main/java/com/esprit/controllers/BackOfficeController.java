package com.esprit.controllers;

import com.esprit.dao.UtilisateurDAO;
import com.esprit.models.Utilisateur;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class BackOfficeController implements Initializable {

    @FXML private TableView<Utilisateur> tableView;
    @FXML private TableColumn<Utilisateur, Integer> colId;
    @FXML private TableColumn<Utilisateur, String> colNom;
    @FXML private TableColumn<Utilisateur, String> colPrenom;
    @FXML private TableColumn<Utilisateur, String> colEmail;
    @FXML private TableColumn<Utilisateur, String> colRole;
    @FXML private TableColumn<Utilisateur, String> colStatus;
    @FXML private TableColumn<Utilisateur, Void> colActions;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label countLabel;

    private final UtilisateurDAO dao = new UtilisateurDAO();
    private ObservableList<Utilisateur> data = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Options de tri
        sortCombo.getItems().addAll(
                "Nom (A-Z)", "Nom (Z-A)",
                "Prénom (A-Z)", "Prénom (Z-A)",
                "Rôle", "Status",
                "ID croissant", "ID décroissant"
        );

        ajouterColonneActions();
        chargerDonnees();
    }

    private void chargerDonnees() {
        data.clear();
        data.addAll(dao.getAll());
        tableView.setItems(data);
        updateCount();
    }

    private void updateCount() {
        countLabel.setText("Affichage de " + data.size() + " utilisateur(s)");
    }

    @FXML
    public void handleSort() {
        String selected = sortCombo.getValue();
        if (selected == null) return;

        switch (selected) {
            case "Nom (A-Z)" -> data.sort(Comparator.comparing(u -> u.getNom().toLowerCase()));
            case "Nom (Z-A)" -> data.sort((a, b) -> b.getNom().toLowerCase().compareTo(a.getNom().toLowerCase()));
            case "Prénom (A-Z)" -> data.sort(Comparator.comparing(u -> u.getPrenom().toLowerCase()));
            case "Prénom (Z-A)" -> data.sort((a, b) -> b.getPrenom().toLowerCase().compareTo(a.getPrenom().toLowerCase()));
            case "Rôle" -> data.sort(Comparator.comparing(u -> u.getRole().toLowerCase()));
            case "Status" -> data.sort(Comparator.comparing(u -> u.getStatus().toLowerCase()));
            case "ID croissant" -> data.sort(Comparator.comparingInt(Utilisateur::getId));
            case "ID décroissant" -> data.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        }
        tableView.setItems(data);
    }

    @FXML
    public void handleSearch() {
        String keyword = searchField.getText().toLowerCase();
        List<Utilisateur> tous = dao.getAll();
        data.clear();
        for (Utilisateur u : tous) {
            if (u.getNom().toLowerCase().contains(keyword) ||
                    u.getPrenom().toLowerCase().contains(keyword) ||
                    u.getEmail().toLowerCase().contains(keyword) ||
                    u.getRole().toLowerCase().contains(keyword)) {
                data.add(u);
            }
        }
        tableView.setItems(data);
        updateCount();
    }

    @FXML
    public void handleReset() {
        searchField.clear();
        sortCombo.setValue(null);
        chargerDonnees();
    }

    // ─── EXPORT EXCEL (vrai .xlsx sans librairie) ───
    @FXML
    public void handleExportExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer Excel");
        fileChooser.setInitialFileName("utilisateurs.xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichier Excel (*.xlsx)", "*.xlsx")
        );
        Stage stage = (Stage) tableView.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                generateXLSX(file);
                showAlert(Alert.AlertType.INFORMATION,
                        "Export réussi", "Fichier Excel exporté !\n" + file.getAbsolutePath());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur export : " + e.getMessage());
            }
        }
    }

    private void generateXLSX(File file) throws Exception {
        // Un fichier xlsx est un ZIP contenant des fichiers XML
        java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(file));

        // [Content_Types].xml
        zos.putNextEntry(new java.util.zip.ZipEntry("[Content_Types].xml"));
        String contentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
                "</Types>";
        zos.write(contentTypes.getBytes("UTF-8"));
        zos.closeEntry();

        // _rels/.rels
        zos.putNextEntry(new java.util.zip.ZipEntry("_rels/.rels"));
        String rels = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                "</Relationships>";
        zos.write(rels.getBytes("UTF-8"));
        zos.closeEntry();

        // xl/_rels/workbook.xml.rels
        zos.putNextEntry(new java.util.zip.ZipEntry("xl/_rels/workbook.xml.rels"));
        String wbRels = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" +
                "</Relationships>";
        zos.write(wbRels.getBytes("UTF-8"));
        zos.closeEntry();

        // xl/workbook.xml
        zos.putNextEntry(new java.util.zip.ZipEntry("xl/workbook.xml"));
        String workbook = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
                "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                "<sheets><sheet name=\"Utilisateurs\" sheetId=\"1\" r:id=\"rId1\"/></sheets>" +
                "</workbook>";
        zos.write(workbook.getBytes("UTF-8"));
        zos.closeEntry();

        // xl/styles.xml
        zos.putNextEntry(new java.util.zip.ZipEntry("xl/styles.xml"));
        String styles = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
                "<fonts><font><b/><sz val=\"11\"/></font><font><sz val=\"10\"/></font></fonts>" +
                "<fills><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill>" +
                "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1a2a4a\"/></patternFill></fill></fills>" +
                "<borders><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
                "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
                "<cellXfs>" +
                "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"2\" borderId=\"0\" xfId=\"0\"/>" + // style 0 : header
                "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" + // style 1 : normal
                "</cellXfs>" +
                "</styleSheet>";
        zos.write(styles.getBytes("UTF-8"));
        zos.closeEntry();

        // xl/worksheets/sheet1.xml
        zos.putNextEntry(new java.util.zip.ZipEntry("xl/worksheets/sheet1.xml"));
        StringBuilder sheet = new StringBuilder();
        sheet.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
                .append("<sheetData>");

        // En-tête
        sheet.append("<row r=\"1\">");
        String[] headers = {"ID", "Nom", "Prénom", "Email", "Rôle", "Status"};
        String[] cols = {"A", "B", "C", "D", "E", "F"};
        for (int i = 0; i < headers.length; i++) {
            sheet.append(String.format(
                    "<c r=\"%s1\" t=\"inlineStr\" s=\"0\"><is><t>%s</t></is></c>", cols[i], headers[i]));
        }
        sheet.append("</row>");

        // Données
        int rowNum = 2;
        for (Utilisateur u : data) {
            sheet.append("<row r=\"").append(rowNum).append("\">");
            sheet.append(String.format("<c r=\"A%d\" s=\"1\"><v>%d</v></c>", rowNum, u.getId()));
            sheet.append(xmlCell("B", rowNum, u.getNom()));
            sheet.append(xmlCell("C", rowNum, u.getPrenom()));
            sheet.append(xmlCell("D", rowNum, u.getEmail()));
            sheet.append(xmlCell("E", rowNum, u.getRole()));
            sheet.append(xmlCell("F", rowNum, u.getStatus()));
            sheet.append("</row>");
            rowNum++;
        }

        sheet.append("</sheetData></worksheet>");
        zos.write(sheet.toString().getBytes("UTF-8"));
        zos.closeEntry();
        zos.close();
    }

    private String xmlCell(String col, int row, String value) {
        String safe = value == null ? "" : value
                .replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
        return String.format("<c r=\"%s%d\" t=\"inlineStr\" s=\"1\"><is><t>%s</t></is></c>",
                col, row, safe);
    }

    @FXML
    public void handleExportPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer PDF");
        fileChooser.setInitialFileName("utilisateurs.pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichier PDF (*.pdf)", "*.pdf")
        );
        Stage stage = (Stage) tableView.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                generatePDF(file);
                showAlert(Alert.AlertType.INFORMATION,
                        "Export réussi", "PDF exporté !\n" + file.getAbsolutePath());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur PDF : " + e.getMessage());
            }
        }
    }

    private void generatePDF(File file) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file)) {

            // Contenu de la page
            StringBuilder stream = new StringBuilder();
            stream.append("BT\n");
            stream.append("/F1 14 Tf\n");
            stream.append("50 800 Td\n");
            stream.append("(Liste des Utilisateurs - MentorAI) Tj\n");
            stream.append("/F1 9 Tf\n");
            stream.append("0 -20 Td\n");
            stream.append("(Total : ").append(data.size()).append(" utilisateur(s)) Tj\n");
            stream.append("0 -20 Td\n");

            // En-tête tableau
            stream.append("/F1 10 Tf\n");
            stream.append("(ID      Nom             Prenom          Email                        Role          Status) Tj\n");
            stream.append("0 -5 Td\n");
            stream.append("(---------------------------------------------------------------------------------------------) Tj\n");
            stream.append("/F1 9 Tf\n");
            stream.append("0 -15 Td\n");

            // Données
            for (Utilisateur u : data) {
                String line = String.format("%-8d %-16s %-16s %-28s %-14s %-10s",
                        u.getId(),
                        truncate(u.getNom(), 15),
                        truncate(u.getPrenom(), 15),
                        truncate(u.getEmail(), 27),
                        truncate(u.getRole(), 13),
                        truncate(u.getStatus(), 10)
                );
                // Garde seulement ASCII basique
                line = line.replaceAll("[^\\x20-\\x7E]", "?");
                stream.append("(").append(line).append(") Tj\n");
                stream.append("0 -13 Td\n");
            }

            stream.append("ET\n");

            byte[] streamBytes = stream.toString().getBytes("ISO-8859-1");

            // Construction du PDF
            StringBuilder pdf = new StringBuilder();

            // Header
            pdf.append("%PDF-1.4\n");

            // Obj 1 - Catalog
            int[] offsets = new int[6];
            offsets[1] = pdf.length();
            pdf.append("1 0 obj\n<</Type /Catalog /Pages 2 0 R>>\nendobj\n");

            // Obj 2 - Pages
            offsets[2] = pdf.length();
            pdf.append("2 0 obj\n<</Type /Pages /Kids [3 0 R] /Count 1>>\nendobj\n");

            // Obj 3 - Page
            offsets[3] = pdf.length();
            pdf.append("3 0 obj\n");
            pdf.append("<</Type /Page /Parent 2 0 R\n");
            pdf.append("/MediaBox [0 0 595 842]\n");
            pdf.append("/Contents 4 0 R\n");
            pdf.append("/Resources <</Font <</F1 5 0 R>>>>>>\n");
            pdf.append(">>\nendobj\n");

            // Obj 4 - Stream
            offsets[4] = pdf.length();
            pdf.append("4 0 obj\n");
            pdf.append("<</Length ").append(streamBytes.length).append(">>\n");
            pdf.append("stream\n");

            byte[] pdfBytes = pdf.toString().getBytes("ISO-8859-1");
            fos.write(pdfBytes);
            fos.write(streamBytes);

            StringBuilder pdf2 = new StringBuilder();
            pdf2.append("\nendstream\nendobj\n");

            // Obj 5 - Font
            offsets[5] = pdfBytes.length + streamBytes.length + pdf2.length();
            pdf2.append("5 0 obj\n");
            pdf2.append("<</Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding>>\n");
            pdf2.append("endobj\n");

            // Xref
            int xrefOffset = pdfBytes.length + streamBytes.length + pdf2.length();
            pdf2.append("xref\n0 6\n");
            pdf2.append("0000000000 65535 f \n");
            pdf2.append(String.format("%010d 00000 n \n", offsets[1]));
            pdf2.append(String.format("%010d 00000 n \n", offsets[2]));
            pdf2.append(String.format("%010d 00000 n \n", offsets[3]));
            pdf2.append(String.format("%010d 00000 n \n", offsets[4]));
            pdf2.append(String.format("%010d 00000 n \n", offsets[5]));
            pdf2.append("trailer\n<</Size 6 /Root 1 0 R>>\n");
            pdf2.append("startxref\n").append(xrefOffset).append("\n%%EOF\n");

            fos.write(pdf2.toString().getBytes("ISO-8859-1"));
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void ajouterColonneActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            Button btnModifier = new Button("Modifier");
            Button btnDesactiver = new Button("Désactiver");
            Button btnVoir = new Button("Voir");
            HBox hbox = new HBox(5, btnVoir, btnModifier, btnDesactiver);

            {
                btnModifier.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 4;");
                btnDesactiver.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 4;");
                btnVoir.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-background-radius: 4;");

                btnModifier.setOnAction(e -> handleModifier(getTableView().getItems().get(getIndex())));
                btnDesactiver.setOnAction(e -> handleDesactiver(getTableView().getItems().get(getIndex())));
                btnVoir.setOnAction(e -> handleVoir(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }

    private void handleModifier(Utilisateur u) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/ModifierUser.fxml"));
            Parent root = loader.load();
            ModifierUserController controller = loader.getController();
            controller.setUtilisateur(u, this);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier utilisateur");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleVoir(Utilisateur u) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/ProfilUser.fxml"));
            Parent root = loader.load();
            ProfilUserController controller = loader.getController();
            controller.setUtilisateur(u);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Profil utilisateur");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleDesactiver(Utilisateur u) {
        u.setStatus(u.getStatus().equals("actif") ? "desactiver" : "actif");
        dao.modifier(u);
        chargerDonnees();
    }

    @FXML
    public void handleAjouter() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/Inscription.fxml"));
            Parent root = loader.load();
            InscriptionController controller = loader.getController();
            controller.setBackOfficeController(this);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Ajouter utilisateur");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tableView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void refreshTable() {
        chargerDonnees();
    }
}