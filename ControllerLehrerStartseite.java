
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

public class ControllerLehrerStartseite {

    private Bibliothek model;
    private PauseTransition feedbackTimer;
    private ArrayList<String> konfliktNamen = new ArrayList<>();

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
    private TextField ausleihdauerFeld;

    @FXML
    private Text nutzernameText;

    public static class tabelleZeile {
        private String isbn;
        private String titel;
        private String nachname;
        private String vorname;
        private String email;
        private Label geplanteRueckgabe;

        public tabelleZeile(String isbn, String titel, String nachname, String vorname, String email,
                String geplante_Rueckgabe) {
            this.isbn = isbn;
            this.titel = titel;
            this.nachname = nachname;
            this.vorname = vorname;
            this.email = email;

            this.geplanteRueckgabe = new Label(geplante_Rueckgabe);

            String heute = LocalDate.now().toString();
            if (geplante_Rueckgabe.compareTo(heute) < 0) {
                this.geplanteRueckgabe.setStyle("-fx-text-fill: red;");
            }
        }

        public String getIsbn() {
            return isbn;
        }

        public String getTitel() {
            return titel;
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

        public Label getGeplanteRueckgabe() {
            return geplanteRueckgabe;
        }
    }

    public void setModel(Bibliothek model) {
        this.model = model;
        loadVerliehenTabelle();
        nutzernameText.setText("Hallo, " + model.getName() + " !");
    }

    public void initialize() {
        verliehenTabelleIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        verliehenTabelleTitel.setCellValueFactory(new PropertyValueFactory<>("titel"));
        verliehenTabelleName.setCellValueFactory(new PropertyValueFactory<>("nachname"));
        verliehenTabelleVorname.setCellValueFactory(new PropertyValueFactory<>("vorname"));
        verliehenTabelleEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        verliehenTabelleGeplanteRueckgabe.setCellValueFactory(new PropertyValueFactory<>("geplanteRueckgabe"));

        ausleihenButton.setDisable(true);
        zuruecknehmenButton.setDisable(true);

        ausleihdauerFeld.setText("28");

        verliehenTabelle.setPlaceholder(new Label("Keine verliehenen Bücher"));

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
    }

    public void loadVerliehenTabelle() {
        QueryResult result = model.getVerlieheneBuecher();
        if (result != null) {
            verliehenTabelle.getItems().clear();
            String[][] data = result.getData();
            for (int i = 0; i < result.getRowCount(); i++) {
                if (data[i].length >= 6) {
                    String isbn = data[i][0];
                    String titel = data[i][1];
                    String nachname = data[i][2];
                    String vorname = data[i][3];
                    String email = data[i][4];
                    String geplanteRueckgabe = data[i][5];

                    tabelleZeile zeile = new tabelleZeile(isbn, titel, nachname, vorname, email, geplanteRueckgabe);
                    verliehenTabelle.getItems().add(zeile);

                }
            }
        }
    }

    public void scannen() {
        if (feedbackTimer != null)
            feedbackTimer.stop();
        feedbackText.setFill(Color.BLACK);
        String code = codeFeld.getText();
        int feedback = model.scannen(code);
        switch (feedback) {
            case 1:
                if (model.getName() != null && model.getErfassteSchuelerName() != "") {
                    feedbackText.setText("weiteres Buch scannen");
                    ausleihenButton.setDisable(false);
                } else {
                    feedbackText.setText("weiteres Buch oder Nutzerausweis scannen");
                }
                break;
            case 2: {
                int tage = model.getTageZuSpaet(code);
                String msg = "Buch erfasst, bitte 'zurücknehmen' drücken";
                if (tage > 0)
                    msg += " – " + tage + " Tage zu spät!";
                feedbackText.setText(msg);
                zuruecknehmenButton.setDisable(false);
                break;
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
                feedbackText.setText(
                        "Buch bereits verliehen, bitte erst Ausleihvorgang abschließen und danach zurücknehmen");
                break;
            case 8:
                feedbackText.setText("Code nicht erkannt!");
                break;
            case 9:
                feedbackText.setText("Es können maximal 10 Bücher gleichzeitig gescannt werden!");
                break;
            case 10:
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
        }
        codeFeld.clear();
        updateGescanntListe();
    }

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

    public void abbrechen() {
        ausleihenButton.setDisable(true);
        zuruecknehmenButton.setDisable(true);
        model.abbrechen();
        ausleihdauerFeld.setText("28");
        feedbackText.setFill(Color.BLACK);
        feedbackText.setText("Buch scannen");
        updateGescanntListe();
        scannenButton.setDisable(false);
    }

    public void zurueckgeben() {
        model.buchRueckgabe();
        model.abbrechen();
        ausleihdauerFeld.setText("28");
        ausleihenButton.setDisable(true);
        zuruecknehmenButton.setDisable(true);
        feedbackText.setText("Buch erfolgreich zurückgegeben.");
        loadVerliehenTabelle();
        updateGescanntListe();
        feedbackZuruecksetzen();
    }

    public void ausleihen() {
        try {
            int dauer = Integer.parseInt(ausleihdauerFeld.getText());
            if (dauer >= 1 && dauer <= 200) {
                model.buchLeihen(dauer);
                model.abbrechen();
                ausleihdauerFeld.setText("28");
                ausleihenButton.setDisable(true);
                zuruecknehmenButton.setDisable(true);
                feedbackText.setText("Bücher erfolgreich verliehen.");
                loadVerliehenTabelle();
                updateGescanntListe();
                feedbackZuruecksetzen();
            } else {
                feedbackText.setText("Bitte eine gültige Dauer (1-200 Tage) eingeben!");
            }
        } catch (NumberFormatException e) {
            feedbackText.setText("Bitte eine gültige Dauer (1-200 Tage) eingeben!");
        }
    }

    public void logout(ActionEvent event) {
        model.logout();
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("scenes/login.fxml"));
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {

        }
    }

    public void loadBuecherVerwaltung(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("scenes/buchVerwaltung.fxml"));
            Parent root = loader.load();
            ControllerBuecherVerwaltung controller = loader.getController();
            controller.setModel(model);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
