import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Label;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;

import java.time.LocalDate;
import java.util.ArrayList;
import javafx.scene.text.Text;
import javafx.scene.control.Button;

public class ControllerBuecherVerwaltung {
    private Bibliothek model;
    private Buch selectedBuch;
    private boolean bearbeitenAktiv = false;
    private boolean neuAktiv = false;

    @FXML
    private TextField searchBar;

    @FXML
    private TableView<Buch> buecherTabelle;

    @FXML
    private TableColumn<Buch, String> isbnSpalte;

    @FXML
    private TableColumn<Buch, String> titelSpalte;

    @FXML
    private TextField isbnFeld;

    @FXML
    private TextField titelFeld;

    @FXML
    private TextField autorFeld;

    @FXML
    private TextField jahrFeld;

    @FXML
    private TextArea beschreibungFeld;

    @FXML
    private Text statusText;

    @FXML
    private Button bearbeitenButton;

    @FXML
    private Button zurueckButton;

    @FXML
    private Button neuButton;

    @FXML
    private Button entfernenButton;

    @FXML
    private TableView<tabelleZeile> verlaufTabelle;

    @FXML
    private TableColumn<tabelleZeile, String> verlaufNachnameSpalte;

    @FXML
    private TableColumn<tabelleZeile, String> verlaufVornameSpalte;

    @FXML
    private TableColumn<tabelleZeile, String> verlaufEmailSpalte;

    @FXML
    private TableColumn<tabelleZeile, String> verlaufAusgabeSpalte;

    @FXML
    private TableColumn<tabelleZeile, String> verlaufRueckgabeSpalte;

    public static class tabelleZeile {
        private String nachname;
        private String vorname;
        private String email;
        private String ausgabe;
        private String rueckgabe;

        public tabelleZeile(String nachname, String vorname, String email, String ausgabe, String rueckgabe) {
            this.nachname = nachname;
            this.vorname = vorname;
            this.email = email;
            this.ausgabe = ausgabe;
            this.rueckgabe = rueckgabe;
        }

        public String getNachname() {
            return nachname;
        }

        public String getVorname() {
            return vorname;
        }

        public String getEmail() {
            return email;
        }

        public String getAusgabe() {
            return ausgabe;
        }

        public String getRueckgabe() {
            return rueckgabe;
        }
    }

    public ControllerBuecherVerwaltung() {

    }

    public void initialize() {
        isbnSpalte.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        titelSpalte.setCellValueFactory(new PropertyValueFactory<>("titel"));
        buecherTabelle.setPlaceholder(new Label("Keine Bücher gefunden"));
        bearbeitenButton.setDisable(true);
        entfernenButton.setDisable(true);

        verlaufNachnameSpalte.setCellValueFactory(new PropertyValueFactory<>("nachname"));
        verlaufVornameSpalte.setCellValueFactory(new PropertyValueFactory<>("vorname"));
        verlaufEmailSpalte.setCellValueFactory(new PropertyValueFactory<>("email"));
        verlaufAusgabeSpalte.setCellValueFactory(new PropertyValueFactory<>("ausgabe"));
        verlaufRueckgabeSpalte.setCellValueFactory(new PropertyValueFactory<>("rueckgabe"));
    }

    public void setModel(Bibliothek model) {
        this.model = model;
    }

    @FXML
    public void suchen() {

        String suchbegriff = searchBar.getText();
        ArrayList<Buch> ergebnisse = model.buecherSuchen(suchbegriff);

        buecherTabelle.getItems().clear();
        if (ergebnisse != null) {
            buecherTabelle.getItems().addAll(ergebnisse);
        }
    }

    public void toStartseite(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("scenes/LehrerStartseite.fxml"));
            Parent root = loader.load();
            ControllerLehrerStartseite controller = loader.getController();
            controller.setModel(model);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void selectBuch() {
        if (!bearbeitenAktiv) {

            selectedBuch = buecherTabelle.getSelectionModel().getSelectedItem();
            if (selectedBuch != null) {
                isbnFeld.setText(selectedBuch.getIsbn());
                titelFeld.setText(selectedBuch.getTitel());
                autorFeld.setText(selectedBuch.getAutor());
                jahrFeld.setText(selectedBuch.getErscheinungsjahr());
                beschreibungFeld.setText(selectedBuch.getBeschreibung());
                String status = selectedBuch.getStatus();
                if (status.equals("verfuegbar")) {
                    status = "verfügbar";
                }
                if (status.equals("verliehen")) {
                    status += " an " + model.getVerleihSchuelerName(selectedBuch.getIsbn());
                }
                statusText.setText("aktueller Status: " + status);
                bearbeitenButton.setDisable(false);
                if (selectedBuch.getStatus().equals("entfernt")) {
                    entfernenButton.setText("freigeben");
                } else {
                    entfernenButton.setText("entfernen");
                }
                if (selectedBuch.getStatus().equals("entfernt")) {
                    entfernenButton.setText("freischalten");
                } else {
                    if (selectedBuch.getStatus().equals("verliehen")) {
                        entfernenButton.setText("entfernen (Buch gilt automatisch als zurückgegeben)");
                    } else {
                        entfernenButton.setText("entfernen");
                    }
                }
                entfernenButton.setDisable(false);
                loadVerlaufTabelle();
            }
        }
    }

