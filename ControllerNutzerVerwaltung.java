import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Label;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableCell;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.util.ArrayList;
import javafx.scene.text.Text;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.geometry.Pos;
import javafx.application.Platform;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import java.io.File;
import org.apache.pdfbox.Loader;
import java.awt.Desktop;



public class ControllerNutzerVerwaltung {
    private Bibliothek model;
    private Benutzer selectedNutzer;
    private boolean bearbeitenAktiv = false;
    private boolean neuAktiv = false;

    @FXML
    private TextField searchBar;

    @FXML
    private TableView<Benutzer> nutzerTabelle;

    @FXML
    private TableColumn<Benutzer, String> nameSpalte;

    @FXML
    private TableColumn<Benutzer, String> vornameSpalte;

    @FXML
    private TextField emailFeld;

    @FXML
    private TextField passwortFeld;

    @FXML
    private TextField nameFeld;

    @FXML
    private TextField vornameFeld;

    @FXML
    private Text statusText;

    @FXML
    private Text errorText;

    @FXML
    private Button bearbeitenButton;

    @FXML
    private Button zurueckButton;

    @FXML
    private Button neuButton;

    @FXML
    private Button entfernenButton;
    
    @FXML
    private ChoiceBox<String> rolleAuswahl;

    @FXML
    private TableView<tabelleZeile> verlaufTabelle;

    @FXML
    private TableColumn<tabelleZeile, String> verlaufIsbnSpalte;

    @FXML
    private TableColumn<tabelleZeile, String> verlaufTitelSpalte;

    @FXML
    private TableColumn<tabelleZeile, String> verlaufGeliehenSpalte;

    @FXML
    private TableColumn<tabelleZeile, String> verlaufGeplRueckgabeSpalte;

    @FXML
    private TableColumn<tabelleZeile, String> verlaufRueckgabeSpalte;
    
    @FXML
    private StackPane background;
    
    @FXML
    private Text hinweisText;
    
    @FXML
    private Button addButton;
    
    @FXML
    private Button druckenButton;
    
    @FXML
    private ListView<Benutzer> schulerList;
    
    @FXML
    private TextField schulerausweis;

    public static class tabelleZeile {
        private String isbn;
        private String titel;
        private String geliehen;
        private String geplRueckgabe;
        private String rueckgabe;

        public tabelleZeile(String isbn, String titel, String geliehen, String geplRueckgabe, String rueckgabe) {
            this.isbn = isbn;
            this.titel = titel;
            this.geliehen = geliehen;
            this.geplRueckgabe = geplRueckgabe;
            this.rueckgabe = rueckgabe;
        }

        public String getIsbn() {
            return isbn;
        }

        public String getTitel() {
            return titel;
        }

        public String getGeliehen() {
            return geliehen;
        }

        public String getGeplRueckgabe() {
            return geplRueckgabe;
        }

        public String getRueckgabe() {
            return rueckgabe;
        }
    }


