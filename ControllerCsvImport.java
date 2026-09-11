import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.paint.Color;
import java.io.File;
import java.util.ArrayList;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.Loader;
import java.awt.Desktop;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTerminalField;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

public class ControllerCsvImport {

    private Bibliothek model;
    private File selectedCsv;
    private File selectedDirectory;

    @FXML
    private Button zurueckButton;
    @FXML
    private Button csvWaehlenButton;
    @FXML
    private Text dateiNameText;
    @FXML
    private CheckBox ausweiseGenerierenCheckBox;
    @FXML
    private Button pfadWaehlenButton;
    @FXML
    private Text pfadText;
    @FXML
    private Text errorText;
    @FXML
    private Button importierenButton;

    public void setModel(Bibliothek model) {
        this.model = model;
    }

    @FXML
    public void zurueck(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("scenes/nutzerVerwaltung.fxml"));
            Parent root = loader.load();
            ControllerNutzerVerwaltung controller = loader.getController();
            controller.setModel(model);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void csvWaehlen(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("CSV-Datei auswählen");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Dateien", "*.csv"),
                new FileChooser.ExtensionFilter("Alle Dateien", "*.*"));

        Stage stage = (Stage) csvWaehlenButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedCsv = file;
            dateiNameText.setText(file.getName());
            errorText.setText("");
        }
    }

    @FXML
    public void ausweiseOptionGeaendert(ActionEvent event) {
        boolean selected = ausweiseGenerierenCheckBox.isSelected();
        pfadWaehlenButton.setDisable(!selected);
        if (!selected) {
            selectedDirectory = null;
            pfadText.setText("kein Ordner ausgewählt");
        }
    }

    @FXML
    public void pfadWaehlen(ActionEvent event) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Speicherpfad für Ausweise wählen");

        Stage stage = (Stage) pfadWaehlenButton.getScene().getWindow();
        File dir = dirChooser.showDialog(stage);

        if (dir != null) {
            selectedDirectory = dir;
            pfadText.setText(dir.getAbsolutePath());
            errorText.setText("");
        }
    }

    @FXML
    public void importieren(ActionEvent event) {
        if (selectedCsv == null) {
            errorText.setFill(Color.RED);
            errorText.setText("Bitte zuerst eine CSV-Datei auswählen!");
            return;
        }

        if (ausweiseGenerierenCheckBox.isSelected() && selectedDirectory == null) {
            errorText.setFill(Color.RED);
            errorText.setText("Bitte einen Zielordner für die Ausweise auswählen!");
            return;
        }

        try {
            ArrayList<Benutzer> importierteNutzer = model.nutzerAusCsvImportieren(selectedCsv);

            if (importierteNutzer.isEmpty()) {
                errorText.setFill(Color.RED);
                errorText.setText("Keine neuen Nutzer importiert (möglicherweise existieren alle bereits).");
                return;
            }

            if (ausweiseGenerierenCheckBox.isSelected()) {
                String zielOrdner = selectedDirectory.getAbsolutePath();
                File dir = new File(zielOrdner);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                ArrayList<Benutzer> gruppe = new ArrayList<>();
                int batchIndex = 1;
                for (int i = 0; i < importierteNutzer.size(); i++) {
                    gruppe.add(importierteNutzer.get(i));
                    if (gruppe.size() == 4 || i == importierteNutzer.size() - 1) {
                        generiereAusweiseFuerGruppe(gruppe, zielOrdner, batchIndex++);
                        gruppe.clear();
                    }
                }
            }

            errorText.setFill(Color.GREEN);
            errorText.setText("Erfolgreich " + importierteNutzer.size() + " Nutzer importiert"
                    + (ausweiseGenerierenCheckBox.isSelected() ? " und Ausweise generiert." : "."));
            selectedCsv = null;
            selectedDirectory = null;
            dateiNameText.setText("keine Datei ausgewählt");
            pfadText.setText("kein Ordner ausgewählt");
        } catch (Exception e) {
            errorText.setFill(Color.RED);
            errorText.setText("Fehler beim Importieren: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generiereAusweiseFuerGruppe(ArrayList<Benutzer> schulerList, String zielOrdner, int batchIndex) {
        try {
            File temp = new File("eulen/Karten.pdf");
            PDDocument kart = Loader.loadPDF(temp);
            PDAcroForm acroForm = kart.getDocumentCatalog().getAcroForm();

            String vorname = null;
            if (acroForm != null) {
                for (int i = 1; i <= schulerList.size(); i++) {
                    Benutzer b = schulerList.get(i - 1);
                    acroForm.getField("vorname" + i).setValue(b.getVorname());
                    acroForm.getField("nachname" + i).setValue(b.getName());

                    if (i == 1) {

                    }

                    try {
                        Code128Writer barcodeWriter = new Code128Writer();

                        BitMatrix bitMatrix = barcodeWriter.encode(String.valueOf(b.getId()),
                                BarcodeFormat.CODE_128, 300, 100);

                        java.awt.image.BufferedImage barcodeImage = com.google.zxing.client.j2se.MatrixToImageWriter
                                .toBufferedImage(bitMatrix);

                        PDField platzhalterFeld = acroForm.getField("code" + i);

                        if (platzhalterFeld != null && platzhalterFeld instanceof PDTerminalField) {
                            PDRectangle position = ((PDTerminalField) platzhalterFeld).getWidgets().get(0)
                                    .getRectangle();
                            PDImageXObject pdImage = org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
                                    .createFromImage(kart, barcodeImage);

                            try (PDPageContentStream contentStream = new PDPageContentStream(
                                    kart, kart.getPage(0), PDPageContentStream.AppendMode.APPEND, true, true)) {

                                contentStream.drawImage(pdImage,
                                        position.getLowerLeftX(),
                                        position.getLowerLeftY(),
                                        position.getWidth(),
                                        position.getHeight());

                            }

                            platzhalterFeld.setValue("");

                        }

                        acroForm.getFields().remove(platzhalterFeld);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                acroForm.flatten();
            }

            if (vorname != null) {
                File pdfDatei = new File(zielOrdner + File.separator + "Ausweis" + vorname + "_" + batchIndex + ".pdf");
                kart.save(pdfDatei);
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    desktop.open(pdfDatei);
                }
            }

            kart.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
