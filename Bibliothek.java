import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.sql.*;
import java.util.ArrayList;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

public class Bibliothek {
    private DatabaseConnector dbConnector;
    private ArrayList<String> erfassteBuecher = new ArrayList<>();
    private Integer erfassterSchueler;
    private Integer angemeldet = null;
    private Argon2PasswordEncoder passwordEncoder = new Argon2PasswordEncoder(16,32,1,60000,10);

    public Bibliothek() {
        dbVerbinden();
        reservierungenAktualisieren();
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

                if (result.getData()[0][0].equals("verfuegbar") || result.getData()[0][0].equals("reserviert")) {
                    String sql = "INSERT INTO ausleihen (schueler_id, isbn, ausleihdatum, geplante_rueckgabe) "
                            + "VALUES (" + erfassterSchueler + ", '" + erfassteBuecher.get(i)
                            + "', CURRENT_DATE(), CURRENT_DATE() + INTERVAL "
                            + ausleihZeitTage + " DAY)";

                    dbConnector.executeStatement(sql);
                    dbConnector.executeStatement(
                            "UPDATE buecher SET status = 'verliehen' WHERE isbn = '" + erfassteBuecher.get(i) + "'");
                    if (result.getData()[0][0].equals("reserviert")) {
                        dbConnector.executeStatement("UPDATE reservierungen SET status = 'abgeschlossen' WHERE isbn = '"
                                + erfassteBuecher.get(i)
                                + "' AND status = 'bereit'");
                    }
                }
            }
            erfassterSchueler = null;
            erfassteBuecher.clear();
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
                    dbConnector.executeStatement(
                            "UPDATE reservierungen SET status = 'bereit', reservierung_ende = DATE_ADD(CURRENT_DATE(), INTERVAL 14 DAY) WHERE isbn = '"
                                    + isbn
                                    + "' AND status = 'wartend'");
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

                if (status.equals("verliehen")) {
                    dbConnector.executeStatement(
                            "UPDATE ausleihen SET ruckgabe_datum = CURRENT_DATE() WHERE isbn = '" + isbn
                                    + "' AND ruckgabe_datum IS NULL");
                }

                dbConnector.executeStatement("UPDATE reservierungen SET status = 'abgesagt' WHERE isbn = '" + isbn
                        + "' AND (status = 'wartend' OR status = 'bereit')");

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
        // 11: Buch ist reserviert, bitte Schüler scannen
        // 12: enthält für Andere reservierte Bücher

