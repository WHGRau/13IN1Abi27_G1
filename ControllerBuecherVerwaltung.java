import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;

import java.io.IOException;

import javafx.event.ActionEvent;

public class ControllerBuecherVerwaltung {
    private Bibliothek model;

    public ControllerBuecherVerwaltung() {

    }

    public void setModel(Bibliothek model) {
        this.model = model;
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
}