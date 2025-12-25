import org.json.JSONArray;
import org.json.JSONObject;

public class AIParser {

    public static String extractText(String response) {

        JSONObject json = new JSONObject(response);

        JSONArray choices = json.getJSONArray("choices");
        JSONObject firstChoice = choices.getJSONObject(0);

        JSONObject message = firstChoice.getJSONObject("message");
        return message.getString("content");
    }
}