    public void initialize() {
        rolleAuswahl.getItems().addAll("Schüler", "Lehrer");
        rolleAuswahl.setDisable(true);
        nameSpalte.setCellValueFactory(new PropertyValueFactory<>("name"));
        vornameSpalte.setCellValueFactory(new PropertyValueFactory<>("vorname"));
        nutzerTabelle.setPlaceholder(new Label("Keine Nutzer gefunden"));
        verlaufTabelle.setPlaceholder(new Label("noch kein Buch entliehen"));
        bearbeitenButton.setDisable(true);
        entfernenButton.setDisable(true);

        verlaufIsbnSpalte.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        verlaufTitelSpalte.setCellValueFactory(new PropertyValueFactory<>("titel"));
        verlaufGeliehenSpalte.setCellValueFactory(new PropertyValueFactory<>("geliehen"));
        verlaufGeplRueckgabeSpalte.setCellValueFactory(new PropertyValueFactory<>("geplRueckgabe"));
        verlaufRueckgabeSpalte.setCellValueFactory(new PropertyValueFactory<>("rueckgabe"));


        verlaufRueckgabeSpalte.setCellFactory(column -> new TableCell<tabelleZeile, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    tabelleZeile zeile = getTableRow().getItem();
                    if (zeile != null && zeile.getGeplRueckgabe() != null) {
                        try {
                            LocalDate gepl = LocalDate.parse(zeile.getGeplRueckgabe());
                            LocalDate rueck = LocalDate.parse(item);
                            if (rueck.isAfter(gepl)) {
                                setTextFill(Color.RED);
                            } else {
                                setTextFill(null);
                            }
                        } catch (Exception e) {
                            setTextFill(null);
                        }
                    } else {
                        setTextFill(null);
                    }
                }
            }
        });
        
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
        
        schulerausweis.setEditable(false);
        
        schulerList.setCellFactory(lv -> new javafx.scene.control.ListCell<Benutzer>() {
            @Override
            protected void updateItem(Benutzer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getEmail());
                }
            }
        });
    }

    public void setModel(Bibliothek model) {
        this.model = model;
    }

    public void suchen() {

        String suchbegriff = searchBar.getText();
        ArrayList<Benutzer> ergebnisse = model.nutzerSuchen(suchbegriff);

        nutzerTabelle.getItems().clear();
        if (ergebnisse != null) {
            nutzerTabelle.getItems().addAll(ergebnisse);
        }
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

    public void selectBenutzer() {
        if (!bearbeitenAktiv) {

            selectedNutzer = nutzerTabelle.getSelectionModel().getSelectedItem();
            if (selectedNutzer != null) {
                emailFeld.setText(selectedNutzer.getEmail());
                nameFeld.setText(selectedNutzer.getName());
                vornameFeld.setText(selectedNutzer.getVorname());
                
                if (selectedNutzer.getRolle() != null && selectedNutzer.getRolle().equalsIgnoreCase("lehrer")) {
                    rolleAuswahl.setValue("Lehrer");
                } else {
                    rolleAuswahl.setValue("Schüler");
                }

                bearbeitenButton.setDisable(false);
                if (!selectedNutzer.isFreigeschaltet()) {
                    entfernenButton.setText("freigeben");
                } else {
                    entfernenButton.setText("entfernen");
                }
                entfernenButton.setDisable(false);
                loadVerlaufTabelle();
            }
        }
    }

    public void bearbeiten(ActionEvent event) {
        if (selectedNutzer == null && !neuAktiv) {
            return;
        }
        if (bearbeitenAktiv) {
            if (emailFeld.getText().isEmpty() || nameFeld.getText().isEmpty() || vornameFeld.getText().isEmpty()) {
                errorText.setText("Bitte füllen Sie Name, Vorname und E-Mail aus!");
                return;
            }
            if (model.emailVorhanden(emailFeld.getText(), selectedNutzer.getId())) {
                errorText.setText("Diese E-Mail ist bereits vergeben!");
                return;
            }
            if (!passwortFeld.getText().isEmpty() && passwortFeld.getText().length() < 8) {
                errorText.setText("Das Passwort muss mindestens 8 Zeichen lang sein!");
                return;
            }
            
            bearbeitenAktiv = false;
            bearbeitenButton.setText("bearbeiten");
            emailFeld.setEditable(false);
            nameFeld.setEditable(false);
            vornameFeld.setEditable(false);
            passwortFeld.setEditable(false);
            rolleAuswahl.setDisable(true);
            zurueckButton.setDisable(false);
            neuButton.setDisable(false);

            entfernenButton.setDisable(false);
            
            String rolle = "schueler";
            if ("Lehrer".equals(rolleAuswahl.getValue())) {
                rolle = "lehrer";
            }
            model.benutzerBearbeiten(selectedNutzer.getId(), rolle, emailFeld.getText(), nameFeld.getText(), vornameFeld.getText());
            
            if(!passwortFeld.getText().equals("")){
                model.passwortAendern(selectedNutzer.getId(), passwortFeld.getText());
            }
            suchen();

        } else {
            if (neuAktiv) {
                if (emailFeld.getText().isEmpty() || nameFeld.getText().isEmpty() || vornameFeld.getText().isEmpty() || passwortFeld.getText().isEmpty()) {
                    errorText.setText("Bitte füllen Sie alle Felder aus!");
                    return;
                }
                if (model.emailVorhanden(emailFeld.getText(), -1)) {
                    errorText.setText("Diese E-Mail ist bereits vergeben!");
                    return;
                }
                if (passwortFeld.getText().length() < 8) {
                    errorText.setText("Das Passwort muss mindestens 8 Zeichen lang sein!");
                    return;
                }

                String rolle = "schueler";
                if ("Lehrer".equals(rolleAuswahl.getValue())) {
                    rolle = "lehrer";
                }
                model.neuerBenutzer(rolle, passwortFeld.getText(), emailFeld.getText(),nameFeld.getText(),vornameFeld.getText());
                suchen();
                neuAktiv = false;
                bearbeitenButton.setText("bearbeiten");
                emailFeld.setEditable(false);
                nameFeld.setEditable(false);
                vornameFeld.setEditable(false);
                passwortFeld.setEditable(false);
                rolleAuswahl.setDisable(true);
                zurueckButton.setDisable(false);
                neuButton.setText("neu");
                entfernenButton.setDisable(false);
                emailFeld.clear();
                nameFeld.clear();
                vornameFeld.clear();
                passwortFeld.clear();

            } else {
                bearbeitenAktiv = true;
                bearbeitenButton.setText("speichern");
                emailFeld.setEditable(true);
                nameFeld.setEditable(true);
                vornameFeld.setEditable(true);
                passwortFeld.setEditable(true);
                rolleAuswahl.setDisable(false);
                zurueckButton.setDisable(true);
                neuButton.setDisable(true);
                entfernenButton.setDisable(true);
            }
        }
    }

    public void sperren() {
        if (selectedNutzer == null) {
            return;
        }


        if (!selectedNutzer.isFreigeschaltet()) {
            model.sperren(selectedNutzer.getId());
        } else {
            model.entsperren(selectedNutzer.getId());
        }

        suchen();

        for (Benutzer b : nutzerTabelle.getItems()) {
            if (b.getId() == (selectedNutzer.getId())) {
                nutzerTabelle.getSelectionModel().select(b);
                break;
            }
        }

        selectBenutzer();
    }

    public void nutzerErstellen() {
        if (!neuAktiv) {

            neuButton.setText("abbrechen");
            entfernenButton.setDisable(true);
            bearbeitenButton.setText("speichern");
            bearbeitenButton.setDisable(false);
            emailFeld.clear();
            nameFeld.clear();
            vornameFeld.clear();
            passwortFeld.clear();
            rolleAuswahl.setValue("Schüler");
            emailFeld.setEditable(true);
            nameFeld.setEditable(true);
            vornameFeld.setEditable(true);
            passwortFeld.setEditable(true);
            rolleAuswahl.setDisable(false);
            zurueckButton.setDisable(true);
            neuAktiv = true;
        } else {
            neuButton.setText("neu");
            entfernenButton.setDisable(true);
            bearbeitenButton.setText("bearbeiten");
            emailFeld.clear();
            nameFeld.clear();
            vornameFeld.clear();
            passwortFeld.clear();
            emailFeld.setEditable(false);
            nameFeld.setEditable(false);
            vornameFeld.setEditable(false);
            passwortFeld.setEditable(false);
            rolleAuswahl.setDisable(true);
            zurueckButton.setDisable(false);
            neuAktiv = false;
            bearbeitenButton.setDisable(true);
        }
    }

    public void loadVerlaufTabelle() {
        QueryResult result = model.getNutzerVerlauf(selectedNutzer.getId());
        if (result != null) {
            verlaufTabelle.getItems().clear();
            String[][] data = result.getData();
            for (int i = 0; i < result.getRowCount(); i++) {
                if (data[i].length >= 6) {
                    String titel = data[i][0];
                    String autor = data[i][1];
                    String isbn = data[i][2];
                    String geliehen = data[i][3];
                    String geplRueckgabe = data[i][4];
                    String rueckgabe = data[i][5];

                    tabelleZeile zeile = new tabelleZeile(isbn, titel, geliehen, geplRueckgabe, rueckgabe);
                    verlaufTabelle.getItems().add(zeile);

                }
            }
        }
    }
    
    public void hinzu(ActionEvent event) {
        if (selectedNutzer!= null){
            int anzahl = 4 - schulerList.getItems().size();
            if(selectedNutzer!= null && anzahl > 0){
                schulerList.getItems().add(0, selectedNutzer);
                anzahl=anzahl-1;
                hinweisText.setText("Du kannst noch " + anzahl + " Nutzer hinzufügen");
            
            }
            if(anzahl == 0){
                hinweisText.setText("Bitte drucken");
            }
        }
        
    }
    
    public void druck(ActionEvent event) {
        if(schulerList.getItems().size() != 0){
            try{
                File temp = new File("eulen/Karten.pdf");
                PDDocument kart = Loader.loadPDF(temp);
                PDAcroForm acroForm = kart.getDocumentCatalog().getAcroForm();
                
                String vorname = null;
                if (acroForm != null) {
                    for(int i = 1; i<=schulerList.getItems().size(); i++){
                        Benutzer b = schulerList.getItems().get(i-1);
                        acroForm.getField("vorname" + i).setValue(b.getVorname());
                        acroForm.getField("nachname" + i).setValue(b.getName());
                        vorname = b.getVorname();
                    }
                    acroForm.flatten();
                    
                }

                if(vorname!= null){
                    File pdfDatei = new File("Ausgefuellt_" + vorname + ".pdf");
                    kart.save(pdfDatei);
                    if(Desktop.isDesktopSupported()){
                        Desktop desktop = Desktop.getDesktop();
                        desktop.open(pdfDatei);
                    }
                }
                 
                 
                
                kart.close();
                hinweisText.setText("");
                schulerList.getItems().clear();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}