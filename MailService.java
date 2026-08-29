import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailService {

    private Bibliothek model;

    public MailService(Bibliothek model) {
        this.model = model;
    }

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

    public void sendeGesperrtMail(String empfaengerEmail, String nutzerName) {
        String betreff = "Dein Bibliotheks-Konto wurde gesperrt";
        String nachricht = "Hallo " + nutzerName + ",\n\n" +
                "dein Konto in der Schulbibliothek wurde soeben gesperrt.\n" +
                "Wende dich bitte an eine zuständige Lehrkraft, um den Grund zu klären.\n\n" +
                "Viele Grüße,\nDeine Schulbibliothek";
        sendeEmail(empfaengerEmail, betreff, nachricht);
    }

    public void sendeReservierungBereitMail(String empfaengerEmail, String nutzerName, String buchTitel) {
        String betreff = "Deine Reservierung ist abholbereit!";
        String nachricht = "Hallo " + nutzerName + ",\n\n" +
                "gute Nachrichten! Das von dir reservierte Buch \"" + buchTitel + "\" ist jetzt für dich verfügbar.\n" +
                "Bitte hole es innerhalb der nächsten " + model.getReservierungDauer()
                + " Tage in der Bibliothek ab.\n\n" +
                "Viele Grüße,\nDeine Schulbibliothek";
        sendeEmail(empfaengerEmail, betreff, nachricht);
    }

    public void sendeMahnungMail(String empfaengerEmail, String nutzerName, String buchTitel, String typ) {
        String betreff = "";
        String nachricht = "Hallo " + nutzerName + ",\n\n";

        if (typ.equals("2_Tage_vorher")) {
            betreff = "Erinnerung: Buchrückgabe in 2 Tagen fällig";
            nachricht += "wir möchten dich daran erinnern, dass du das Buch \"" + buchTitel
                    + "\" in 2 Tagen zurückgeben musst.\n" +
                    "Bitte denke daran, es rechtzeitig in der Bibliothek abzugeben.";
        } else if (typ.equals("Stichtag")) {
            betreff = "Buchrückgabe heute fällig!";
            nachricht += "du musst das Buch \"" + buchTitel + "\" heute in der Bibliothek zurückgeben!\n" +
                    "Bitte erledige das schnellstmöglich.";
        } else if (typ.equals("1_Woche_danach")) {
            betreff = "Buchrückgabe überfällig!";
            nachricht += "du hast das Buch \"" + buchTitel + "\" leider nicht rechtzeitig zurückgegeben.\n" +
                    "Bitte bringe das Buch umgehend in die Bibliothek.";
        }

        nachricht += "\n\n Viele Grüße,\nDeine Schulbibliothek";
        sendeEmail(empfaengerEmail, betreff, nachricht);
    }

}
