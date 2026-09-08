
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.transform.Scale;
import javafx.scene.paint.Color;

public class View extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("scenes/login.fxml"));
        primaryStage.setTitle("Schülerbibliothek");
        
        final double targetWidth = 1920.0;
        final double targetHeight = 1080.0;
        
        Scene scene = new Scene(root, targetWidth, targetHeight);
        
        scene.setFill(Color.web("#E9E9D3"));
        
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
