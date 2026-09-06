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
import javafx.scene.control.ChoiceBox;

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
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.CategoryAxis;


public class ControllerStatistiken
{
    private Bibliothek model;
    
    @FXML
    private StackPane background;
    
    @FXML
    private Button zuruck;
    
    @FXML
    private ChoiceBox<String> statistikAuswahl;
    
    @FXML
    private BarChart<String, Number> bucherGraph;

    
    public void setModel(Bibliothek model) {
        this.model = model;
        
    }
    
    public void initialize(){
        statistikAuswahl.getItems().addAll("beliebteste Bucher", "unbeliebstete Bucher");
        
        statistikAuswahl.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->{
            updateGraphBuch(newValue);
        });
        
        
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

    public void toStartseite(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("scenes/LehrerStartseite.fxml"));
            Parent root = loader.load();
            ControllerLehrerStartseite controller = loader.getController();
            controller.setModel(model);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void updateGraphBuch(String statistik){
        bucherGraph.getData().clear();
        XYChart.Series<String, Number> hauptSerie = new XYChart.Series<>();
        String[] farben = {"#FF5733", "#FFC300", "#3498DB", "#9B59B6", "#1ABC9C"};
        if(statistik.equals("beliebteste Bucher")){
            QueryResult result = model.beliebtesteBucher();
            
            for(int i = 0; i< result.getRowCount(); i++){
                
                XYChart.Data<String, Number> serie = new XYChart.Data<>(result.getData()[i][0], Integer.parseInt(result.getData()[i][1]));
                hauptSerie.getData().add(serie);
                
                final int b = i;
                serie.nodeProperty().addListener((observable, oldNode, newNode) -> {
                    if (newNode != null) {
                        newNode.setStyle("-fx-bar-fill: " + farben[b] + ";");
                    }
                });
            }
            bucherGraph.getData().add(hauptSerie);
            CategoryAxis xAxis = (CategoryAxis) bucherGraph.getXAxis();
            xAxis.setTickLabelRotation(45);
 
        }else if (statistik.equals("unbeliebstete Bucher")){
            QueryResult result = model.unbeliebtesteBucher();
            
            for(int i = 0; i< result.getRowCount(); i++){
                
                XYChart.Series<String, Number> serie = new XYChart.Series<>();
                serie.setName(result.getData()[i][0]); 
                serie.getData().add(new XYChart.Data<>(result.getData()[i][0], Integer.parseInt(result.getData()[i][1])));
                bucherGraph.getData().add(serie);
            }
            CategoryAxis xAxis = (CategoryAxis) bucherGraph.getXAxis();
            
        }
    }
}
