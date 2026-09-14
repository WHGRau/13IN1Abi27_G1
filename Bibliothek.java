import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.sql.*;
import java.util.ArrayList;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import java.util.Random;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import java.io.IOException;
import java.awt.Desktop;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Die Hauptklasse des Models, die die Geschäftslogik
 * und den Datenbankzugriff für die Bibliotheksverwaltung zentral steuert.
 */
public class Bibliothek {
    private DatabaseConnector dbConnector;
    private ArrayList<String> erfassteBuecher = new ArrayList<>();
    private Integer erfassterSchueler;
    private Integer angemeldet = null;
    private Argon2PasswordEncoder passwordEncoder = new Argon2PasswordEncoder(16, 32, 1, 60000, 10);
    private ArrayList<String> letzteBuecher = new ArrayList<>();
    private int letzterSchueler;
    private boolean letzteAktionAusleihen;
    private String aktuellerModus = "LEER";
    private Random random = new Random();

    /**
     * Konstruktor der Bibliothek. Baut die Datenbankverbindung auf,
     * aktualisiert abgelaufene Reservierungen und prüft/versendet Mahnungen.
     */
    public Bibliothek() {
        dbVerbinden();
        reservierungenAktualisieren();
        erinnerungenPruefenUndVersenden();
        lateDaysAktualisieren();
    }


    /**
     * Prüft anhand des aktuellen Datums alle Ausleihen auf fällige Mahnungen
     * (2 Tage vorher, Stichtag, 1 Woche überfällig) und versendet E-Mails.
     */
    public void erinnerungenPruefenUndVersenden() {
        // 2 Tage vorher Erinnerung
        dbConnector.executeStatement(
                "SELECT ausleihen.id, benutzer.email, benutzer.vorname, buecher.titel, ausleihen.isbn, DATE_FORMAT(ausleihen.geplante_rueckgabe, '%d.%m.%Y') FROM ausleihen INNER JOIN benutzer ON ausleihen.schueler_id = benutzer.id INNER JOIN buecher ON ausleihen.isbn = buecher.isbn WHERE ausleihen.ruckgabe_datum IS NULL AND ausleihen.geplante_rueckgabe <= DATE_ADD(CURRENT_DATE(), INTERVAL 2 DAY) AND ausleihen.geplante_rueckgabe > CURRENT_DATE() AND ausleihen.erinnerung_2tage_gesendet = 0");
        QueryResult result2Tage = dbConnector.getCurrentQueryResult();
        if (result2Tage != null && result2Tage.getRowCount() > 0) {
            MailService mailService = new MailService(this);
            for (int i = 0; i < result2Tage.getRowCount(); i++) {
                String ausleihId = result2Tage.getData()[i][0];
                String email = result2Tage.getData()[i][1];
                String vorname = result2Tage.getData()[i][2];
                String titel = result2Tage.getData()[i][3];
                String datum = result2Tage.getData()[i][5];
                if (email != null && !email.trim().isEmpty() && !email.equals("null")) {
                    mailService.sendeMahnungMail(email, vorname, titel, "2_Tage_vorher", datum);
                }
                dbConnector
                        .executeStatement("UPDATE ausleihen SET erinnerung_2tage_gesendet = 1 WHERE id = ?", ausleihId);
            }
        }

        // Stichtag Erinnerung
        dbConnector.executeStatement(
                "SELECT ausleihen.id, benutzer.email, benutzer.vorname, buecher.titel, ausleihen.isbn, DATE_FORMAT(ausleihen.geplante_rueckgabe, '%d.%m.%Y') FROM ausleihen INNER JOIN benutzer ON ausleihen.schueler_id = benutzer.id INNER JOIN buecher ON ausleihen.isbn = buecher.isbn WHERE ausleihen.ruckgabe_datum IS NULL AND ausleihen.geplante_rueckgabe <= CURRENT_DATE() AND ausleihen.geplante_rueckgabe > DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY) AND ausleihen.erinnerung_heute_gesendet = 0");
        QueryResult resultStichtag = dbConnector.getCurrentQueryResult();
        if (resultStichtag != null && resultStichtag.getRowCount() > 0) {
            MailService mailService = new MailService(this);
            for (int i = 0; i < resultStichtag.getRowCount(); i++) {
                String ausleihId = resultStichtag.getData()[i][0];
                String email = resultStichtag.getData()[i][1];
                String vorname = resultStichtag.getData()[i][2];
                String titel = resultStichtag.getData()[i][3];
                String datum = resultStichtag.getData()[i][5];
                if (email != null && !email.trim().isEmpty() && !email.equals("null")) {
                    mailService.sendeMahnungMail(email, vorname, titel, "Stichtag", datum);
                }
                dbConnector
                        .executeStatement("UPDATE ausleihen SET erinnerung_heute_gesendet = 1 WHERE id = ?", ausleihId);
            }
        }

        // 1 Woche überfällig
        dbConnector.executeStatement(
                "SELECT ausleihen.id, benutzer.email, benutzer.vorname, buecher.titel, ausleihen.isbn, DATE_FORMAT(ausleihen.geplante_rueckgabe, '%d.%m.%Y') FROM ausleihen INNER JOIN benutzer ON ausleihen.schueler_id = benutzer.id INNER JOIN buecher ON ausleihen.isbn = buecher.isbn WHERE ausleihen.ruckgabe_datum IS NULL AND ausleihen.geplante_rueckgabe <= DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY) AND ausleihen.erinnerung_1woche_gesendet = 0");
        QueryResult result1Woche = dbConnector.getCurrentQueryResult();
        if (result1Woche != null && result1Woche.getRowCount() > 0) {
            MailService mailService = new MailService(this);
            for (int i = 0; i < result1Woche.getRowCount(); i++) {
                String ausleihId = result1Woche.getData()[i][0];
                String email = result1Woche.getData()[i][1];
                String vorname = result1Woche.getData()[i][2];
                String titel = result1Woche.getData()[i][3];
                String datum = result1Woche.getData()[i][5];
                if (email != null && !email.trim().isEmpty() && !email.equals("null")) {
                    mailService.sendeMahnungMail(email, vorname, titel, "1_Woche_danach", datum);
                }
                dbConnector.executeStatement(
                        "UPDATE ausleihen SET erinnerung_1woche_gesendet = 1 WHERE id = ?", ausleihId);
            }
        }
    }

    /**
     * Ermittelt den Status der Ausleihen des angemeldeten Schülers.
     * 
     * @return 0 = alles ok, 1 = Rückgabe heute fällig, 2 = überfällig.
     */
    public int tagefuerSchueler() {

        int i = 0;
        dbConnector.executeStatement(
                "SELECT ausleihen.id ,DATE_FORMAT(ausleihen.geplante_rueckgabe, '%d.%m.%Y') FROM ausleihen INNER JOIN benutzer ON ausleihen.schueler_id = benutzer.id INNER JOIN buecher ON ausleihen.isbn = buecher.isbn WHERE ausleihen.ruckgabe_datum IS NULL AND ausleihen.geplante_rueckgabe = CURRENT_DATE() AND benutzer.id = ?",
                angemeldet);
        QueryResult resultStichtag = dbConnector.getCurrentQueryResult();
        if (resultStichtag != null && resultStichtag.getRowCount() > 0)
            i = 1;
        dbConnector.executeStatement(
                "SELECT ausleihen.id, DATE_FORMAT(ausleihen.geplante_rueckgabe, '%d.%m.%Y') FROM ausleihen INNER JOIN benutzer ON ausleihen.schueler_id = benutzer.id INNER JOIN buecher ON ausleihen.isbn = buecher.isbn WHERE ausleihen.ruckgabe_datum IS NULL AND ausleihen.geplante_rueckgabe < CURRENT_DATE() AND benutzer.id = ?",
                angemeldet);
        QueryResult result1Woche = dbConnector.getCurrentQueryResult();
        if (result1Woche != null && result1Woche.getRowCount() > 0)
            i = 2;
        return i;

    }

    /**
     * Stellt die Verbindung zur lokalen MySQL-Datenbank her.
     */
    private void dbVerbinden() {
        dbConnector = new DatabaseConnector("localhost", 3306, "Bibliothek", "root", "");
    }