        if (isLehrer()) {
            dbConnector.executeStatement("SELECT status FROM buecher WHERE isbn = '" + code + "'");
            QueryResult buchResult = dbConnector.getCurrentQueryResult();

            if (buchResult != null && buchResult.getRowCount() > 0) {
                String status = buchResult.getData()[0][0];

                switch (status) {
                    case "verfuegbar":
                        if (erfassteBuecher.size() > 0) {
                            dbConnector.executeStatement(
                                    "SELECT status FROM buecher WHERE isbn = '" + erfassteBuecher.get(0) + "'");
                            QueryResult firstBookResult = dbConnector.getCurrentQueryResult();
                            if (firstBookResult != null && firstBookResult.getRowCount() > 0
                                    && firstBookResult.getData()[0][0].equals("verliehen")) {
                                return 7;
                            }
                        }
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
                        if (erfassteBuecher.size() > 0) {
                            dbConnector.executeStatement(
                                    "SELECT status FROM buecher WHERE isbn = '" + erfassteBuecher.get(0) + "'");
                            QueryResult firstBookResult = dbConnector.getCurrentQueryResult();
                            if (firstBookResult != null && firstBookResult.getRowCount() > 0
                                    && firstBookResult.getData()[0][0].equals("verliehen")) {
                                return 7;
                            }
                        }
                        dbConnector.executeStatement(
                                "SELECT schueler_id FROM reservierungen WHERE isbn = '" + code
                                        + "' AND status = 'bereit'");
                        if (dbConnector.getCurrentQueryResult() != null
                                && dbConnector.getCurrentQueryResult().getRowCount() > 0) {
                            if (erfassterSchueler == null) {
                                if (!erfassteBuecher.contains(code)) {
                                    erfassteBuecher.add(code);
                                }
                                return 11;
                            } else {
                                int resSchuelerId = Integer
                                        .parseInt(dbConnector.getCurrentQueryResult().getData()[0][0]);
                                if (erfassterSchueler == resSchuelerId) {
                                    if (!erfassteBuecher.contains(code)) {
                                        erfassteBuecher.add(code);
                                    }
                                    return 1;
                                } else {
                                    return 3;
                                }
                            }
                        }
                        break;
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
                        if (checkBuecherReserviert().size() > 0) {
                            return 12;
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

    public void gescanntesBuchEntfernen(int index) {
        if (index >= 0 && index < erfassteBuecher.size()) {
            erfassteBuecher.remove(index);
        }
    }

    public ArrayList<String> getErfassteBuecherNamen() {
        ArrayList<String> list = new ArrayList<String>();
        for (String isbn : erfassteBuecher) {
            dbConnector.executeStatement("SELECT titel FROM buecher WHERE isbn = '" + isbn + "'");
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0) {
                String titel = result.getData()[0][0];
                list.add(titel);
            }
        }
        return list;
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
        // Vorname grosgeschrieben ist das Passwort
        String gespeichertesPasswort;
        dbConnector.executeStatement(
                "SELECT id, passwort FROM benutzer WHERE email = '" + email.toLowerCase() + "'");
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0){
            
            gespeichertesPasswort = result.getData()[0][1];
            boolean passwortStimmt = passwordEncoder.matches(passwort,gespeichertesPasswort);
            if(passwortStimmt){
                angemeldet = Integer.parseInt(result.getData()[0][0]);
                return 1;  
            } 
            
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

    public QueryResult getNutzerVerlauf(int nutzerId) {
        int id;
        if(nutzerId == 0){
            id = angemeldet;
        } else{
            id = nutzerId;
        }
        dbConnector.executeStatement(
                "SELECT buecher.titel, buecher.autor, buecher.isbn, ausleihen.ausleihdatum, ausleihen.geplante_rueckgabe, ausleihen.ruckgabe_datum FROM ausleihen INNER JOIN buecher ON buecher.isbn = ausleihen.isbn WHERE ausleihen.schueler_id = "
                        + id
                        + " AND ausleihen.ruckgabe_datum IS NOT NULL ORDER BY ausleihen.ausleihdatum DESC");
            return dbConnector.getCurrentQueryResult();
    }


    public void reservieren(String isbn) {
        if (angemeldet != null) {
            dbConnector.executeStatement("SELECT freigeschaltet FROM benutzer WHERE id = " + angemeldet + "");
            if (dbConnector.getCurrentQueryResult().getData()[0][0].equals(0)) {
                return;
            }
            if (reservierungMoeglich(isbn)) {
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
                                .executeStatement(
                                        "UPDATE buecher SET status = 'reserviert' WHERE isbn = '" + isbn + "'");
                        dbConnector.executeStatement(
                                "INSERT INTO reservierungen (isbn, schueler_id, status, reservierung_beginn, reservierung_ende) VALUES ('"
                                        + isbn + "', " + angemeldet
                                        + ", 'bereit', CURRENT_DATE(), DATE_ADD(CURRENT_DATE(), INTERVAL 14 DAY))");
                    }
                }
            }
        }
    }

    public boolean reservierungMoeglich(String isbn) {
        if (angemeldet != null) {
            dbConnector.executeStatement("SELECT COUNT(*) FROM reservierungen WHERE schueler_id = " + angemeldet
                    + " AND (status = 'wartend' OR status = 'bereit')");
            QueryResult countResult = dbConnector.getCurrentQueryResult();
            if (countResult != null && countResult.getRowCount() > 0) {
                int count = Integer.parseInt(countResult.getData()[0][0]);
                if (count >= 5) {
                    return false;
                }
            }
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
                    + pVn + "', '" + pNn + "', '" + pEmail.toLowerCase() + "','" + hashen(pPw) + "','" + pRolle + "','"+ 1 +"')";
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
        if(isLehrer()){
            dbConnector.executeStatement("SELECT nachname FROM benutzer WHERE id = '" + pID + "'");
            QueryResult result = dbConnector.getCurrentQueryResult();
            if(result != null){
                dbConnector.executeStatement("UPDATE benutzer SET freigeschaltet = 0 WHERE id = '" + pID + "'");
            
            }
        }
    }

    public void entsperren(int pID){
        if(isLehrer()){
            dbConnector.executeStatement("SELECT nachname FROM benutzer WHERE id = '" + pID + "'");
            QueryResult result = dbConnector.getCurrentQueryResult();
            if(result != null){
                dbConnector.executeStatement("UPDATE benutzer SET freigeschaltet = 1 WHERE id = '" + pID + "'");
            
            }
        }
    }

    public void benutzerBearbeiten(int pID, String pRolle, String pEmail, String pNn, String pVn){
        if(isLehrer()){
            dbConnector.executeStatement("SELECT email FROM benutzer WHERE id = '" + pID + "'");
            QueryResult result = dbConnector.getCurrentQueryResult();
            if(result != null){
                dbConnector.executeStatement("UPDATE benutzer SET vorname = '" + pVn + "', nachname = '" + pNn + "', rolle = '" + pRolle + "', email = '" + pEmail.toLowerCase() + "' WHERE id = '" + pID + "'");
            
            }
        }
    }

    public boolean emailVorhanden(String email, int ignoreId) {
        if (email == null) return false;
        dbConnector.executeStatement("SELECT id FROM benutzer WHERE email = '" + email + "' AND id != " + ignoreId);
        QueryResult result = dbConnector.getCurrentQueryResult();
        return result != null && result.getRowCount() > 0;
    }

    public void passwortAendern(int pID, String pNewPW){
        if(isLehrer() || pID == angemeldet){
            dbConnector.executeStatement("SELECT nachname FROM benutzer WHERE id = '" + pID + "'");
            QueryResult result = dbConnector.getCurrentQueryResult();
            if(result != null){
                dbConnector.executeStatement("UPDATE benutzer SET passwort = '" + hashen(pNewPW) + "' WHERE id = '" + pID + "'");
            
            }
        }
    }

    public void reservierungenAktualisieren() {

        dbConnector.executeStatement(
                "SELECT id, isbn FROM reservierungen WHERE reservierung_ende <= CURRENT_DATE() AND status = 'bereit';");
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            for (int i = 0; i < result.getRowCount(); i++) {
                String id = result.getData()[i][0];
                dbConnector.executeStatement("UPDATE reservierungen SET status = 'abgelaufen' WHERE id = " + id + ";");
                dbConnector.executeStatement(
                        "UPDATE buecher SET status = 'verfuegbar' WHERE isbn = '" + result.getData()[i][1] + "';");
            }
        }
    }

    public String getreserviertSchuelerName(String isbn) {
        dbConnector.executeStatement(
                "SELECT benutzer.nachname, benutzer.vorname FROM reservierungen INNER JOIN benutzer ON reservierungen.schueler_id = benutzer.id WHERE reservierungen.isbn = '"
                        + isbn + "' AND (reservierungen.status = 'wartend' OR reservierungen.status = 'bereit');");
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            return result.getData()[0][0] + " " + result.getData()[0][1];
        }
        return null;
    }

    public ArrayList<String> checkBuecherReserviert() {
        ArrayList<String> reserviert = new ArrayList<String>();
        if (erfassterSchueler == null)
            return reserviert;
        for (int i = 0; i < erfassteBuecher.size(); i++) {
            String isbn = erfassteBuecher.get(i);
            dbConnector.executeStatement("SELECT schueler_id FROM reservierungen WHERE isbn = '"
                    + isbn + "' AND status = 'bereit'");
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0) {
                int resSchuelerId = Integer.parseInt(result.getData()[0][0]);
                if (resSchuelerId != erfassterSchueler) {
                    reserviert.add(isbn);
                }
            }
        }
        return reserviert;
    }

