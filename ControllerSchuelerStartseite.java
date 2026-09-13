import javafx.scene.control.TextField;
import java.io.IOException;
import javafx.scene.input.MouseEvent;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.ListView;
import java.util.ArrayList;
import javafx.scene.control.TextArea;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;

import javafx.scene.text.Text;
import javafx.scene.text.Font;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.tk.FontMetrics;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.geometry.Pos;

/**
 * Controller für die Schüler-Startseite.
 * Verwaltet die Anzeige von geliehenen und reservierten Büchern,
 * die Buchsuche, Reservierungen und den eigenen Ausleihverlauf.
 */
public class ControllerSchuelerStartseite {

    private Bibliothek model;
    private Buch selectedBuch;

    private final double maxText = 802;
    private final double normaleSchriftgros = 55;
    
    private int bild;

    @FXML
    private Text nutzernameText;

    @FXML
    private TableView<TabellenZeile> geliehenTabelle;
    @FXML
    private TableColumn<TabellenZeile, String> geliehenTitelSpalte;
    @FXML
    private TableColumn<TabellenZeile, Label> geliehenRueckgabeSpalte;

    @FXML
    private TableView<TabellenZeile> reserviertTabelle;
    @FXML
    private TableColumn<TabellenZeile, String> reserviertTitelSpalte;
    @FXML
    private TableColumn<TabellenZeile, Label> reserviertBisSpalte;

    @FXML
    private TableView<TabellenZeile> verlaufTabelle;
    @FXML
    private TableColumn<TabellenZeile, String> verlaufTitelSpalte;
    @FXML
    private TableColumn<TabellenZeile, String> verlaufAutorSpalte;
    @FXML
    private TableColumn<TabellenZeile, String> verlaufIsbnSpalte;
    @FXML
    private TableColumn<TabellenZeile, Label> verlaufAusleihdatumSpalte;

    @FXML
    private TextField searchBar;

    @FXML
    private ListView<Buch> suchergebnisse;

    @FXML
    private TextArea buchInfoFeld;

    @FXML
    private Text statusText;

    @FXML
    private Button reservierenButton;

    @FXML
    private StackPane background;

    @FXML
    private Text gesperrtText;
    
    @FXML
    private ImageView mahnung;

    /**
     * Hilfsklasse für die Zeilen der Tabellen (Geliehen, Reserviert, Verlauf).
     */
    public static class TabellenZeile {
        private String titel;
        private String autor;
        private String isbn;
        private Label datum;

        /**
         * Konstruktor für eine Tabellenzeile.
         * 
         * @param titel Der Buchtitel.
         * @param autor Der Autor.
         * @param isbn Die ISBN.
         * @param datumText Das anzuzeigende Datum (Rückgabe, Reservierung, etc.).
         * @param verspaetetRot Ob ein überfälliges Datum rot markiert werden soll.
         */
        public TabellenZeile(String titel, String autor, String isbn, String datumText, boolean verspaetetRot) {
            this.titel = titel;
            this.autor = autor;
            this.isbn = isbn;
            this.datum = new Label(datumText);

            if (verspaetetRot) {
                String heute = java.time.LocalDate.now().toString();
                if (datumText.compareTo(heute) < 0) {
                    this.datum.setStyle("-fx-text-fill: red;");
                }
            }
        }

        /** @return Der Buchtitel. */
        public String getTitel() {
            return titel;
        }

        /** @return Der Autor. */
        public String getAutor() {
            return autor;
        }

        /** @return Die ISBN. */
        public String getIsbn() {
            return isbn;
        }

        /** @return Das formatierte Label für das Datum. */
        public Label getDatum() {
            return datum;
        }
    }

    /**
     * Setzt das Modell, aktualisiert die Nutzerdaten/Tabellen und wählt 
     * je nach Status (verspätet etc.) das passende Eulen-Bild aus.
     * 
     * @param model Die Bibliotheksinstanz.
     */
    public void setModel(Bibliothek model) {
        this.model = model;
        String text = "Hallo, " + model.getName() + " !";
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
        gesperrtText.setVisible(!model.isFreigeschaltet());
        updateTabellen();
        
        bild = model.tagefuerSchueler();
        Image eule = null;
        
        if(bild == 1){
            eule = new Image(getClass().getResource("/eulen/EuleAngry.png").toExternalForm());
        }
        else if (bild == 2){
            eule = new Image(getClass().getResource("/eulen/keineEule.png").toExternalForm());
        }
        else{
            eule = new Image(getClass().getResource("/eulen/normaleEule.png").toExternalForm());
        }
        
        mahnung.setImage(eule);
        mahnung.setPreserveRatio(true);
    }

