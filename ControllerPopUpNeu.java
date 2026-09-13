
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
import javafx.scene.control.CheckBox;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;


/**
 * Controller-Klasse für das Popup-Fenster zur manuellen Eingabe oder
 * automatisierten API-Abfrage (ISBN) eines neuen Buches.
 */
public class ControllerPopUpNeu
{
    String isbn;
    Bibliothek model;
    
    @FXML
    private TextField isbnFeld;
    
    @FXML
    private TextField titelFeld;
    
    @FXML
    private TextField autorFeld;
    
    @FXML
    private TextField jahrFeld;
    
    @FXML
    private TextField alterFeld;
    
    @FXML
    private TextArea beschreibungFeld;
    
    @FXML
    private Button addKnopf;
    
    @FXML
    private Text errorText;
    
    /**
     * Setzt die anfängliche ISBN und das Model.
     * Ruft bei Vorhandensein einer ISBN direkt die API zur Datenabfrage auf.
     * 
     * @param isbn Die vorab eingegebene ISBN (kann null sein).
     * @param model Das aktuelle Bibliotheks-Model.
     */
    public void setISBN(String isbn, Bibliothek model){
        this.isbn = isbn;
        this.model = model;
        if(isbn != null){
            buchDatenAbrufen(isbn);
        }
        isbnFeld.setText(isbn);
    }

    /**
     * Initialisiert den Controller.
     */
    public void initialize(){
        
    }
    
    /**
     * Führt eine Buchdaten-Abfrage durch, wenn die Enter-Taste im ISBN-Feld gedrückt wird.
     * 
     * @param event Das KeyEvent-Objekt.
     */
    public void enter(KeyEvent event) {
        if (event.getCode().equals(KeyCode.ENTER)) {
            buchDatenAbrufen(isbnFeld.getText());
        }
    }
    
    /**
     * Überprüft die eingegebenen Daten und fügt das neue Buch der Bibliothek hinzu.
     * Schließt danach das Popup-Fenster.
     * 
     * @param event Das ActionEvent des Hinzufügen-Buttons.
     */
    public void hinzu(ActionEvent event){
        try {
            if (isbnFeld.getText().trim().isEmpty() || titelFeld.getText().trim().isEmpty()) {
                errorText.setText("Fehler: ISBN und Titel sind Pflichtfelder!");
                return;
            }
            Integer jahr = null;
            if (!jahrFeld.getText().trim().isEmpty()) {
                jahr = Integer.parseInt(jahrFeld.getText().trim());
            }
            String neueIsbn = isbnFeld.getText().trim();
            if (!neueIsbn.matches("[0-9]+") || neueIsbn.length() != 13
                            || (!neueIsbn.startsWith("978") && !neueIsbn.startsWith("979"))) {
                errorText.setText("ungültige ISBN! Bitte ohne Leerzeichen oder Bindestriche eingeben");
                return;
            }
            if (model.isbnVorhanden(neueIsbn)) {
                errorText.setText("Diese ISBN existiert bereits!");
                return;
            }
            model.buchHinzufuegen(neueIsbn, titelFeld.getText(), autorFeld.getText(),
                            jahr, beschreibungFeld.getText(), alterFeld.getText());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        
            stage.close();        
        } catch (NumberFormatException e) {
            errorText.setText("Fehler: Jahr muss eine Zahl sein");
            e.printStackTrace();
        }
    }
    
    /**
     * Ruft Metadaten zu einem Buch (Titel, Autor, etc.) von einer externen API
     * (Google Books oder OpenLibrary) basierend auf der ISBN ab und füllt die Felder aus.
     * 
     * @param isbn Die ISBN des abzufragenden Buches.
     */
    public void buchDatenAbrufen(String isbn) {
        try {
            String dbSetting = model.getEinstellung("buechersuche_datenbank");
            String apiQuelle = (dbSetting != null && dbSetting.equals("Google Books")) ? "google" : "openlibrary";
            String apiKeySetting = model.getEinstellung("buechersuche_api_key");
            String googleApiKey = apiKeySetting != null ? apiKeySetting : "";

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
            if (!titel.isEmpty())
                titelFeld.setText(titel);

            if (apiQuelle.equals("google")) {
                String autor = arrayWertAuslesen(json, "authors");
                if (!autor.isEmpty())
                    autorFeld.setText(autor);

                String datum = wertAuslesen(json, "publishedDate");
                if (datum.length() >= 4)
                    jahrFeld.setText(datum.substring(0, 4));

                String beschreibung = wertAuslesen(json, "description");
                if (!beschreibung.isEmpty())
                    beschreibungFeld.setText(beschreibung);
            } else {
                String autor = wertAuslesen(json, "name");
                if (!autor.isEmpty())
                    autorFeld.setText(autor);

                String datum = wertAuslesen(json, "publish_date");
                if (datum.length() >= 4)
                    jahrFeld.setText(datum.substring(datum.length() - 4));

                String beschreibung = wertAuslesen(json, "notes");
                if (!beschreibung.isEmpty())
                    beschreibungFeld.setText(beschreibung);
            }

        } catch (Exception e) {
            errorText.setText("Fehler beim Abrufen der Buchdaten");
        }
    }
    
    /**
     * Hilfsmethode: Extrahiert den ersten Wert aus einem JSON-Array
     * anhand eines bestimmten Schlüssels (z. B. "authors").
     * 
     * @param json Der gesamte JSON-String.
     * @param schluessel Der gesuchte Array-Schlüssel.
     * @return Der extrahierte Wert oder ein leerer String, falls nicht gefunden.
     */
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
    
    /**
     * Hilfsmethode: Extrahiert einen einfachen Textwert aus einem JSON-String
     * anhand eines bestimmten Schlüssels.
     * 
     * @param json Der gesamte JSON-String.
     * @param schluessel Der gesuchte Schlüssel.
     * @return Der extrahierte Wert oder ein leerer String, falls nicht gefunden.
     */
    private String wertAuslesen(String json, String schluessel) {
        String suche1 = "\"" + schluessel + "\":\"";
        String suche2 = "\"" + schluessel + "\": \"";

        int startPos = json.indexOf(suche1);
        if (startPos != -1) {
            startPos += suche1.length();
        } else {
            startPos = json.indexOf(suche2);
            if (startPos != -1)
                startPos += suche2.length();
        }

        if (startPos != -1) {
            int endPos = json.indexOf("\"", startPos);
            if (endPos != -1) {
                return json.substring(startPos, endPos);
            }
        }
        return "";
    }
    
}
