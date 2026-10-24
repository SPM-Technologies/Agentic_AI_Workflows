package com.pravin.appeal.tools;

import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.pravin.appeal.data.DemoData;
import com.pravin.appeal.data.DemoData.DeniedClaim;

import dev.langchain4j.agent.tool.Tool;

/**
 * Read-only tools the AI may call while working on ONE denied claim.
 *
 * Guardrails shown here:
 *  - a new ClaimTools object is created for each claim, bound to that claim's patient; the tools take
 *    no patient id from the AI, so the AI cannot ask for anyone else's data ("minimum necessary")
 *  - every tool call is reported immediately (onCall), so the audit log shows exactly what the AI read, live
 */
public class ClaimTools {

    private final DemoData data;
    private final DeniedClaim claim;
    private final Consumer<String> onCall;

    public ClaimTools(DemoData data, DeniedClaim claim, Consumer<String> onCall) {
        this.data = data;
        this.claim = claim;
        this.onCall = onCall;
    }

    @Tool("Returns the details of the denied claim: service, codes, dates, amount, insurer and denial reason")
    public String getClaimDetails() {
        onCall.accept("claim details");
        return claimDetails();
    }

    @Tool("Returns the patient's chart entries (visits, therapy sessions, imaging, test results), each with date and source")
    public String getChartEntries() {
        onCall.accept("chart entries");
        return chartEntries();
    }

    @Tool("Returns the insurer's published coverage criteria for the denied service")
    public String getInsurerPolicy() {
        onCall.accept("insurer policy");
        return insurerPolicy();
    }

    @Tool("Returns prior-authorization records for this patient")
    public String getAuthorizations() {
        onCall.accept("authorizations");
        return authorizations();
    }

    /**
     * NOT a tool: the full source records, read by our code (not by the AI).
     * The checker compares every draft with these - the system's own data - never with an AI-written summary,
     * so a fact the AI invented cannot "check itself".
     */
    public String sourceRecords() {
        return "CLAIM: " + claimDetails()
                + "\n\nCHART:\n" + chartEntries()
                + "\n\nINSURER POLICY: " + insurerPolicy()
                + "\n\nAUTHORIZATIONS:\n" + authorizations();
    }

    private String claimDetails() {
        return "Claim " + claim.id() + ": " + claim.service() + " (codes " + claim.serviceCodes() + ") on " + claim.serviceDate()
                + ", amount $" + claim.amount() + ", insurer " + claim.insurer()
                + ". Denied on " + claim.deniedOn() + " with " + claim.denialCode() + ": " + claim.denialRemark();
    }

    private String chartEntries() {
        return data.chart(claim.patientId()).stream()
                .map(e -> "- [" + e.date() + "] [" + e.source() + "] " + e.type() + ": " + e.text())
                .collect(Collectors.joining("\n"));
    }

    private String insurerPolicy() {
        return claim.insurer() + " policy: " + data.policy(claim);
    }

    private String authorizations() {
        var auths = data.authorizations(claim.patientId());
        if (auths.isEmpty()) {
            return "No authorization records.";
        }
        return auths.stream()
                .map(a -> "- " + a.service() + ": " + a.status() + ". " + a.details())
                .collect(Collectors.joining("\n"));
    }
}