    @FXML
    public void bearbeiten(ActionEvent event) {
        if (selectedBuch == null && !neuAktiv) {
            return;
        }
        if (bearbeitenAktiv) {
            bearbeitenAktiv = false;
            bearbeitenButton.setText("bearbeiten");
            titelFeld.setEditable(false);
            autorFeld.setEditable(false);
            jahrFeld.setEditable(false);
            zurueckButton.setDisable(false);
            neuButton.setDisable(false);
            beschreibungFeld.setEditable(false);

            entfernenButton.setDisable(false);
            try {
                int jahr = Integer.parseInt(jahrFeld.getText().trim());
                model.buchBearbeiten(isbnFeld.getText(), titelFeld.getText(), autorFeld.getText(),
                        jahr, beschreibungFeld.getText(),
                        selectedBuch.getStatus());
                suchen();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        } else {
            if (neuAktiv) {
                try {
                    int jahr = Integer.parseInt(jahrFeld.getText().trim());
                    model.buchHinzufuegen(isbnFeld.getText(), titelFeld.getText(), autorFeld.getText(),
                            jahr, beschreibungFeld.getText());
                    suchen();
                    neuAktiv = false;
                    bearbeitenButton.setText("bearbeiten");
                    titelFeld.setEditable(false);
                    autorFeld.setEditable(false);
                    jahrFeld.setEditable(false);
                    beschreibungFeld.setEditable(false);
                    zurueckButton.setDisable(false);
                    neuButton.setText("neu");
                    entfernenButton.setDisable(false);
                    titelFeld.clear();
                    autorFeld.clear();
                    jahrFeld.clear();
                    beschreibungFeld.clear();
                    isbnFeld.clear();
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            } else {
                bearbeitenAktiv = true;
                bearbeitenButton.setText("speichern");
                titelFeld.setEditable(true);
                autorFeld.setEditable(true);
                jahrFeld.setEditable(true);
                beschreibungFeld.setEditable(true);
                zurueckButton.setDisable(true);
                neuButton.setDisable(true);
                entfernenButton.setDisable(true);
            }
        }
    }

    @FXML
    public void entfernen() {
        if (selectedBuch == null) {
            return;
        }

        String savedIsbn = selectedBuch.getIsbn();

        if (!selectedBuch.getStatus().equals("entfernt")) {
            model.buchLoeschen(selectedBuch.getIsbn());
        } else {
            model.buchFreigeben(selectedBuch.getIsbn());
        }

        suchen();

        for (Buch b : buecherTabelle.getItems()) {
            if (b.getIsbn().equals(savedIsbn)) {
                buecherTabelle.getSelectionModel().select(b);
                break;
            }
        }

        selectBuch();
    }

    public void buchErstellen() {
        if (!neuAktiv) {

            neuButton.setText("abbrechen");
            entfernenButton.setDisable(true);
            bearbeitenButton.setDisable(false);
            bearbeitenButton.setText("speichern");
            isbnFeld.clear();
            titelFeld.clear();
            autorFeld.clear();
            jahrFeld.clear();
            beschreibungFeld.clear();
            isbnFeld.setEditable(true);
            titelFeld.setEditable(true);
            autorFeld.setEditable(true);
            jahrFeld.setEditable(true);
            beschreibungFeld.setEditable(true);
            zurueckButton.setDisable(true);
            neuAktiv = true;
        } else {
            neuButton.setText("neu");
            entfernenButton.setDisable(true);
            bearbeitenButton.setText("speichern");
            isbnFeld.clear();
            titelFeld.clear();
            autorFeld.clear();
            jahrFeld.clear();
            beschreibungFeld.clear();
            isbnFeld.setEditable(false);
            titelFeld.setEditable(false);
            autorFeld.setEditable(false);
            jahrFeld.setEditable(false);
            beschreibungFeld.setEditable(false);
            zurueckButton.setDisable(false);
            neuAktiv = false;
            bearbeitenButton.setDisable(true);
        }
    }

    public void loadVerlaufTabelle() {
        QueryResult result = model.getBuchVerlauf(selectedBuch.getIsbn());
        if (result != null) {
            verlaufTabelle.getItems().clear();
            String[][] data = result.getData();
            for (int i = 0; i < result.getRowCount(); i++) {
                if (data[i].length >= 5) {
                    String nachname = data[i][0];
                    String vorname = data[i][1];
                    String email = data[i][2];
                    String ausgabe = data[i][3];
                    String rueckgabe = data[i][4];

                    tabelleZeile zeile = new tabelleZeile(nachname, vorname, email, ausgabe, rueckgabe);
                    verlaufTabelle.getItems().add(zeile);

                }
            }
        }
    }
}