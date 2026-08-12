import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.sql.*;
import java.util.ArrayList;

public class Bibliothek {
    private DatabaseConnector dbConnector;
    private ArrayList<String> erfassteBuecher = new ArrayList<>();
    private Integer erfassterSchueler;

    public Bibliothek() {
        dbVerbinden();
    }

    private void dbVerbinden() {
        dbConnector = new DatabaseConnector("localhost", 3306, "Bibliothek", "root", "");
    }

    public void buchLeihen(int ausleihZeitTage) {
        for(int i=0; i < erfassteBuecher.size(); i++){
            dbConnector.executeStatement("SELECT status FROM buecher WHERE isbn = '" + erfassteBuecher.get(i) + "'");
            QueryResult result = dbConnector.getCurrentQueryResult();
    
            if (result.getData()[0][0].equals("verfuegbar")) {
                String sql = "INSERT INTO ausleihen (schueler_id, isbn, ausleihdatum, geplante_rueckgabe) "
                        + "VALUES (" + erfassterSchueler + ", '" + erfassteBuecher.get(i) + "', CURRENT_DATE(), CURRENT_DATE() + INTERVAL "
                        + ausleihZeitTage + " DAY)";
    
                dbConnector.executeStatement(sql);
                dbConnector.executeStatement("UPDATE buecher SET status = 'verliehen' WHERE isbn = '" + erfassteBuecher.get(i) + "'");
            }
        }
    }

    public void buchRueckgabe() {
        if (erfassteBuecher.isEmpty()) return;
        String isbn = erfassteBuecher.get(0);
        dbConnector.executeStatement("SELECT status FROM buecher WHERE isbn = '" + isbn + "'");
        QueryResult result = dbConnector.getCurrentQueryResult();

        if (result != null && result.getRowCount() > 0 && result.getData()[0][0].equals("verliehen")) {
            dbConnector.executeStatement("UPDATE ausleihen SET ruckgabe_datum = CURRENT_DATE() WHERE isbn = '" + isbn
                    + "' AND ruckgabe_datum IS NULL");

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

    public QueryResult getVerlieheneBuecher() {
        dbConnector.executeStatement(
                "SELECT buecher.isbn, buecher.titel, benutzer.nachname, benutzer.vorname, benutzer.email, ausleihen.geplante_rueckgabe FROM ausleihen INNER JOIN benutzer ON ausleihen.schueler_id = benutzer.id INNER JOIN buecher ON buecher.isbn = ausleihen.isbn WHERE ausleihen.ruckgabe_datum IS NULL ORDER BY ausleihen.geplante_rueckgabe;");
        return dbConnector.getCurrentQueryResult();
    }

    // 1: Buch kann ausgeliehen werden
    // 2: Buch kann zurueckgegeben werden
    // 3: Buch ist reserviert
    // 4: Buch ist nicht verfügbar
    // 5: Buch kann zurückgegeben werden und ist reserviert
    // 6: Schueler erfasst
    // 7: Buch berits verliehen (ausleihen und zurück geben nicht gleichzeitig)
    // 8: Code ist kein Buch oder Schüler
    // 9: Maximale Anzahl Bücher
    // 10: schueler gesperrt
    // 11: schuler vor Buch gescannt

    // gegen doppelt scannen sichern

    public int scannen(String code) {
                // 1. Suche nach Buch
                dbConnector.executeStatement("SELECT status FROM buecher WHERE isbn = '" + code + "'");
                QueryResult buchResult = dbConnector.getCurrentQueryResult();
    
        if(buchResult!=null&&buchResult.getRowCount()>0)
        {
            // Es ist ein Buch!
            String status = buchResult.getData()[0][0];
    
            switch (status) {
                case "verfuegbar":
                    if (erfassteBuecher.size() >= 10) {
                        return 9;
                    } else {
                        if (!erfassteBuecher.contains(code)) {
                            erfassteBuecher.add(code);
                        }
                        return 1;
                    }
                case "verliehen":
                    if (erfassteBuecher.size() > 0) {
                        return 7;
                    }
                    dbConnector.executeStatement(
                            "SELECT status FROM reservierungen WHERE isbn = '" + code + "' AND status = 'wartend'");
                    if (!erfassteBuecher.contains(code)) {
                        erfassteBuecher.add(code);
                    }
                    if (dbConnector.getCurrentQueryResult() != null && dbConnector.getCurrentQueryResult().getRowCount() > 0) {
                        return 5;
                    } else {
                        return 2;
                    }
                case "reserviert":
                    return 3;
                case "entfernt":
                    return 4;
                default:
                    // Unbekannter Status, breche ab und suche nach Schüler
                    break;
            }
        }
    
        // 2. War kein Buch, also Suche nach Schüler
        // Try-Catch, falls jemand Buchstaben scannt (was keine ID sein kann)
        try
        {
            int schuelerId = Integer.parseInt(code);
            dbConnector.executeStatement("SELECT id FROM benutzer WHERE id = " + schuelerId);
            QueryResult schuelerResult = dbConnector.getCurrentQueryResult();
    
            if (schuelerResult != null && schuelerResult.getRowCount() > 0) {
                dbConnector.executeStatement("SELECT freigeschaltet FROM benutzer WHERE id = " + schuelerId);
                if (dbConnector.getCurrentQueryResult().getData()[0][0].equals("1")) {
                    erfassterSchueler=schuelerId;
                    if(erfassteBuecher.size() < 1){
                        return 11;
                    }
                    return 6; // Schüler erfasst
                } else {
                    return 10; // Schüler gesperrt
                }
            }
        }catch(
        NumberFormatException e)
        {
            // Code war Text (z.B. "HALLO"), kann also keine auto-increment ID sein
        }
    
        // 3. Weder Buch noch Schüler gefunden
        return 8;
    }
    
    public void abbrechen(){
        erfassteBuecher.clear();
        erfassterSchueler = null;
    }
    
    public String getErfassteBuecherNamen() {
        if (erfassteBuecher.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String isbn : erfassteBuecher) {
            dbConnector.executeStatement("SELECT titel FROM buecher WHERE isbn = '" + isbn + "'");
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0) {
                String titel = result.getData()[0][0];
                if (titel.length() > 60) {
                    titel = titel.substring(0, 60) + "...";
                }
                sb.append(titel).append("\n");
            }
        }
        return sb.toString();
    }
    
    public int getTageZuSpaet(String isbn) {
        dbConnector.executeStatement("SELECT DATEDIFF(CURRENT_DATE(), geplante_rueckgabe) FROM ausleihen WHERE isbn = '" + isbn + "' AND ruckgabe_datum IS NULL");
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            String daysStr = result.getData()[0][0];
            if (daysStr != null) {
                try {
                    int days = Integer.parseInt(daysStr);
                    if (days > 0) return days;
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
    
}
