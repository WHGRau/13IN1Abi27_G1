public class Buch {

    private String isbn;
    private String titel;
    private String autor;
    private String erscheinungsjahr;
    private String beschreibung;
    private String status;

    public Buch(String isbn, String titel, String autor, String erscheinungsjahr, String beschreibung, String status) {
        this.isbn = isbn;
        this.titel = titel;
        this.autor = autor;
        this.erscheinungsjahr = erscheinungsjahr.substring(0, 4);
        this.beschreibung = beschreibung;
        this.status = status;
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

    @Override
    public String toString() {
        return titel;
    }

}