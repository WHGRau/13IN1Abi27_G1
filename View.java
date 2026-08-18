
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
//toller Kommentar

public class View extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("scenes/login.fxml"));
        primaryStage.setTitle("Eulenmörder");
        primaryStage.setScene(new Scene(root, 1728, 972));
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
