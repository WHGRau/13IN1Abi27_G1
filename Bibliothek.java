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
    private Integer angemeldet = null;

    public Bibliothek() {
        dbVerbinden();
    }

    private void dbVerbinden() {
        dbConnector = new DatabaseConnector("localhost", 3306, "Bibliothek", "root", "");
    }

    public void buchLeihen(int ausleihZeitTage) {
        if (isLehrer()) {
            for (int i = 0; i < erfassteBuecher.size(); i++) {
                dbConnector
                        .executeStatement("SELECT status FROM buecher WHERE isbn = '" + erfassteBuecher.get(i) + "'");
                QueryResult result = dbConnector.getCurrentQueryResult();

                if (result.getData()[0][0].equals("verfuegbar")) {
                    String sql = "INSERT INTO ausleihen (schueler_id, isbn, ausleihdatum, geplante_rueckgabe) "
                            + "VALUES (" + erfassterSchueler + ", '" + erfassteBuecher.get(i)
                            + "', CURRENT_DATE(), CURRENT_DATE() + INTERVAL "
                            + ausleihZeitTage + " DAY)";

                    dbConnector.executeStatement(sql);
                    dbConnector.executeStatement(
                            "UPDATE buecher SET status = 'verliehen' WHERE isbn = '" + erfassteBuecher.get(i) + "'");
                }
            }
        }
    }

    public void buchRueckgabe() {
        if (isLehrer()) {
            if (erfassteBuecher.isEmpty())
                return;
            String isbn = erfassteBuecher.get(0);
            dbConnector.executeStatement("SELECT status FROM buecher WHERE isbn = '" + isbn + "'");
            QueryResult result = dbConnector.getCurrentQueryResult();

            if (result != null && result.getRowCount() > 0 && result.getData()[0][0].equals("verliehen")) {
                dbConnector
                        .executeStatement("UPDATE ausleihen SET ruckgabe_datum = CURRENT_DATE() WHERE isbn = '" + isbn
                                + "' AND ruckgabe_datum IS NULL");

                dbConnector.executeStatement(
                        "SELECT status FROM reservierungen WHERE isbn = '" + isbn + "' AND status = 'wartend'");
                QueryResult resResult = dbConnector.getCurrentQueryResult();

                if (resResult != null && resResult.getRowCount() > 0) {
                    dbConnector
                            .executeStatement("UPDATE buecher SET status = 'reserviert' WHERE isbn = '" + isbn + "'");
                } else {
                    dbConnector
                            .executeStatement("UPDATE buecher SET status = 'verfuegbar' WHERE isbn = '" + isbn + "'");
                }
            }
        }
    }

    public void buchHinzufuegen(String isbn, String titel, String autor, int jahr, String beschreibung) {
        if (isLehrer()) {
            if (titel != null)
                titel = titel.replace("'", "''");
            if (autor != null)
                autor = autor.replace("'", "''");
            if (beschreibung != null)
                beschreibung = beschreibung.replace("'", "''");

            String sql = "INSERT INTO buecher (isbn, titel,autor,erscheinungsjahr, beschreibung, status)" + " VALUES('"
                    + isbn + "', '" + titel + "', '" + autor + "'," + jahr + ",'" + beschreibung + "','verfuegbar')";
            dbConnector.executeStatement(sql);
        }
    }

    public void buchLoeschen(String isbn) {
        if (isLehrer()) {
            dbConnector.executeStatement("SELECT status FROM buecher WHERE isbn = '" + isbn + "'");
            QueryResult result = dbConnector.getCurrentQueryResult();

            if (result != null && result.getRowCount() > 0) {
                String status = result.getData()[0][0];

                if (status.equals("reserviert")) {
                    dbConnector.executeStatement("UPDATE reservierungen SET status = 'abgesagt' WHERE isbn = '" + isbn
                            + "' AND (status = 'wartend' OR status = 'bereit')");
                } else {
                    if (status.equals("verliehen")) {
                        dbConnector.executeStatement(
                                "UPDATE ausleihen SET ruckgabe_datum = CURRENT_DATE() WHERE isbn = '" + isbn
                                        + "' AND ruckgabe_datum IS NULL");
                    }
                }
                dbConnector.executeStatement("UPDATE buecher SET status = 'entfernt' WHERE isbn = '" + isbn + "'");
            }
        }
    }

    public ArrayList<Buch> buecherSuchen(String pS) {
        if (pS == null)
            pS = "";
        pS = pS.replace("'", "''");
        if (isLehrer()) {
            dbConnector.executeStatement(
                    "SELECT isbn, titel, autor, erscheinungsjahr, beschreibung, status FROM buecher WHERE (titel LIKE '%"
                            + pS + "%' OR isbn LIKE '%" + pS + "%' OR autor LIKE '%" + pS + "%')");
        } else {
            dbConnector.executeStatement(
                    "SELECT isbn, titel, autor, erscheinungsjahr, beschreibung, status FROM buecher WHERE (titel LIKE '%"
                            + pS + "%' OR isbn LIKE '%" + pS + "%' OR autor LIKE '%" + pS
                            + "%') AND status NOT LIKE 'entfernt'");
        }

        QueryResult result = dbConnector.getCurrentQueryResult();
        ArrayList<Buch> buecher = new ArrayList<>();

        if (result != null) {
            for (int i = 0; i < result.getRowCount(); i++) {
                buecher.add(new Buch(result.getData()[i][0], result.getData()[i][1], result.getData()[i][2],
                        result.getData()[i][3], result.getData()[i][4], result.getData()[i][5]));
            }
        }
        return buecher;
    }

    public QueryResult getVerlieheneBuecher() {
        if (isLehrer()) {
            dbConnector.executeStatement(
                    "SELECT buecher.isbn, buecher.titel, benutzer.nachname, benutzer.vorname, benutzer.email, ausleihen.geplante_rueckgabe FROM ausleihen INNER JOIN benutzer ON ausleihen.schueler_id = benutzer.id INNER JOIN buecher ON buecher.isbn = ausleihen.isbn WHERE ausleihen.ruckgabe_datum IS NULL ORDER BY ausleihen.geplante_rueckgabe;");
            return dbConnector.getCurrentQueryResult();
        }
        return null;
    }

    public int scannen(String code) {

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

        if (isLehrer()) {
            dbConnector.executeStatement("SELECT status FROM buecher WHERE isbn = '" + code + "'");
            QueryResult buchResult = dbConnector.getCurrentQueryResult();

            if (buchResult != null && buchResult.getRowCount() > 0) {
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
                        if (dbConnector.getCurrentQueryResult() != null
                                && dbConnector.getCurrentQueryResult().getRowCount() > 0) {
                            return 5;
                        } else {
                            return 2;
                        }
                    case "reserviert":
                        return 3;
                    case "entfernt":
                        return 4;
                    default:
                        break;
                }
            }

            try {
                int schuelerId = Integer.parseInt(code);
                dbConnector.executeStatement("SELECT id FROM benutzer WHERE id = " + schuelerId);
                QueryResult schuelerResult = dbConnector.getCurrentQueryResult();

                if (schuelerResult != null && schuelerResult.getRowCount() > 0) {
                    dbConnector.executeStatement("SELECT freigeschaltet FROM benutzer WHERE id = " + schuelerId);
                    if (dbConnector.getCurrentQueryResult().getData()[0][0].equals("1")) {
                        erfassterSchueler = schuelerId;
                        if (erfassteBuecher.size() < 1) {
                            return 11;
                        }
                        return 6;
                    } else {
                        return 10;
                    }
                }
            } catch (NumberFormatException e) {

            }

            return 8;
        }
        return 0;
    }

    public void abbrechen() {
        erfassteBuecher.clear();
        erfassterSchueler = null;
    }

    public String getErfassteBuecherNamen() {
        if (erfassteBuecher.isEmpty())
            return "";
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
        dbConnector.executeStatement("SELECT DATEDIFF(CURRENT_DATE(), geplante_rueckgabe) FROM ausleihen WHERE isbn = '"
                + isbn + "' AND ruckgabe_datum IS NULL");
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            String daysStr = result.getData()[0][0];
            if (daysStr != null) {
                try {
                    int days = Integer.parseInt(daysStr);
                    if (days > 0)
                        return days;
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    public int login(String email, String passwort) {
        dbConnector.executeStatement(
                "SELECT id FROM benutzer WHERE email = '" + email + "' AND passwort = '" + passwort + "'");
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            angemeldet = Integer.parseInt(result.getData()[0][0]);
            return 1;
        }
        return 0;
    }

    public void logout() {
        angemeldet = null;
    }

    public String getName() {
        dbConnector.executeStatement("SELECT vorname, nachname FROM benutzer WHERE id = " + angemeldet);
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            return result.getData()[0][0] + " " + result.getData()[0][1];
        }
        return "";
    }

    public boolean isLehrer() {
        dbConnector.executeStatement("SELECT rolle FROM benutzer WHERE id = " + angemeldet);
        QueryResult result = dbConnector.getCurrentQueryResult();
        return result != null && result.getRowCount() > 0
                && "lehrer".equals(result.getData()[0][0]);
    }

    public String getVerleihSchuelerName(String isbn) {
        dbConnector.executeStatement(
                "SELECT schueler_id FROM ausleihen WHERE isbn = '" + isbn + "' AND ruckgabe_datum IS NULL");
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            String schuelerId = result.getData()[0][0];
            dbConnector.executeStatement("SELECT vorname, nachname FROM benutzer WHERE id = " + schuelerId);
            result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0) {
                return result.getData()[0][0] + " " + result.getData()[0][1];
            }
        }
        return "";
    }

    public void buchBearbeiten(String isbn, String titel, String autor, int jahr, String beschreibung, String status) {
        if (isLehrer()) {
            if (titel != null)
                titel = titel.replace("'", "''");
            if (autor != null)
                autor = autor.replace("'", "''");
            if (beschreibung != null)
                beschreibung = beschreibung.replace("'", "''");

            dbConnector.executeStatement(
                    "UPDATE buecher SET titel = '" + titel + "', autor = '" + autor + "', erscheinungsjahr = "
                            + jahr + ", beschreibung = '" + beschreibung + "', status = '" + status + "' WHERE isbn = '"
                            + isbn + "'");
        }
    }

    public void buchFreigeben(String isbn) {
        if (isLehrer()) {
            dbConnector.executeStatement("UPDATE buecher SET status = 'verfuegbar' WHERE isbn = '" + isbn + "'");
        }
    }

    public QueryResult getBuchVerlauf(String isbn) {
        if (isLehrer()) {
            dbConnector.executeStatement(
                    "SELECT nachname, vorname, email, ausleihdatum, ruckgabe_datum FROM ausleihen INNER JOIN benutzer ON ausleihen.schueler_id = benutzer.id WHERE isbn = '"
                            + isbn + "'");
            return dbConnector.getCurrentQueryResult();
        }
        return null;
    }

    public QueryResult getMeineGeliehenenBuecher() {
        if (angemeldet != null) {
            dbConnector.executeStatement(
                    "SELECT buecher.titel, ausleihen.geplante_rueckgabe FROM ausleihen INNER JOIN buecher ON buecher.isbn = ausleihen.isbn WHERE ausleihen.schueler_id = "
                            + angemeldet
                            + " AND ausleihen.ruckgabe_datum IS NULL ORDER BY ausleihen.geplante_rueckgabe");
            return dbConnector.getCurrentQueryResult();
        }
        return null;
    }

    public QueryResult getMeineReserviertenBuecher() {
        if (angemeldet != null) {
            dbConnector.executeStatement(
                    "SELECT buecher.titel, reservierungen.reservierung_ende, reservierungen.status FROM reservierungen INNER JOIN buecher ON buecher.isbn = reservierungen.isbn WHERE reservierungen.schueler_id = "
                            + angemeldet
                            + " AND (reservierungen.status = 'wartend' OR reservierungen.status = 'bereit') ORDER BY reservierung_beginn");
            return dbConnector.getCurrentQueryResult();
        }
        return null;
    }

    public QueryResult getMeinVerlauf() {
        if (angemeldet != null) {
            dbConnector.executeStatement(
                    "SELECT buecher.titel, buecher.autor, buecher.isbn, ausleihen.ausleihdatum FROM ausleihen INNER JOIN buecher ON buecher.isbn = ausleihen.isbn WHERE ausleihen.schueler_id = "
                            + angemeldet
                            + " AND ausleihen.ruckgabe_datum IS NOT NULL ORDER BY ausleihen.ausleihdatum DESC");
            return dbConnector.getCurrentQueryResult();
        }
        return null;
    }

    public void reservieren(String isbn) {
        if (angemeldet != null) {
            dbConnector.executeStatement("SELECT status FROM buecher WHERE isbn = '" + isbn + "'");
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0) {
                String status = result.getData()[0][0];

                if (status.equals("verliehen")) {
                    dbConnector.executeStatement(
                            "SELECT id FROM reservierungen WHERE isbn = '" + isbn + "' AND status = 'wartend'");
                    QueryResult resResult = dbConnector.getCurrentQueryResult();
                    if (resResult == null || resResult.getRowCount() == 0) {
                        dbConnector.executeStatement(
                                "INSERT INTO reservierungen (isbn, schueler_id, status, reservierung_beginn, reservierung_ende) VALUES ('"
                                        + isbn + "', " + angemeldet + ", 'wartend', CURRENT_DATE(), NULL)");
                    }
                } else if (status.equals("verfuegbar")) {
                    dbConnector
                            .executeStatement("UPDATE buecher SET status = 'reserviert' WHERE isbn = '" + isbn + "'");
                    dbConnector.executeStatement(
                            "INSERT INTO reservierungen (isbn, schueler_id, status, reservierung_beginn, reservierung_ende) VALUES ('"
                                    + isbn + "', " + angemeldet
                                    + ", 'bereit', CURRENT_DATE(), DATE_ADD(CURRENT_DATE(), INTERVAL 14 DAY))");
                }
            }
        }
    }

    public boolean reservierungMoeglich(String isbn) {
        if (angemeldet != null) {
            dbConnector.executeStatement("SELECT status FROM buecher WHERE isbn = '" + isbn + "'");
            QueryResult result = dbConnector.getCurrentQueryResult();
            dbConnector.executeStatement(
                    "SELECT id FROM reservierungen WHERE isbn = '" + isbn + "' AND status = 'wartend'");
            if (result != null && result.getRowCount() > 0
                    && (result.getData()[0][0].equals("verfuegbar") || result.getData()[0][0].equals("verliehen"))
                    && dbConnector.getCurrentQueryResult().getRowCount() == 0) {
                return true;
            }
        }
        return false;
    }

    public void reservierungStornieren(String isbn) {
        if (angemeldet != null) {
            dbConnector.executeStatement("SELECT id, status FROM reservierungen WHERE isbn = '" + isbn
                    + "' AND (status = 'wartend' OR status = 'bereit') AND schueler_id = " + angemeldet);
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0) {
                if (result.getData()[0][1].equals("bereit")) {
                    dbConnector
                            .executeStatement("UPDATE buecher SET status = 'verfuegbar' WHERE isbn = '" + isbn + "'");
                } else {
                    dbConnector.executeStatement("UPDATE buecher SET status = 'verliehen' WHERE isbn = '" + isbn + "'");
                }
                dbConnector.executeStatement(
                        "UPDATE reservierungen SET status = 'abgesagt' WHERE id = " + result.getData()[0][0]);
            }
        }
    }

    public boolean selbstReserviert(String isbn) {
        if (angemeldet != null) {
            dbConnector.executeStatement("SELECT id FROM reservierungen WHERE isbn = '" + isbn
                    + "' AND (status = 'wartend' OR status = 'bereit') AND schueler_id = " + angemeldet);
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean buchGeliehen(String isbn) {
        if (angemeldet != null) {
            dbConnector.executeStatement("SELECT id FROM ausleihen WHERE isbn = '" + isbn
                    + "' AND schueler_id = " + angemeldet + " AND ruckgabe_datum IS NULL");
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0) {
                return true;
            }
        }
        return false;
    }
    
    public void neuerBenutzer(String pRolle, String pPw, String pEmail, String pNn, String pVn){
        if(isLehrer()){
            String sql = "INSERT INTO benutzer (vorname, nachname, email,passwort,rolle, freigeschaltet)" + " VALUES('"
                    + pVn + "', '" + pNn + "', '" + pEmail + "','" + pPw + "','" + pRolle + "','"+ 1 +"')";
            dbConnector.executeStatement(sql);
        }
    
    }
    
    public void benutzerLoeschen(int pID){
        if(isLehrer()){
            dbConnector.executeStatement("SELECT nachname FROM benutzer WHERE id = '" + pID + "'");
            QueryResult result = dbConnector.getCurrentQueryResult();
            if(result != null){
                dbConnector.executeStatement("DELETE FROM benutzer WHERE id='"+pID+"'");
                }
        }
    }
    
    public void sperren(int pID){
            dbConnector.executeStatement("SELECT nachname FROM benutzer WHERE id = '" + pID + "'");
            QueryResult result = dbConnector.getCurrentQueryResult();
            if(result != null){
                dbConnector.executeStatement("UPDATE benutzer SET freigeschaltet = 1 WHERE id = '" + pID + "'");
            
            }
    }
}
