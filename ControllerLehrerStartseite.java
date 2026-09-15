
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDate;
import java.util.ArrayList;

import javafx.scene.paint.Color;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import java.io.IOException;
import javafx.scene.Node;
import javafx.scene.control.ListView;

import java.time.LocalDate;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.tk.FontMetrics;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.geometry.Pos;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.scene.shape.Rectangle;


/**
 * Controller für die Lehrer-Startseite (Hauptmenü der Anwendung).
 * Verwaltet Ausleihen, Rückgaben, Scanner-Eingaben, Tabellenübersichten und
 * Navigation.
 */
public class ControllerLehrerStartseite {

    private Bibliothek model;
    private PauseTransition feedbackTimer;
    private ArrayList<String> konfliktNamen = new ArrayList<>();

    private final double maxText = 802;
    private final double normaleSchriftgros = 55;

    private final StringBuilder isbnbuild = new StringBuilder();
    private String isbn;
    private long letzteTastenZeit;
    private String isbnNeu;
    private Scene registeredScannerScene;
    private Scene registeredScaleScene;
    private String letzteGueltigeDauer = "28";
    private TranslateTransition currentTransition;

    @FXML
    private TableView<tabelleZeile> verliehenTabelle;

    @FXML
    private TableColumn<tabelleZeile, String> verliehenTabelleIsbn;

    @FXML
    private TableColumn<tabelleZeile, String> verliehenTabelleTitel;

    @FXML
    private TableColumn<tabelleZeile, String> verliehenTabelleName;

    @FXML
    private TableColumn<tabelleZeile, String> verliehenTabelleVorname;

    @FXML
    private TableColumn<tabelleZeile, String> verliehenTabelleEmail;

    @FXML
    private TableColumn<tabelleZeile, Label> verliehenTabelleGeplanteRueckgabe;

    @FXML
    private TableColumn<tabelleZeile, Integer> verliehenTabelleMahnungen;

    @FXML
    private TableView<tabelleZeileReservierung> reserviertTabelle;

    @FXML
    private TableColumn<tabelleZeileReservierung, String> reserviertTabelleIsbn;

    @FXML
    private TableColumn<tabelleZeileReservierung, String> reserviertTabelleTitel;

    @FXML
    private TableColumn<tabelleZeileReservierung, String> reserviertTabelleName;

    @FXML
    private TableColumn<tabelleZeileReservierung, String> reserviertTabelleVorname;

    @FXML
    private TableColumn<tabelleZeileReservierung, String> reserviertTabelleEmail;

    @FXML
    private TextField codeFeld;

    @FXML
    private Text feedbackText;

    @FXML
    private ListView gescanntListe;

    @FXML
    private Button abbrechenButton;

    @FXML
    private Button ausleihenButton;

    @FXML
    private Button zuruecknehmenButton;

    @FXML
    private Button scannenButton;

    @FXML
    private Button statistiken;

    @FXML
    private TextField ausleihdauerFeld;

    @FXML
    private Text nutzernameText;

    @FXML
    private StackPane background;

    @FXML
    private Button rueckgaengigButton;

    @FXML
    private VBox menuPane;

    @FXML
    private Button aufMenu;

    @FXML
    private Button zuMenu;

    @FXML
    private Button neuesBuch;

    @FXML
    private Button loadBuecherVerwaltung;

    @FXML
    private Button loadNutzerVerwaltung;

    @FXML
    private Button einstellungenButton;

    @FXML
    private Button mahnungButton;

    /**
     * Hilfsklasse für die Zeilen der Tabelle "Verliehene Bücher".
     */
    public static class tabelleZeile {
        private String isbn;
        private String titel;
        private String nachname;
        private String vorname;
        private String email;
        private Label geplanteRueckgabe;
        private int anzahlMahnungen;
        private int id;

