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

public class ControllerPopUp
{
    private Bibliothek model;
    private Buch selectedBuch;
    
    private String barcodePuffer = "";
    private long letzteTastenZeit = 0;
    
    @FXML
    private Button entfernenButton;
    
    @FXML
    private CheckBox schuler;
    
    @FXML
    private Text titelText;
    
    @FXML
    private Text fehlerText;
    
    @FXML
    private TextField schulerFeld;
    
    @FXML
    private StackPane background;
    
    public void setBuch(Buch b, Bibliothek model){
        selectedBuch = b;
        titelText.setText(selectedBuch.getTitel());
        this.model = model;
    }
    
    public void initialize(){
        
        Platform.runLater(() -> {
            Scene scene = background.getScene();
            if (scene != null){
                scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, event -> {
                    if (schuler.isSelected()) {
                        long jetzt = System.currentTimeMillis();
                        if (jetzt - letzteTastenZeit > 100) {
                            barcodePuffer = "";
                        }
                        if (event.getCharacter().matches("[0-9]")) {
                            barcodePuffer += event.getCharacter();
                        }
                        letzteTastenZeit = jetzt;
                    }
                });

                scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                    
                    if (schuler.isSelected() && selectedBuch != null){
                        schulerFeld.setText(barcodePuffer);
                    }
                });
            }
                
        });
    }
    
    public void entfernen(ActionEvent event) {
        if (selectedBuch == null) {
            return;
        }

        String savedIsbn = selectedBuch.getIsbn();

        
        if(schuler.isSelected() && schulerFeld.getText() != null){
                model.buchLoeschenS(savedIsbn, schulerFeld.getText());
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        
                stage.close();
        }
        else if(schuler.isSelected()){
            
            fehlerText.setText("Bitte Schüler scannen");
                
        }
        else{
                model.buchLoeschen(selectedBuch.getIsbn());
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        
                stage.close();
        }
        
        
        

    }
}
