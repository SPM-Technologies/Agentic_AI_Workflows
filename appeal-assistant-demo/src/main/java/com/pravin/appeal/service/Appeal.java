package com.pravin.appeal.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.pravin.appeal.agents.AppealAgents.DenialCategory;
import com.pravin.appeal.service.AppealWorkflow.Step;

/**
 * One item in the biller's work queue: the AI draft for a denied claim and what happened to it.
 * Kept in memory for the demo (a real product would store it in SQL Server).
 */
public class Appeal {

    public enum Status {
        PREPARING("Preparing...", false),
        READY_FOR_REVIEW("Ready for review", true),
        NOT_RECOMMENDED("Not recommended - review", true),
        CORRECTION_SUGGESTED("Corrected claim suggested - review", true),
        APPROVED("Approved - sent (simulated)", false),
        REJECTED("Rejected by biller", false),
        FAILED("Failed", false);

        private final String label;
        private final boolean needsReview;

        Status(String label, boolean needsReview) {
            this.label = label;
            this.needsReview = needsReview;
        }

        public String getLabel() { return label; }
        public boolean isNeedsReview() { return needsReview; }
    }

    /** One line of the audit trail: who did what, when. */
    public record AuditEvent(LocalDateTime time, String actor, String action) {
        public String when() {
            return time.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }

    private final String claimId;
    private volatile Status status = Status.PREPARING;
    private volatile DenialCategory category;
    private volatile String evidence;
    private volatile String draft;
    private volatile String finalText;
    private volatile String openIssues;
    private volatile String error;
    private volatile List<Step> steps = List.of();
    private final List<AuditEvent> audit = new CopyOnWriteArrayList<>();

    public Appeal(String claimId) {
        this.claimId = claimId;
    }

    public void log(String actor, String action) {
        audit.add(new AuditEvent(LocalDateTime.now().withNano(0), actor, action));
    }

    // getters (used by the JSP pages) and setters (used by AppealService)
    public String getClaimId() { return claimId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public DenialCategory getCategory() { return category; }
    public void setCategory(DenialCategory category) { this.category = category; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public String getDraft() { return draft; }
    public void setDraft(String draft) { this.draft = draft; }
    public String getFinalText() { return finalText; }
    public void setFinalText(String finalText) { this.finalText = finalText; }
    public String getOpenIssues() { return openIssues; }
    public void setOpenIssues(String openIssues) { this.openIssues = openIssues; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public List<Step> getSteps() { return steps; }
    public void setSteps(List<Step> steps) { this.steps = steps; }
    public List<AuditEvent> getAudit() { return audit; }
}
