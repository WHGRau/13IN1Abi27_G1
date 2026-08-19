
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.transform.Scale;

public class View extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("scenes/login.fxml"));
        primaryStage.setTitle("Eulenmörder");
        
        final double targetWidth = 1920.0;
        final double targetHeight = 1080.0;
        
        Scene scene = new Scene(root, targetWidth, targetHeight);
        Scale scale = new Scale(1, 1, 0, 0);
        scale.xProperty().bind(scene.widthProperty().divide(targetWidth));
        scale.yProperty().bind(scene.heightProperty().divide(targetHeight));
        
        root.getTransforms().add(scale);
        
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
