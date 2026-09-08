
/**
 * Beschreiben Sie hier die Klasse Benutzer.
 * 
 * @author (Ihr Name) 
 * @version (eine Versionsnummer oder ein Datum)
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
    
    public Benutzer(String pRolle, String pPw, String pEmail, String pNn, String pVn, int pId, boolean pFreigeschaltet, int pGesperrtVon)
    {
        this(pRolle, pPw, pEmail, pNn, pVn, pId, pFreigeschaltet, pGesperrtVon, null, 0);
    }

    public Benutzer(String pRolle, String pPw, String pEmail, String pNn, String pVn, int pId, boolean pFreigeschaltet, int pGesperrtVon, String pGeburtsdatum)
    {
        this(pRolle, pPw, pEmail, pNn, pVn, pId, pFreigeschaltet, pGesperrtVon, pGeburtsdatum, 0);
    }

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

    public String getEmail() {
        return email;
    }

    public String getPasswort() {
        return passwort;
    }

    public String getName() {
        return nachname;
    }

    public String getVorname() {
        return vorname;
    }

    public String getRolle() {
        return rolle;
    }

    public int getId() {
        return id;
    }

    public boolean isFreigeschaltet() {
        return freigeschaltet;
    }
    
    public void setLateDays(int days){
        lateDays = days;
    }
    
    public int getLateDays(){
        return lateDays;
    }
    public int getGesperrtVon() {
        return gesperrtVon;
    }
    
    public String getGeburtsdatum() {
        return geburtsdatum;
    }

    public int getMaxBuecherGleichzeitig() {
        return maxBuecherGleichzeitig;
    }

    public int getAusleihlimit() {
        return maxBuecherGleichzeitig;
    }
}
