import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SchulmanagerConnector {
    private String jwt;
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
    public boolean login() {
        if (model == null) {
            System.err.println("Bibliothek-Model ist nicht gesetzt!");
            return false;
        }
        return login(model.getEinstellung("schulmanagerUsername"), model.getEinstellung("schulmanagerPassword"));
    }

    /**
     * Führt den Login mit den übergebenen Zugangsdaten aus.
     */
    public boolean login(String userName, String userPasswort) {
        try {
            if (userName == null || userPasswort == null) {
                System.err.println("Zugangsdaten für Schulmanager fehlen!");
                return false;
            }

            String loginPayload = String.format(
                    "{\"emailOrUsername\":\"%s\",\"password\":\"%s\",\"hash\":null,\"mobileApp\":false,\"institutionId\":null}",
                    userName, userPasswort);

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
     * Hilfsmethode mit automatischer 401-Erkennung und Re-Login-Schleife
     */
    private HttpResponse<String> sendSchulmanagerCallWithRetry(String payloadJson) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        for (int attempt = 0; attempt < 2; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/calls"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + jwt)
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
        if (klasse == null || klasse.trim().isEmpty()) {
            return -1;
        }
        try {
            String payload = String.format(
                    "{\"bundleVersion\":\"%s\",\"requests\":[{\"moduleName\":\"messenger\",\"endpointName\":\"get-chat-users\"}]}",
                    bundleVersion);

            HttpResponse<String> response = sendSchulmanagerCallWithRetry(payload);

            if (response.statusCode() == 200) {
                String responseJson = response.body();
                System.out.println("Schulmanager get-chat-users Response: " + responseJson);
                int foundId = parseUserIdFromChatUsers(responseJson, vorname, nachname, klasse);
                if (foundId != -1) {
                    return foundId;
                }
            } else {
                System.err.println("Schulmanager get-chat-users Fehler! Status: " + response.statusCode() + ", Body: " + response.body());
            }

            return -1;

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Parst die User-ID aus der get-chat-users JSON-Antwort.
     * Prüft flexibel auf Übereinstimmung von Name/Vorname und Klasse/Subtext.
     */
    private int parseUserIdFromChatUsers(String json, String vorname, String nachname, String klasse) {
        if (json == null || klasse == null || klasse.trim().isEmpty()) return -1;

        String normVorname = vorname != null ? vorname.trim().toLowerCase() : "";
        String normNachname = nachname != null ? nachname.trim().toLowerCase() : "";
        String normKlasse = klasse.trim().toLowerCase();

        // Suche nach Blöcken/Objekten innerhalb der Antwort
        // Typisches Format in Schulmanager: {"name":"Christoph Rauwolf","subtext":"Informatik",...,"id":1234}
        // oder {"id":1234,"name":"Christoph Rauwolf","subtext":"Informatik"}
        // oder verschachtelt: "user":{"id":1234,...}

        // Wir iterieren über Vorkommen von "id" oder "name"
        int pos = 0;

        while ((pos = json.indexOf("{", pos)) != -1) {
            int endObj = findClosingBrace(json, pos);
            if (endObj == -1) {
                pos++;
                continue;
            }

            // Prüfe nur flachere Objekte (oder prüfe jedes Objekt einzeln)
            String objJson = json.substring(pos, endObj + 1);

            String nameVal = extractJsonStringValue(objJson, "name");
            String firstnameVal = extractJsonStringValue(objJson, "firstname");
            String lastnameVal = extractJsonStringValue(objJson, "lastname");
            String subtextVal = extractJsonStringValue(objJson, "subtext");
            int idVal = extractJsonIntValue(objJson, "id");

            // Prüfe, ob dieses Objekt nicht das äußere Root-Objekt ist (das evtl. viele Kinder enthält)
            boolean isLeafOrUserObj = !objJson.contains("\"requests\"") && !objJson.contains("\"results\"");

            if (isLeafOrUserObj && idVal != -1 && (nameVal != null || firstnameVal != null || lastnameVal != null)) {
                boolean nameMatches = false;
                if (nameVal != null) {
                    String normNameVal = nameVal.toLowerCase();
                    if (!normVorname.isEmpty() && !normNachname.isEmpty()) {
                        nameMatches = normNameVal.contains(normVorname) && normNameVal.contains(normNachname);
                    } else if (!normNachname.isEmpty()) {
                        nameMatches = normNameVal.contains(normNachname);
                    } else if (!normVorname.isEmpty()) {
                        nameMatches = normNameVal.contains(normVorname);
                    }
                }
                if (!nameMatches && (firstnameVal != null || lastnameVal != null)) {
                    String fn = firstnameVal != null ? firstnameVal.toLowerCase() : "";
                    String ln = lastnameVal != null ? lastnameVal.toLowerCase() : "";
                    nameMatches = (normVorname.isEmpty() || fn.contains(normVorname))
                            && (normNachname.isEmpty() || ln.contains(normNachname));
                }

                if (nameMatches) {
                    String normSubtext = subtextVal != null ? subtextVal.trim().toLowerCase() : "";
                    if (!normSubtext.isEmpty() && (normSubtext.contains(normKlasse) || normKlasse.contains(normSubtext))) {
                        return idVal; // Match für Name und Klasse!
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
        if (idx == -1) return null;

        int colon = json.indexOf(":", idx + search.length());
        if (colon == -1) return null;

        int quoteStart = json.indexOf("\"", colon + 1);
        if (quoteStart == -1) return null;

        // Prüfen, ob dazwischen kein null steht
        String between = json.substring(colon + 1, quoteStart).trim();
        if (between.startsWith("null")) return null;

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
        if (idx == -1) return -1;

        int colon = json.indexOf(":", idx + search.length());
        if (colon == -1) return -1;

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
            } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    /**
     * Sendet eine Nachricht direkt an den Schulmanager (startet einen neuen
     * Chat/Thread).
     */
    public void sendNachricht(int userId, String betreff, String nachricht) {
        try {
            String recipientName = "Empfaenger";
            String recipientSubtextJson = "null";

            // 1. Versuche Name und Subtext über get-chat-users für die userId zu ermitteln
            try {
                String usersPayload = String.format(
                        "{\"bundleVersion\":\"%s\",\"requests\":[{\"moduleName\":\"messenger\",\"endpointName\":\"get-chat-users\"}]}",
                        bundleVersion);
                HttpResponse<String> usersResp = sendSchulmanagerCallWithRetry(usersPayload);
                if (usersResp.statusCode() == 200 && usersResp.body() != null) {
                    String[] userInfo = findUserInfoById(usersResp.body(), userId);
                    if (userInfo != null && userInfo[0] != null) {
                        recipientName = userInfo[0];
                        if (userInfo[1] != null) {
                            recipientSubtextJson = "\"" + escapeJson(userInfo[1]) + "\"";
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            // 2. Fallback: Name aus der lokalen Datenbank holen, falls über get-chat-users nicht gefunden
            if ("Empfaenger".equals(recipientName) && model != null && model.getDbConnector() != null
                    && model.getDbConnector().isConnected()) {
                model.getDbConnector().executeStatement(
                        "SELECT vorname, nachname FROM benutzer WHERE schulmanagerUserId = ?", userId);
                QueryResult qr = model.getDbConnector().getCurrentQueryResult();
                if (qr != null && qr.getRowCount() > 0) {
                    String vn = qr.getData()[0][0];
                    String nn = qr.getData()[0][1];
                    recipientName = ((vn != null ? vn : "") + " " + (nn != null ? nn : "")).trim();
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sucht zu einer gegebenen userId das [name, subtext] Paar aus der get-chat-users Antwort.
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
            boolean isLeafOrUserObj = !objJson.contains("\"requests\"") && !objJson.contains("\"results\"");
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
     * Escaped Sonderzeichen (Anführungszeichen, Backslashes, Zeilenumbrüche), damit der JSON-String valide bleibt.
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
}