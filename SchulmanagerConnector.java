import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

public class SchulmanagerConnector {
    private volatile String jwt;
    private volatile String cachedChatUsersJson;
    private final String apiUrl = "https://login.schulmanager-online.de/api";
    private final String bundleVersion = "860c7bfd2a";
    private final String userAgent = "SchulmanagerAutomatisierung/1.0 (rauwolf@whg-neuwied.de)";

    private Bibliothek model;

    public SchulmanagerConnector(Bibliothek model) {
        this.model = model;
    }

    /**
     * Holt Benutzername und Passwort aus der Bibliothek und führt den Login aus.
     */
    public synchronized boolean login() {
        if (model == null) {
            System.err.println("Bibliothek-Model ist nicht gesetzt!");
            return false;
        }
        return login(model.getEinstellung("schulmanagerUsername"), model.getEinstellung("schulmanagerPassword"));
    }

    /**
     * Führt den Login mit den übergebenen Zugangsdaten aus.
     */
    public synchronized boolean login(String userName, String userPasswort) {
        this.jwt = null;
        this.cachedChatUsersJson = null;
        try {
            if (userName == null || userName.trim().isEmpty() || userPasswort == null || userPasswort.isEmpty()) {
                System.err.println("Zugangsdaten für Schulmanager fehlen!");
                return false;
            }

            String loginPayload = String.format(
                    "{\"emailOrUsername\":\"%s\",\"password\":\"%s\",\"hash\":null,\"mobileApp\":false,\"institutionId\":null}",
                    escapeJson(userName), escapeJson(userPasswort));

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/login"))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", userAgent)
                    .POST(HttpRequest.BodyPublishers.ofString(loginPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                int jwtIndex = body.indexOf("\"jwt\":\"");
                if (jwtIndex != -1) {
                    int startIndex = jwtIndex + 7;
                    int endIndex = body.indexOf("\"", startIndex);
                    this.jwt = body.substring(startIndex, endIndex);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Setzt das gespeicherte JWT-Token zurück.
     */
    public synchronized void logout() {
        this.jwt = null;
        this.cachedChatUsersJson = null;
    }

    /**
     * Hilfsmethode mit automatischer 401-Erkennung und Re-Login-Schleife
     */
    private HttpResponse<String> sendSchulmanagerCallWithRetry(String payloadJson) throws Exception {
        if (this.jwt == null) {
            if (!login()) {
                throw new RuntimeException("API-Call nicht möglich: Schulmanager-Login fehlgeschlagen.");
            }
        }

        HttpClient client = HttpClient.newHttpClient();

        for (int attempt = 0; attempt < 2; attempt++) {
            String currentJwt = this.jwt;
            if (currentJwt == null && !login()) {
                throw new RuntimeException("API-Call nicht möglich: Kein gültiges JWT-Token.");
            }
            currentJwt = this.jwt;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/calls"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + currentJwt)
                    .header("User-Agent", userAgent)
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 && attempt == 0) {
                System.out.println("JWT abgelaufen (401). Führe automatisches Re-Login durch...");
                if (login()) {
                    continue;
                }
            }

            return response;
        }
        throw new RuntimeException("API-Call fehlgeschlagen nach manuellem Re-Login.");
    }

    /**
     * Sucht die User-ID anhand von Vorname, Nachname und Klasse (abgeglichen über
     * den Subtext).
     */
    public int getUserId(String vorname, String nachname, String klasse) {
        if (vorname == null || vorname.trim().isEmpty()
                || nachname == null || nachname.trim().isEmpty()
                || klasse == null || klasse.trim().isEmpty()) {
            return -1;
        }
        try {
            String payload = String.format(
                    "{\"bundleVersion\":\"%s\",\"requests\":[{\"moduleName\":\"messenger\",\"endpointName\":\"get-chat-users\"}]}",
                    bundleVersion);

            HttpResponse<String> response = sendSchulmanagerCallWithRetry(payload);

            if (response.statusCode() == 200) {
                String responseJson = response.body();
                this.cachedChatUsersJson = responseJson;
                System.out.println("Schulmanager get-chat-users Response: " + responseJson);
                int foundId = parseUserIdFromChatUsers(responseJson, vorname, nachname, klasse);
                if (foundId != -1) {
                    return foundId;
                }
            } else {
                System.err.println("Schulmanager get-chat-users Fehler! Status: " + response.statusCode() + ", Body: "
                        + response.body());
            }

            return -1;

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Holt einmalig alle Nutzer vom Schulmanager und sucht für alle übergebenen
     * Schüler (sofern eine Klasse angegeben ist) die passende Schulmanager-ID
     * und speichert diese direkt in den übergebenen Benutzer-Objekten.
     * 
     * @param schuelerListe Die Liste der Benutzer, für die die Schulmanager-ID
     *                      gesucht werden soll.
     * @return Anzahl der erfolgreich zugeordneten Schüler.
     */
    public int getUserIds(ArrayList<Benutzer> schuelerListe) {
        if (schuelerListe == null || schuelerListe.isEmpty()) {
            return 0;
        }
        ArrayList<String> klassen = new ArrayList<>();
        for (Benutzer b : schuelerListe) {
            klassen.add(b != null ? b.getKlasse() : null);
        }
        return getUserIds(schuelerListe, klassen);
    }

    /**
     * Holt einmalig alle Nutzer vom Schulmanager und sucht anhand der übergebenen
     * Klassenliste für alle übergebenen Schüler die passende Schulmanager-ID
     * und speichert diese direkt in den Benutzer-Objekten.
     * 
     * @param schuelerListe Die Liste der Benutzer.
     * @param klassen       Die zugehörigen Klassen der Benutzer (gleicher Index wie
     *                      schuelerListe).
     * @return Anzahl der erfolgreich zugeordneten Schüler.
     */
    public int getUserIds(ArrayList<Benutzer> schuelerListe, ArrayList<String> klassen) {
        if (schuelerListe == null || schuelerListe.isEmpty()) {
            return 0;
        }

        boolean hatMindestensEinenGueltigenEintrag = false;
        for (int i = 0; i < schuelerListe.size(); i++) {
            Benutzer b = schuelerListe.get(i);
            if (b == null || b.getVorname() == null || b.getVorname().trim().isEmpty()
                    || b.getName() == null || b.getName().trim().isEmpty()) {
                continue;
            }
            String klasse = (klassen != null && i < klassen.size() && klassen.get(i) != null)
                    ? klassen.get(i)
                    : b.getKlasse();
            if (klasse != null && !klasse.trim().isEmpty()) {
                hatMindestensEinenGueltigenEintrag = true;
                break;
            }
        }
        if (!hatMindestensEinenGueltigenEintrag) {
            return 0;
        }

        int verbundenCount = 0;
        try {
            String payload = String.format(
                    "{\"bundleVersion\":\"%s\",\"requests\":[{\"moduleName\":\"messenger\",\"endpointName\":\"get-chat-users\"}]}",
                    bundleVersion);

            HttpResponse<String> response = sendSchulmanagerCallWithRetry(payload);

            if (response.statusCode() == 200) {
                String responseJson = response.body();
                this.cachedChatUsersJson = responseJson;
                System.out.println("Schulmanager get-chat-users Response: " + responseJson);

                for (int i = 0; i < schuelerListe.size(); i++) {
                    Benutzer b = schuelerListe.get(i);
                    if (b == null || b.getVorname() == null || b.getVorname().trim().isEmpty()
                            || b.getName() == null || b.getName().trim().isEmpty()) {
                        continue;
                    }
                    String klasse = (klassen != null && i < klassen.size() && klassen.get(i) != null)
                            ? klassen.get(i)
                            : b.getKlasse();
                    if (klasse == null || klasse.trim().isEmpty()) {
                        continue;
                    }
                    int foundId = parseUserIdFromChatUsers(responseJson, b.getVorname(), b.getName(), klasse);
                    if (foundId != -1) {
                        b.setSchulmanagerId(foundId);
                        verbundenCount++;
                    }
                }
            } else {
                System.err.println("Schulmanager get-chat-users Fehler! Status: " + response.statusCode()
                        + ", Body: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return verbundenCount;
    }

    /**
     * Parst die User-ID aus der get-chat-users JSON-Antwort.
     * Prüft auf exakte Übereinstimmung von Vorname, Nachname und Klasse/Subtext.
     */
    private int parseUserIdFromChatUsers(String json, String vorname, String nachname, String klasse) {
        if (json == null
                || vorname == null || vorname.trim().isEmpty()
                || nachname == null || nachname.trim().isEmpty()
                || klasse == null || klasse.trim().isEmpty()) {
            return -1;
        }

        String normVorname = vorname.trim().toLowerCase();
        String normNachname = nachname.trim().toLowerCase();
        String normKlasse = klasse.trim().toLowerCase();

        String expectedVorNach = (normVorname + " " + normNachname).replaceAll("\\s+", " ");
        String expectedNachVor = (normNachname + " " + normVorname).replaceAll("\\s+", " ");
        String expectedNachKommaVor = (normNachname + ", " + normVorname).replaceAll("\\s+", " ");

        int pos = 0;

        while ((pos = json.indexOf("{", pos)) != -1) {
            int endObj = findClosingBrace(json, pos);
            if (endObj == -1) {
                pos++;
                continue;
            }

            String objJson = json.substring(pos, endObj + 1);

            // Prüfe, ob dieses Objekt nicht das äußere Root- oder Ergebnis-Array-Objekt ist
            boolean isLeafOrUserObj = !objJson.contains("\"requests\"")
                    && !objJson.contains("\"results\"")
                    && !objJson.contains("\"data\"");

            if (isLeafOrUserObj) {
                String nameVal = extractJsonStringValue(objJson, "name");
                String firstnameVal = extractJsonStringValue(objJson, "firstname");
                String lastnameVal = extractJsonStringValue(objJson, "lastname");
                String subtextVal = extractJsonStringValue(objJson, "subtext");
                int idVal = extractJsonIntValue(objJson, "id");

                if (idVal != -1 && (nameVal != null || (firstnameVal != null && lastnameVal != null))) {
                    boolean nameMatches = false;
                    if (firstnameVal != null && lastnameVal != null) {
                        nameMatches = firstnameVal.trim().equalsIgnoreCase(normVorname)
                                && lastnameVal.trim().equalsIgnoreCase(normNachname);
                    }
                    if (!nameMatches && nameVal != null) {
                        String normNameVal = nameVal.trim().replaceAll("\\s+", " ").toLowerCase();
                        nameMatches = normNameVal.equals(expectedVorNach)
                                || normNameVal.equals(expectedNachVor)
                                || normNameVal.equals(expectedNachKommaVor);
                    }

                    if (nameMatches) {
                        String normSubtext = subtextVal != null ? subtextVal.trim().toLowerCase() : "";
                        if (!normSubtext.isEmpty() && normSubtext.equals(normKlasse)) {
                            return idVal;
                        }
                    }
                }
            }

            pos++;
        }

        return -1;
    }

    private int findClosingBrace(String text, int openPos) {
        int depth = 0;
        boolean inQuotes = false;
        for (int i = openPos; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && inQuotes) {
                i++; // Skip escaped char
                continue;
            }
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private String extractJsonStringValue(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1)
            return null;

        int colon = json.indexOf(":", idx + search.length());
        if (colon == -1)
            return null;

        int quoteStart = json.indexOf("\"", colon + 1);
        if (quoteStart == -1)
            return null;

        // Prüfen, ob dazwischen kein null steht
        String between = json.substring(colon + 1, quoteStart).trim();
        if (between.startsWith("null"))
            return null;

        int quoteEnd = quoteStart + 1;
        while (quoteEnd < json.length()) {
            if (json.charAt(quoteEnd) == '\\') {
                quoteEnd += 2;
                continue;
            }
            if (json.charAt(quoteEnd) == '"') {
                return json.substring(quoteStart + 1, quoteEnd);
            }
            quoteEnd++;
        }
        return null;
    }

    private int extractJsonIntValue(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1)
            return -1;

        int colon = json.indexOf(":", idx + search.length());
        if (colon == -1)
            return -1;

        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }

        if (end > start) {
            try {
                return Integer.parseInt(json.substring(start, end));
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    /**
     * Sendet eine Nachricht direkt an den Schulmanager (startet einen neuen
     * Chat/Thread).
     */
    public boolean sendNachricht(int userId, String betreff, String nachricht) {
        try {
            if (this.jwt == null && !login()) {
                return false;
            }

            String recipientName = "Empfaenger";
            String recipientSubtextJson = "null";

            // 1. Versuche Name und Subtext über get-chat-users (einmalig gecacht) für die userId zu ermitteln
            try {
                String chatUsersJson = this.cachedChatUsersJson;
                if (chatUsersJson == null) {
                    String usersPayload = String.format(
                            "{\"bundleVersion\":\"%s\",\"requests\":[{\"moduleName\":\"messenger\",\"endpointName\":\"get-chat-users\"}]}",
                            bundleVersion);
                    HttpResponse<String> usersResp = sendSchulmanagerCallWithRetry(usersPayload);
                    if (usersResp != null && usersResp.statusCode() == 200 && usersResp.body() != null) {
                        chatUsersJson = usersResp.body();
                        this.cachedChatUsersJson = chatUsersJson;
                    }
                }
                if (chatUsersJson != null) {
                    String[] userInfo = findUserInfoById(chatUsersJson, userId);
                    if (userInfo != null && userInfo[0] != null) {
                        recipientName = userInfo[0];
                        if (userInfo[1] != null) {
                            recipientSubtextJson = "\"" + escapeJson(userInfo[1]) + "\"";
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            // 2. Fallback: Name aus der lokalen Datenbank holen, falls über get-chat-users
            // nicht gefunden
            if ("Empfaenger".equals(recipientName) && model != null && model.getDbConnector() != null
                    && model.getDbConnector().isConnected()) {
                synchronized (model.getDbConnector()) {
                    model.getDbConnector().executeStatement(
                            "SELECT vorname, nachname FROM benutzer WHERE schulmanagerUserId = ?", userId);
                    QueryResult qr = model.getDbConnector().getCurrentQueryResult();
                    if (qr != null && qr.getRowCount() > 0) {
                        String vn = qr.getData()[0][0];
                        String nn = qr.getData()[0][1];
                        recipientName = ((vn != null ? vn : "") + " " + (nn != null ? nn : "")).trim();
                    }
                }
            }

            String payload = String.format(
                    "{\"bundleVersion\":\"%s\",\"requests\":[{\"moduleName\":\"messenger\",\"endpointName\":\"create-thread\",\"parameters\":{\"subject\":\"%s\",\"users\":[{\"id\":%d}],\"allowAnswers\":false,\"recipientOptions\":[{\"name\":\"%s\",\"subtext\":%s,\"type\":\"user\",\"user\":{\"id\":%d}}],\"firstMessage\":{\"text\":\"%s\",\"attachments\":[]}}}]}",
                    bundleVersion,
                    escapeJson(betreff),
                    userId,
                    escapeJson(recipientName),
                    recipientSubtextJson,
                    userId,
                    escapeJson(nachricht));

            HttpResponse<String> response = sendSchulmanagerCallWithRetry(payload);
            return response != null && response.statusCode() == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sucht zu einer gegebenen userId das [name, subtext] Paar aus der
     * get-chat-users Antwort.
     */
    private String[] findUserInfoById(String json, int targetId) {
        int pos = 0;
        while ((pos = json.indexOf("{", pos)) != -1) {
            int endObj = findClosingBrace(json, pos);
            if (endObj == -1) {
                pos++;
                continue;
            }
            String objJson = json.substring(pos, endObj + 1);
            boolean isLeafOrUserObj = !objJson.contains("\"requests\"")
                    && !objJson.contains("\"results\"")
                    && !objJson.contains("\"data\"");
            if (isLeafOrUserObj) {
                int idVal = extractJsonIntValue(objJson, "id");
                String nameVal = extractJsonStringValue(objJson, "name");
                if (idVal == targetId && nameVal != null) {
                    String subtextVal = extractJsonStringValue(objJson, "subtext");
                    return new String[] { nameVal, subtextVal };
                }
            }
            pos++;
        }
        return null;
    }

    /**
     * Escaped Sonderzeichen (Anführungszeichen, Backslashes, Zeilenumbrüche), damit
     * der JSON-String valide bleibt.
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n")
                .replace("\t", "\\t");
    }

    public boolean sendErinnerung(int smUserId, String nutzerName, String buchTitel, String typ,
            String rueckgabeDatum) {
        if (model != null && !model.isSchulmanagerErinnerungAktiv()) {
            return false;
        }
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
        return sendNachricht(smUserId, betreff, nachricht);
    }

    /**
     * Sendet eine Benachrichtigung über eine bereitgestellte Reservierung per
     * Schulmanager (synchron).
     *
     * @param smUserId   Die Schulmanager-User-ID des Nutzers.
     * @param nutzerName Der Name/Vorname des Nutzers.
     * @param buchTitel  Der Titel des abholbereiten Buches.
     * @return true bei erfolgreichem Versand, sonst false.
     */
    public boolean sendReservierungBereit(int smUserId, String nutzerName, String buchTitel) {
        if (model != null && !model.isSchulmanagerErinnerungAktiv()) {
            return false;
        }
        int dauer = (model != null && model.getReservierungDauer() > 0) ? model.getReservierungDauer() : 7;
        String betreff = "Deine Reservierung ist abholbereit!";
        String nachricht = "Hallo " + nutzerName + ",\n\n" +
                "gute Nachrichten! Das von dir reservierte Buch \"" + buchTitel
                + "\" ist jetzt für dich verfügbar.\n" +
                "Bitte hole es innerhalb der nächsten " + dauer
                + " Tage in der Bibliothek ab.\n\n" +
                "Viele Grüße,\nDeine Schulbibliothek";
        return sendNachricht(smUserId, betreff, nachricht);
    }

    /**
     * Sendet eine Benachrichtigung über eine bereitgestellte Reservierung per
     * Schulmanager (asynchron).
     *
     * @param smUserId   Die Schulmanager-User-ID des Nutzers.
     * @param nutzerName Der Name/Vorname des Nutzers.
     * @param buchTitel  Der Titel des abholbereiten Buches.
     */
    public void sendeReservierungBereit(int smUserId, String nutzerName, String buchTitel) {
        new Thread(() -> sendReservierungBereit(smUserId, nutzerName, buchTitel)).start();
    }

    /**
     * Sendet eine Benachrichtigung über die Sperrung des Bibliothekskontos per
     * Schulmanager (synchron).
     *
     * @param smUserId   Die Schulmanager-User-ID des Nutzers.
     * @param nutzerName Der Name/Vorname des Nutzers.
     * @return true bei erfolgreichem Versand, sonst false.
     */
    public boolean sendKontoGesperrt(int smUserId, String nutzerName) {
        if (model != null && !model.isSchulmanagerErinnerungAktiv()) {
            return false;
        }
        String betreff = "Dein Bibliotheks-Konto wurde gesperrt";
        String nachricht = "Hallo " + nutzerName + ",\n\n" +
                "dein Konto in der Schulbibliothek wurde soeben gesperrt.\n" +
                "Wende dich bitte an eine zuständige Lehrkraft, um den Grund zu klären.\n\n" +
                "Viele Grüße,\nDeine Schulbibliothek";
        return sendNachricht(smUserId, betreff, nachricht);
    }

    /**
     * Sendet eine Benachrichtigung über die Sperrung des Bibliothekskontos per
     * Schulmanager (asynchron).
     *
     * @param smUserId   Die Schulmanager-User-ID des Nutzers.
     * @param nutzerName Der Name/Vorname des Nutzers.
     */
    public void sendeKontoGesperrt(int smUserId, String nutzerName) {
        new Thread(() -> sendKontoGesperrt(smUserId, nutzerName)).start();
    }

    /**
     * Alias für sendErinnerung (synchron).
     */
    public boolean sendeMahnungSync(int smUserId, String nutzerName, String buchTitel, String typ,
            String rueckgabeDatum) {
        return sendErinnerung(smUserId, nutzerName, buchTitel, typ, rueckgabeDatum);
    }

    /**
     * Alias für sendErinnerung (asynchron).
     */
    public void sendeMahnung(int smUserId, String nutzerName, String buchTitel, String typ, String rueckgabeDatum) {
        new Thread(() -> sendErinnerung(smUserId, nutzerName, buchTitel, typ, rueckgabeDatum)).start();
    }
}