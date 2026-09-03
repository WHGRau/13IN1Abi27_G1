public class Buch {

    private String isbn;
    private String titel;
    private String autor;
    private String erscheinungsjahr;
    private String beschreibung;
    private String status;
    private String altersbeschraenkung;

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

    public String getIsbn() {
        return isbn;
    }

    public String getTitel() {
        return titel;
    }

    public String getAutor() {
        return autor;
    }

    public String getErscheinungsjahr() {
        return erscheinungsjahr;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public String getStatus() {
        return status;
    }

    public String getAlter() {
        return altersbeschraenkung;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public void setErscheinungsjahr(String erscheinungsjahr) {
        this.erscheinungsjahr = erscheinungsjahr;
    }

    public void setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAlter(String alter) {
        this.altersbeschraenkung = alter;
    }

    @Override
    public String toString() {
        return titel;
    }

}