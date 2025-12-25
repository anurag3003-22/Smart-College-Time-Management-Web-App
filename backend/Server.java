import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class Server {

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // ================= STATIC FILE SERVER =================
        server.createContext("/", exchange -> {
            try {
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/")) path = "/index.html";

                Path filePath = Path.of("." + path);
                if (!Files.exists(filePath)) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }

                String type = "text/plain";
                if (path.endsWith(".html")) type = "text/html";
                else if (path.endsWith(".css")) type = "text/css";
                else if (path.endsWith(".js")) type = "application/javascript";
                else if (path.endsWith(".png")) type = "image/png";
                else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) type = "image/jpeg";

                exchange.getResponseHeaders().add("Content-Type", type);
                byte[] data = Files.readAllBytes(filePath);
                exchange.sendResponseHeaders(200, data.length);
                exchange.getResponseBody().write(data);

            } finally {
                exchange.close();
            }
        });

        // ================= AI ENDPOINT =================
        server.createContext("/ai", exchange -> {

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes());

 String prompt =
"You are a senior academic mentor for a B.Tech student.\n\n" +

"TASK:\n" +
"Create a SHORT, REALISTIC, DAY-WISE study plan that a student can actually follow.\n\n" +

"STRICT RULES:\n" +
"- Use ONLY student-provided data.\n" +
"- Do NOT write long explanations.\n" +
"- Do NOT add summaries or motivation.\n" +
"- Keep each day minimal and practical.\n\n" +

"DATE & DAY FORMAT (MANDATORY):\n" +
"- Start from TODAY.\n" +
"- Each day must include date.\n" +
"- Format exactly like:\n" +
"  Day 1 (Tue, 24 Dec):\n\n" +

"TIME RULES:\n" +
"- Respect exact study hours per day.\n" +
"- Split into Morning / Afternoon / Night.\n" +
"- Keep tasks short and doable.\n\n" +

"CONTENT RULES:\n" +
"- Mention unit names clearly.\n" +
"- Gym / activity only if given.\n" +
"- Assignments or quizzes ONLY on deadline day.\n\n" +

"STYLE RULES:\n" +
"- Use → and : only.\n" +
"- One line per slot.\n" +
"- No paragraphs.\n" +
"- No overloading.\n\n" +

"STOP RULE:\n" +
"- Stop after last day.\n\n" +

"STUDENT DATA:\n" +
body;

            try {
                String aiText = AIParser.extractText(
                        AITimetableGenerator.callGPT(prompt)
                );

                // CLEAN EXTRA TEXT
                aiText = aiText
                        .replace("*", "")
                        .replace("+", "")
                        .replace("###", "")
                        .replace("##", "")
                        .replace("—", "-")
                        .replaceAll("(?s)⏱ Planning Summary.*", "");

                String html = """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>AI Study Plan</title>

<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap" rel="stylesheet">

<style>
body{
  margin:0;
  font-family:Inter, Arial;
  background:linear-gradient(135deg,#d8b4fe,#6366f1);
  color:white;
}

.page{
  min-height:100vh;
  padding:40px;
}

h1{
  text-align:center;
  margin-bottom:30px;
  font-weight:700;
}

.grid{
  display:grid;
  grid-template-columns:1fr;   /* 🔥 FULL WIDTH FIX */
}

.card{
  background:linear-gradient(135deg,#7c3aed,#2563eb);
  border-radius:24px;
  padding:30px;
  box-shadow:0 20px 40px rgba(0,0,0,0.35);
}

.card h2{
  margin-bottom:18px;
  font-size:22px;
}

.plan{
  white-space:pre-wrap;
  line-height:1.7;
  font-size:15px;
  background:rgba(0,0,0,0.25);
  padding:22px;
  border-radius:14px;
}
</style>
</head>

<body>
<div class="page">

<h1>AI Generated Study Plan</h1>

<div class="grid">
  <div class="card">
    <h2>📘 Study Strategy</h2>
    <div class="plan">
""" + aiText + """
    </div>
  </div>
</div>

</div>
</body>
</html>
""";

                exchange.sendResponseHeaders(200, html.getBytes().length);
                exchange.getResponseBody().write(html.getBytes());

            } catch (Exception e) {
                String err = "AI Error: " + e.getMessage();
                exchange.sendResponseHeaders(500, err.length());
                exchange.getResponseBody().write(err.getBytes());
            }

            exchange.close();
        });

        server.start();
        System.out.println("✅ Server running at http://localhost:8080");
    }
}