    /**
     * Lädt einen Einstellungswert aus der Datenbank.
     * 
     * @param schluessel Der Einstellungs-Schlüssel.
     * @return Den Wert als String oder null.
     */
    public String getEinstellung(String schluessel) {
        dbConnector.executeStatement("SELECT wert FROM einstellungen WHERE schluessel = ?", schluessel);
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0 && result.getData()[0][0] != null) {
            return result.getData()[0][0];
        }
        return null;
    }

    /**
     * Speichert einen Einstellungswert in der Datenbank.
     * 
     * @param schluessel Der Einstellungs-Schlüssel.
     * @param wert       Der zu speichernde Wert.
     */
    public void setEinstellung(String schluessel, String wert) {
        dbConnector.executeStatement(
                "INSERT INTO einstellungen (schluessel, wert) VALUES (?, ?) ON DUPLICATE KEY UPDATE wert = ?",
                schluessel, wert, wert);
    }

    /**
     * Leiht alle aktuell erfassten Bücher an den erfassten Schüler aus.
     * 
     * @param ausleihZeitTage Die Ausleihdauer in Tagen.
     */
    public void buchLeihen(int ausleihZeitTage) {
        if (isLehrer()) {
            if (erfassteBuecher.isEmpty() || erfassterSchueler == null)
                return;
            letzteAktionAusleihen = true;
            letzteBuecher.clear();
            letzterSchueler = erfassterSchueler;
            for (int i = 0; i < erfassteBuecher.size(); i++) {
                letzteBuecher.add(erfassteBuecher.get(i));
                dbConnector.executeStatement("SELECT status, anzahlLiehen FROM buecher WHERE isbn = ?", erfassteBuecher.get(i));
                QueryResult result = dbConnector.getCurrentQueryResult();
                if (result != null && result.getRowCount() > 0) {
                    if (result.getData()[0][0].equals("verfuegbar") || result.getData()[0][0].equals("reserviert")) {
                        String sql = "INSERT INTO ausleihen (schueler_id, isbn, ausleihdatum, geplante_rueckgabe, lehrerId) "
                                + "VALUES (?, ?, CURRENT_DATE(), CURRENT_DATE() + INTERVAL ? DAY, ?)";

                        dbConnector.executeStatement(sql, erfassterSchueler, erfassteBuecher.get(i), ausleihZeitTage, angemeldet);
                        hinzuLI(erfassteBuecher.get(i));
                        
                        dbConnector.executeStatement("SELECT id, status FROM reservierungen WHERE schueler_id = ? AND isbn = ? AND (status = 'bereit' OR status = 'wartend') ORDER BY id DESC LIMIT 1", erfassterSchueler, erfassteBuecher.get(i));
                        QueryResult res = dbConnector.getCurrentQueryResult();
                        if (res != null && res.getRowCount() > 0) {
                            String resId = res.getData()[0][0];
                            String resStatus = res.getData()[0][1];
                            dbConnector.executeStatement("UPDATE reservierungen SET status = 'abgeschlossen' WHERE id = ?", resId);
                            if (resStatus.equals("bereit")) {
                                loeschRE(erfassteBuecher.get(i));
                            } else {
                                loeschDA(erfassteBuecher.get(i));
                            }
                        } else {
                            loeschDA(erfassteBuecher.get(i));
                        }
                        updateBuchStatus(erfassteBuecher.get(i));
                    }
                }
            }
            erfassterSchueler = null;
            erfassteBuecher.clear();
            aktuellerModus = "LEER";
        }
    }

    /**
     * Bucht alle aktuell erfassten Bücher als zurückgegeben ein.
     * Prüft, ob das Buch für jemanden reserviert war, und informiert diesen per
     * Mail.
     */
    public void buchRueckgabe() {
        if (isLehrer()) {
            if (erfassteBuecher.isEmpty() || erfassterSchueler == null)
                return;
            letzteBuecher.clear();
            for (int i = 0; i < erfassteBuecher.size(); i++) {
                String isbn = erfassteBuecher.get(i);
                dbConnector.executeStatement("SELECT anzahlLiehen FROM buecher WHERE isbn = ?", isbn);
                QueryResult result = dbConnector.getCurrentQueryResult();
                letzterSchueler = erfassterSchueler;
                if (result != null && result.getRowCount() > 0 && !result.getData()[0][0].equals("0")) {
                    letzteAktionAusleihen = false;
                    letzteBuecher.add(isbn);

                    int verspaetung = getTageZuSpaet(isbn, letzterSchueler);
                    if (verspaetung > 0 && "1".equals(getEinstellung("sperren_aktiv"))) {
                        dbConnector.executeStatement("UPDATE benutzer SET tage_spaet = tage_spaet + ? WHERE id = ?",
                                verspaetung, letzterSchueler);
                        String sperrungTageStr = getEinstellung("sperren_verspaetung_tage");
                        int sperrungTage = 14;
                        if (sperrungTageStr != null && !sperrungTageStr.trim().isEmpty()) {
                            try {
                                sperrungTage = Integer.parseInt(sperrungTageStr.trim());
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        dbConnector.executeStatement("SELECT tage_spaet FROM benutzer WHERE id = ?", letzterSchueler);
                        QueryResult tRes = dbConnector.getCurrentQueryResult();
                        if (tRes != null && tRes.getRowCount() > 0) {
                            try {
                                int curLate = Integer.parseInt(tRes.getData()[0][0]);
                                if (curLate > sperrungTage && !isGesperrt(letzterSchueler)) {
                                    sperren(letzterSchueler, true);
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }

                    dbConnector.executeStatement(
                            "UPDATE ausleihen SET ruckgabe_datum = CURRENT_DATE() WHERE isbn = ? AND ruckgabe_datum IS NULL AND schueler_id = ?",
                            isbn, letzterSchueler);

                    dbConnector.executeStatement(
                            "SELECT status FROM reservierungen WHERE isbn = ? AND status = 'wartend'", isbn);
                    QueryResult resResult = dbConnector.getCurrentQueryResult();

                    if (resResult != null && resResult.getRowCount() > 0) {
                        hinzuRE(isbn);
                        loeschLI(isbn);
                        updateBuchStatus(isbn);

                        updateReservierung(isbn);

                        // Email senden an den wartenden Schüler
                        dbConnector.executeStatement(
                                "SELECT benutzer.email, benutzer.vorname, buecher.titel FROM reservierungen INNER JOIN benutzer ON reservierungen.schueler_id = benutzer.id INNER JOIN buecher ON buecher.isbn = reservierungen.isbn WHERE reservierungen.isbn = ? AND reservierungen.status = 'bereit'",
                                isbn);
                        QueryResult mailResult = dbConnector.getCurrentQueryResult();
                        if (mailResult != null && mailResult.getRowCount() > 0) {
                            String empfaengerEmail = mailResult.getData()[0][0];
                            String vorname = mailResult.getData()[0][1];
                            String titel = mailResult.getData()[0][2];
                            MailService mailService = new MailService(this);
                            mailService.sendeReservierungBereitMail(empfaengerEmail, vorname, titel);
                        }
                    } else {
                        loeschLI(isbn);
                        hinzuDA(isbn);
                        updateBuchStatus(isbn);
                    }
                }
            }
            erfassterSchueler = null;
            erfassteBuecher.clear();
            aktuellerModus = "LEER";
        }
    }

    /**
     * Fügt ein neues Buch zur Datenbank hinzu.
     * 
     * @param isbn         Die ISBN des Buches.
     * @param titel        Der Titel.
     * @param autor        Der Autor.
     * @param jahr         Das Erscheinungsjahr.
     * @param beschreibung Eine Beschreibung.
     * @param alter        Die Altersbeschränkung in Jahren.
     */
    public void buchHinzufuegen(String isbn, String titel, String autor, Integer jahr, String beschreibung,
            String alter) {
        if (isLehrer()) {
            String jahrValue = (jahr != null && jahr > 0) ? String.valueOf(jahr) : null;
            String alterValue = (alter != null && !alter.trim().isEmpty()) ? alter.trim() : null;

            String sql = "INSERT INTO buecher (isbn, titel, autor, erscheinungsjahr, beschreibung, status, altersbeschraenkung, anzahlDa, anzahlLiehen, anzahlRes)"
                    + " VALUES(?, ?, ?, ?, ?, 'verfuegbar', ?, 1, 0, 0)";
            dbConnector.executeStatement(sql, isbn, titel, autor, jahrValue, beschreibung, alterValue);
        }
    }

    public void buchHinzufuegen(String isbn, String titel, String autor, int jahr, String beschreibung) {
        buchHinzufuegen(isbn, titel, autor, Integer.valueOf(jahr), beschreibung, null);
    }

    /**
     * Löscht (reduziert Exemplaranzahl / markiert als 'entfernt') ein Buch aus der Datenbank.
     * 
     * @param isbn Die ISBN des Buches.
     */
    public void buchLoeschen(String isbn) {
        if (isLehrer()) {
            dbConnector.executeStatement("SELECT status FROM buecher WHERE isbn = ?", isbn);
            QueryResult result = dbConnector.getCurrentQueryResult();

            if (result != null && result.getRowCount() > 0) {
                if (maxRes(isbn)) {
                    neusteResAbsagen(isbn);
                    dbConnector.executeStatement("SELECT anzahlDa FROM buecher WHERE isbn = ?", isbn);
                    result = dbConnector.getCurrentQueryResult();
                    if (result != null && result.getRowCount() > 0 && result.getData()[0][0].equals("0")) {
                        loeschRE(isbn);
                    } else {
                        loeschDA(isbn);
                    }
                } else {
                    loeschDA(isbn);
                }
                updateBuchStatus(isbn);
            }
        }
    }

    public void buchLoeschenS(String isbn, String code) {
        if (isLehrer()) {
            int schuelerId = Integer.parseInt(code);
            dbConnector.executeStatement("SELECT status FROM buecher WHERE isbn = ?", isbn);
            QueryResult result = dbConnector.getCurrentQueryResult();

            if (result != null && result.getRowCount() > 0) {
                dbConnector.executeStatement(
                        "UPDATE ausleihen SET ruckgabe_datum = CURRENT_DATE() WHERE isbn = ? AND ruckgabe_datum IS NULL AND schueler_id = ?",
                        isbn, schuelerId);
                
                if (maxRes(isbn)) {
                    neusteResAbsagen(isbn);
                }
                
                loeschLI(isbn);
                updateBuchStatus(isbn);
            }
        }
    }

    public void buchLoeschenR(String isbn, String code) {
        if (isLehrer()) {
            try {
                int schuelerId = Integer.parseInt(code);
                dbConnector.executeStatement("SELECT id, status FROM reservierungen WHERE isbn = ? AND schueler_id = ? AND (status = 'bereit' OR status = 'wartend') LIMIT 1", isbn, schuelerId);
                QueryResult result = dbConnector.getCurrentQueryResult();
                if (result != null && result.getRowCount() > 0) {
                    String resId = result.getData()[0][0];
                    String resStatus = result.getData()[0][1];
                    dbConnector.executeStatement("UPDATE reservierungen SET status = 'abgesagt' WHERE id = ?", resId);
                    if (resStatus.equals("bereit")) {
                        loeschRE(isbn);
                    }
                    updateBuchStatus(isbn);
                }
            } catch (NumberFormatException e) {
            }
        }
    }

    /**
     * Sucht nach Büchern anhand eines Suchbegriffs (Titel, ISBN, Autor).
     * 
     * @param pS Der Suchbegriff.
     * @return Eine Liste von Buch-Objekten, die den Suchkriterien entsprechen.
     */
    public ArrayList<Buch> buecherSuchen(String pS) {
        if (pS == null)
            pS = "";
        String suchbegriff = "%" + pS + "%";
        if (isLehrer()) {

            dbConnector.executeStatement(
                    "SELECT isbn, titel, autor, erscheinungsjahr, beschreibung, status, altersbeschraenkung FROM buecher WHERE "
                            + "(titel LIKE ? OR isbn LIKE ? OR autor LIKE ?)",
                    suchbegriff, suchbegriff, suchbegriff);
        } else {
            dbConnector.executeStatement(
                    "SELECT isbn, titel, autor, erscheinungsjahr, beschreibung, status, altersbeschraenkung FROM buecher WHERE (titel LIKE ?"
                            + " OR isbn LIKE ? OR autor LIKE ?) AND status NOT LIKE 'entfernt'",
                    suchbegriff, suchbegriff, suchbegriff);
        }

        QueryResult result = dbConnector.getCurrentQueryResult();
        ArrayList<Buch> buecher = new ArrayList<>();

        if (result != null) {
            for (int i = 0; i < result.getRowCount(); i++) {
                buecher.add(new Buch(result.getData()[i][0], result.getData()[i][1], result.getData()[i][2],
                        result.getData()[i][3], result.getData()[i][4], result.getData()[i][5],
                        result.getData()[i][6]));
            }
        }
        return buecher;
    }

    /**
     * Liefert eine Übersicht aller aktuell verliehenen Bücher.
     * 
     * @return Ein QueryResult mit den Ausleih-Daten.
     */
    public QueryResult getVerlieheneBuecher() {
        if (isLehrer()) {
            dbConnector.executeStatement(
                    "SELECT buecher.isbn, buecher.titel, benutzer.nachname, benutzer.vorname, benutzer.email, ausleihen.geplante_rueckgabe, manuelle_mahnungen, ausleihen.id FROM ausleihen INNER JOIN benutzer ON ausleihen.schueler_id = benutzer.id INNER JOIN buecher ON buecher.isbn = ausleihen.isbn WHERE ausleihen.ruckgabe_datum IS NULL ORDER BY ausleihen.geplante_rueckgabe;");
            return dbConnector.getCurrentQueryResult();
        }
        return null;
    }

    /**
     * Liefert eine Übersicht aller aktiven Reservierungen.
     * 
     * @return Ein QueryResult mit den Reservierungs-Daten.
     */
    public QueryResult getAlleReservierungen() {
        if (isLehrer()) {
            dbConnector.executeStatement(
                    "SELECT buecher.isbn, buecher.titel, benutzer.nachname, benutzer.vorname, benutzer.email FROM reservierungen INNER JOIN benutzer ON reservierungen.schueler_id = benutzer.id INNER JOIN buecher ON buecher.isbn = reservierungen.isbn WHERE reservierungen.status = 'wartend' OR reservierungen.status = 'bereit' ORDER BY reservierungen.reservierung_beginn;");
            return dbConnector.getCurrentQueryResult();
        }
        return null;
    }
    /**
     * Liefert eine Übersicht aller abholbereiten Reservierungen.
     * 
     * @return Ein QueryResult mit den Reservierungs-Daten.
     */
    public QueryResult getBereiteReservierungen() {
        if (isLehrer()) {
            dbConnector.executeStatement(
                    "SELECT buecher.isbn, buecher.titel, benutzer.nachname, benutzer.vorname, benutzer.email FROM reservierungen INNER JOIN benutzer ON reservierungen.schueler_id = benutzer.id INNER JOIN buecher ON buecher.isbn = reservierungen.isbn WHERE reservierungen.status = 'bereit' ORDER BY reservierungen.reservierung_beginn;");
            return dbConnector.getCurrentQueryResult();
        }
        return null;
    }

    /**
     * Verarbeitet einen gescannten Barcode (ISBN oder Schüler-ID).
     * 
     * @param code Der gescannte Barcode.
     * @return Einen Statuscode, der dem Controller sagt, wie die UI reagieren soll.
     */
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
        // 13: Buch altersbeschraenkt
        // 14: Schueler zu jung
        // 15: Ueberschreitung der maximalen Buecheranzahl pro Schueler
        // 16: Buch kann zuruckgegeben und ausgeliehen werden
        // 17: Schuler furs zuruckgeben
        // 19: Schueler muss gescannt werden, um doppelte ausgabe verhindern zu koennen

        if (isLehrer()) {
            dbConnector.executeStatement("SELECT status, anzahlDa, anzahlLiehen, anzahlRes FROM buecher WHERE isbn = ?", code);
            QueryResult buchResult = dbConnector.getCurrentQueryResult();

            if (buchResult != null && buchResult.getRowCount() > 0) {
                String status = buchResult.getData()[0][0];
                int da = Integer.parseInt(buchResult.getData()[0][1]);
                int li = Integer.parseInt(buchResult.getData()[0][2]);
                
                if (erfassteBuecher.isEmpty()) {
                    if (status.equals("verliehen") || (status.equals("verfuegbar") && li > 0 && bereitsAusgeliehen(code))) {
                        aktuellerModus = "RUECKGABE";
                    } else {
                        aktuellerModus = "AUSLEIHE";
                    }
                } else {
                    if (aktuellerModus.equals("RUECKGABE")) {
                        if (li == 0 && !status.equals("verliehen")) {
                            return 7;
                        }
                        if (erfassterSchueler != null && !bereitsAusgeliehen(code)) {
                            return 7;
                        }
                    } else if (aktuellerModus.equals("AUSLEIHE")) {
                        if (status.equals("verliehen") || (da == 0 && !status.equals("reserviert"))) {
                            return 7;
                        }
                    }
                }

                switch (status) {
                    case "verfuegbar":
                        if (buecherAnzahlUeberschritten()) {
                            return 15;
                        }

                        dbConnector.executeStatement(
                                "SELECT altersbeschraenkung FROM buecher WHERE isbn = ?", code);
                        QueryResult alterRes = dbConnector.getCurrentQueryResult();
                        if (alterRes != null && alterRes.getRowCount() > 0 && alterRes.getData()[0][0] != null) {
                            try {
                                int ab = Integer.parseInt(alterRes.getData()[0][0]);
                                if (ab > 0) {
                                    if (erfassterSchueler == null) {
                                        if (!erfassteBuecher.contains(code)) {
                                            erfassteBuecher.add(code);
                                        }
                                        return 13;
                                    } else {
                                        if (getNutzerAlter(erfassterSchueler) < ab) {
                                            return 14;
                                        }
                                    }
                                }
                            } catch (NumberFormatException e) {
                            }
                        }

                        if (erfassteBuecher.size() >= 10) {
                            return 9;
                        } else {
                            if (!erfassteBuecher.contains(code)) {
                                erfassteBuecher.add(code);
                            }
                            if (li > 0) {
                                if (bereitsAusgeliehen(code)) {
                                    return 2;
                                }
                                if (erfassterSchueler == null) {
                                    return 16;
                                }
                            }
                            return 1;
                        }
                    case "verliehen":
                        dbConnector.executeStatement(
                                "SELECT status FROM reservierungen WHERE isbn = ? AND status = 'wartend'", code);
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
                        dbConnector.executeStatement(
                                "SELECT schueler_id FROM reservierungen WHERE isbn = ? AND status = 'bereit'", code);
                        if (dbConnector.getCurrentQueryResult() != null
                                && dbConnector.getCurrentQueryResult().getRowCount() > 0) {
                            if (erfassterSchueler == null) {
                                if (!erfassteBuecher.contains(code)) {
                                    erfassteBuecher.add(code);
                                }
                                return 11;
                            } else {
                                for (int i = 0; i < dbConnector.getCurrentQueryResult().getRowCount(); i++) {
                                    int resSchuelerId = Integer
                                            .parseInt(dbConnector.getCurrentQueryResult().getData()[i][0]);
                                    if (erfassterSchueler == resSchuelerId) {
                                        if (buecherAnzahlUeberschritten()) {
                                            return 15;
                                        }
                                        if (!erfassteBuecher.contains(code)) {
                                            erfassteBuecher.add(code);
                                        }
                                        return 1;
                                    }
                                }
                                return 3;
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
                dbConnector.executeStatement("SELECT id FROM benutzer WHERE id = ?", schuelerId);
                QueryResult schuelerResult = dbConnector.getCurrentQueryResult();

                if (schuelerResult != null && schuelerResult.getRowCount() > 0) {
                    dbConnector.executeStatement("SELECT freigeschaltet FROM benutzer WHERE id = ?", schuelerId);
                    if (dbConnector.getCurrentQueryResult().getData()[0][0].equals("1")) {
                        erfassterSchueler = schuelerId;
                        if (checkBuecherReserviert().size() > 0) {
                            return 12;
                        }
                        if (!erfassteBuecher.isEmpty() && richtigerSchuelerRuckListe()) {
                            aktuellerModus = "RUECKGABE";
                            return 17;
                        }
                        if (checkBuecherBereitsAusgeliehen().size() > 0) {
                            return 19;
                        }
                        if (checkBuecherAlter().size() > 0) {
                            return 14;
                        }
                        if (buecherAnzahlUeberschritten()) {
                            return 15;
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

    /**
     * Bricht den aktuellen Scan-/Ausleih-Vorgang ab und leert den Puffer.
     */
    public void abbrechen() {
        erfassteBuecher.clear();
        erfassterSchueler = null;
        aktuellerModus = "LEER";
    }

    /**
     * Entfernt ein gescanntes Buch aus dem lokalen Puffer anhand des Index.
     * 
     * @param index Der Listen-Index.
     */
    public void gescanntesBuchEntfernen(int index) {
        if (index >= 0 && index < erfassteBuecher.size()) {
            erfassteBuecher.remove(index);
            if (erfassteBuecher.isEmpty()) {
                aktuellerModus = "LEER";
            }
        }
    }

    /**
     * @return Die Titel aller aktuell im Puffer erfassten Bücher.
     */
    public ArrayList<String> getErfassteBuecherNamen() {
        ArrayList<String> list = new ArrayList<String>();
        for (String isbn : erfassteBuecher) {
            dbConnector.executeStatement("SELECT titel FROM buecher WHERE isbn = ?", isbn);
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0) {
                String titel = result.getData()[0][0];
                list.add(titel);
            }
        }
        return list;
    }
    /**
     * Berechnet, um wie viele Tage ein verliehenes Buch für einen bestimmten Schüler überfällig ist.
     * 
     * @param isbn Die ISBN des Buches.
     * @param schuelerId Die ID des Schülers.
     * @return Die Anzahl der Tage (0 wenn nicht überfällig).
     */
    public int getTageZuSpaet(String isbn, int schuelerId) {
        dbConnector.executeStatement(
                "SELECT DATEDIFF(CURRENT_DATE(), geplante_rueckgabe) FROM ausleihen WHERE isbn = ? AND schueler_id = ? AND ruckgabe_datum IS NULL",
                isbn, schuelerId);
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

    /**
     * Berechnet, um wie viele Tage ein verliehenes Buch überfällig ist.
     * 
     * @param isbn Die ISBN des Buches.
     * @return Die Anzahl der Tage (0 wenn nicht überfällig).
     */
    public int getTageZuSpaet(String isbn) {
        if (erfassterSchueler != null) {
            return getTageZuSpaet(isbn, erfassterSchueler);
        } else {
            dbConnector.executeStatement("SELECT DATEDIFF(CURRENT_DATE(), geplante_rueckgabe) FROM ausleihen WHERE isbn = ? AND ruckgabe_datum IS NULL ORDER BY geplante_rueckgabe ASC", isbn);
        }
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

    /**
     * Überprüft die Login-Daten (E-Mail und Passwort) gegen die Datenbank.
     * 
     * @param email    Die E-Mail.
     * @param passwort Das unverschlüsselte Passwort.
     * @return 1 = normaler Login, 2 = Passwort muss zwingend geändert werden, 0 =
     *         fehlgeschlagen.
     */
    public int login(String email, String passwort) {
        // Vorname grosgeschrieben ist das Passwort
        String gespeichertesPasswort;
        dbConnector.executeStatement(
                "SELECT id, passwort, passwortAendern FROM benutzer WHERE email = ?", email.toLowerCase());
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {

            gespeichertesPasswort = result.getData()[0][1];
            boolean passwortStimmt = passwordEncoder.matches(passwort, gespeichertesPasswort);

            if (passwortStimmt) {
                angemeldet = Integer.parseInt(result.getData()[0][0]);
                if (result.getData()[0][2].equals("1")) {
                    return 2;
                }
                return 1;
            }

        }

        return 0;
    }

    /**
     * Meldet den aktuellen Nutzer ab (setzt die angemeldete ID auf null).
     */
    public void logout() {
        angemeldet = null;
    }

    /**
     * @return Der Vor- und Nachname des aktuell angemeldeten Nutzers.
     */
    public String getName() {
        dbConnector.executeStatement("SELECT vorname, nachname FROM benutzer WHERE id = ? ", angemeldet);
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            return result.getData()[0][0] + " " + result.getData()[0][1];
        }
        return "";
    }

    /**
     * @return True, wenn der angemeldete Nutzer die Rolle "lehrer" hat, sonst
     *         false.
     */
    public boolean isLehrer() {
        dbConnector.executeStatement("SELECT rolle FROM benutzer WHERE id = ?", angemeldet);
        QueryResult result = dbConnector.getCurrentQueryResult();
        return result != null && result.getRowCount() > 0
                && "lehrer".equals(result.getData()[0][0]);
    }

    /**
     * @return True, wenn der angemeldete Nutzer die Rolle "helfer" hat, sonst
     *         false.
     */
    public boolean isHelfer() {
        dbConnector.executeStatement("SELECT rolle FROM benutzer WHERE id = ?", angemeldet);
        QueryResult result = dbConnector.getCurrentQueryResult();
        return result != null && result.getRowCount() > 0
                && "helfer".equals(result.getData()[0][0]);
    }

    /**
     * @param isbn Die ISBN des gesuchten Buches.
     * @return Den Vor- und Nachnamen des Schülers, der das Buch aktuell geliehen
     *         hat.
     */
    public String getVerleihSchuelerName(String isbn) {
        dbConnector.executeStatement(
                "SELECT schueler_id FROM ausleihen WHERE isbn = ? AND ruckgabe_datum IS NULL", isbn);
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            String schuelerId = result.getData()[0][0];
            dbConnector.executeStatement("SELECT vorname, nachname FROM benutzer WHERE id = ?", schuelerId);
            result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0) {
                return result.getData()[0][0] + " " + result.getData()[0][1];
            }
        }
        return "";
    }
    /**
     * Aktualisiert die Daten eines vorhandenen Buches in der Datenbank.
     * 
     * @param isbn         Die ISBN.
     * @param titel        Der neue Titel.
     * @param autor        Der neue Autor.
     * @param jahr         Das neue Erscheinungsjahr.
     * @param beschreibung Die neue Beschreibung.
     * @param status       Der neue Status.
     * @param alter        Die neue Altersbeschränkung.
     */
    public void buchBearbeiten(String isbn, String titel, String autor, Integer jahr, String beschreibung,
            String status, String alter) {
        if (isLehrer()) {
            String jahrValue = (jahr != null && jahr > 0) ? String.valueOf(jahr) : null;
            String alterValue = (alter != null && !alter.trim().isEmpty()) ? alter.trim() : null;

            dbConnector.executeStatement(
                    "UPDATE buecher SET titel = ?, autor = ?, erscheinungsjahr = ?, beschreibung = ?, status = ?"
                            + ", altersbeschraenkung = ? WHERE isbn = ?",
                    titel, autor, jahrValue, beschreibung, status, alterValue, isbn);
        }
    }

    public void buchBearbeiten(String isbn, String titel, String autor, int jahr, String beschreibung) {
        buchBearbeiten(isbn, titel, autor, Integer.valueOf(jahr), beschreibung, "verfuegbar", null);
    }

    /**
     * Setzt den Status eines Buches auf "verfuegbar".
     * 
     * @param isbn Die ISBN des Buches.
     */
    public void buchFreigeben(String isbn) {
        if (isLehrer()) {
            hinzuDA(isbn);
            updateBuchStatus(isbn);
        }
    }

    /**
     * @param isbn Die ISBN des Buches.
     * @return Ein QueryResult mit der Historie (Ausleihen/Rückgaben) dieses Buches.
     */
    public QueryResult getBuchVerlauf(String isbn) {
        if (isLehrer()) {
            dbConnector.executeStatement(
                    "SELECT nachname, vorname, email, ausleihdatum, ruckgabe_datum, lehrerId FROM ausleihen INNER JOIN benutzer ON ausleihen.schueler_id = benutzer.id WHERE isbn = ?",
                    isbn);
            return dbConnector.getCurrentQueryResult();
        }
        return null;
    }

    /**
     * @return Ein QueryResult mit den aktuell geliehenen Büchern des angemeldeten
     *         Nutzers.
     */
    public QueryResult getMeineGeliehenenBuecher() {
        if (angemeldet != null) {
            dbConnector.executeStatement(
                    "SELECT buecher.titel, ausleihen.geplante_rueckgabe, buecher.isbn FROM ausleihen INNER JOIN buecher ON buecher.isbn = ausleihen.isbn WHERE ausleihen.schueler_id = ?"
                            + " AND ausleihen.ruckgabe_datum IS NULL ORDER BY ausleihen.geplante_rueckgabe",
                    angemeldet);
            return dbConnector.getCurrentQueryResult();
        }
        return null;
    }

    /**
     * @return Ein QueryResult mit den aktuellen Reservierungen des angemeldeten
     *         Nutzers.
     */
    public QueryResult getMeineReserviertenBuecher() {
        if (angemeldet != null) {
            dbConnector.executeStatement(
                    "SELECT buecher.titel, reservierungen.reservierung_ende, reservierungen.status, buecher.isbn FROM reservierungen INNER JOIN buecher ON buecher.isbn = reservierungen.isbn WHERE reservierungen.schueler_id = ?"
                            + " AND (reservierungen.status = 'wartend' OR reservierungen.status = 'bereit') ORDER BY reservierung_beginn",
                    angemeldet);
            return dbConnector.getCurrentQueryResult();
        }
        return null;
    }

    /**
     * @param nutzerId Die ID des Nutzers (oder 0 für den angemeldeten Nutzer).
     * @return Ein QueryResult mit dem kompletten Ausleihverlauf dieses Nutzers.
     */
    public QueryResult getNutzerVerlauf(int nutzerId) {
        int id;
        if (nutzerId == 0) {
            id = angemeldet;
        } else {
            id = nutzerId;
        }
        dbConnector.executeStatement(
                "SELECT buecher.titel, buecher.autor, buecher.isbn, ausleihen.ausleihdatum, ausleihen.geplante_rueckgabe, ausleihen.ruckgabe_datum, lehrerId FROM ausleihen INNER JOIN buecher ON buecher.isbn = ausleihen.isbn WHERE ausleihen.schueler_id = ?"
                        + " ORDER BY ausleihen.ausleihdatum DESC",
                id);
        return dbConnector.getCurrentQueryResult();
    }

    /**
     * @return True, wenn der aktuelle Account aktiv (freigeschaltet) ist.
     */
    public boolean isFreigeschaltet() {
        if (angemeldet != null) {
            dbConnector.executeStatement("SELECT freigeschaltet FROM benutzer WHERE id = ?", angemeldet);
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0) {
                return !result.getData()[0][0].equals("0");
            }
        }
        return false;
    }

    /**
     * Legt im Model eine Reservierung für ein Buch an,
     * sofern der angemeldete Nutzer freigeschaltet und berechtigt ist.
     * 
     * @param isbn Die ISBN des Buches.
     */
    public void reservieren(String isbn) {
        if (angemeldet != null) {
            dbConnector.executeStatement("SELECT freigeschaltet FROM benutzer WHERE id = ?", angemeldet);
            if (dbConnector.getCurrentQueryResult().getData()[0][0].equals("0")) {
                return;
            }
            if (reservierungMoeglich(isbn)) {
                dbConnector.executeStatement("SELECT status FROM buecher WHERE isbn = ?", isbn);
                QueryResult result = dbConnector.getCurrentQueryResult();
                if (result != null && result.getRowCount() > 0) {
                    String status = result.getData()[0][0];

                    if (status.equals("verliehen")) {
                        dbConnector.executeStatement(
                                "SELECT COUNT(id) FROM reservierungen WHERE isbn = ? AND status = 'wartend'", isbn);
                        QueryResult resResult = dbConnector.getCurrentQueryResult();
                        
                        int reserviert = Integer.parseInt(resResult.getData()[0][0]);
                        dbConnector.executeStatement("SELECT anzahlDa, anzahlLiehen, anzahlRes FROM buecher WHERE isbn = ?", isbn);
                        QueryResult verfug = dbConnector.getCurrentQueryResult();
                        int existieren = Integer.parseInt(verfug.getData()[0][0]) + Integer.parseInt(verfug.getData()[0][1]) + Integer.parseInt(verfug.getData()[0][2]);
                        if (reserviert < existieren) {
                            dbConnector.executeStatement(
                                    "INSERT INTO reservierungen (isbn, schueler_id, status, reservierung_beginn, reservierung_ende) VALUES (?, ?, 'wartend', CURRENT_DATE(), NULL)", isbn, angemeldet);
                        }
                    } else if (status.equals("verfuegbar")) {
                        hinzuRE(isbn);
                        loeschDA(isbn);
                        updateBuchStatus(isbn);

                        int dauer = getReservierungDauer();
                        dbConnector.executeStatement(
                                "INSERT INTO reservierungen (isbn, schueler_id, status, reservierung_beginn, reservierung_ende) VALUES (?, ?, 'bereit', CURRENT_DATE(), DATE_ADD(CURRENT_DATE(), INTERVAL ? DAY))", isbn, angemeldet, dauer);
                    }
                }
            }
        }
    }

    /**
     * @return Die aktuell konfigurierte Dauer für bereitliegende Reservierungen in
     *         Tagen.
     */
    public int getReservierungDauer() {
        String dauerStr = getEinstellung("reservierung_dauer_tage");
        try {
            if (dauerStr != null) {
                return Integer.parseInt(dauerStr);
            }
        } catch (NumberFormatException e) {
            System.err.println("Ungültiger Wert für reservierung_dauer_tage: " + dauerStr);
        }
        return 7; // Default 7 Tage
    }

    /**
     * @return Die aktuell konfigurierte Ausleihdauer in Tagen.
     */
    public int getAusleihDauer() {
        String dauerStr = getEinstellung("ausleih_dauer_tage");
        try {
            if (dauerStr != null) {
                return Integer.parseInt(dauerStr);
            }
        } catch (NumberFormatException e) {
            System.err.println("Ungültiger Wert für ausleih_dauer_tage: " + dauerStr);
        }
        return 28; // Default 28 Tage
    }

    /**
     * @return Die in den Einstellungen definierte Sperrzeit (in Tagen) nach einer
     *         abgelaufenen Reservierung.
     */
    public int getReservierungSperre() {
        String sperreStr = getEinstellung("reservierung_sperre_tage");
        try {
            if (sperreStr != null)
                return Integer.parseInt(sperreStr);
        } catch (NumberFormatException e) {
        }
        return 7; // Default
    }

    /**
     * @return Die maximal erlaubte Anzahl gleichzeitiger Reservierungen pro Nutzer.
     */
    public int getReservierungMaxAnzahl() {
        String limitStr = getEinstellung("reservierung_max_anzahl");
        try {
            if (limitStr != null) {
                return Integer.parseInt(limitStr);
            }
        } catch (NumberFormatException e) {
        }
        return 5;
    }

    /**
     * Prüft anhand von Limitierungen, Status und Einstellungen,
     * ob der Nutzer das angegebene Buch reservieren darf.
     * 
     * @param isbn Die ISBN des Buches.
     * @return True, falls eine Reservierung möglich ist, sonst false.
     */
    public boolean reservierungMoeglich(String isbn) {
        String resAktiv = getEinstellung("reservierungen_aktiv");
        if (resAktiv != null && resAktiv.equals("0")) {
            return false;
        }

        if (angemeldet != null) {
            int sperre = getReservierungSperre();
            dbConnector.executeStatement("SELECT COUNT(*) FROM reservierungen WHERE schueler_id = ?"
                    + " AND isbn = ?"
                    + " AND status = 'abgelaufen' AND reservierung_ende >= DATE_SUB(CURRENT_DATE(), INTERVAL "
                    + "? DAY)", angemeldet, isbn, sperre);
            QueryResult blockResult = dbConnector.getCurrentQueryResult();
            if (blockResult != null && blockResult.getRowCount() > 0) {
                int blockCount = Integer.parseInt(blockResult.getData()[0][0]);
                if (blockCount > 0) {
                    return false;
                }
            }

            dbConnector.executeStatement("SELECT COUNT(*) FROM reservierungen WHERE schueler_id =? "
                    + " AND (status = 'wartend' OR status = 'bereit')", angemeldet);
            QueryResult countResult = dbConnector.getCurrentQueryResult();
            if (countResult != null && countResult.getRowCount() > 0) {
                int count = Integer.parseInt(countResult.getData()[0][0]);
                int maxAnzahl = getReservierungMaxAnzahl();
                if (count >= maxAnzahl) {
                    return false;
                }
            }
            dbConnector.executeStatement("SELECT status, anzahlLiehen FROM buecher WHERE isbn = ?", isbn);
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0
                    && (result.getData()[0][0].equals("verfuegbar") || result.getData()[0][0].equals("verliehen"))) {
                int verliehen = Integer.parseInt(result.getData()[0][1]);
                dbConnector.executeStatement(
                        "SELECT COUNT(id) FROM reservierungen WHERE isbn = ? AND status = 'wartend'", isbn);
                QueryResult wartendResult = dbConnector.getCurrentQueryResult();
                int wartend = Integer.parseInt(wartendResult.getData()[0][0]);
                
                if (wartend < verliehen || (verliehen == 0 && wartend == 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Storniert eine eigene Reservierung des angemeldeten Nutzers.
     * 
     * @param isbn Die ISBN des Buches.
     */
    public void reservierungStornieren(String isbn) {
        if (angemeldet != null) {
            dbConnector.executeStatement("SELECT id, status FROM reservierungen WHERE isbn = ?"
                    + " AND (status = 'wartend' OR status = 'bereit') AND schueler_id = ?", isbn, angemeldet);
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0) {
                if (result.getData()[0][1].equals("bereit")) {
                    hinzuDA(isbn);
                    loeschRE(isbn);
                    updateBuchStatus(isbn);
                } else {
                    updateBuchStatus(isbn);
                }
                dbConnector.executeStatement(
                        "UPDATE reservierungen SET status = 'abgesagt' WHERE id = ?", result.getData()[0][0]);
            }
        }
    }

    /**
     * Prüft, ob der angemeldete Nutzer dieses Buch selbst reserviert hat.
     * 
     * @param isbn Die ISBN des Buches.
     * @return True, wenn reserviert, sonst false.
     */
    public boolean selbstReserviert(String isbn) {
        if (angemeldet != null) {
            dbConnector.executeStatement("SELECT id FROM reservierungen WHERE isbn = ?"
                    + " AND (status = 'wartend' OR status = 'bereit') AND schueler_id = ?", isbn, angemeldet);
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Prüft, ob der angemeldete Nutzer dieses Buch aktuell ausgeliehen hat.
     * 
     * @param isbn Die ISBN des Buches.
     * @return True, wenn geliehen, sonst false.
     */
    public boolean buchGeliehen(String isbn) {
        if (angemeldet != null) {
            dbConnector.executeStatement("SELECT id FROM ausleihen WHERE isbn = ?"
                    + " AND schueler_id = ? AND ruckgabe_datum IS NULL", isbn, angemeldet);
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Legt einen neuen Nutzer in der Datenbank an (ohne spezifisches Ausleihlimit).
     * 
     * @param pRolle        Die Rolle (z.B. "schueler").
     * @param pEmail        Die E-Mail-Adresse.
     * @param pNn           Der Nachname.
     * @param pVn           Der Vorname.
     * @param pGeburtsdatum Das Geburtsdatum im SQL-Format.
     */
    public void neuerBenutzer(String pRolle, String pEmail, String pNn, String pVn, String pGeburtsdatum) {
        neuerBenutzer(pRolle, pEmail, pNn, pVn, pGeburtsdatum, 0);
    }

    /**
     * Legt einen neuen Nutzer in der Datenbank an.
     * 
     * @param pRolle        Die Rolle (z.B. "schueler", "lehrer").
     * @param pEmail        Die E-Mail-Adresse.
     * @param pNn           Der Nachname.
     * @param pVn           Der Vorname.
     * @param pGeburtsdatum Das Geburtsdatum im SQL-Format (YYYY-MM-DD).
     * @param pMaxBuecher   Die maximale Anzahl an Büchern, die der Nutzer
     *                      gleichzeitig leihen darf.
     */
    public void neuerBenutzer(String pRolle, String pEmail, String pNn, String pVn, String pGeburtsdatum,
            int pMaxBuecher) {
        if (isLehrer()) {
            String passwort = Integer.toString(random.nextInt(10000000, 100000000));
            String gebDatumSql = (pGeburtsdatum == null || pGeburtsdatum.isEmpty()) ? null
                    : pGeburtsdatum;
            String emailSql = (pEmail == null || pEmail.trim().isEmpty()) ? null : pEmail.toLowerCase();
            String sql = "INSERT INTO benutzer (vorname, nachname, email,passwort,rolle, freigeschaltet, geburtsdatum, passwortAendern, maxBuecherGleichzeitig)"
                    + " VALUES(?,?,?,?,?,?,?,'1',?)";
            dbConnector.executeStatement(sql, pVn, pNn, emailSql, hashen(passwort), pRolle, 1, gebDatumSql,
                    pMaxBuecher);
            if (pEmail != null && !pEmail.trim().isEmpty()) {
                initialesPasswortSenden(pEmail);
            }
        }

    }

    /**
     * Liest eine CSV-Datei ein und legt für jeden validen Eintrag einen neuen
     * Schüler an.
     * 
     * @param csvDatei Die eingelesene CSV-Datei.
     * @return Eine Liste der importierten Benutzer.
     */
    public ArrayList<Benutzer> nutzerAusCsvImportieren(java.io.File csvDatei) {
        ArrayList<Benutzer> importierteNutzer = new ArrayList<>();
        if (!isLehrer())
            return importierteNutzer;

        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(csvDatei))) {
            String zeile;
            boolean ersteZeile = true;

            while ((zeile = br.readLine()) != null) {
                zeile = zeile.replace("\uFEFF", "");
                if (zeile.trim().isEmpty())
                    continue;

                String[] spalten = zeile.split("[,;]");

                if (spalten.length >= 3) {
                    String nachname = spalten[0].trim();
                    String vorname = spalten[1].trim();
                    String geburtsdatum = spalten[2].trim();
                    String email = "";
                    if (spalten.length >= 4) {
                        email = spalten[3].trim();
                    }

                    int maxBuecher = getStandartAusleihlimit();

                    if (!email.isEmpty() && emailVorhanden(email)) {
                        continue;
                    }

                    String gebDatumCheckSql = (geburtsdatum.isEmpty()) ? "geburtsdatum IS NULL"
                            : "geburtsdatum = ?";
                    String checkSql = "SELECT id FROM benutzer WHERE LOWER(TRIM(vorname)) = ?"
                            + " AND (LOWER(TRIM(nachname)) = ?"
                            + " OR LOWER(TRIM(nachname)) = ?)"
                            + " AND " + gebDatumCheckSql;
                    QueryResult existResult;
                    if (geburtsdatum.isEmpty()) {
                        dbConnector.executeStatement(checkSql, vorname.toLowerCase().trim(),
                                nachname.toLowerCase().trim(), ("\uFEFF" + nachname).toLowerCase().trim());
                    } else {
                        dbConnector.executeStatement(checkSql, vorname.toLowerCase().trim(),
                                nachname.toLowerCase().trim(), ("\uFEFF" + nachname).toLowerCase().trim(),
                                geburtsdatum);
                    }
                    existResult = dbConnector.getCurrentQueryResult();

                    if (existResult != null && existResult.getRowCount() > 0) {
                        continue;
                    }

                    neuerBenutzer("schueler", email, nachname, vorname, geburtsdatum, maxBuecher);

                    String sql;
                    if (!email.isEmpty()) {
                        sql = "SELECT id, nachname, vorname, email, passwort, rolle, freigeschaltet, gesperrt_von, geburtsdatum, maxBuecherGleichzeitig FROM benutzer WHERE email = ?";
                        dbConnector.executeStatement(sql, email.toLowerCase());
                    } else {
                        sql = "SELECT id, nachname, vorname, email, passwort, rolle, freigeschaltet, gesperrt_von, geburtsdatum, maxBuecherGleichzeitig FROM benutzer WHERE vorname = ?"
                                + " AND nachname = ? AND " + gebDatumCheckSql
                                + " ORDER BY id DESC LIMIT 1";
                        if (geburtsdatum.isEmpty()) {
                            dbConnector.executeStatement(sql, vorname, nachname);
                        } else {
                            dbConnector.executeStatement(sql, vorname, nachname, geburtsdatum);
                        }

                    }
                    QueryResult result = dbConnector.getCurrentQueryResult();
                    if (result != null && result.getRowCount() > 0) {
                        boolean freigeschaltet = result.getData()[0][6] != null && result.getData()[0][6].equals("1");
                        int gesperrtVon = (result.getData()[0][7] != null
                                && !result.getData()[0][7].equalsIgnoreCase("null")
                                && !result.getData()[0][7].trim().isEmpty()) ? Integer.parseInt(result.getData()[0][7])
                                        : 0;
                        String geb = (result.getData()[0].length > 8 && result.getData()[0][8] != null
                                && !result.getData()[0][8].equalsIgnoreCase("null")) ? result.getData()[0][8] : null;
                        int maxB = 0;
                        if (result.getData()[0].length > 9 && result.getData()[0][9] != null
                                && !result.getData()[0][9].equalsIgnoreCase("null")
                                && !result.getData()[0][9].trim().isEmpty()) {
                            try {
                                maxB = Integer.parseInt(result.getData()[0][9]);
                            } catch (NumberFormatException e) {
                                maxB = 0;
                            }
                        }

                        Benutzer b = new Benutzer(result.getData()[0][5], result.getData()[0][4],
                                result.getData()[0][3], result.getData()[0][1], result.getData()[0][2],
                                Integer.parseInt(result.getData()[0][0]), freigeschaltet, gesperrtVon, geb,
                                maxB);
                        importierteNutzer.add(b);
                    }
                }
            }
        } catch (Exception e) {

        }

        return importierteNutzer;
    }

    /**
     * Löscht den Benutzer mit der angegebenen ID aus der Datenbank.
     * 
     * @param pID Die ID des Benutzers.
     */
    public void benutzerLoeschen(int pID) {
        if (isLehrer()) {
            dbConnector.executeStatement("SELECT nachname FROM benutzer WHERE id = ?", pID);
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null) {
                dbConnector.executeStatement("DELETE FROM benutzer WHERE id=?", pID);
            }
        }
    }

    /**
     * Sperrt einen Benutzer (freigeschaltet = 0) und speichert, wer ihn gesperrt
     * hat.
     * 
     * @param pID Die ID des Benutzers.
     */
    public void sperren(int pID, boolean automatisch) {

        dbConnector.executeStatement("SELECT email, vorname FROM benutzer WHERE id = ?", pID);
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            if (automatisch) {
                dbConnector.executeStatement("UPDATE benutzer SET freigeschaltet = 0, gesperrt_von = NULL"
                        + " WHERE id = ?", pID);
            } else {
                dbConnector.executeStatement("UPDATE benutzer SET freigeschaltet = 0, gesperrt_von = ?"
                        + " WHERE id = ?", angemeldet, pID);
            }

            String email = result.getData()[0][0];
            String vorname = result.getData()[0][1];
            MailService mailService = new MailService(this);
            mailService.sendeGesperrtMail(email, vorname);

        }
    }

    /**
     * Entsperrt einen zuvor gesperrten Benutzer (freigeschaltet = 1).
     * 
     * @param pID Die ID des Benutzers.
     */
    public void entsperren(int pID) {
        if (isLehrer()) {
            dbConnector.executeStatement("SELECT nachname FROM benutzer WHERE id = ?", pID);
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null) {
                dbConnector.executeStatement(
                        "UPDATE benutzer SET freigeschaltet = 1, gesperrt_von = NULL WHERE id = ?", pID);
                dbConnector.executeStatement("UPDATE benutzer SET tage_spaet = 0 WHERE id = ?", pID);

            }
        }
    }

    /**
     * @param pID Die ID des Benutzers.
     * @return Vor- und Nachname des Benutzers oder "Unbekannt".
     */
    public String getBenutzerName(int pID) {
        dbConnector.executeStatement("SELECT vorname, nachname FROM benutzer WHERE id = ?", pID);
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            return result.getData()[0][0] + " " + result.getData()[0][1];
        }
        return "Unbekannt";
    }

    /**
     * Bearbeitet einen Benutzer in der Datenbank (ohne Max-Bücher-Limit anzugeben).
     * 
     * @param pID           Die ID.
     * @param pRolle        Die neue Rolle.
     * @param pEmail        Die neue E-Mail.
     * @param pNn           Der neue Nachname.
     * @param pVn           Der neue Vorname.
     * @param pGeburtsdatum Das neue Geburtsdatum.
     */
    public void benutzerBearbeiten(int pID, String pRolle, String pEmail, String pNn, String pVn,
            String pGeburtsdatum) {
        benutzerBearbeiten(pID, pRolle, pEmail, pNn, pVn, pGeburtsdatum, 0);
    }

    /**
     * Bearbeitet einen Benutzer in der Datenbank inkl. Ausleihlimit.
     * 
     * @param pID           Die ID.
     * @param pRolle        Die neue Rolle.
     * @param pEmail        Die neue E-Mail.
     * @param pNn           Der neue Nachname.
     * @param pVn           Der neue Vorname.
     * @param pGeburtsdatum Das neue Geburtsdatum.
     * @param pMaxBuecher   Das neue maximale Ausleihlimit.
     */
    public void benutzerBearbeiten(int pID, String pRolle, String pEmail, String pNn, String pVn, String pGeburtsdatum,
            int pMaxBuecher) {
        if (isLehrer()) {
            dbConnector.executeStatement("SELECT email FROM benutzer WHERE id = ?", pID);
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null) {
                String gebDatumSql = (pGeburtsdatum == null || pGeburtsdatum.isEmpty()) ? null
                        : pGeburtsdatum;
                String emailSql = (pEmail == null || pEmail.trim().isEmpty()) ? null
                        : pEmail.toLowerCase();
                dbConnector.executeStatement(
                        "UPDATE benutzer SET vorname = ?, nachname = ?, rolle = ?"
                                + ", email = ?, geburtsdatum = ?"
                                + ", maxBuecherGleichzeitig = ? WHERE id = ?",
                        pVn, pNn, pRolle, emailSql, gebDatumSql, pMaxBuecher, pID);

            }
        }
    }

    /**
     * Prüft, ob eine E-Mail-Adresse bereits in der Datenbank existiert.
     * 
     * @param email Die zu prüfende E-Mail-Adresse.
     * @return True, wenn vorhanden, sonst false.
     */
    public boolean emailVorhanden(String email) {
        return emailVorhanden(email, -1);
    }

    /**
     * Prüft, ob eine E-Mail-Adresse bereits existiert, ignoriert dabei jedoch eine
     * bestimmte Nutzer-ID (für Updates).
     * 
     * @param email    Die zu prüfende E-Mail.
     * @param ignoreId Die ID des Nutzers, dessen eigene E-Mail nicht als Duplikat
     *                 gelten soll.
     * @return True, wenn vergeben, sonst false.
     */
    public boolean emailVorhanden(String email, int ignoreId) {
        if (email == null || email.trim().isEmpty())
            return false;
        dbConnector.executeStatement(
                "SELECT id FROM benutzer WHERE LOWER(email) = LOWER(?) AND id != ?", email, ignoreId);
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            int besitzerId = Integer.parseInt(result.getData()[0][0]);
            return besitzerId != ignoreId;
        }
        return false;
    }

    /**
     * Prüft, ob eine ISBN bereits in der Buch-Datenbank existiert.
     * 
     * @param isbn Die zu prüfende ISBN.
     * @return True, wenn vorhanden, sonst false.
     */
    public boolean isbnVorhanden(String isbn) {
        if (isbn == null || isbn.trim().isEmpty())
            return false;
        dbConnector.executeStatement("SELECT isbn FROM buecher WHERE isbn = ?", isbn);
        QueryResult result = dbConnector.getCurrentQueryResult();
        return result != null && result.getRowCount() > 0;
    }

    /**
     * Setzt ein neues Passwort für einen bestimmten Benutzer.
     * 
     * @param pID    Die ID des Benutzers.
     * @param pNewPW Das neue, unverschlüsselte Passwort.
     */
    public void passwortAendern(int pID, String pNewPW) {
        if (isLehrer() || pID == angemeldet) {
            dbConnector.executeStatement("SELECT nachname FROM benutzer WHERE id = ?", pID);
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null) {
                dbConnector.executeStatement(
                        "UPDATE benutzer SET passwort = ? WHERE id = ?", hashen(pNewPW), pID);

            }
        }
    }

    /**
     * Aktualisiert den Status aller abgelaufenen Reservierungen ('bereit' ->
     * 'abgelaufen')
     * und setzt die entsprechenden Bücher wieder auf 'verfuegbar'.
     */
    public void reservierungenAktualisieren() {
        dbConnector.executeStatement(
                "SELECT id, isbn FROM reservierungen WHERE reservierung_ende <= CURRENT_DATE() AND status = 'bereit';");
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            for (int i = 0; i < result.getRowCount(); i++) {
                String id = result.getData()[i][0];
                String buchIsbn = result.getData()[i][1];
                dbConnector.executeStatement("UPDATE reservierungen SET status = 'abgelaufen' WHERE id = ?", id);
                hinzuDA(buchIsbn);
                loeschRE(buchIsbn);
                updateBuchStatus(buchIsbn);
            }
        }
    }

    /**
     * @param isbn Die ISBN des Buches.
     * @return Vor- und Nachname des Schülers, für den das Buch aktuell 'bereit'
     *         reserviert ist.
     */
    public String getreserviertSchuelerName(String isbn) {
        dbConnector.executeStatement(
                "SELECT benutzer.nachname, benutzer.vorname FROM reservierungen INNER JOIN benutzer ON reservierungen.schueler_id = benutzer.id WHERE reservierungen.isbn ="
                        + "? AND reservierungen.status = 'bereit';",
                isbn);
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            String nachname = result.getData()[0][0] != null ? result.getData()[0][0] : "";
            String vorname = result.getData()[0][1] != null ? result.getData()[0][1] : "";
            return (vorname + " " + nachname).trim();
        }
        return "";
    }

    /**
     * Ermittelt, ob es unter den aktuell erfassten Büchern Konflikte
     * mit Reservierungen anderer Schüler gibt.
     * 
     * @return Eine Liste mit den betroffenen ISBNs.
     */
    public ArrayList<String> checkBuecherReserviert() {
        ArrayList<String> reserviert = new ArrayList<String>();
        if (erfassterSchueler == null)
            return reserviert;
        for (int i = 0; i < erfassteBuecher.size(); i++) {
            String isbn = erfassteBuecher.get(i);
            dbConnector.executeStatement("SELECT reservierungen.schueler_id, buecher.anzahlDa FROM reservierungen INNER JOIN buecher ON buecher.isbn = reservierungen.isbn WHERE reservierungen.isbn = ? AND reservierungen.status = 'bereit'", isbn);
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0) {
                int da = Integer.parseInt(result.getData()[0][1]);
                if (da == 0) {
                    boolean foundMatchingRes = false;
                    for (int r = 0; r < result.getRowCount(); r++) {
                        int resSchuelerId = Integer.parseInt(result.getData()[r][0]);
                        if (resSchuelerId == erfassterSchueler) {
                            foundMatchingRes = true;
                            break;
                        }
                    }
                    if (!foundMatchingRes) {
                        reserviert.add(isbn);
                    }
                }
            }
        }
        return reserviert;
    }
    
    public ArrayList<String> checkBuecherBereitsAusgeliehen() {
        ArrayList<String> ausgeliehen = new ArrayList<String>();
        if (erfassterSchueler == null)
            return ausgeliehen;
        for (int i = 0; i < erfassteBuecher.size(); i++) {
            String isbn = erfassteBuecher.get(i);
            
            if (bereitsAusgeliehen(isbn)) {
                
                ausgeliehen.add(isbn);
                    
                
            }
        }
        return ausgeliehen;
    }

    /**
     * Ermittelt, ob der aktuell erfasste Schüler für eines der erfassten Bücher zu
     * jung ist.
     * 
     * @return Eine Liste mit den betroffenen ISBNs.
     */
    public ArrayList<String> checkBuecherAlter() {
        ArrayList<String> alterKonflikt = new ArrayList<String>();
        if (erfassterSchueler == null)
            return alterKonflikt;

        int nutzerAlter = getNutzerAlter(erfassterSchueler);

        for (int i = 0; i < erfassteBuecher.size(); i++) {
            String isbn = erfassteBuecher.get(i);
            dbConnector.executeStatement("SELECT altersbeschraenkung FROM buecher WHERE isbn = ?", isbn);
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0 && result.getData()[0][0] != null) {
                try {
                    int ab = Integer.parseInt(result.getData()[0][0]);
                    if (ab > 0 && nutzerAlter < ab) {
                        alterKonflikt.add(isbn);
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        return alterKonflikt;
    }

    /**
     * Kombiniert Alters-, Reservierungs- und Doppelausleihe-Konflikte und gibt deren Buchtitel zurück.
     * 
     * @return Eine Liste der betroffenen Buchtitel.
     */
    public ArrayList<String> getKonfliktBuecherNamen() {
        ArrayList<String> namen = new ArrayList<String>();
        ArrayList<String> konfliktIsbns = new ArrayList<String>();
        for (String isbn : checkBuecherReserviert()) {
            if (!konfliktIsbns.contains(isbn)) {
                konfliktIsbns.add(isbn);
            }
        }
        for (String isbn : checkBuecherAlter()) {
            if (!konfliktIsbns.contains(isbn)) {
                konfliktIsbns.add(isbn);
            }
        }
        for (String isbn : checkBuecherBereitsAusgeliehen()) {
            if (!konfliktIsbns.contains(isbn)) {
                konfliktIsbns.add(isbn);
            }
        }
        for (String isbn : konfliktIsbns) {
            dbConnector.executeStatement("SELECT titel FROM buecher WHERE isbn = ?", isbn);
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0) {
                String titel = result.getData()[0][0];
                if (!namen.contains(titel)) {
                    namen.add(titel);
                }
            }
        }
        return namen;
    }

    /**
     * @return Vor- und Nachname des aktuell für Ausleihe/Rückgabe erfassten
     *         Schülers.
     */
    public String getErfassteSchuelerName() {
        if (erfassterSchueler == null)
            return "";
        dbConnector.executeStatement("SELECT nachname, vorname FROM benutzer WHERE id = ?", erfassterSchueler);
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            return result.getData()[0][0] + " " + result.getData()[0][1];
        }
        return "";
    }

    /**
     * Verschlüsselt ein Passwort mittels Argon2PasswordEncoder.
     * 
     * @param pP Das unverschlüsselte Passwort.
     * @return Das gehashte Passwort.
     */
    public String hashen(String pP) {
        // Passwort wird gesaltet und gehasht
        String verschlusselt;

        verschlusselt = passwordEncoder.encode(pP);

        return verschlusselt;
    }

    /**
     * Sucht nach Nutzern anhand eines Suchstrings (Name oder E-Mail).
     * 
     * @param pS Der Suchbegriff.
     * @return Eine nach Relevanz sortierte Liste der gefundenen Benutzer.
     */
    public ArrayList<Benutzer> nutzerSuchen(String pS) {
        if (pS == null) {
            pS = "";
        }
        pS = pS.trim();
        String[] terms = pS.split("\\s+");

        String whereClause = "";
        ArrayList<String> params = new ArrayList<>();
        if (pS.isEmpty()) {
            whereClause = "1=1";
        } else {
            for (int i = 0; i < terms.length; i++) {
                if (i > 0) {
                    whereClause += " OR ";
                }
                whereClause += "(nachname LIKE ? "
                        + "OR vorname LIKE ? "
                        + "OR email LIKE ?)";

                String suchbegriff = "%" + terms[i] + "%";

                params.add(suchbegriff);
                params.add(suchbegriff);
                params.add(suchbegriff);
            }
        }

        dbConnector.executeStatement(
                "SELECT id, nachname, vorname, email, passwort, rolle, freigeschaltet, gesperrt_von, geburtsdatum, maxBuecherGleichzeitig FROM benutzer WHERE "
                        + whereClause,
                params.toArray());

        QueryResult result = dbConnector.getCurrentQueryResult();
        ArrayList<Benutzer> nutzerListe = new ArrayList<>();

        if (result != null) {
            for (int i = 0; i < result.getRowCount(); i++) {
                boolean freigeschaltet = result.getData()[i][6].equals("1");
                int gesperrtVon = (result.getData()[i][7] != null && !result.getData()[i][7].equalsIgnoreCase("null")
                        && !result.getData()[i][7].trim().isEmpty()) ? Integer.parseInt(result.getData()[i][7]) : 0;
                String geburtsdatum = (result.getData()[i].length > 8 && result.getData()[i][8] != null
                        && !result.getData()[i][8].equalsIgnoreCase("null")) ? result.getData()[i][8] : null;

                int maxBuecher = Integer.parseInt(result.getData()[i][9]);

                Benutzer b = new Benutzer(result.getData()[i][5], result.getData()[i][4],
                        result.getData()[i][3], result.getData()[i][1], result.getData()[i][2],
                        Integer.parseInt(result.getData()[i][0]), freigeschaltet, gesperrtVon, geburtsdatum,
                        maxBuecher);
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

    /**
     * Hilfsmethode zur Berechnung eines Relevanz-Scores für Suchergebnisse.
     * 
     * @param b     Der Benutzer.
     * @param terms Die gesuchten Wörter.
     * @param pS    Der originale Suchstring.
     * @return Der berechnete Score (höher = relevanter).
     */
    private int berechneTreffer(Benutzer b, String[] terms, String pS) {
        if (pS == null || pS.trim().isEmpty())
            return 1;

        int score = 0;
        String nn = (b.getName() != null) ? b.getName().toLowerCase() : "";
        String vn = (b.getVorname() != null) ? b.getVorname().toLowerCase() : "";
        String em = (b.getEmail() != null) ? b.getEmail().toLowerCase() : "";

        for (String term : terms) {
            if (term == null || term.trim().isEmpty())
                continue;
            term = term.toLowerCase();
            if (nn.contains(term) || vn.contains(term) || em.contains(term)) {
                score++;
            }
        }
        return score;
    }

    public void lateDaysAktualisieren() {
        String resetDatumStr = getEinstellung("sperren_reset_datum");
        if (resetDatumStr != null && !resetDatumStr.trim().isEmpty()) {
            try {
                LocalDate resetDatum = LocalDate.parse(resetDatumStr.trim());
                if (!resetDatum.isAfter(LocalDate.now())) {
                    dbConnector.executeStatement("UPDATE benutzer SET tage_spaet = 0 WHERE tage_spaet > 0");

                    LocalDate naechstesReset = resetDatum.plusYears(1);
                    while (!naechstesReset.isAfter(LocalDate.now())) {
                        naechstesReset = naechstesReset.plusYears(1);
                    }
                    setEinstellung("sperren_reset_datum", naechstesReset.toString());
                }
            } catch (Exception ignored) {
            }
        }
        if ("1".equals(getEinstellung("sperren_aktiv"))) {
            String sperrungTageStr = getEinstellung("sperren_verspaetung_tage");
            int sperrungTage = 14;
            if (sperrungTageStr != null && !sperrungTageStr.trim().isEmpty()) {
                try {
                    sperrungTage = Integer.parseInt(sperrungTageStr.trim());
                } catch (NumberFormatException ignored) {
                }
            }

            dbConnector.executeStatement(
                    "SELECT schueler_id, DATEDIFF(CURRENT_DATE(), geplante_rueckgabe) FROM ausleihen WHERE geplante_rueckgabe < CURRENT_DATE() AND ruckgabe_datum IS NULL ORDER BY schueler_id");
            QueryResult result = dbConnector.getCurrentQueryResult();
            int sumDaysLate = 0;
            int lastStudent = -1;
            int lateDays;
            if (result != null && result.getRowCount() > 0) {
                for (int i = 0; i < result.getRowCount(); i++) {
                    int schuelerID = Integer.parseInt(result.getData()[i][0]);
                    int buchLateDays = 0;
                    try {
                        buchLateDays = Integer.parseInt(result.getData()[i][1]);
                    } catch (NumberFormatException ignored) {
                    }
                    if (buchLateDays < 0) {
                        buchLateDays = 0;
                    }

                    if (schuelerID == lastStudent) {
                        sumDaysLate += buchLateDays;
                    } else {
                        if (lastStudent != -1) {
                            dbConnector.executeStatement("SELECT tage_spaet FROM benutzer WHERE id = ?", lastStudent);
                            QueryResult lateRes = dbConnector.getCurrentQueryResult();
                            if (lateRes != null && lateRes.getRowCount() > 0) {
                                try {
                                    lateDays = Integer.parseInt(lateRes.getData()[0][0]);
                                    if (lateDays + sumDaysLate > sperrungTage && !isGesperrt(lastStudent)) {
                                        sperren(lastStudent, true);
                                    }
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }
                        sumDaysLate = buchLateDays;
                    }

                    lastStudent = schuelerID;
                }
                if (lastStudent != -1) {
                    dbConnector.executeStatement("SELECT tage_spaet FROM benutzer WHERE id = ?", lastStudent);
                    QueryResult lateRes = dbConnector.getCurrentQueryResult();
                    if (lateRes != null && lateRes.getRowCount() > 0) {
                        try {
                            lateDays = Integer.parseInt(lateRes.getData()[0][0]);
                            if (lateDays + sumDaysLate > sperrungTage && !isGesperrt(lastStudent)) {
                                sperren(lastStudent, true);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }
    }

    /**
     * @return True, wenn die letzte gebuchte Aktion eine Ausleihe war, sonst false
     *         (Rückgabe).
     */
    public boolean letzteAktionAusleihen() {
        return letzteAktionAusleihen;
    }

    /**
     * @return Eine Liste der Titel der bei der letzten Aktion betroffenen Bücher.
     */
    public ArrayList<String> getLetzteBuecher() {
        ArrayList<String> titelListe = new ArrayList<>();
        for (String isbn : letzteBuecher) {
            dbConnector.executeStatement("SELECT titel FROM buecher WHERE isbn = ?", isbn);
            QueryResult result = dbConnector.getCurrentQueryResult();
            if (result != null && result.getRowCount() > 0) {
                titelListe.add(result.getData()[0][0]);
            } else {
                titelListe.add(isbn);
            }
        }
        return titelListe;
    }

    /**
     * @return Der Name des Schülers, der an der letzten Aktion beteiligt war.
     */
    public String getLetzterSchuelerName() {
        dbConnector.executeStatement("SELECT nachname, vorname FROM benutzer WHERE id = " + letzterSchueler);
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            return result.getData()[0][0] + " " + result.getData()[0][1];
        }
        return "";
    }

    /**
     * Macht die zuletzt durchgeführte Ausleihe oder Rückgabe in der Datenbank
     * rückgängig.
     */
    public void letzteAktionZuruecknehmen() {
        if (letzteAktionAusleihen) {
            for (String isbn : letzteBuecher) {
                dbConnector.executeStatement("DELETE FROM ausleihen WHERE isbn = ? AND schueler_id = ? AND ruckgabe_datum IS NULL", isbn, letzterSchueler);
                        
                dbConnector.executeStatement("SELECT id FROM reservierungen WHERE isbn = ? AND schueler_id = ? AND status = 'abgeschlossen' ORDER BY id DESC LIMIT 1", isbn, letzterSchueler);
                QueryResult resCheck = dbConnector.getCurrentQueryResult();
                if (resCheck != null && resCheck.getRowCount() > 0) {
                    dbConnector.executeStatement("UPDATE reservierungen SET status = 'bereit' WHERE id = ?", resCheck.getData()[0][0]);
                    hinzuRE(isbn);
                    loeschLI(isbn);
                } else {
                    hinzuDA(isbn);
                    loeschLI(isbn);
                }
                updateBuchStatus(isbn);
            }
            erfassterSchueler = null;
            erfassteBuecher.clear();
            aktuellerModus = "LEER";
        } else {
            for (String isbn : letzteBuecher) {
                dbConnector.executeStatement("UPDATE ausleihen SET ruckgabe_datum = NULL WHERE isbn = ? AND schueler_id = ? ORDER BY ausleihdatum DESC LIMIT 1", isbn, letzterSchueler);
                        
                hinzuLI(isbn);
                dbConnector.executeStatement("SELECT COUNT(id) FROM reservierungen WHERE isbn = ? AND status = 'bereit'", isbn);
                QueryResult result = dbConnector.getCurrentQueryResult();
                if (result != null && result.getRowCount() > 0) {
                    dbConnector.executeStatement("SELECT anzahlRes FROM buecher WHERE isbn = ?", isbn);
                    QueryResult res = dbConnector.getCurrentQueryResult();
                    int reserviert = Integer.parseInt(res.getData()[0][0]);
                    int reservierungen = Integer.parseInt(result.getData()[0][0]);
                    if (reservierungen == reserviert) {
                        loeschRE(isbn);
                        int dauer = getReservierungDauer();
                        dbConnector.executeStatement("SELECT id FROM reservierungen WHERE isbn = ? AND status = 'bereit' ORDER BY reservierung_beginn ASC", isbn);
                        result = dbConnector.getCurrentQueryResult();
                        
                        if (result != null && result.getRowCount() > 0) {
                            dbConnector.executeStatement(
                                    "UPDATE reservierungen SET status = 'wartend', reservierung_ende = DATE_ADD(CURRENT_DATE(), INTERVAL ? DAY) WHERE id = ?",
                                    dauer, result.getData()[0][0]);
                        }
                    } else {
                        loeschDA(isbn);
                    }
                } else {
                    loeschDA(isbn);
                }
                    
                updateBuchStatus(isbn);
            }
            erfassterSchueler = null;
            erfassteBuecher.clear();
            aktuellerModus = "LEER";
        }
    }
    public void updateBuchStatus(String isbn){
        if (isbnVorhanden(isbn)){
            dbConnector.executeStatement("SELECT anzahlDa, anzahlLiehen, anzahlRes FROM buecher WHERE isbn = '" + isbn +"'");
            QueryResult result = dbConnector.getCurrentQueryResult();
            if(result!= null &&result.getRowCount() > 0){
                int da = Integer.parseInt(result.getData()[0][0]);
                int li = Integer.parseInt(result.getData()[0][1]);
                int re = Integer.parseInt(result.getData()[0][2]);
                if (da != 0){
                    dbConnector.executeStatement("UPDATE buecher SET status = 'verfuegbar' WHERE isbn = '" + isbn + "'");
                }
                else if (li != 0){
                    dbConnector.executeStatement("UPDATE buecher SET status = 'verliehen' WHERE isbn = '" + isbn + "'");
                }
                else if (re != 0){
                    dbConnector.executeStatement("UPDATE buecher SET status = 'reserviert' WHERE isbn = '" + isbn + "'");
                }
                else {
                    dbConnector.executeStatement("UPDATE buecher SET status = 'entfernt' WHERE isbn = '" + isbn + "'");
                }
            }
            
        }
    }

    public void hinzuDA(String isbn){
        dbConnector.executeStatement("SELECT anzahlDa FROM buecher WHERE isbn = '"+isbn+"'");
                QueryResult result = dbConnector.getCurrentQueryResult();
                if (result != null && result.getRowCount() > 0) {
                    int da = Integer.parseInt(result.getData()[0][0]);
                    da = da + 1;
                    dbConnector.executeStatement("UPDATE buecher SET anzahlDa = '"+da+"' WHERE isbn = '"+isbn+"'");
                } 
    }
    
    private void hinzuLI(String isbn){
         dbConnector.executeStatement("SELECT anzahlLiehen FROM buecher WHERE isbn = '"+isbn+"'");
                QueryResult result = dbConnector.getCurrentQueryResult();
                if (result != null && result.getRowCount() > 0) {
                    int da = Integer.parseInt(result.getData()[0][0]);
                    da = da + 1;
                    dbConnector.executeStatement("UPDATE buecher SET anzahlLiehen = '"+da+"' WHERE isbn = '"+isbn+"'");
                }
    }
    
    private void hinzuRE(String isbn){
        dbConnector.executeStatement("SELECT anzahlRes FROM buecher WHERE isbn = '"+isbn+"'");
                QueryResult result = dbConnector.getCurrentQueryResult();
                if (result != null && result.getRowCount() > 0) {
                    int da = Integer.parseInt(result.getData()[0][0]);
                    da = da + 1;
                    dbConnector.executeStatement("UPDATE buecher SET anzahlRes = '"+da+"' WHERE isbn = '"+isbn+"'");
                } 
    }
    
    public void loeschDA(String isbn){
        dbConnector.executeStatement("SELECT anzahlDa FROM buecher WHERE isbn = '"+isbn+"'");
                QueryResult result = dbConnector.getCurrentQueryResult();
                if (result != null && result.getRowCount() > 0) {
                    int da = Integer.parseInt(result.getData()[0][0]);
                    if(da!=0){
                        da = da - 1;
                        dbConnector.executeStatement("UPDATE buecher SET anzahlDa = '"+da+"' WHERE isbn = '"+isbn+"'");
                    }
                }
    }
    
    private void loeschLI(String isbn){
        dbConnector.executeStatement("SELECT anzahlLiehen FROM buecher WHERE isbn = '"+isbn+"'");
                QueryResult result = dbConnector.getCurrentQueryResult();
                if (result != null && result.getRowCount() > 0) {
                    int da = Integer.parseInt(result.getData()[0][0]);
                    if(da!=0){
                        da = da - 1;
                        dbConnector.executeStatement("UPDATE buecher SET anzahlLiehen = '"+da+"' WHERE isbn = '"+isbn+"'");
                    }
                } 
    }
    
    private void loeschRE(String isbn){
        dbConnector.executeStatement("SELECT anzahlRes FROM buecher WHERE isbn = '"+isbn+"'");
                QueryResult result = dbConnector.getCurrentQueryResult();
                if (result != null && result.getRowCount() > 0) {
                    int da = Integer.parseInt(result.getData()[0][0]);
                    if(da!=0){
                        da = da - 1;
                        dbConnector.executeStatement("UPDATE buecher SET anzahlRes = '"+da+"' WHERE isbn = '"+isbn+"'");
                    }
                } 
    }
    
    private void updateReservierung(String isbn){
        int dauer = getReservierungDauer();
        dbConnector.executeStatement("SELECT id FROM reservierungen WHERE isbn= '"+ isbn
                                    + "' AND status = 'wartend' ORDER BY reservierung_beginn ASC");
        QueryResult result = dbConnector.getCurrentQueryResult();
        
        if(result != null && result.getRowCount() > 0){
            dbConnector.executeStatement(
                            "UPDATE reservierungen SET status = 'bereit', reservierung_ende = DATE_ADD(CURRENT_DATE(), INTERVAL "
                                    + dauer + " DAY) WHERE id ='" +result.getData()[0][0] + "'");
        }
        
    }
    
    
    
    private void neusteResAbsagen(String isbn){
        dbConnector.executeStatement("SELECT id FROM reservierungen WHERE isbn= '"+ isbn
                                    + "' AND status = 'wartend' ORDER BY reservierung_beginn DESC");
        QueryResult result = dbConnector.getCurrentQueryResult();
        
        if(result != null && result.getRowCount() > 0){
            dbConnector.executeStatement(
                            "UPDATE reservierungen SET status = 'abgesagt' WHERE id ='" +result.getData()[0][0] + "'");
        }
    }
    private boolean maxRes(String isbn){
        dbConnector.executeStatement("SELECT anzahlDa,anzahlLiehen,anzahlRes FROM buecher WHERE isbn = '"+isbn+"'");
        QueryResult result = dbConnector.getCurrentQueryResult();
        int existieren = Integer.parseInt(result.getData()[0][0])+Integer.parseInt(result.getData()[0][1])+Integer.parseInt(result.getData()[0][2]);
        dbConnector.executeStatement("SELECT COUNT(isbn) FROM reservierungen WHERE isbn = '"+isbn+"' AND (status = 'wartend' OR status = 'bereit')");
        result = dbConnector.getCurrentQueryResult();
        if(result != null && result.getRowCount() > 0){
            int anzahl = Integer.parseInt(result.getData()[0][0]);
            if(anzahl >= existieren){
                return true;
            }
            return false;
        }
        if (existieren == 0){
            return true;
        }
        return false;
    }
    /**
     * Zählt die Gesamtzahl aller Exemplare (vorhanden, geliehen, reserviert) für
     * eine ISBN.
     * 
     * @param isbn Die ISBN.
     * @return Die ermittelte Gesamtzahl als String.
     */
    public String getExemplare(String isbn) {
        dbConnector.executeStatement("SELECT anzahlDa, anzahlLiehen, anzahlRes FROM buecher WHERE isbn = ?", isbn);
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            int existieren = Integer.parseInt(result.getData()[0][0]) + Integer.parseInt(result.getData()[0][1]) + Integer.parseInt(result.getData()[0][2]);
            return String.valueOf(existieren);
        }
        return "0";
    }
    
    public String getDa(String isbn){
        dbConnector.executeStatement("SELECT anzahlDa FROM buecher WHERE isbn = '"+isbn+"'");
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            int existieren = Integer.parseInt(result.getData()[0][0]);
            return String.valueOf(existieren);
        }
        return "0";
    }
    
    public QueryResult getLiehen(String isbn){
        dbConnector.executeStatement("SELECT vorname, nachname, benutzer.id FROM benutzer, ausleihen WHERE ausleihen.isbn='"+isbn+"' AND ausleihen.schueler_id = benutzer.id AND ruckgabe_datum IS NULL");
        QueryResult result = dbConnector.getCurrentQueryResult();
        return result;
    }
    
    public QueryResult getReserviert(String isbn){
        dbConnector.executeStatement("SELECT vorname, nachname, benutzer.id FROM benutzer, reservierungen WHERE reservierungen.isbn='"+isbn+"' AND reservierungen.schueler_id = benutzer.id AND reservierungen.status ='bereit'");
        QueryResult result = dbConnector.getCurrentQueryResult();
        return result;
    }
    
    public boolean richtigerSchulerRuck(String isbn){
        dbConnector.executeStatement("SELECT id FROM ausleihen WHERE schueler_id ='"+erfassterSchueler+"' AND isbn='"+isbn+"'AND ruckgabe_datum IS NULL");
        QueryResult result = dbConnector.getCurrentQueryResult();
        if(result != null && result.getRowCount() > 0){
            return true;
        }
        return false;
    }
    
    public boolean richtigerSchuelerRuckListe(){
        if (erfassteBuecher.isEmpty()) {
            return false;
        }
        for(int i= 0; i< erfassteBuecher.size(); i++){
            dbConnector.executeStatement("SELECT id FROM ausleihen WHERE schueler_id ='"+erfassterSchueler+"' AND isbn='"+erfassteBuecher.get(i)+"'AND ruckgabe_datum IS NULL");
            QueryResult result = dbConnector.getCurrentQueryResult();
            if(result == null || result.getRowCount() == 0){
                return false;
            }
        }
        return true;
    }
    
    public boolean bereitsAusgeliehen(String isbn) {
        if (erfassterSchueler == null) {
            return false;
        }
        dbConnector.executeStatement("SELECT id FROM ausleihen WHERE isbn = ? AND schueler_id = ? AND ruckgabe_datum IS NULL", isbn, erfassterSchueler);
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result == null || result.getRowCount() == 0) {
            return false;
        }
        return true;
    }

    /**
     * Erstellt eine Bestandsliste aller Bücher im System als PDF (inklusive Anzahl
     * Exemplare)
     * und öffnet diese anschließend im Standard-PDF-Viewer.
     */
    public void bestandListeErstellen() {
        dbConnector.executeStatement("SELECT titel, status, isbn FROM buecher ORDER BY titel");
        QueryResult result = dbConnector.getCurrentQueryResult();

        LocalDate heute = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String datumText = "Datum: " + heute.format(formatter);
        try (PDDocument dokument = new PDDocument()) {
            float yStart = 700;
            float yPosition = yStart;
            float zeilenAbstand = 15;
            float untererRand = 50;

            PDType0Font arialFont = PDType0Font.load(dokument, new File("C:/Windows/Fonts/arial.ttf"));

            PDPage aktseite = new PDPage();
            dokument.addPage(aktseite);

            PDPageContentStream inhalt = new PDPageContentStream(dokument, aktseite);
            inhalt.beginText();
            inhalt.setFont(arialFont, 12);
            inhalt.newLineAtOffset(450, 750);
            inhalt.showText(datumText);
            inhalt.endText();

            inhalt.beginText();
            inhalt.setFont(arialFont, 12);
            inhalt.newLineAtOffset(50, yStart);

            for (int i = 0; i < result.getRowCount(); i++) {
                String e = getExemplare(result.getData()[i][2]);
                if (yPosition - zeilenAbstand < untererRand) {

                    inhalt.endText();
                    inhalt.close();

                    aktseite = new PDPage();
                    dokument.addPage(aktseite);

                    inhalt = new PDPageContentStream(dokument, aktseite);
                    inhalt.beginText();
                    inhalt.setFont(arialFont, 12);

                    inhalt.newLineAtOffset(50, yStart);
                    yPosition = yStart;
                }
                String status = result.getData()[i][1];
                if (status.equals("entfernt")) {
                    inhalt.setNonStrokingColor(1, 0, 0);
                } else {
                    inhalt.setNonStrokingColor(0, 0, 0);
                }
                String originalTitel = result.getData()[i][0];

                String gekuerzterTitel = originalTitel;
                if (gekuerzterTitel != null && gekuerzterTitel.length() > 35) {
                    gekuerzterTitel = gekuerzterTitel.substring(0, 20) + "...";
                }

                inhalt.showText(gekuerzterTitel + "--- Exemplare: " + e);
                inhalt.newLineAtOffset(0, -zeilenAbstand); // Gehe nach unten
                yPosition -= zeilenAbstand;
            }
            inhalt.endText();
            inhalt.close();

            File pdfDatei = new File("Bestandsliste.pdf");
            dokument.save(pdfDatei);
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                desktop.open(pdfDatei);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Ermittelt die beliebtesten Bücher (häufigste Ausleihen).
     * 
     * @return Ein QueryResult mit Titel und Ausleihanzahl absteigend sortiert.
     */
    public QueryResult beliebtesteBucher() {
        dbConnector.executeStatement(
                "SELECT buecher.titel, COUNT(ausleihen.isbn) AS anzahl FROM buecher LEFT JOIN ausleihen ON buecher.isbn = ausleihen.isbn GROUP BY buecher.isbn ORDER BY anzahl DESC");
        QueryResult result = dbConnector.getCurrentQueryResult();
        return result;
    }

    /**
     * Ermittelt die unbeliebtesten Bücher (seltenste Ausleihen).
     * 
     * @return Ein QueryResult mit Titel und Ausleihanzahl aufsteigend sortiert.
     */
    public QueryResult unbeliebtesteBucher() {
        dbConnector.executeStatement(
                "SELECT buecher.titel, COUNT(ausleihen.isbn) AS anzahl FROM buecher LEFT JOIN ausleihen ON buecher.isbn = ausleihen.isbn GROUP BY buecher.isbn ORDER BY ausleihen.isbn IS NOT NULL, anzahl ASC");
        QueryResult result = dbConnector.getCurrentQueryResult();
        return result;
    }

    /**
     * Liest die Altersbeschränkung eines Buches aus.
     * 
     * @param isbn Die ISBN des Buches.
     * @return Das erforderliche Mindestalter (in Jahren), 0 wenn keine Beschränkung
     *         besteht.
     */
    public int getBuchAltersbeschraenkung(String isbn) {
        dbConnector.executeStatement("SELECT altersbeschraenkung FROM buecher WHERE isbn = ?", isbn);
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0 && result.getData()[0][0] != null) {
            try {
                return Integer.parseInt(result.getData()[0][0]);
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Berechnet das Alter eines Benutzers in Jahren.
     * 
     * @param nutzerId Die ID des Nutzers.
     * @return Das Alter in Jahren (0 bei Fehler oder fehlendem Datum).
     */
    private int getNutzerAlter(int nutzerId) {
        dbConnector.executeStatement(
                "SELECT TIMESTAMPDIFF(YEAR, geburtsdatum, CURDATE()) FROM benutzer WHERE id = ?", nutzerId);
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0 && result.getData()[0][0] != null) {
            try {
                return Integer.parseInt(result.getData()[0][0]);
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Ändert das Passwort des aktuell angemeldeten Nutzers und hebt
     * die Flag 'passwortAendern' auf.
     * 
     * @param passwort Das neue unverschlüsselte Passwort.
     */
    public void passwortAendern(String passwort) {
        dbConnector.executeStatement("UPDATE benutzer SET passwort = ?"
                + ", passwortAendern = '0' WHERE id = ? ", passwordEncoder.encode(passwort), angemeldet);
    }

    /**
     * Generiert ein neues zufälliges Passwort für die angegebene E-Mail-Adresse,
     * speichert es gehasht ab und schickt es dem Nutzer per E-Mail zu.
     * 
     * @param email Die E-Mail-Adresse.
     */
    public void passwortVergessen(String email) {
        dbConnector.executeStatement("SELECT * FROM benutzer WHERE email = ?", email);
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            String passwort = Integer.toString(random.nextInt(10000000, 100000000));
            dbConnector.executeStatement("UPDATE benutzer SET passwort = ?"
                    + ", passwortAendern = '1' WHERE email = ?", passwordEncoder.encode(passwort), email);
            MailService mailService = new MailService(this);
            mailService.sendePasswortResetMail(email, result.getData()[0][1] + " " + result.getData()[0][2], passwort);
        }
    }

    /**
     * Sendet einem neu angelegten Benutzer sein initial generiertes Passwort per
     * E-Mail zu.
     * 
     * @param email Die E-Mail-Adresse.
     */
    public void initialesPasswortSenden(String email) {
        dbConnector.executeStatement("SELECT * FROM benutzer WHERE email = ?", email);
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            String passwort = Integer.toString(random.nextInt(10000000, 100000000));
            dbConnector.executeStatement("UPDATE benutzer SET passwort = ?"
                    + ", passwortAendern = '1' WHERE email = ?", passwordEncoder.encode(passwort), email);
            MailService mailService = new MailService(this);
            mailService.sendeAnmeldeMail(email, result.getData()[0][1] + " " + result.getData()[0][2], passwort);
        }
    }

    /**
     * Prüft, ob der aktuell erfasste Schüler mit den neu erfassten Büchern
     * sein persönliches Ausleihlimit überschreiten würde.
     * 
     * @return True, wenn das Limit überschritten wird, sonst false.
     */
    public boolean buecherAnzahlUeberschritten() {
        if (erfassterSchueler == null) {
            return false;
        }

        dbConnector.executeStatement("SELECT count(*) FROM ausleihen WHERE schueler_id = " + erfassterSchueler
                + " AND ruckgabe_datum IS NULL");
        QueryResult countResult = dbConnector.getCurrentQueryResult();
        if (countResult != null && countResult.getRowCount() > 0) {
            int count = Integer.parseInt(countResult.getData()[0][0]);
            dbConnector.executeStatement("SELECT maxBuecherGleichzeitig FROM benutzer WHERE id = " + erfassterSchueler);
            QueryResult maxResult = dbConnector.getCurrentQueryResult();

            if (maxResult != null && maxResult.getRowCount() > 0 && maxResult.getData()[0][0] != null) {
                int max = Integer.parseInt(maxResult.getData()[0][0]);
                if (count + erfassteBuecher.size() >= max) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return True, wenn es erfasste Schüler oder Bücher im Puffer gibt
     *         (Abbrechen-Button macht Sinn).
     */
    public boolean abbrechenMoeglich() {
        if (erfassterSchueler == null && erfassteBuecher.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * @return Das Standard-Ausleihlimit für neu angelegte Schüler (aus den
     *         Einstellungen).
     */
    public int getStandartAusleihlimit() {
        dbConnector.executeStatement("SELECT wert FROM einstellungen WHERE schluessel = 'ausleihlimit_standart'");
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            String value = result.getData()[0][0];
            if (value != null && !value.equals("null")) {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {

                }
            }
        }
        return 0;
    }

    /**
     * Sucht ein bestimmtes Buch anhand der ISBN in der Datenbank.
     * 
     * @param isbn Die ISBN des Buches.
     * @return Ein Buch-Objekt oder null, falls nicht gefunden.
     */
    public Buch getBuch(String isbn) {
        dbConnector.executeStatement("SELECT * FROM buecher WHERE isbn = ?", isbn);
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0) {
            return new Buch(result.getData()[0][0], result.getData()[0][1], result.getData()[0][2],
                    result.getData()[0][3], result.getData()[0][4], result.getData()[0][5], result.getData()[0][6]);
        }
        return null;
    }

    /**
     * Erhöht den Zähler für manuelle Mahnungen einer bestimmten Ausleihe um 1.
     * 
     * @param ausleiheId Die ID der Ausleihe.
     */
    public void mahnungHinzufuegen(int ausleiheId) {
        if (isLehrer()) {
            dbConnector
                    .executeStatement("UPDATE ausleihen SET manuelle_mahnungen = manuelle_mahnungen + 1 WHERE id = ?"
                            + " AND ruckgabe_datum IS NULL", ausleiheId);
        }
    }

    public boolean isGesperrt(int id) {

        dbConnector.executeStatement("SELECT freigeschaltet FROM benutzer WHERE id = ?", id);
        QueryResult result = dbConnector.getCurrentQueryResult();
        if (result != null && result.getRowCount() > 0 && result.getData()[0][0].equals("0")) {
            return true;
        }
        return false;
    }

}
