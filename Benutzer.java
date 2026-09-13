
/**
 * Repräsentiert einen Benutzer der Bibliothek (z. B. Schüler, Lehrer oder Helfer).
 * Speichert alle relevanten Daten wie Name, Rolle und Ausleihlimits.
 */
public class Benutzer
{
    // Instanzvariablen - ersetzen Sie das folgende Beispiel mit Ihren Variablen
    private String email;
    private String passwort;
    private String nachname;
    private String vorname;
    private String rolle;
    private int id;
    private boolean freigeschaltet;
    private int lateDays;
    private int gesperrtVon;
    private String geburtsdatum;
    private int maxBuecherGleichzeitig;
    
    /**
     * Erstellt einen neuen Benutzer ohne Geburtsdatum und ohne spezielles Ausleihlimit.
     * 
     * @param pRolle Die Rolle des Benutzers (z. B. "Schüler", "Lehrer").
     * @param pPw Das Passwort des Benutzers.
     * @param pEmail Die E-Mail-Adresse.
     * @param pNn Der Nachname.
     * @param pVn Der Vorname.
     * @param pId Die eindeutige ID des Benutzers.
     * @param pFreigeschaltet Status, ob der Benutzer freigeschaltet ist.
     * @param pGesperrtVon ID des Admins, der den Benutzer gesperrt hat (0 falls nicht gesperrt).
     */
    public Benutzer(String pRolle, String pPw, String pEmail, String pNn, String pVn, int pId, boolean pFreigeschaltet, int pGesperrtVon)
    {
        this(pRolle, pPw, pEmail, pNn, pVn, pId, pFreigeschaltet, pGesperrtVon, null, 0);
    }

    /**
     * Erstellt einen neuen Benutzer mit Geburtsdatum, aber ohne spezielles Ausleihlimit.
     * 
     * @param pRolle Die Rolle des Benutzers.
     * @param pPw Das Passwort des Benutzers.
     * @param pEmail Die E-Mail-Adresse.
     * @param pNn Der Nachname.
     * @param pVn Der Vorname.
     * @param pId Die eindeutige ID.
     * @param pFreigeschaltet Freischaltungsstatus.
     * @param pGesperrtVon ID des sperrenden Admins.
     * @param pGeburtsdatum Das Geburtsdatum als String.
     */
    public Benutzer(String pRolle, String pPw, String pEmail, String pNn, String pVn, int pId, boolean pFreigeschaltet, int pGesperrtVon, String pGeburtsdatum)
    {
        this(pRolle, pPw, pEmail, pNn, pVn, pId, pFreigeschaltet, pGesperrtVon, pGeburtsdatum, 0);
    }

    /**
     * Erstellt einen neuen Benutzer mit allen relevanten Daten inklusive Geburtsdatum und maximalem Ausleihlimit.
     * 
     * @param pRolle Die Rolle des Benutzers.
     * @param pPw Das Passwort des Benutzers.
     * @param pEmail Die E-Mail-Adresse.
     * @param pNn Der Nachname.
     * @param pVn Der Vorname.
     * @param pId Die eindeutige ID.
     * @param pFreigeschaltet Freischaltungsstatus.
     * @param pGesperrtVon ID des sperrenden Admins.
     * @param pGeburtsdatum Das Geburtsdatum als String.
     * @param pMaxBuecherGleichzeitig Das Limit für gleichzeitig ausleihbare Bücher.
     */
    public Benutzer(String pRolle, String pPw, String pEmail, String pNn, String pVn, int pId, boolean pFreigeschaltet, int pGesperrtVon, String pGeburtsdatum, int pMaxBuecherGleichzeitig)
    {
        email = pEmail;
        passwort = pPw;
        nachname = pNn;
        vorname = pVn;
        rolle = pRolle;
        id = pId;
        freigeschaltet = pFreigeschaltet;
        lateDays = 0;
        gesperrtVon = pGesperrtVon;
        geburtsdatum = pGeburtsdatum;
        maxBuecherGleichzeitig = pMaxBuecherGleichzeitig;
    }

    /**
     * Gibt die E-Mail-Adresse des Benutzers zurück.
     * 
     * @return E-Mail-Adresse.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Gibt das Passwort des Benutzers zurück.
     * 
     * @return Passwort.
     */
    public String getPasswort() {
        return passwort;
    }

    /**
     * Gibt den Nachnamen des Benutzers zurück.
     * 
     * @return Nachname.
     */
    public String getName() {
        return nachname;
    }

    /**
     * Gibt den Vornamen des Benutzers zurück.
     * 
     * @return Vorname.
     */
    public String getVorname() {
        return vorname;
    }

    /**
     * Gibt die Rolle des Benutzers zurück.
     * 
     * @return Rolle des Benutzers.
     */
    public String getRolle() {
        return rolle;
    }

    /**
     * Gibt die eindeutige ID des Benutzers zurück.
     * 
     * @return Benutzer-ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Prüft, ob der Benutzer freigeschaltet ist.
     * 
     * @return true, wenn der Benutzer freigeschaltet ist, andernfalls false.
     */
    public boolean isFreigeschaltet() {
        return freigeschaltet;
    }
    
    public void setLateDays(int days){
        lateDays = days;
    }
    
    public int getLateDays(){
        return lateDays;
    }
    /**
     * Gibt die ID des Admins zurück, der diesen Benutzer gesperrt hat.
     * 
     * @return ID des Admins (0, falls nicht gesperrt).
     */
    public int getGesperrtVon() {
        return gesperrtVon;
    }
    
    /**
     * Gibt das Geburtsdatum des Benutzers zurück.
     * 
     * @return Geburtsdatum als String.
     */
    public String getGeburtsdatum() {
        return geburtsdatum;
    }

    /**
     * Gibt das maximale Buchausleih-Limit des Benutzers zurück.
     * 
     * @return Anzahl der maximal gleichzeitig ausleihbaren Bücher.
     */
    public int getMaxBuecherGleichzeitig() {
        return maxBuecherGleichzeitig;
    }

    /**
     * Alias für getMaxBuecherGleichzeitig().
     * Gibt das Ausleihlimit zurück.
     * 
     * @return Ausleihlimit.
     */
    public int getAusleihlimit() {
        return maxBuecherGleichzeitig;
    }
}