    /**
     * Initialisiert den Controller, setzt Placeholder für Tabellen, 
     * definiert Spalten und richtet die Skalierung ein.
     */
    public void initialize() {
        geliehenTabelle.setPlaceholder(new Label("aktuell keine Buecher geliehen"));
        reserviertTabelle.setPlaceholder(new Label("aktuell keine Buecher reserviert"));
        verlaufTabelle.setPlaceholder(new Label("noch kein Buch geliehen"));

        geliehenTitelSpalte.setCellValueFactory(new PropertyValueFactory<>("titel"));
        geliehenRueckgabeSpalte.setCellValueFactory(new PropertyValueFactory<>("datum"));

        reserviertTitelSpalte.setCellValueFactory(new PropertyValueFactory<>("titel"));
        reserviertBisSpalte.setCellValueFactory(new PropertyValueFactory<>("datum"));

        verlaufTitelSpalte.setCellValueFactory(new PropertyValueFactory<>("titel"));
        verlaufAutorSpalte.setCellValueFactory(new PropertyValueFactory<>("autor"));
        verlaufIsbnSpalte.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        verlaufAusleihdatumSpalte.setCellValueFactory(new PropertyValueFactory<>("datum"));
        reservierenButton.setDisable(true);
        gesperrtText.setVisible(false);
        

        Platform.runLater(() -> {
            Scene scene = background.getScene();
            if (scene != null) {
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
        });
    }

    /**
     * Meldet den aktuellen Nutzer ab und navigiert zum Login-Bildschirm.
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
     * Aktualisiert alle Tabellen (Geliehen, Reserviert, Verlauf) 
     * mit den neuesten Daten aus dem Model für den aktuellen Nutzer.
     */
    public void updateTabellen() {
        if (model == null)
            return;

        geliehenTabelle.getItems().clear();
        QueryResult geliehen = model.getMeineGeliehenenBuecher();
        if (geliehen != null) {
            for (String[] row : geliehen.getData()) {
                geliehenTabelle.getItems().add(new TabellenZeile(row[0], "", row[2], row[1], true));
            }
        }

        reserviertTabelle.getItems().clear();
        QueryResult reserviert = model.getMeineReserviertenBuecher();
        if (reserviert != null) {
            for (String[] row : reserviert.getData()) {
                if ("bereit".equals(row[2])) {
                    reserviertTabelle.getItems()
                            .add(new TabellenZeile(row[0], "", row[3], "bereit zum abholen bis " + row[1], false));
                } else {
                    reserviertTabelle.getItems().add(
                            new TabellenZeile(row[0], "", row[3], row[1] != null ? row[1] : "Warte auf Rückgabe", false));
                }
            }
        }

        verlaufTabelle.getItems().clear();
        QueryResult verlauf = model.getNutzerVerlauf(0);
        if (verlauf != null) {
            for (String[] row : verlauf.getData()) {
                verlaufTabelle.getItems().add(new TabellenZeile(row[0], row[1], row[2], row[3], false));
            }
        }
    }

    /**
     * Führt eine Buchsuche anhand der SearchBar durch und aktualisiert die Suchergebnis-Liste.
     */
    public void suchen() {
        if (model == null || searchBar.getText().isEmpty())
            return;

        Buch altSelected = suchergebnisse.getSelectionModel().getSelectedItem();
        String altIsbn = altSelected != null ? altSelected.getIsbn() : null;

        ArrayList<Buch> erg = model.buecherSuchen(searchBar.getText());

        suchergebnisse.getItems().clear();
        if (erg != null) {
            for (Buch b : erg) {
                suchergebnisse.getItems().add(b);
            }
        }

        if (altIsbn != null) {
            for (Buch b : suchergebnisse.getItems()) {
                if (b.getIsbn().equals(altIsbn)) {
                    suchergebnisse.getSelectionModel().select(b);
                    selectedBuch = b;
                    selectBuch();
                    break;
                }
            }
        }
    }

    /**
     * Wird aufgerufen, wenn ein Buch in der Suchergebnis-Liste angeklickt wird.
     * 
     * @param event Das MouseEvent.
     */
    public void selectBuchAusListe(MouseEvent event) {
        Buch listSelected = suchergebnisse.getSelectionModel().getSelectedItem();
        if (listSelected != null) {
            selectedBuch = listSelected;
            selectBuch();
        }
    }

    /**
     * Wird aufgerufen, wenn eine Tabellenzeile angeklickt wird, 
     * um die dazugehörigen Buchdetails anzuzeigen.
     * 
     * @param event Das MouseEvent.
     */
    public void selectBuchAusTabelle(MouseEvent event) {
        if (event.getSource() instanceof TableView) {
            TableView<?> table = (TableView<?>) event.getSource();
            Object selectedItem = table.getSelectionModel().getSelectedItem();
            if (selectedItem instanceof TabellenZeile) {
                TabellenZeile zeile = (TabellenZeile) selectedItem;
                String isbn = zeile.getIsbn();
                if (isbn != null && !isbn.isEmpty()) {
                    selectedBuch = model.getBuch(isbn);
                    selectBuch();
                }
            }
        }
    }

    /**
     * Aktualisiert das Info-Feld und den Reservieren-Button 
     * basierend auf dem aktuell ausgewählten Buch.
     */
    public void selectBuch() {
        if (selectedBuch != null) {
            StringBuilder infoBuilder = new StringBuilder();

            if (selectedBuch.getTitel() != null && !selectedBuch.getTitel().isEmpty()) {
                infoBuilder.append("Titel: ").append(selectedBuch.getTitel()).append("\n");
            }
            if (selectedBuch.getAutor() != null && !selectedBuch.getAutor().isEmpty()) {
                infoBuilder.append("Autor: ").append(selectedBuch.getAutor()).append("\n");
            }
            if (selectedBuch.getIsbn() != null && !selectedBuch.getIsbn().isEmpty()) {
                infoBuilder.append("ISBN: ").append(selectedBuch.getIsbn()).append("\n");
            }
            if (selectedBuch.getErscheinungsjahr() != null && !selectedBuch.getErscheinungsjahr().isEmpty()) {
                infoBuilder.append("Erscheinungsjahr: ").append(selectedBuch.getErscheinungsjahr()).append("\n");
            }
            if (selectedBuch.getAlter() != null && !selectedBuch.getAlter().isEmpty()) {
                infoBuilder.append("Altersbeschränkung: ").append(selectedBuch.getAlter()).append(" Jahre\n");
            }
            if (selectedBuch.getBeschreibung() != null && !selectedBuch.getBeschreibung().isEmpty()) {
                infoBuilder.append("\nBeschreibung:\n").append(selectedBuch.getBeschreibung());
            }

            buchInfoFeld.setText(infoBuilder.toString().trim());
            String status = selectedBuch.getStatus();
            if (status.equals("verfuegbar")) {
                status = "verfügbar";
            }
            if (status.equals("entfernt")) {
                status = "nicht verfügbar";
            }
            statusText.setText("aktueller Status: " + status);
            if (model.reservierungMoeglich(selectedBuch.getIsbn())) {
                if (model.buchGeliehen(selectedBuch.getIsbn()) || !model.isFreigeschaltet()) {
                    reservierenButton.setDisable(true);
                    reservierenButton.setText("reservieren");
                } else {
                    reservierenButton.setDisable(false);
                    reservierenButton.setText("reservieren");
                }
            } else {
                if (model.selbstReserviert(selectedBuch.getIsbn())) {
                    reservierenButton.setText("reservierung stornieren");
                    reservierenButton.setDisable(false);
                } else {
                    reservierenButton.setText("reservieren");
                    reservierenButton.setDisable(true);
                }
            }
        }
    }

    /**
     * Führt eine Reservierung des ausgewählten Buches durch oder storniert sie,
     * falls das Buch bereits vom aktuellen Nutzer reserviert wurde.
     */
    public void reservieren() {
        if (selectedBuch != null) {
            if (!model.selbstReserviert(selectedBuch.getIsbn())) {
                model.reservieren(selectedBuch.getIsbn());
                updateTabellen();
                suchen();
            } else {
                model.reservierungStornieren(selectedBuch.getIsbn());
                updateTabellen();
                suchen();
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
