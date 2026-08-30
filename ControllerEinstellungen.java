import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.paint.Color;

import java.io.IOException;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.transform.Scale;

public class ControllerEinstellungen {

    @FXML
    private TextField emailFeld;
    @FXML
    private PasswordField passwortFeld;
    @FXML
    private TextField serverFeld;
    @FXML
    private TextField portFeld;
    @FXML
    private Button speichernButton;
    @FXML
    private Button zurueckButton;
    @FXML
    private StackPane background;

    @FXML
    private TextField ausleihDauerFeld;
    @FXML
    private CheckBox reservierungenAktivierenCheckBox;
    @FXML
    private TextField reservierungAbholzeitFeld;
    @FXML
    private TextField reservierungSperrzeitFeld;
    @FXML
    private TextField sperrenVerspaetungFeld;
    @FXML
    private CheckBox sperrenAktivierenCheckBox;
    @FXML
    private TextField sperrenZuruecksetzenFeld;
    @FXML
    private ChoiceBox<String> buechersucheDatenbankChoiceBox;
    @FXML
    private TextField buechersucheApiKeyFeld;

    private Bibliothek model;

    @FXML
    public void initialize() {

        background.setStyle("-fx-background-color: #E9E9D3;");

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

    public void setModel(Bibliothek model) {
        this.model = model;

        String email = model.getEinstellung("email_adresse");
        if (email != null)
            emailFeld.setText(email);

        String passwort = model.getEinstellung("email_passwort");
        if (passwort != null)
            passwortFeld.setText(passwort);

        String server = model.getEinstellung("smtp_server");
        if (server != null)
            serverFeld.setText(server);

        String port = model.getEinstellung("smtp_port");
        if (port != null)
            portFeld.setText(port);

        String ausleihDauer = model.getEinstellung("ausleih_dauer_tage");
        if (ausleihDauer != null)
            ausleihDauerFeld.setText(ausleihDauer);
        else
            ausleihDauerFeld.setText("28");

        String resAktiv = model.getEinstellung("reservierungen_aktiv");
        if (resAktiv != null)
            reservierungenAktivierenCheckBox.setSelected(resAktiv.equals("1"));

        String resAbholzeit = model.getEinstellung("reservierung_dauer_tage");
        if (resAbholzeit != null)
            reservierungAbholzeitFeld.setText(resAbholzeit);
        else
            reservierungAbholzeitFeld.setText("14");

        String resSperrzeit = model.getEinstellung("reservierung_sperre_tage");
        if (resSperrzeit != null)
            reservierungSperrzeitFeld.setText(resSperrzeit);
        else
            reservierungSperrzeitFeld.setText("7");

        String sperrenAktiv = model.getEinstellung("sperren_aktiv");
        if (sperrenAktiv != null)
            sperrenAktivierenCheckBox.setSelected(sperrenAktiv.equals("1"));

        String sperrenVerspaetung = model.getEinstellung("sperren_verspaetung_tage");
        if (sperrenVerspaetung != null)
            sperrenVerspaetungFeld.setText(sperrenVerspaetung);

        String sperrenZuruecksetzen = model.getEinstellung("sperren_zuruecksetzen_monate");
        if (sperrenZuruecksetzen != null)
            sperrenZuruecksetzenFeld.setText(sperrenZuruecksetzen);

        buechersucheDatenbankChoiceBox.getItems().addAll("Open Library", "Google Books");
        String buecherDb = model.getEinstellung("buechersuche_datenbank");
        if (buecherDb != null && !buecherDb.isEmpty()) {
            buechersucheDatenbankChoiceBox.setValue(buecherDb);
        } else {
            buechersucheDatenbankChoiceBox.setValue("Open Library");
        }

        String apiKey = model.getEinstellung("buechersuche_api_key");
        if (apiKey != null)
            buechersucheApiKeyFeld.setText(apiKey);

        reservierungAbholzeitFeld.disableProperty().bind(reservierungenAktivierenCheckBox.selectedProperty().not());
        reservierungSperrzeitFeld.disableProperty().bind(reservierungenAktivierenCheckBox.selectedProperty().not());

        sperrenVerspaetungFeld.disableProperty().bind(sperrenAktivierenCheckBox.selectedProperty().not());
        sperrenZuruecksetzenFeld.disableProperty().bind(sperrenAktivierenCheckBox.selectedProperty().not());

        buechersucheDatenbankChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            buechersucheApiKeyFeld.setDisable("Open Library".equals(newVal));
        });
        buechersucheApiKeyFeld.setDisable("Open Library".equals(buechersucheDatenbankChoiceBox.getValue()));
    }

    @FXML
    public void speichern(ActionEvent event) {
        String email = emailFeld.getText().trim();
        String passwort = passwortFeld.getText().trim();
        String server = serverFeld.getText().trim();
        String port = portFeld.getText().trim();

        model.setEinstellung("email_adresse", email);
        model.setEinstellung("email_passwort", passwort);
        model.setEinstellung("smtp_server", server);
        model.setEinstellung("smtp_port", port);

        model.setEinstellung("ausleih_dauer_tage", ausleihDauerFeld.getText().trim());

        model.setEinstellung("reservierungen_aktiv", reservierungenAktivierenCheckBox.isSelected() ? "1" : "0");
        model.setEinstellung("reservierung_dauer_tage", reservierungAbholzeitFeld.getText().trim());
        model.setEinstellung("reservierung_sperre_tage", reservierungSperrzeitFeld.getText().trim());

        model.setEinstellung("sperren_aktiv", sperrenAktivierenCheckBox.isSelected() ? "1" : "0");
        model.setEinstellung("sperren_verspaetung_tage", sperrenVerspaetungFeld.getText().trim());
        model.setEinstellung("sperren_zuruecksetzen_monate", sperrenZuruecksetzenFeld.getText().trim());

        String dbSelection = buechersucheDatenbankChoiceBox.getValue();
        if (dbSelection != null)
            model.setEinstellung("buechersuche_datenbank", dbSelection);

        model.setEinstellung("buechersuche_api_key", buechersucheApiKeyFeld.getText().trim());
        toStartseite(event);
    }

    @FXML
    public void toStartseite(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("scenes/LehrerStartseite.fxml"));
            Parent root = loader.load();

            ControllerLehrerStartseite controller = loader.getController();
            controller.setModel(model);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
