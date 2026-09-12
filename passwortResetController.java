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

public class passwortResetController {

    private Bibliothek model;
    private Scene previousScene;

    @FXML private PasswordField passwort1;
    @FXML private PasswordField passwort2;
    @FXML private Text errorText;
    @FXML private Button fertigButton;
    @FXML private Button zurueckButton;

    public void setModel(Bibliothek model) {
        this.model = model;
    }

    public void setPreviousScene(Scene scene) {
        this.previousScene = scene;
    }

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