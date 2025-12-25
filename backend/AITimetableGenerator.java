import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class AITimetableGenerator {

private static final String GROQ_API_KEY =
    "PASTE-YOUR-API-KEY";


    public static String callGPT(String situation) throws Exception {

        if (GROQ_API_KEY == null) {
            throw new RuntimeException("GROQ_API_KEY is missing");
        }

        String safeInput = escapeJson(situation);

        String url = "https://api.groq.com/openai/v1/chat/completions";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + GROQ_API_KEY);
        conn.setDoOutput(true);

        // ✅ ONLY VALID MODEL
        String jsonBody =
            "{"
          + "\"model\":\"llama-3.1-8b-instant\","
          + "\"messages\":["
          + " {\"role\":\"system\",\"content\":\"You are a smart time management assistant.\"},"
          + " {\"role\":\"user\",\"content\":\"" + safeInput + "\"}"
          + "]"
          + "}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes("UTF-8"));
        }

        int code = conn.getResponseCode();
        InputStream is = (code < 400) ? conn.getInputStream() : conn.getErrorStream();
        String response = new String(is.readAllBytes(), "UTF-8");

        if (code >= 400) {
            throw new RuntimeException("Groq error " + code + ": " + response);
        }

        return response;
    }

    private static String escapeJson(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
