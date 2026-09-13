/**
 * Repräsentiert ein Buch in der Bibliothek mit all seinen Eigenschaften 
 * wie ISBN, Titel, Autor und Status.
 */
public class Buch {

    private String isbn;
    private String titel;
    private String autor;
    private String erscheinungsjahr;
    private String beschreibung;
    private String status;
    private String altersbeschraenkung;

    /**
     * Erstellt ein neues Buch-Objekt mit den übergebenen Daten.
     * 
     * @param isbn Die ISBN des Buches.
     * @param titel Der Titel des Buches.
     * @param autor Der Autor des Buches.
     * @param erscheinungsjahr Das Erscheinungsjahr.
     * @param beschreibung Eine kurze Beschreibung oder Zusammenfassung.
     * @param status Der Ausleihstatus (z. B. "verfügbar", "ausgeliehen").
     * @param alter Die Altersbeschränkung (z. B. "FSK 12").
     */
    public Buch(String isbn, String titel, String autor, String erscheinungsjahr, String beschreibung, String status, String alter) {
        this.isbn = isbn != null ? isbn : "";
        this.titel = titel != null ? titel : "";
        this.autor = autor != null ? autor : "";
        
        if (erscheinungsjahr != null && (erscheinungsjahr.equals("0000") || erscheinungsjahr.equals("0001"))) {
            erscheinungsjahr = "";
        }
        this.erscheinungsjahr = (erscheinungsjahr != null && erscheinungsjahr.length() >= 4) ? erscheinungsjahr.substring(0, 4) : "";
        this.beschreibung = beschreibung != null ? beschreibung : "";
        this.status = status != null ? status : "";
        this.altersbeschraenkung = alter != null ? alter : "";
    }

    /**
     * Gibt die ISBN des Buches zurück.
     * @return Die ISBN.
     */
    public String getIsbn() {
        return isbn;
    }

    /**
     * Gibt den Titel des Buches zurück.
     * @return Der Titel.
     */
    public String getTitel() {
        return titel;
    }

    /**
     * Gibt den Autor des Buches zurück.
     * @return Der Autor.
     */
    public String getAutor() {
        return autor;
    }

    /**
     * Gibt das Erscheinungsjahr des Buches zurück.
     * @return Das Erscheinungsjahr.
     */
    public String getErscheinungsjahr() {
        return erscheinungsjahr;
    }

    /**
     * Gibt die Beschreibung des Buches zurück.
     * @return Die Beschreibung.
     */
    public String getBeschreibung() {
        return beschreibung;
    }

    /**
     * Gibt den aktuellen Status des Buches zurück.
     * @return Der Status (z. B. "verfügbar").
     */
    public String getStatus() {
        return status;
    }

    /**
     * Gibt die Altersbeschränkung des Buches zurück.
     * @return Die Altersbeschränkung.
     */
    public String getAlter() {
        return altersbeschraenkung;
    }

    /**
     * Setzt die ISBN des Buches.
     * @param isbn Die neue ISBN.
     */
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    /**
     * Setzt den Titel des Buches.
     * @param titel Der neue Titel.
     */
    public void setTitel(String titel) {
        this.titel = titel;
    }

    /**
     * Setzt den Autor des Buches.
     * @param autor Der neue Autor.
     */
    public void setAutor(String autor) {
        this.autor = autor;
    }

    /**
     * Setzt das Erscheinungsjahr des Buches.
     * @param erscheinungsjahr Das neue Erscheinungsjahr.
     */
    public void setErscheinungsjahr(String erscheinungsjahr) {
        this.erscheinungsjahr = erscheinungsjahr;
    }

    /**
     * Setzt die Beschreibung des Buches.
     * @param beschreibung Die neue Beschreibung.
     */
    public void setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
    }

    /**
     * Setzt den Status des Buches.
     * @param status Der neue Status.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Setzt die Altersbeschränkung des Buches.
     * @param alter Die neue Altersbeschränkung.
     */
    public void setAlter(String alter) {
        this.altersbeschraenkung = alter;
    }

    @Override
    /**
     * Gibt eine textuelle Repräsentation des Buches zurück (den Titel).
     * @return Der Titel des Buches.
     */
    public String toString() {
        return titel;
    }

}