    public ArrayList<String> getKonfliktBuecherNamen() {
        ArrayList<String> namen = new ArrayList<String>();
        ArrayList<String> konfliktIsbns = checkBuecherReserviert();
        for (String isbn : konfliktIsbns) {
            dbConnector.executeStatement("SELECT titel FROM buecher WHERE isbn = '" + isbn + "'");
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0) {
                namen.add(result.getData()[0][0]);
            }
        }
        return namen;
    }

    public String getErfassteSchuelerName() {
        if (erfassterSchueler == null)
            return "";
        dbConnector.executeStatement("SELECT nachname, vorname FROM benutzer WHERE id = " + erfassterSchueler);
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            return result.getData()[0][0] + " " + result.getData()[0][1];
        }
        return "";
    }
    
    public String hashen(String pP){
        // Passwort wird gesaltet und gehasht
        String verschlusselt;
        
        verschlusselt = passwordEncoder.encode(pP);
                
        return verschlusselt;
    }

    public ArrayList<Benutzer> nutzerSuchen(String pS) {
        if (pS == null) {
            pS = "";
        }
        pS = pS.trim().replace("'", "''");
        String[] terms = pS.split("\\s+");
        

        String whereClause = "";
        if (pS.isEmpty()) {
            whereClause = "1=1";
        } else {
            for (int i = 0; i < terms.length; i++) {
                if (i > 0) {
                    whereClause += " OR ";
                }
                whereClause += "(nachname LIKE '%" + terms[i] + "%' ";
                whereClause += "OR vorname LIKE '%" + terms[i] + "%' ";
                whereClause += "OR email LIKE '%" + terms[i] + "%')";
            }
        }

        dbConnector.executeStatement(
                "SELECT id, nachname, vorname, email, passwort, rolle, freigeschaltet FROM benutzer WHERE " + whereClause);

        QueryResult result = dbConnector.getCurrentQueryResult();
        ArrayList<Benutzer> nutzerListe = new ArrayList<>();

        if (result != null) {
            for (int i = 0; i < result.getRowCount(); i++) {
                boolean freigeschaltet = result.getData()[i][6].equals("1");
                Benutzer b = new Benutzer(result.getData()[i][5], result.getData()[i][4],
                        result.getData()[i][3], result.getData()[i][1], result.getData()[i][2], Integer.parseInt(result.getData()[i][0]), freigeschaltet);
                nutzerListe.add(b);
            }
            
            for (int i = 0; i < nutzerListe.size() - 1; i++) {
                for (int j = 0; j < nutzerListe.size() - i - 1; j++) {
                    int score1 = berechneTreffer(nutzerListe.get(j), terms, pS);
                    int score2 = berechneTreffer(nutzerListe.get(j + 1), terms, pS);
                    
                    if (score1 < score2) { 
                        Benutzer temp = nutzerListe.get(j);
                        nutzerListe.set(j, nutzerListe.get(j + 1));
                        nutzerListe.set(j + 1, temp);
                    }
                }
            }
        }
        return nutzerListe;
    }
    
    private int berechneTreffer(Benutzer b, String[] terms, String pS) {
        if (pS.isEmpty()) return 1;
        
        int score = 0;
        String nn = b.getName().toLowerCase();
        String vn = b.getVorname().toLowerCase();
        String em = b.getEmail().toLowerCase();
        
        for (String term : terms) {
            term = term.toLowerCase();
            if (nn.contains(term) || vn.contains(term) || em.contains(term)) {
                score++;
            }
        }
        return score;
    }
    
}
