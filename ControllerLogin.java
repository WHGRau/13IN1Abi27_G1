import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.io.IOException;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import java.io.File;
import javafx.scene.layout.StackPane;
import javafx.scene.control.PasswordField;
import javafx.scene.input.KeyEvent;
import javafx.scene.transform.Scale;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.input.KeyCode;

public class ControllerLogin {
    private Bibliothek model;

    public void initialize() {
        model = new Bibliothek();
        Platform.runLater(() -> loginButton.requestFocus());
        
        
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

    @FXML
    private Button loginButton;

    @FXML
    private TextField emailFeld;

    @FXML
    private TextField passwortFeld;

    @FXML
    private Text fehlerText;
    
    @FXML
    private MediaView meinVideo;
    
    @FXML
    private StackPane background;
    
    


    public void login(ActionEvent event) {
        if (model.login(emailFeld.getText(), passwortFeld.getText()) == 1) {
            try {
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                if (model.isLehrer()) {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("scenes/LehrerStartseite.fxml"));
                    Parent root = loader.load();
                    ControllerLehrerStartseite controller = loader.getController();
                    controller.setModel(model);
                    Scene scene = new Scene(root);
                    scene.setFill(Color.web("#E9E9D3"));
                    stage.setScene(scene);
                    stage.show();
                } else {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("scenes/SchuelerStartseite.fxml"));
                    Parent root = loader.load();
                    ControllerSchuelerStartseite controller = loader.getController();
                    controller.setModel(model);
                    Scene scene = new Scene(root);
                    scene.setFill(Color.web("#E9E9D3"));
                stage.setScene(scene);
                    stage.show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            fehlerText.setText("Anmeldung fehlgeschlagen");
        }
    }
    
    public void enter(KeyEvent event){
        if (event.getCode().equals(KeyCode.ENTER)){
            passwortFeld.requestFocus();
        }
    }
    
    public void anmeldenEnter(KeyEvent event){
        if (event.getCode().equals(KeyCode.ENTER)){
            loginButton.fire();
        }
    }
    
    
}
