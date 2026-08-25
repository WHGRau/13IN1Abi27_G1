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

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.time.LocalDate;
import java.util.ArrayList;
import javafx.scene.text.Text;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.geometry.Pos;
import javafx.application.Platform;

public class ControllerBuecherVerwaltung {
    private Bibliothek model;
    private Buch selectedBuch;
    private boolean bearbeitenAktiv = false;
    private boolean neuAktiv = false;
    
    private String apiQuelle = "openlibrary"; // oder "google"
    private String googleApiKey = "AIzaSyA59yFeATSjQo9pIgPAzamkbUWUzZ6zLtI";
    private String barcodePuffer = "";
    private long letzteTastenZeit = 0;

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
    
    @FXML
    private StackPane background;

    @FXML
    private Text errorText;

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
        
        Platform.runLater(() ->{
            Scene scene = background.getScene();
            if(scene != null){
                final double targetWidth = 1920.0;
                final double targetHeight = 1080.0;
        
                Scale scale = new Scale(1, 1, 0, 0);
                scale.xProperty().bind(scene.widthProperty().divide(targetWidth));
                scale.yProperty().bind(scene.heightProperty().divide(targetHeight));
                
                
                background.getTransforms().clear();
                background.getTransforms().add(scale);
                
                background.setPrefWidth(targetWidth);
                background.setPrefHeight(targetHeight);
                background.setMaxWidth(targetWidth);
                background.setMaxHeight(targetHeight);
                
                StackPane.setAlignment(background, Pos.TOP_LEFT);
                
                scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, event -> {
                    if (neuAktiv) {
                        long jetzt = System.currentTimeMillis();
                        if (jetzt - letzteTastenZeit > 100) {
                            barcodePuffer = "";
                        }
                        if (event.getCharacter().matches("[0-9]")) {
                            barcodePuffer += event.getCharacter();
                        }
                        letzteTastenZeit = jetzt;
                    }
                });
                
                scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                    if (neuAktiv && event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                        if (barcodePuffer.length() >= 10) {
                            isbnFeld.setText(barcodePuffer);
                            
                            if (titelFeld.getText().equals(barcodePuffer)) titelFeld.clear();
                            if (autorFeld.getText().equals(barcodePuffer)) autorFeld.clear();
                            if (jahrFeld.getText().equals(barcodePuffer)) jahrFeld.clear();
                            
                            buchDatenAbrufen(barcodePuffer);
                            barcodePuffer = "";
                        }
                    }
                });
            }
        });
    }

    public void setModel(Bibliothek model) {
        this.model = model;
    }

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
            Scene scene = new Scene(root);
            scene.setFill(Color.web("#E9E9D3"));
            stage.setScene(scene);
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
                errorText.setText("Fehler: Ungültiges Jahr");
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
                    errorText.setText("Fehler: Ungültiges Jahr");
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
            Platform.runLater(() -> isbnFeld.requestFocus());
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

    private String pruefeFelderAufIsbn() {
        TextField[] felder = {isbnFeld, titelFeld, autorFeld, jahrFeld};
        for (TextField feld : felder) {
            String text = feld.getText().trim();
            if (text.length() >= 10 && text.matches("[0-9]+")) {
                feld.clear();
                isbnFeld.setText(text);
                return text;
            }
        }
        return "";
    }

    public void buchDatenAbrufen(String isbn) {
        try {
            String urlText = "";
            if (apiQuelle.equals("google")) {
                urlText = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn;
                if (!googleApiKey.isEmpty()) {
                    urlText += "&key=" + googleApiKey;
                }
            } else {
                urlText = "https://openlibrary.org/api/books?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data";
            }
            
            URL url = new URL(urlText);
            HttpURLConnection verbindung = (HttpURLConnection) url.openConnection();
            verbindung.setRequestMethod("GET");
            
            BufferedReader leser = new BufferedReader(new InputStreamReader(verbindung.getInputStream()));
            String zeile;
            String json = "";
            while ((zeile = leser.readLine()) != null) {
                json += zeile;
            }
            leser.close();
            
            String titel = wertAuslesen(json, "title");
            if (!titel.isEmpty()) titelFeld.setText(titel);
            
            if (apiQuelle.equals("google")) {
                String autor = arrayWertAuslesen(json, "authors");
                if (!autor.isEmpty()) autorFeld.setText(autor);
                
                String datum = wertAuslesen(json, "publishedDate");
                if (datum.length() >= 4) jahrFeld.setText(datum.substring(0, 4));
                
                String beschreibung = wertAuslesen(json, "description");
                if (!beschreibung.isEmpty()) beschreibungFeld.setText(beschreibung);
            } else {
                String autor = wertAuslesen(json, "name");
                if (!autor.isEmpty()) autorFeld.setText(autor);
                
                String datum = wertAuslesen(json, "publish_date");
                if (datum.length() >= 4) jahrFeld.setText(datum.substring(datum.length() - 4));
                
                String beschreibung = wertAuslesen(json, "notes");
                if (!beschreibung.isEmpty()) beschreibungFeld.setText(beschreibung);
            }
            
            
        } catch (Exception e) {
            errorText.setText("Fehler beim Abrufen der Buchdaten");
        }
    }

    private String wertAuslesen(String json, String schluessel) {
        String suche1 = "\"" + schluessel + "\":\"";
        String suche2 = "\"" + schluessel + "\": \"";
        
        int startPos = json.indexOf(suche1);
        if (startPos != -1) {
            startPos += suche1.length();
        } else {
            startPos = json.indexOf(suche2);
            if (startPos != -1) startPos += suche2.length();
        }
        
        if (startPos != -1) {
            int endPos = json.indexOf("\"", startPos);
            if (endPos != -1) {
                return json.substring(startPos, endPos);
            }
        }
        return "";
    }

    private String arrayWertAuslesen(String json, String schluessel) {
        int startPos = json.indexOf("\"" + schluessel + "\"");
        if (startPos != -1) {
            int klammerStart = json.indexOf("[", startPos);
            int anfuehrungszeichenStart = json.indexOf("\"", klammerStart);
            if (anfuehrungszeichenStart != -1) {
                int anfuehrungszeichenEnde = json.indexOf("\"", anfuehrungszeichenStart + 1);
                if (anfuehrungszeichenEnde != -1) {
                    return json.substring(anfuehrungszeichenStart + 1, anfuehrungszeichenEnde);
                }
            }
        }
        return "";
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

    public void errorTextZuruecksetzen(){
        errorText.setText("");
    }
}