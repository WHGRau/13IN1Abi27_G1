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
import javafx.scene.layout.AnchorPane;
import javafx.scene.transform.Scale;

public class ControllerLogin {
    private Bibliothek model;

    public void initialize() {
        model = new Bibliothek();
        Platform.runLater(() -> loginButton.requestFocus());
        
        String videoPfad = getClass().getResource("/eulen/hintergrundEulen.mp4").toExternalForm();
        Media  media = new Media(videoPfad);
        MediaPlayer mediaplayer = new MediaPlayer(media);
        meinVideo.setMediaPlayer(mediaplayer);
        mediaplayer.setCycleCount(MediaPlayer.INDEFINITE);
        mediaplayer.play();
        
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
    


    public void login(ActionEvent event) {
        if (model.login(emailFeld.getText(), passwortFeld.getText()) == 1) {
            try {
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                if (model.isLehrer()) {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("scenes/LehrerStartseite.fxml"));
                    Parent root = loader.load();
                    ControllerLehrerStartseite controller = loader.getController();
                    controller.setModel(model);
                    stage.setScene(new Scene(root));
                    stage.show();
                } else {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("scenes/SchuelerStartseite.fxml"));
                    Parent root = loader.load();
                    ControllerSchuelerStartseite controller = loader.getController();
                    controller.setModel(model);
                    stage.setScene(new Scene(root));
                    stage.show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            fehlerText.setText("Anmeldung fehlgeschlagen");
        }
    }
    
    
}