        /**
         * Konstruktor für eine Zeile in der Verliehen-Tabelle.
         * 
         * @param isbn               Die ISBN des Buches.
         * @param titel              Der Titel des Buches.
         * @param nachname           Der Nachname des Entleihers.
         * @param vorname            Der Vorname des Entleihers.
         * @param email              Die E-Mail-Adresse.
         * @param geplante_Rueckgabe Das geplante Rückgabedatum.
         * @param anzahlMahnungen    Die Anzahl bisheriger Mahnungen.
         * @param id                 Die ID des Verleihvorgangs.
         */
        public tabelleZeile(String isbn, String titel, String nachname, String vorname, String email,
                String geplante_Rueckgabe, int anzahlMahnungen, int id) {
            this.isbn = isbn;
            this.titel = titel;
            this.nachname = nachname;
            this.vorname = vorname;
            this.email = email;
            this.anzahlMahnungen = anzahlMahnungen;
            this.id = id;

            this.geplanteRueckgabe = new Label(geplante_Rueckgabe);

            String heute = LocalDate.now().toString();
            if (geplante_Rueckgabe.compareTo(heute) < 0) {
                this.geplanteRueckgabe.setStyle("-fx-text-fill: red;");
            }
        }

        /** @return ISBN des Buches. */
        public String getIsbn() {
            return isbn;
        }

        /** @return Titel des Buches. */
        public String getTitel() {
            return titel;
        }

        /** @return Nachname des Entleihers. */
        public String getNachname() {
            return nachname;
        }

        /** @return Vorname des Entleihers. */
        public String getVorname() {
            return vorname;
        }

        /** @return E-Mail des Entleihers. */
        public String getEmail() {
            return email;
        }

        /** @return Geplante Rückgabe als farblich angepasstes Label. */
        public Label getGeplanteRueckgabe() {
            return geplanteRueckgabe;
        }

        /** @return Anzahl der Mahnungen. */
        public int getAnzahlMahnungen() {
            return anzahlMahnungen;
        }

        /** @return ID des Verleih-Eintrags. */
        public int getId() {
            return id;
        }
    }

    /**
     * Hilfsklasse für die Zeilen der Tabelle "Reservierte Bücher".
     */
    public static class tabelleZeileReservierung {
        private String isbn;
        private String titel;
        private String nachname;
        private String vorname;
        private String email;

        /**
         * Konstruktor für eine Reservierungszeile.
         * 
         * @param isbn     Die ISBN des Buches.
         * @param titel    Der Titel des Buches.
         * @param nachname Der Nachname des Reservierenden.
         * @param vorname  Der Vorname des Reservierenden.
         * @param email    Die E-Mail-Adresse.
         */
        public tabelleZeileReservierung(String isbn, String titel, String nachname, String vorname, String email) {
            this.isbn = isbn;
            this.titel = titel;
            this.nachname = nachname;
            this.vorname = vorname;
            this.email = email;
        }

        /** @return ISBN des Buches. */
        public String getIsbn() {
            return isbn;
        }

        /** @return Titel des Buches. */
        public String getTitel() {
            return titel;
        }

        /** @return Nachname des Reservierenden. */
        public String getNachname() {
            return nachname;
        }

        /** @return Vorname des Reservierenden. */
        public String getVorname() {
            return vorname;
        }

        /** @return E-Mail des Reservierenden. */
        public String getEmail() {
            return email;
        }
    }

    /**
     * Setzt das Modell, aktualisiert die Anzeige (Name, Tabellen)
     * und prüft die Rechte des aktuellen Benutzers (Lehrer/Schüler/Helfer).
     * 
     * @param model Die Bibliotheksinstanz.
     */
    public void setModel(Bibliothek model) {
        this.model = model;

        ausleihdauerFeld.setText(String.valueOf(model.getAusleihDauer()));

        loadVerliehenTabelle();
        loadReserviertTabelle();

        String text = "Hallo, " + model.getName() + "!";
        // dynamisch die Schriftgrose an Text Lange anpassen
        Text tempText = new Text(text);
        tempText.setFont(Font.font("Candara", normaleSchriftgros));
        double textBreite = tempText.getLayoutBounds().getWidth();
        if (textBreite <= maxText) {
            nutzernameText.setFont(Font.font("Candara", normaleSchriftgros));
        } else {
            double neueSchrift = normaleSchriftgros * maxText / textBreite;
            nutzernameText.setFont(Font.font("Candara", neueSchrift));
        }

        nutzernameText.setText(text);
        if (!model.isLehrer()) {
            loadBuecherVerwaltung.setVisible(false);
            loadNutzerVerwaltung.setVisible(false);
            einstellungenButton.setVisible(false);
            mahnungButton.setVisible(false);
        }
    }

