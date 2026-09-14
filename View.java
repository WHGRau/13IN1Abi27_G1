
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.transform.Scale;
import javafx.scene.paint.Color;

/**
 * Hauptklasse der JavaFX-Anwendung.
 * Startet das grafische Benutzerinterface und lädt die initiale Szene (Login).
 */
public class View extends Application {

    /**
     * Startet die Anwendung und initialisiert das Hauptfenster (Stage).
     * 
     * @param primaryStage Das Hauptfenster der JavaFX-Anwendung.
     * @throws Exception Wenn die FXML-Datei nicht geladen werden kann.
     */
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

    /**
     * Die Main-Methode als Einstiegspunkt für das Programm.
     * 
     * @param args Kommandozeilenargumente.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
