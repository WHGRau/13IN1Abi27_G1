import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.sql.*;

public class Bibliothek {
    private DatabaseConnector dbConnector;

    public Bibliothek() {
        dbVerbinden();
    }

    private void dbVerbinden() {
        dbConnector = new DatabaseConnector("localhost", 3306, "Bibliothek", "root", "");
    }

    public void buchLeihen(String isbn, int schuelerId, int ausleihZeitTage) {
        dbConnector.executeStatement("SELECT status FROM buecher WHERE isbn = '" + isbn + "'");
        QueryResult result = dbConnector.getCurrentQueryResult();

        if (result.getData()[0][0].equals("verfuegbar")) {
            String sql = "INSERT INTO ausleihen (schueler_id, isbn, ausleihdatum, geplante_rueckgabe) "
                    + "VALUES (" + schuelerId + ", '" + isbn + "', CURRENT_DATE(), CURRENT_DATE() + INTERVAL "
                    + ausleihZeitTage + " DAY)";

            dbConnector.executeStatement(sql);
            dbConnector.executeStatement("UPDATE buecher SET status = 'ausgeliehen' WHERE isbn = '" + isbn + "'");
        }
    }

    public void buchRueckgabe(String isbn) {
        dbConnector.executeStatement("SELECT status FROM buecher WHERE isbn = '" + isbn + "'");
        QueryResult result = dbConnector.getCurrentQueryResult();

        if (result.getData()[0][0].equals("ausgeliehen")) {
            dbConnector.executeStatement("UPDATE ausleihen SET rueckgabedatum = CURRENT_DATE() WHERE isbn = '" + isbn
                    + "' AND rueckgabedatum IS NULL");

            dbConnector.executeStatement(
                    "SELECT status FROM reservierungen WHERE isbn = '" + isbn + "' AND status = 'wartend'");
            QueryResult resResult = dbConnector.getCurrentQueryResult();

            if (resResult != null && resResult.getRowCount() > 0) {
                dbConnector.executeStatement("UPDATE buecher SET status = 'reserviert' WHERE isbn = '" + isbn + "'");
            } else {
                dbConnector.executeStatement("UPDATE buecher SET status = 'verfuegbar' WHERE isbn = '" + isbn + "'");
            }
        }
    }

    public void buchHinzufugen(String isbn,String titel, String autor, int jahr, String beschreibung){
        String sql = "INSERT INTO buecher (isbn, titel,autor,erscheinungsjahr, beschreibung, status)"+" VALUES(" + isbn + ", '" + titel + "', '" + autor + "',"+ jahr +",'"+ beschreibung +"','verfuegbar')";
        dbConnector.executeStatement(sql);
    }

    public void buchLoschen(String isbn){
        dbConnector.executeStatement("SELECT * FROM buecher WHERE isbn = " + isbn + "");
        
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null){
            dbConnector.executeStatement("UPDATE buecher SET status = 'entfernt' WHERE isbn = " + isbn + "");
            
        }
    }
    
    public QueryResult buecherSuchen(String pS){
        dbConnector.executeStatement("SELECT isbn, titel, autor, beschreibung, status FROM buecher WHERE (titel LIKE '%" + pS + "%' OR isbn LIKE '%" + pS + "%' )AND status NOT LIKE 'entfernt'");
        
        QueryResult result = dbConnector.getCurrentQueryResult();
        return result;
    }
}
