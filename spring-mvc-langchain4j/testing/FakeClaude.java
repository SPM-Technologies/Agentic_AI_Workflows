import com.sun.net.httpserver.HttpServer;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

// Fake Anthropic Messages API: picks a believable reply from keywords in the request.
// Every request body is appended to requests.log for inspection.
public class FakeClaude {
    static final AtomicInteger scoreCalls = new AtomicInteger();
    static final AtomicInteger ids = new AtomicInteger();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 18090), 0);
        server.setExecutor(Executors.newFixedThreadPool(8)); // so parallel calls really run in parallel
        server.createContext("/", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            synchronized (FakeClaude.class) {
                try (PrintWriter w = new PrintWriter(new FileWriter("requests.log", true))) {
                    w.println("===== " + System.currentTimeMillis() + " beta=" + ex.getRequestHeaders().getFirst("anthropic-beta"));
                    w.println(body);
                }
            }
            String resp = reply(body);
            byte[] b = resp.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        server.start();
        System.out.println("FakeClaude listening on 18090");
    }

    static String reply(String body) {
        String lastUser = body.substring(body.lastIndexOf("\"role\" : \"user\""));
        if (body.contains("SECURITY issues") || body.contains("PERFORMANCE issues") || body.contains("READABILITY issues")) {
            sleep(1500); // each reviewer "works" 1.5 s - parallel run should take ~1.5 s, not 4.5 s
            String kind = body.contains("SECURITY issues") ? "security" : body.contains("PERFORMANCE issues") ? "performance" : "readability";
            return text("- fake " + kind + " finding");
        }
        if (body.contains("Classify this request")) {
            String q = lastUser.substring(lastUser.indexOf("Request: "), lastUser.indexOf("\\nYou must")).toLowerCase();
            return text(q.contains("order") ? "ORDER" : (q.contains("java") || q.contains("stream")) ? "CODE" : "GENERAL");
        }
        if (body.contains("rate the code quality")) {
            return text(scoreCalls.incrementAndGet() % 2 == 1 ? "0.5" : "0.9"); // 1st round low, 2nd round good
        }
        if (body.contains("Improve this Java code")) return text("// improved version\nboolean isPalindrome(String s) { /* v2 */ }");
        if (body.contains("Review this Java code")) return text("1. Handle null input\n2. Ignore case");
        if (body.contains("Write Java code for this task")) return text("boolean isPalindrome(String s) { /* v1 */ }");
        if (body.contains("customer support agent") || body.contains("small online shop")) {
            // assistant / order agent: use the tool once, then answer
            if (lastUser.contains("tool_result")) return text("Order 101 is shipped (from tool).");
            if (lastUser.toLowerCase().contains("order 101")) return toolUse("getOrderStatus", "{\"orderId\":\"101\"}");
            int userTurns = body.split("\"role\" : \"user\"", -1).length - 1;
            return text("Fake answer. I can see " + userTurns + " user message(s) in this conversation.");
        }
        if (body.contains("Java expert")) return text("Fake Java expert answer.");
        return text("Fake general answer.");
    }

    static String text(String t) {
        String esc = t.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return envelope("{\"type\":\"text\",\"text\":\"" + esc + "\"}", "end_turn");
    }

    static String toolUse(String name, String inputJson) {
        return envelope("{\"type\":\"tool_use\",\"id\":\"toolu_" + ids.incrementAndGet() + "\",\"name\":\"" + name + "\",\"input\":" + inputJson + "}", "tool_use");
    }

    static String envelope(String block, String stop) {
        return "{\"id\":\"msg_" + ids.incrementAndGet() + "\",\"type\":\"message\",\"role\":\"assistant\",\"model\":\"claude-opus-5-5\","
             + "\"content\":[{\"type\":\"thinking\",\"thinking\":\"\",\"signature\":\"SIG" + ids.get() + "\"}," + block + "],"
             + "\"stop_reason\":\"" + stop + "\",\"stop_sequence\":null,\"stop_details\":null,"
             + "\"usage\":{\"input_tokens\":10,\"output_tokens\":10}}";
    }

    static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
}
