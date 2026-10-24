package com.pravin.appeal.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import com.pravin.appeal.agents.AppealAgents;
import com.pravin.appeal.agents.AppealAgents.DenialCategory;
import com.pravin.appeal.config.ClaudeModel;
import com.pravin.appeal.data.DemoData;
import com.pravin.appeal.data.DemoData.DeniedClaim;
import com.pravin.appeal.service.Appeal.Status;
import com.pravin.appeal.tools.ClaimTools;

/**
 * The biller's work queue.
 *  - prepare(): starts the AI workflow in the BACKGROUND (it can take a minute) - the web request returns at once
 *  - approve() / reject(): the human decision; nothing is "sent" without it
 *  - every step is written to the appeal's audit log, which is never wiped
 */
@Service
public class AppealService implements DisposableBean {

    private static final String AI = "AI workflow";

    private final DemoData data;
    private final ClaudeModel claudeModel;
    private final AppealWorkflow workflow;
    private final Map<String, Appeal> appeals = new ConcurrentHashMap<>();   // claim id -> appeal
    private final ExecutorService background = Executors.newFixedThreadPool(4);

    public AppealService(DemoData data, ClaudeModel claudeModel, AppealWorkflow workflow) {
        this.data = data;
        this.claudeModel = claudeModel;
        this.workflow = workflow;
    }

    public Appeal find(String claimId) {
        return appeals.get(claimId);
    }

    public Map<String, Appeal> all() {
        return appeals;
    }

    /**
     * Starts AI preparation for a claim and returns immediately.
     * synchronized: a fast double-click cannot start two (paid) AI runs for the same claim.
     */
    public synchronized Appeal prepare(String claimId, String user) {
        DeniedClaim claim = data.claim(claimId).orElseThrow();
        Appeal previous = appeals.get(claimId);
        if (previous != null && previous.getStatus() == Status.PREPARING) {
            return previous;                                   // already running
        }
        if (previous != null && previous.getStatus() == Status.APPROVED) {
            throw new IllegalStateException("Claim " + claimId + " was already approved and sent.");
        }

        Appeal appeal = new Appeal(claimId);
        if (previous != null) {
            appeal.getAudit().addAll(previous.getAudit());     // audit trail is never wiped
        }
        appeals.put(claimId, appeal);
        appeal.log(user, previous == null ? "Requested AI preparation" : "Requested AI preparation again");

        if (!claudeModel.isConfigured()) {
            fail(appeal, ClaudeModel.NO_KEY_MESSAGE);
            return appeal;
        }
        background.submit(() -> runWorkflow(appeal, claim));
        return appeal;
    }

    private void runWorkflow(Appeal appeal, DeniedClaim claim) {
        // bound to THIS claim's patient only; every tool read goes straight into the audit log
        ClaimTools tools = new ClaimTools(data, claim, call -> appeal.log(AI, "Read (tool): " + call));
        appeal.log(AI, "Started - source records loaded for the checker");
        try {
            AppealWorkflow.Outcome outcome = workflow.run(claim, tools);
            appeal.setCategory(outcome.category());
            appeal.setEvidence(outcome.evidence());
            appeal.setDraft(outcome.letter());
            appeal.setOpenIssues(outcome.openIssues());
            appeal.setSteps(outcome.steps());
            appeal.setStatus(statusFor(outcome));
            appeal.log(AI, "Finished: " + outcome.category() + " -> " + appeal.getStatus().getLabel()
                    + " (" + outcome.steps().size() + " agent steps"
                    + (outcome.openIssues() == null ? ", all statements checked" : ", checker still has doubts") + ")");
        } catch (Exception e) {
            fail(appeal, "Error calling Claude: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
        } finally {
            if (appeal.getStatus() == Status.PREPARING) {      // never leave an item stuck in "Preparing..."
                fail(appeal, "Stopped unexpectedly");
            }
        }
    }

    private static Status statusFor(AppealWorkflow.Outcome outcome) {
        String letter = outcome.letter() == null ? "" : outcome.letter().trim();
        if (outcome.category() == DenialCategory.CODING_ERROR || letter.startsWith(AppealAgents.CORRECTED_CLAIM)) {
            return Status.CORRECTION_SUGGESTED;
        }
        if (letter.startsWith(AppealAgents.NOT_RECOMMENDED)) {
            return Status.NOT_RECOMMENDED;
        }
        return Status.READY_FOR_REVIEW;
    }

    private static void fail(Appeal appeal, String message) {
        appeal.setError(message);
        appeal.setStatus(Status.FAILED);
        appeal.log(AI, "Failed: " + message);
    }

    /** Human approval. The biller may have edited the text. */
    public void approve(String claimId, String finalText, String user) {
        if (finalText == null || finalText.isBlank()) {
            throw new IllegalArgumentException("The approved text cannot be empty.");
        }
        Appeal appeal = reviewable(claimId);
        boolean edited = !finalText.strip().equals(appeal.getDraft() == null ? "" : appeal.getDraft().strip());
        appeal.setFinalText(finalText);
        appeal.setStatus(Status.APPROVED);
        appeal.log(user, "Approved" + (edited ? " with edits" : " without edits") + " - sent to insurer (simulated)");
    }

    public void reject(String claimId, String reason, String user) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required to reject.");
        }
        Appeal appeal = reviewable(claimId);
        appeal.setStatus(Status.REJECTED);
        appeal.log(user, "Rejected: " + reason.strip());
    }

    private Appeal reviewable(String claimId) {
        Appeal appeal = appeals.get(claimId);
        if (appeal == null || !appeal.getStatus().isNeedsReview()) {
            throw new IllegalStateException("Nothing to review for claim " + claimId);
        }
        return appeal;
    }

    // Spring calls this when the application stops - stop background work cleanly
    @Override
    public void destroy() {
        background.shutdownNow();
    }
}
