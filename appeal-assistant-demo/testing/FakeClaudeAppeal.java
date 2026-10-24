import com.sun.net.httpserver.HttpServer;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

// Fake Anthropic API for the appeal demo. Picks replies by keywords; logs every request to appeal-requests.log.
public class FakeClaudeAppeal {
    static final AtomicInteger ids = new AtomicInteger();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 18095), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.createContext("/", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            synchronized (FakeClaudeAppeal.class) {
                try (PrintWriter w = new PrintWriter(new FileWriter("appeal-requests.log", true))) { w.println("=====\n" + body); }
            }
            String resp;
            try { Thread.sleep(400); resp = reply(body); } catch (Exception e) { resp = text("fake error " + e); }
            byte[] b = resp.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        server.start();
        System.out.println("FakeClaudeAppeal listening on 18095");
    }

    static String claimId(String s) {
        for (String id : new String[] {"C-1001", "C-1002", "C-1003", "C-1004"}) if (s.contains(id)) return id;
        return "?";
    }

    static String reply(String body) {
        String lastUser = body.substring(body.lastIndexOf("\"role\" : \"user\""));
        boolean hasToolResult = lastUser.contains("tool_result");
        String id = claimId(body);

        if (body.contains("Classify why this insurance claim was denied")) {
            String code = body.substring(body.indexOf("Denial code: ") + 13, body.indexOf("\\n", body.indexOf("Denial code: ")));
            return text(switch (code) { case "CO-50" -> "MEDICAL_NECESSITY"; case "CO-197" -> "MISSING_AUTHORIZATION"; case "CO-4" -> "CODING_ERROR"; default -> "OTHER"; });
        }
        if (body.contains("Collect the evidence for appealing")) {
            if (!hasToolResult) return toolUses("getClaimDetails", "getChartEntries", "getInsurerPolicy", "getAuthorizations");
            return text(switch (id) {
                case "C-1001" -> "- [2026-07-01] [Visit note, Dr. Patel] Low back pain radiating to left leg\n- [2026-07-08] [Radiology report] X-ray: mild degenerative changes\n- [2026-07-10 to 2026-08-14] [PT notes] 6 PT sessions, no improvement\n- [2026-08-26] [Visit note, Dr. Patel] 8 weeks of symptoms; positive straight-leg raise; reduced ankle reflex\nPOLICY: MRI covered after 6 weeks of radicular pain and failed conservative therapy, or with neurological deficits.";
                case "C-1002" -> "- [2026-09-30] [PT note] PT session 12\n- [2026-10-03] [PT note] PT session 13\n- [Authorization] Approved 12 visits; none requested beyond 12\nPOLICY: PT beyond 12 visits needs a new authorization BEFORE the visit; retroactive only for emergencies.";
                default -> "- [2026-09-01] [Visit note, Dr. Garcia] Shortness of breath 3 weeks; new murmur\n- [2026-09-01] [ECG report] Abnormal ECG\nPOLICY: Echo covered for new murmur, heart-failure symptoms or abnormal ECG.";
            });
        }
        if (body.contains("Write an appeal letter for this denied claim")) {
            if (id.equals("C-1002")) return text("NOT RECOMMENDED: Visit 13 was after the 12 authorized visits and no new authorization was requested; the insurer only allows retroactive authorization for emergencies. Better next step: request a new authorization before any further visits.");
            return text("Dear Appeals Department,\n\nRe: Claim " + id + "\n\nWe request reconsideration. The evidence shows the criteria were met (see attached chart notes with dates). The patient has also seen a neurosurgeon.\n\nBilling Department");
        }
        if (body.contains("You check a draft before a human biller reviews it")) {
            if (!body.contains("Source records - the ground truth")) return text("FAKE-ERROR: checker did not receive source records");
            if (lastUser.contains("neurosurgeon")) return text("The draft says the patient saw a neurosurgeon - this is not in the source records.");
            return text("NONE");
        }
        if (body.contains("Revise this draft")) {
            // C-1004: a stubborn reviser that never fixes it -> tests "rounds used up -> final check -> warning"
            String extra = id.equals("C-1004") ? " The patient has also seen a neurosurgeon." : "";
            return text("Dear Appeals Department,\n\nRe: Claim " + id + "\n\nWe request reconsideration. The evidence shows the criteria were met (see attached chart notes with dates)." + extra + "\n\nBilling Department");
        }
        if (body.contains("This claim was denied for a coding problem")) {
            if (!hasToolResult) return toolUses("getChartEntries", "getInsurerPolicy");
            return text("CORRECTED CLAIM: The office visit (99213) billed with the injection (20610) is missing modifier 25. The 2026-09-18 visit note (Dr. Singh) documents a separate new problem (headaches) that was evaluated. Correction: add modifier 25 to 99213 and resubmit.");
        }
        return text("fake: unrecognised request");
    }

    static String esc(String t) { return t.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"); }

    static String text(String t) { return envelope("{\"type\":\"text\",\"text\":\"" + esc(t) + "\"}", "end_turn"); }

    static String toolUses(String... names) {
        StringBuilder sb = new StringBuilder();
        for (String n : names) {
            if (sb.length() > 0) sb.append(",");
            sb.append("{\"type\":\"tool_use\",\"id\":\"toolu_").append(ids.incrementAndGet()).append("\",\"name\":\"").append(n).append("\",\"input\":{}}");
        }
        return envelope(sb.toString(), "tool_use");
    }

    static String envelope(String blocks, String stop) {
        return "{\"id\":\"msg_" + ids.incrementAndGet() + "\",\"type\":\"message\",\"role\":\"assistant\",\"model\":\"claude-opus-5-5\","
             + "\"content\":[{\"type\":\"thinking\",\"thinking\":\"\",\"signature\":\"SIG" + ids.get() + "\"}," + blocks + "],"
             + "\"stop_reason\":\"" + stop + "\",\"stop_sequence\":null,\"stop_details\":null,\"usage\":{\"input_tokens\":10,\"output_tokens\":10}}";
    }
}
