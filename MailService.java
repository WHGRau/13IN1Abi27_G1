import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Verarbeitet den E-Mail-Versand der Bibliothek.
 * Sendet Benachrichtigungen über Reservierungen, Mahnungen, Kontosperrungen und mehr.
 */
public class MailService {

    private Bibliothek model;

    /**
     * Erstellt einen neuen MailService und verknüpft ihn mit dem Bibliotheks-Model.
     * @param model Die Hauptinstanz der Bibliothek.
     */
    public MailService(Bibliothek model) {
        this.model = model;
    }

    /**
     * Prüft, ob die E-Mail-Zugangsdaten in den Einstellungen hinterlegt wurden.
     * @return true, falls konfiguriert, andernfalls false.
     */
    public boolean isConfigured() {
        String email = model.getEinstellung("email_adresse");
        String passwort = model.getEinstellung("email_passwort");
        String smtpServer = model.getEinstellung("smtp_server");
        String smtpPort = model.getEinstellung("smtp_port");

        return email != null && !email.isEmpty() &&
                passwort != null && !passwort.isEmpty() &&
                smtpServer != null && !smtpServer.isEmpty() &&
                smtpPort != null && !smtpPort.isEmpty();
    }

    /**
     * Sendet eine generische E-Mail asynchron an die angegebene Adresse.
     * 
     * @param empfaengerEmail Die E-Mail-Adresse des Empfängers.
     * @param betreff Der Betreff der E-Mail.
     * @param nachricht Der Inhalt der E-Mail.
     */
    public void sendeEmail(String empfaengerEmail, String betreff, String nachricht) {
        if (!isConfigured()) {
            return;
        }

        final String username = model.getEinstellung("email_adresse");
        final String password = model.getEinstellung("email_passwort");
        String smtpServer = model.getEinstellung("smtp_server");
        String smtpPort = model.getEinstellung("smtp_port");

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpServer);
        props.put("mail.smtp.port", smtpPort);

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(empfaengerEmail));
            message.setSubject(betreff);
            message.setText(nachricht);

            // Using thread so it doesn't freeze the UI
            new Thread(() -> {
                try {
                    Transport.send(message);
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sendet eine E-Mail an einen Nutzer, dass sein Konto gesperrt wurde.
     * 
     * @param empfaengerEmail Die E-Mail-Adresse des gesperrten Nutzers.
     * @param nutzerName Der Name des Nutzers für die Anrede.
     */
    public void sendeGesperrtMail(String empfaengerEmail, String nutzerName) {
        String betreff = "Dein Bibliotheks-Konto wurde gesperrt";
        String nachricht = "Hallo " + nutzerName + ",\n\n" +
                "dein Konto in der Schulbibliothek wurde soeben gesperrt.\n" +
                "Wende dich bitte an eine zuständige Lehrkraft, um den Grund zu klären.\n\n" +
                "Viele Grüße,\nDeine Schulbibliothek";
        sendeEmail(empfaengerEmail, betreff, nachricht);
    }

    /**
     * Benachrichtigt einen Nutzer darüber, dass ein reserviertes Buch abholbereit ist.
     * 
     * @param empfaengerEmail Die E-Mail-Adresse des Nutzers.
     * @param nutzerName Der Name des Nutzers für die Anrede.
     * @param buchTitel Der Titel des bereitliegenden Buches.
     */
    public void sendeReservierungBereitMail(String empfaengerEmail, String nutzerName, String buchTitel) {
        String betreff = "Deine Reservierung ist abholbereit!";
        String nachricht = "Hallo " + nutzerName + ",\n\n" +
                "gute Nachrichten! Das von dir reservierte Buch \"" + buchTitel + "\" ist jetzt für dich verfügbar.\n" +
                "Bitte hole es innerhalb der nächsten " + model.getReservierungDauer()
                + " Tage in der Bibliothek ab.\n\n" +
                "Viele Grüße,\nDeine Schulbibliothek";
        sendeEmail(empfaengerEmail, betreff, nachricht);
    }

    /**
     * Sendet eine Erinnerung oder Mahnung zur Buchrückgabe.
     * 
     * @param empfaengerEmail Die E-Mail-Adresse des Nutzers.
     * @param nutzerName Der Name des Nutzers.
     * @param buchTitel Der Titel des ausgeliehenen Buches.
     * @param typ Der Typ der Mahnung (z. B. "2_Tage_vorher", "Stichtag", "1_Woche_danach").
     * @param rueckgabeDatum Das fällige Rückgabedatum als String.
     */
    public void sendeMahnungMail(String empfaengerEmail, String nutzerName, String buchTitel, String typ,
            String rueckgabeDatum) {
        String betreff = "";
        String nachricht = "Hallo " + nutzerName + ",\n\n";

        if (typ.equals("2_Tage_vorher")) {
            betreff = "Erinnerung: Buchrückgabe bald fällig";
            nachricht += "wir möchten dich daran erinnern, dass du das Buch \"" + buchTitel
                    + "\" am " + rueckgabeDatum + " zurückgeben musst.\n" +
                    "Bitte denke daran, es rechtzeitig in der Bibliothek abzugeben.";
        } else if (typ.equals("Stichtag")) {
            betreff = "Buchrückgabe heute fällig!";
            nachricht += "du musst das Buch \"" + buchTitel + "\" in der Bibliothek zurückgeben!\n" +
                    "Bitte erledige das schnellstmöglich.";
        } else if (typ.equals("1_Woche_danach")) {
            betreff = "Buchrückgabe überfällig!";
            nachricht += "du hast das Buch \"" + buchTitel + "\" leider nicht rechtzeitig zurückgegeben.\n" +
                    "Bitte bringe das Buch umgehend in die Bibliothek.";
        }

        nachricht += "\n\nViele Grüße,\nDeine Schulbibliothek";
        sendeEmail(empfaengerEmail, betreff, nachricht);
    }

    /**
     * Sendet eine Willkommens-E-Mail mit den Anmeldedaten nach der Registrierung.
     * 
     * @param empfaengerEmail Die E-Mail-Adresse des neuen Nutzers.
     * @param nutzerName Der Name des Nutzers.
     * @param passwort Das vergebene Initialpasswort.
     */
    public void sendeAnmeldeMail(String empfaengerEmail, String nutzerName, String passwort) {
        String betreff = "Willkommen in der Schülerbibliothek!";
        String nachricht = "Hallo " + nutzerName
                + ",\n\n Wir freuen uns sehr, dass du dich für die Bibliothek angemeldet hast. \n Mit folgendem Passwort kannst du dich im Onlineportal anmelden: \n"
                + passwort
                + "\n Wir hoffen dich bald in der Bibliothek zu sehen! \n\n Viele Grüße, \n Deine Schülerbibliothek";
        sendeEmail(empfaengerEmail, betreff, nachricht);
    }

    /**
     * Sendet eine E-Mail mit einem neuen Passwort, wenn dieses zurückgesetzt wurde.
     * 
     * @param empfaengerEmail Die E-Mail-Adresse des Nutzers.
     * @param nutzerName Der Name des Nutzers.
     * @param passwort Das neu vergebene Passwort.
     */
    public void sendePasswortResetMail(String empfaengerEmail, String nutzerName, String passwort) {
        String betreff = "Passwort zurücksetzen";
        String nachricht = "Hallo " + nutzerName
                + ",\n\n  \n Mit folgendem Passwort kannst du dich nun im Onlineportal anmelden: \n" + passwort
                + "\n Wir hoffen dich bald in der Bibliothek zu sehen! \n\n Viele Grüße, \n Deine Schülerbibliothek";
        sendeEmail(empfaengerEmail, betreff, nachricht);
    }
}
