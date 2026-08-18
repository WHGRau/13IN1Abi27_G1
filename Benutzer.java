
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
    
    public Benutzer(String pRolle, String pPw, String pEmail, String pNn, String pVn)
    {
        email = pEmail;
        passwort = pPw;
        nachname = pNn;
        vorname = pVn;
        rolle = pRolle;
        freigeschaltet = true;
    }

    
}
