
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
    
    public Benutzer(String pRolle, String pPw, String pEmail, String pNn, String pVn, int pId, boolean pFreigeschaltet)
    {
        email = pEmail;
        passwort = pPw;
        nachname = pNn;
        vorname = pVn;
        rolle = pRolle;
        id = pId;
        freigeschaltet = pFreigeschaltet;
        lateDays = 0;
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
}
