import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Controller für den Bildschirm zum Zurücksetzen bzw. Ändern des Passworts.
 * Handhabt die Eingabe und Validierung des neuen Passworts.
 */
public class passwortResetController {

    private Bibliothek model;
    private Scene previousScene;

    @FXML private PasswordField passwort1;
    @FXML private PasswordField passwort2;
    @FXML private Text errorText;
    @FXML private Button fertigButton;
    @FXML private Button zurueckButton;

    /**
     * Setzt das Model (die Bibliotheks-Instanz) für diesen Controller.
     * @param model Das aktuelle Bibliotheks-Model.
     */
    public void setModel(Bibliothek model) {
        this.model = model;
    }

    /**
     * Speichert die vorherige Szene, um bei einem Abbruch dorthin zurückkehren zu können.
     * @param scene Die Szene, von der aus dieser Controller aufgerufen wurde.
     */
    public void setPreviousScene(Scene scene) {
        this.previousScene = scene;
    }

    /**
     * Bricht den Vorgang ab und navigiert zurück zur vorherigen Ansicht 
     * (Startseite, Login oder gespeicherte Szene).
     * 
     * @param event Das ausgelöste ActionEvent.
     */
    public void zurueck(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            if (previousScene != null) {
                stage.setScene(previousScene);
                stage.show();
            } else if (model != null && (model.isLehrer() || model.isHelfer())) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("scenes/LehrerStartseite.fxml"));
                Parent root = loader.load();
                ControllerLehrerStartseite controller = loader.getController();
                controller.setModel(model);
                Scene scene = new Scene(root);
                scene.setFill(Color.web("#E9E9D3"));
                stage.setScene(scene);
                stage.show();
            } else if (model != null && !model.getName().isEmpty()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("scenes/SchuelerStartseite.fxml"));
                Parent root = loader.load();
                ControllerSchuelerStartseite controller = loader.getController();
                controller.setModel(model);
                Scene scene = new Scene(root);
                scene.setFill(Color.web("#E9E9D3"));
                stage.setScene(scene);
                stage.show();
            } else {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("scenes/login.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root);
                scene.setFill(Color.web("#E9E9D3"));
                stage.setScene(scene);
                stage.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Überprüft die eingegebenen Passwörter auf Übereinstimmung und Mindestlänge.
     * Speichert das neue Passwort bei Erfolg ab und leitet zum Login zurück.
     * 
     * @param event Das ausgelöste ActionEvent.
     */
    public void speichern(ActionEvent event) {
        if (passwort1.getText().equals(passwort2.getText())) {
            if(passwort1.getText().length() < 8) {
                errorText.setText("Passwort muss mindestens 8 Zeichen lang sein");
            } else {
                model.passwortAendern(passwort1.getText());
                try {
                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("scenes/login.fxml"));
                    Parent root = loader.load();
                    Scene scene = new Scene(root);
                    scene.setFill(Color.web("#E9E9D3"));
                    stage.setScene(scene);
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
        } else {
            errorText.setText("Passwörter stimmen nicht überein");
        }
        
    }

}