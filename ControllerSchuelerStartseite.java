import javafx.scene.control.TextField;
import java.io.IOException;

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

import javafx.scene.text.Text;
import javafx.scene.text.Font;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.tk.FontMetrics;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.geometry.Pos;



public class ControllerSchuelerStartseite {

    private Bibliothek model;
    private Buch selectedBuch;
    
    private final double maxText = 521;
    private final double normaleSchriftgros = 55;

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
    private Button reservierenButton;
    
    @FXML
    private StackPane background;

    public static class TabellenZeile {
        private String titel;
        private String autor;
        private String isbn;
        private Label datum;

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

        public String getTitel() {
            return titel;
        }

        public String getAutor() {
            return autor;
        }

        public String getIsbn() {
            return isbn;
        }

        public Label getDatum() {
            return datum;
        }
    }

    public void setModel(Bibliothek model) {
        this.model = model;
        String text = "Hallo, " + model.getName() + " !";
        //dynamisch die Schriftgrose an Text Lange anpassen
        Text tempText = new Text(text);
        tempText.setFont(Font.font("Candara", normaleSchriftgros));
        double textBreite = tempText.getLayoutBounds().getWidth();
        if (textBreite <= maxText){
            nutzernameText.setFont(Font.font("Candara",normaleSchriftgros));
        }
        else{
            double neueSchrift = normaleSchriftgros * maxText/textBreite;
            nutzernameText.setFont(Font.font("Candara",neueSchrift));
        }
        
        nutzernameText.setText(text);
        updateTabellen();
    }

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
            }
        });
    }

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

    public void updateTabellen() {
        if (model == null)
            return;

        geliehenTabelle.getItems().clear();
        QueryResult geliehen = model.getMeineGeliehenenBuecher();
        if (geliehen != null) {
            for (String[] row : geliehen.getData()) {
                geliehenTabelle.getItems().add(new TabellenZeile(row[0], "", "", row[1], true));
            }
        }

        reserviertTabelle.getItems().clear();
        QueryResult reserviert = model.getMeineReserviertenBuecher();
        if (reserviert != null) {
            for (String[] row : reserviert.getData()) {
                if ("bereit".equals(row[2])) {
                    reserviertTabelle.getItems()
                            .add(new TabellenZeile(row[0], "", "", "bereit zum abholen bis " + row[1], false));
                } else {
                    reserviertTabelle.getItems().add(
                            new TabellenZeile(row[0], "", "", row[1] != null ? row[1] : "Warte auf Rückgabe", false));
                }
            }
        }

        verlaufTabelle.getItems().clear();
        QueryResult verlauf = model.getMeinVerlauf();
        if (verlauf != null) {
            for (String[] row : verlauf.getData()) {
                verlaufTabelle.getItems().add(new TabellenZeile(row[0], row[1], row[2], row[3], false));
            }
        }
    }

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
                    selectBuch();
                    break;
                }
            }
        }
    }

    public void selectBuch() {
        selectedBuch = suchergebnisse.getSelectionModel().getSelectedItem();
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
            if (status.equals("entfernt")) {
                status = "nicht verfügbar";
            }
            statusText.setText("aktueller Status: " + status);
            if (model.reservierungMoeglich(selectedBuch.getIsbn())) {
                if (model.buchGeliehen(selectedBuch.getIsbn())) {
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
}