    /**
     * Initialisiert den Controller, setzt Skalierungseinstellungen für das Fenster,
     * konfiguriert das Seitenmenü und EventListener für den Scanner.
     */
    public void initialize() {
        neuesBuch.setVisible(false);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(menuPane.widthProperty());
        clip.heightProperty().bind(menuPane.heightProperty());

        menuPane.setClip(clip);

        menuPane.setVisible(false);

        verliehenTabelleIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        verliehenTabelleTitel.setCellValueFactory(new PropertyValueFactory<>("titel"));
        verliehenTabelleName.setCellValueFactory(new PropertyValueFactory<>("nachname"));
        verliehenTabelleVorname.setCellValueFactory(new PropertyValueFactory<>("vorname"));
        verliehenTabelleEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        verliehenTabelleGeplanteRueckgabe.setCellValueFactory(new PropertyValueFactory<>("geplanteRueckgabe"));
        verliehenTabelleMahnungen.setCellValueFactory(new PropertyValueFactory<>("anzahlMahnungen"));

        reserviertTabelleIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        reserviertTabelleTitel.setCellValueFactory(new PropertyValueFactory<>("titel"));
        reserviertTabelleName.setCellValueFactory(new PropertyValueFactory<>("nachname"));
        reserviertTabelleVorname.setCellValueFactory(new PropertyValueFactory<>("vorname"));
        reserviertTabelleEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        ausleihenButton.setDisable(true);
        zuruecknehmenButton.setDisable(true);
        rueckgaengigButton.setDisable(true);
        abbrechenButton.setDisable(true);
        mahnungButton.disableProperty().bind(verliehenTabelle.getSelectionModel().selectedItemProperty().isNull());

        verliehenTabelle.setPlaceholder(new Label("Keine verliehenen Bücher"));
        reserviertTabelle.setPlaceholder(new Label("Keine reservierten Bücher"));

        gescanntListe.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTextFill(Color.BLACK);
                } else {
                    setText(item);
                    if (konfliktNamen != null && konfliktNamen.contains(item)) {
                        setTextFill(Color.RED);
                    } else {
                        setTextFill(Color.BLACK);
                    }
                }
            }
        });

        letzteGueltigeDauer = ausleihdauerFeld.getText();

        Platform.runLater(() -> {
            Scene scene = background.getScene();
            if (scene != null) {
                setupSceneScaling(scene);
                registerGlobalScanner(scene);
            }
        });

        background.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> {
                    setupSceneScaling(newScene);
                    registerGlobalScanner(newScene);
                });
            }
        });
    }

    /**
     * Lädt alle aktuell verliehenen Bücher aus der Datenbank
     * und füllt die Verliehen-Tabelle.
     */
    public void loadVerliehenTabelle() {
        QueryResult result = model.getVerlieheneBuecher();
        if (result != null) {
            verliehenTabelle.getItems().clear();
            String[][] data = result.getData();
            for (int i = 0; i < result.getRowCount(); i++) {
                if (data[i].length >= 8) {
                    String isbn = data[i][0];
                    String titel = data[i][1];
                    String nachname = data[i][2];
                    String vorname = data[i][3];
                    String email = data[i][4];
                    String geplanteRueckgabe = data[i][5];
                    int anzahlMahnungen = Integer.parseInt(data[i][6]);
                    int id = Integer.parseInt(data[i][7]);

                    tabelleZeile zeile = new tabelleZeile(isbn, titel, nachname, vorname, email, geplanteRueckgabe,
                            anzahlMahnungen, id);
                    verliehenTabelle.getItems().add(zeile);

                }
            }
        }
    }

    /**
     * Konfiguriert die dynamische Skalierung für das Fenster (1920x1080).
     * 
     * @param scene Die aktuelle JavaFX-Szene.
     */
    private void setupSceneScaling(Scene scene) {
        if (scene == null || scene == registeredScaleScene) {
            return;
        }
        registeredScaleScene = scene;

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
    }

    /**
     * Registriert EventFilter auf der Szene, um Barcode-Scanner-Eingaben
     * anhand der Eingabegeschwindigkeit (unter 100ms zwischen Tasten) systemweit
     * abzufangen.
     * 
     * @param scene Die aktuelle JavaFX-Szene.
     */
    public void registerGlobalScanner(javafx.scene.Scene scene) {
        if (scene == null || scene == registeredScannerScene) {
            return;
        }
        registeredScannerScene = scene;

        // KEY_TYPED fängt die eingegebenen Zeichen ab und puffert sie bei schneller
        // Folge (Scanner)
        scene.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            long jetzt = System.currentTimeMillis();
            String zeichen = event.getCharacter();

            if (zeichen == null || zeichen.isEmpty() || zeichen.equals("\r") || zeichen.equals("\n")) {
                return;
            }

            // Wenn zwischen zwei Zeichen mehr als 100ms liegen, tippt ein Mensch ->
            // Scannerpuffer leeren
            if (jetzt - letzteTastenZeit > 100) {
                isbnbuild.setLength(0);
                if (ausleihdauerFeld != null && ausleihdauerFeld.isFocused()) {
                    letzteGueltigeDauer = ausleihdauerFeld.getText();
                }
            }

            // Gültige Ziffern für Barcode puffern
            if (zeichen.matches("[0-9]")) {
                isbnbuild.append(zeichen);
            }

            // Scanner-Erkennung: Schnelle Eingabe (< 100ms)
            // Wenn ausleihdauerFeld fokussiert ist, Text wiederherstellen und Event
            // konsumieren
            if (jetzt - letzteTastenZeit < 100) {
                if (ausleihdauerFeld != null && ausleihdauerFeld.isFocused()) {
                    ausleihdauerFeld.setText(letzteGueltigeDauer);
                    event.consume();
                }
            }

            letzteTastenZeit = jetzt;
        });

        // KEY_PRESSED fängt das abschließende ENTER des Scanners oder manuelles ENTER
        // im Code-Feld ab
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                long jetzt = System.currentTimeMillis();
                String gescanntISBN = isbnbuild.toString().trim();

                // Schnelle Folge zum ENTER (< 250ms) und gepufferter Barcode vorhanden ->
                // Scanner!
                if (jetzt - letzteTastenZeit < 250 && !gescanntISBN.isEmpty()) {
                    isbn = gescanntISBN;
                    isbnbuild.setLength(0);
                    scannen();
                    event.consume();
                } else if (codeFeld != null && codeFeld.isFocused() && !codeFeld.getText().trim().isEmpty()) {
                    // Manuelle Eingabe im Code-Feld per ENTER bestätigen
                    isbnbuild.setLength(0);
                    scannen();
                    event.consume();
                }
            }
        });
    }

    /**
     * Lädt alle reservierten Bücher aus der Datenbank und füllt die
     * Reserviert-Tabelle.
     */
    public void loadReserviertTabelle() {
        QueryResult result = model.getBereiteReservierungen();
        if (result != null) {
            reserviertTabelle.getItems().clear();
            String[][] data = result.getData();
            for (int i = 0; i < result.getRowCount(); i++) {
                if (data[i].length >= 5) {
                    String isbn = data[i][0];
                    String titel = data[i][1];
                    String nachname = data[i][2];
                    String vorname = data[i][3];
                    String email = data[i][4];

                    tabelleZeileReservierung zeile = new tabelleZeileReservierung(isbn, titel, nachname, vorname,
                            email);
                    reserviertTabelle.getItems().add(zeile);
                }
            }
        }
    }

    /**
     * Wird aufgerufen, wenn ein Barcode (ISBN oder Schüler-ID) gescannt wurde.
     * Führt die Logik im Model aus (Feedback-Code) und aktualisiert UI-Elemente.
     */
    public void scannen() {
        neuesBuch.setVisible(false);
        if (feedbackTimer != null)
            feedbackTimer.stop();
        feedbackText.setFill(Color.BLACK);
        String code;
        if (isbn != null) {
            code = isbn;
            isbn = null;
        } else {
            code = codeFeld.getText();
        }
        isbnNeu = code;
        int feedback = model.scannen(code);

        switch (feedback) {
            case 1:
                if (!model.getErfassteSchuelerName().isEmpty()) {
                    feedbackText.setText("weiteres Buch scannen");
                    ausleihenButton.setDisable(false);
                    
                } else {
                    feedbackText.setText("weiteres Buch oder Nutzerausweis scannen");
                }
                break;
            case 2: {
                if (model.getErfassteSchuelerName().isEmpty()){
                    feedbackText.setText("Rückgabe: bitte Nutzerausweis oder weiteres Buch scannen");
                    break;
                }
                else{
                    if(model.richtigerSchuelerRuckListe()){
                       
                        String msg = "Rückgabe: (weiteres Buch scannen möglich) Buch erfasst";
                        int tage = model.getTageZuSpaet(code);
                        
                        if (tage > 0)
                            msg += " – " + tage + " Tage zu spät!";
                        feedbackText.setText(msg);
                        
                        zuruecknehmenButton.setDisable(false);
                        break; 
                    }
                    else{
                        feedbackText.setText("falscher Schülerausweis gescannt");
                        break;
                    }
                }
                
            }
            case 3:
                feedbackText.setFill(Color.RED);
                feedbackText.setText("Das Buch ist für einen anderen Schüler reserviert!");
                break;
            case 4:
                feedbackText.setFill(Color.RED);
                feedbackText.setText("Buch kann nicht verliehen werden!");
                break;
            case 5: {
                int tage = model.getTageZuSpaet(code);
                String msg = "Buch erfasst und bereits für nächsten Schüler reserviert";
                if (tage > 0)
                    msg += " – " + tage + " Tage zu spät!";
                feedbackText.setText(msg);
                zuruecknehmenButton.setDisable(false);
                break;
            }
            case 6:
                feedbackText.setText("Schüler " + model.getErfassteSchuelerName() + " erfasst");
                if (model.getErfassteBuecherNamen().size() > 0) {
                    ausleihenButton.setDisable(false);
                }
                break;
            case 7:
                feedbackText.setFill(Color.RED);
                feedbackText.setText(
                        "Buch bereits verliehen, bitte erst Ausleihvorgang abschließen und danach zurücknehmen");
                break;
            case 8:
                feedbackText.setFill(Color.RED);
                feedbackText.setText("Code nicht erkannt! \nWollen Sie ein neues Buch anlegen?");
                neuesBuch.setVisible(true);
                break;
            case 9:
                feedbackText.setFill(Color.RED);
                feedbackText.setText("Es können maximal 10 Bücher gleichzeitig gescannt werden!");
                break;
            case 10:
                feedbackText.setFill(Color.RED);
                feedbackText.setText("Schüler gesperrt! Verleih nicht möglich");
                break;
            case 11:
                feedbackText.setFill(Color.RED);
                feedbackText.setText("Buch ist reserviert für " + model.getreserviertSchuelerName(code)
                        + ", zum Prüfen Schüler scannen");
                break;
            case 12:
                feedbackText.setFill(Color.RED);
                feedbackText.setText("Bücher für andere Schüler reserviert");
                scannenButton.setDisable(true);
                ausleihenButton.setDisable(true);
                break;
            case 13:
                int ab = model.getBuchAltersbeschraenkung(code);
                feedbackText.setFill(Color.RED);
                feedbackText
                        .setText("Buch hat eine Altersbeschränkung von " + ab + " Jahren. Zum Prüfen Schüler scannen.");
                break;
            case 14:
                feedbackText.setFill(Color.RED);
                feedbackText.setText("Schüler zu jung oder kein Geburtsdatum hinterlegt");
                scannenButton.setDisable(true);
                ausleihenButton.setDisable(true);
                break;
            case 15:
                feedbackText.setFill(Color.RED);
                feedbackText.setText("Maximale Anzahl gleichzeitiger Bücher überschritten");
                scannenButton.setDisable(true);
                ausleihenButton.setDisable(true);
                break;
            case 16:
                if (model.getErfassteSchuelerName().isEmpty()){
                    feedbackText.setText("Nutzerausweis oder weiteres Buch scannen");
                    break;
                }
                else{
                    if(model.richtigerSchuelerRuckListe()){
                        String msg = "Rückgabe: (weiteres Buch scannen möglich) Buch erfasst";
                        int tage = model.getTageZuSpaet(code);
                        if (tage > 0)
                            msg += " – " + tage + " Tage zu spät!";
                        feedbackText.setText(msg);
                        zuruecknehmenButton.setDisable(false);
                        break; 
                    }
                    else{
                        feedbackText.setText("weiteres Buch scannen");
                        ausleihenButton.setDisable(false);
                        break;
                    }
                }
            case 17:
                feedbackText.setText("Schüler " + model.getErfassteSchuelerName() + " erfasst");
                if (model.getErfassteBuecherNamen().size() > 0) {
                    zuruecknehmenButton.setDisable(false);
                }
                break;
            case 19:
                feedbackText.setFill(Color.RED);
                feedbackText.setText("Schüler hat ein Buch bereits ausgeliehen");
                ausleihenButton.setDisable(true);
                break;
        }
        if (model.abbrechenMoeglich()) {
            abbrechenButton.setDisable(false);
        }
        codeFeld.clear();
        updateGescanntListe();
    }

    /**
     * Aktualisiert die Liste der aktuell im Ausleih-/Rückgabevorgang
     * gepufferten bzw. gescannten Bücher (Konflikte rot markiert).
     */
    private void updateGescanntListe() {
        if (gescanntListe != null && model != null) {
            gescanntListe.getItems().clear();
            ArrayList<String> namen = model.getErfassteBuecherNamen();
            konfliktNamen = model.getKonfliktBuecherNamen();
            if (namen != null) {
                gescanntListe.getItems().addAll(namen);
            }
        }
    }

    /**
     * Setzt den Status-/Feedback-Text nach 10 Sekunden automatisch zurück.
     */
    private void feedbackZuruecksetzen() {
        if (feedbackTimer != null)
            feedbackTimer.stop();
        feedbackTimer = new PauseTransition(Duration.seconds(10));
        feedbackTimer.setOnFinished(e -> {
            feedbackText.setText("Buch scannen");
            feedbackText.setFill(Color.BLACK);
        });
        feedbackTimer.play();
    }

    /**
     * Bricht den aktuellen Scan-/Ausleih-/Rückgabevorgang ab
     * und leert die entsprechenden Listen im Modell.
     */
    public void abbrechen() {
        ausleihenButton.setDisable(true);
        zuruecknehmenButton.setDisable(true);
        model.abbrechen();
        ausleihdauerFeld.setText(String.valueOf(model.getAusleihDauer()));
        feedbackText.setFill(Color.BLACK);
        feedbackText.setText("Buch scannen");
        updateGescanntListe();
        scannenButton.setDisable(false);
        abbrechenButton.setDisable(true);
    }

    /**
     * Führt die Rückgabe für alle aktuell erfassten Bücher im Model durch
     * und aktualisiert anschließend die Anzeige.
     */
    public void zurueckgeben() {
        rueckgaengigButton.setDisable(false);
        model.buchRueckgabe();
        model.abbrechen();
        ausleihdauerFeld.setText(String.valueOf(model.getAusleihDauer()));
        ausleihenButton.setDisable(true);
        zuruecknehmenButton.setDisable(true);
        feedbackText.setText("Buch erfolgreich zurückgegeben.");
        loadVerliehenTabelle();
        loadReserviertTabelle();
        updateGescanntListe();
        feedbackZuruecksetzen();
        letzteAktionAnzeigen();
       
    }

    /**
     * Führt die Ausleihe für den aktuell erfassten Schüler und die erfassten Bücher
     * im Model durch und aktualisiert anschließend die Anzeige.
     */
    public void ausleihen() {
        
        try {
            int dauer = Integer.parseInt(ausleihdauerFeld.getText());
            if (dauer >= 1 && dauer <= 200) {
                model.buchLeihen(dauer);
                model.abbrechen();
                ausleihdauerFeld.setText(String.valueOf(model.getAusleihDauer()));
                ausleihenButton.setDisable(true);
                zuruecknehmenButton.setDisable(true);
                feedbackText.setText("Bücher erfolgreich verliehen.");
                loadVerliehenTabelle();
                loadReserviertTabelle();
                updateGescanntListe();
                feedbackZuruecksetzen();
                letzteAktionAnzeigen();
                rueckgaengigButton.setDisable(false);
            } else {
                feedbackText.setFill(Color.RED);
                feedbackText.setText("Bitte eine gültige Dauer (1-200 Tage) eingeben!");
            }
        } catch (NumberFormatException e) {
            feedbackText.setText("Bitte eine gültige Dauer (1-200 Tage) eingeben!");
        }
    }

    /**
     * Meldet den aktuellen Nutzer ab und lädt die Login-Szene.
     * 
     * @param event Das ActionEvent.
     */
    public void logout(ActionEvent event) {
        model.logout();
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("scenes/login.fxml"));
            Scene scene = new Scene(root);
            scene.setFill(Color.web("#E9E9D3"));
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {

        }
    }

    /**
     * Wechselt zur Szene "Bücherverwaltung" (nur für Lehrer verfügbar).
     * 
     * @param event Das ActionEvent.
     */
    public void loadBuecherVerwaltung(ActionEvent event) {
        if (model.isLehrer()) {
            try {
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

                FXMLLoader loader = new FXMLLoader(getClass().getResource("scenes/buchVerwaltung.fxml"));
                Parent root = loader.load();
                ControllerBuecherVerwaltung controller = loader.getController();
                controller.setModel(model);
                Scene scene = new Scene(root);
                scene.setFill(Color.web("#E9E9D3"));
                stage.setScene(scene);
                stage.show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Wechselt zur Szene "Nutzerverwaltung" (nur für Lehrer verfügbar).
     * 
     * @param event Das ActionEvent.
     */
    public void loadNutzerVerwaltung(ActionEvent event) {
        if (model.isLehrer()) {
            try {
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

                FXMLLoader loader = new FXMLLoader(getClass().getResource("scenes/nutzerVerwaltung.fxml"));
                Parent root = loader.load();
                ControllerNutzerVerwaltung controller = loader.getController();
                controller.setModel(model);
                Scene scene = new Scene(root);
                scene.setFill(Color.web("#E9E9D3"));
                stage.setScene(scene);
                stage.show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Wechselt zur Szene "Statistiken".
     * 
     * @param event Das ActionEvent.
     */
    public void loadStatistiken(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("scenes/Statistiken.fxml"));
            Parent root = loader.load();
            ControllerStatistiken controller = loader.getController();
            controller.setModel(model);
            Scene scene = new Scene(root);
            scene.setFill(Color.web("#E9E9D3"));
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Wechselt zur Szene "Einstellungen" (nur für Lehrer verfügbar).
     * 
     * @param event Das ActionEvent.
     */
    public void toEinstellungen(ActionEvent event) {
        if (model.isLehrer()) {
            try {
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("scenes/Einstellungen.fxml"));
                Parent root = loader.load();

                ControllerEinstellungen controller = loader.getController();
                controller.setModel(model);

                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Wechselt zur Szene "Passwort ändern".
     * 
     * @param event Das ActionEvent.
     */
    public void passwortAendern(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene currentScene = ((Node) event.getSource()).getScene();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("scenes/passwortReset.fxml"));
            Parent root = loader.load();
            passwortResetController controller = loader.getController();
            controller.setModel(model);
            controller.setPreviousScene(currentScene);
            Scene scene = new Scene(root);
            scene.setFill(Color.web("#E9E9D3"));
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Entfernt das in der Liste markierte (gescannte) Buch aus dem Puffer
     * und aktualisiert die UI (Buttons/Liste).
     */
    public void gescanntesBuchEntfernen() {
        int selectedIndex = gescanntListe.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            model.gescanntesBuchEntfernen(selectedIndex);
            updateGescanntListe();
            if(model.getKonfliktBuecherNamen().isEmpty() && !model.getErfassteBuecherNamen().isEmpty() && !model.getErfassteSchuelerName().isEmpty()){
                ausleihenButton.setDisable(false);
            }
            if (model.getErfassteBuecherNamen().isEmpty()) {
                ausleihenButton.setDisable(true);
                zuruecknehmenButton.setDisable(true);
                scannenButton.setDisable(false);
                feedbackText.setFill(Color.BLACK);
                feedbackText.setText("Buch scannen");
            } else if (model.getKonfliktBuecherNamen().isEmpty()) {
                scannenButton.setDisable(false);
                feedbackText.setFill(Color.BLACK);
                if (model.getErfassteSchuelerName() != null && !model.getErfassteSchuelerName().isEmpty()) {
                    ausleihenButton.setDisable(false);
                    feedbackText.setText("weiteres Buch scannen");
                } else {
                    feedbackText.setText("weiteres Buch oder Nutzerausweis scannen");
                }
            }
        }
    }

    /**
     * Zeigt in der Liste die zuletzt ausgeführte Aktion (Ausleihe/Rückgabe)
     * mitsamt betroffenen Büchern und Schülern an.
     */
    public void letzteAktionAnzeigen() {
        neuesBuch.setVisible(false);
        ArrayList<String> liste = new ArrayList<>();
        liste.add("Letzte Aktion: ");
        liste.addAll(model.getLetzteBuecher());
        if (model.letzteAktionAusleihen()) {
            liste.add("verliehen an: " + model.getLetzterSchuelerName());
        } else {
            liste.add("zurückgenommen von: " + model.getLetzterSchuelerName());
        }
        gescanntListe.getItems().clear();
        gescanntListe.getItems().addAll(liste);
    }

    /**
     * Macht die zuletzt durchgeführte Ausleihe oder Rückgabe im Model rückgängig.
     */
    public void letzteAktionZureucknehmen() {
        model.letzteAktionZuruecknehmen();
        loadVerliehenTabelle();
        loadReserviertTabelle();
        updateGescanntListe();
        feedbackZuruecksetzen();
        letzteAktionAnzeigen();
        rueckgaengigButton.setDisable(true);
        feedbackText.setFill(Color.BLACK);
        feedbackText.setText("Letzte Aktion erfolgreich zurückgenommen.");
        gescanntListe.getItems().clear();
    }

    /**
     * Öffnet das Seitenmenü.
     * 
     * @param event Das ActionEvent.
     */
    public void openmenu(ActionEvent event) {
        if (menuPane.getTranslateX() == 0) {
            menuPane.setTranslateX(-200);
        }
        if (currentTransition != null) {
            currentTransition.stop();
        }
        aufMenu.setDisable(true); 
        menuPane.setVisible(true);
        currentTransition = new TranslateTransition(Duration.seconds(0.3), menuPane);
        currentTransition.setToX(0);
        menuPane.setMouseTransparent(false);
        currentTransition.setOnFinished(null);
        aufMenu.setVisible(false);
        currentTransition.play();
        zuMenu.setDisable(false);
    }

    /**
     * Schließt das Seitenmenü.
     * 
     * @param event Das ActionEvent.
     */
    public void closemenu(ActionEvent event) {
        if (currentTransition != null) {
            currentTransition.stop();
        }
        zuMenu.setDisable(true);
        menuPane.setVisible(false);
        currentTransition = new TranslateTransition(Duration.seconds(0.3), menuPane);
        currentTransition.setToX(-200);
        menuPane.setMouseTransparent(true);
        currentTransition.setOnFinished(e -> menuPane.setVisible(false));
        aufMenu.setVisible(true);
        currentTransition.play();
        aufMenu.setDisable(false);
    }

    /**
     * Fügt dem Entleiher des ausgewählten Buches (in der Verliehen-Tabelle) eine
     * Mahnung hinzu
     * (nur für Lehrer möglich).
     */
    public void mahnungHinzufuegen() {
        if (model.isLehrer()) {
            tabelleZeile selectedItem = verliehenTabelle.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                model.mahnungHinzufuegen(selectedItem.getId());
                loadVerliehenTabelle();
            }
        }
    }

    /**
     * Öffnet ein Popup-Fenster, um ein neues Buch hinzuzufügen,
     * falls ein unbekannter Barcode gescannt wurde.
     * 
     * @param event Das ActionEvent.
     */
    public void openPopUp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("scenes/PopUpNeu.fxml"));
            Parent root = loader.load();
            ControllerPopUpNeu popupController = loader.getController();
            popupController.setISBN(isbnNeu, model);
            Stage stage = new Stage();
            stage.setTitle("Neues Buch");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            feedbackText.setText("");
            neuesBuch.setVisible(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
