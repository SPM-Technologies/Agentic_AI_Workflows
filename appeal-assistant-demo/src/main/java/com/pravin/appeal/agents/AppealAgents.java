package com.pravin.appeal.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * The agents of the denied-claim appeal workflow. Each has ONE job.
 *  - {{name}} in a prompt is filled from the parameter marked @V("name")
 *  - outputKey = where the answer is stored in the shared workflow state, so later agents can read it
 */
public final class AppealAgents {

    private AppealAgents() { }

    /** Why was the claim denied? Decides which path the workflow takes. */
    public enum DenialCategory { MEDICAL_NECESSITY, MISSING_AUTHORIZATION, CODING_ERROR, OTHER }

    /** Line that marks "do not appeal" advice instead of a letter. */
    public static final String NOT_RECOMMENDED = "NOT RECOMMENDED:";

    /** Line that starts a corrected-claim suggestion. */
    public static final String CORRECTED_CLAIM = "CORRECTED CLAIM:";

    // ---------- Step 1: routing ----------

    public interface DenialClassifier {
        @UserMessage("""
                Classify why this insurance claim was denied.
                MEDICAL_NECESSITY     = the insurer says the service was not medically necessary
                MISSING_AUTHORIZATION = a required prior authorization / precertification was missing
                CODING_ERROR          = wrong, missing or inconsistent codes or modifiers
                OTHER                 = anything else

                Denial code: {{denialCode}}
                Insurer remark: {{denialRemark}}""")
        @Agent(description = "Classifies the denial reason", outputKey = "category")
        DenialCategory classify(@V("denialCode") String denialCode, @V("denialRemark") String denialRemark);
    }

    // ---------- Appeal path: collect proof -> write (then the check / revise loop below) ----------

    public interface EvidenceCollector {
        @SystemMessage("""
                You are a medical billing specialist preparing an insurance appeal.
                Use the tools to read the claim, the insurer's coverage policy, prior authorizations and the patient's chart.
                Report only facts that appear in the tool results.""")
        @UserMessage("""
                Collect the evidence for appealing this denied claim: {{claimSummary}}

                List each relevant fact on its own line as:  - [date] [source] fact
                Then add one line:  POLICY: <the insurer criteria that apply>
                Do not add anything that is not in the tool results.""")
        @Agent(description = "Collects proof from the chart and the insurer policy", outputKey = "evidence")
        String collect(@V("claimSummary") String claimSummary);
    }

    public interface AppealWriter {
        @SystemMessage("You write insurance appeal letters for a medical practice. Be factual, polite and concise.")
        @UserMessage("""
                Write an appeal letter for this denied claim: {{claimSummary}}

                Evidence - the ONLY facts you may use:
                {{evidence}}

                Rules:
                - Cite each fact with its date and source.
                - Show how the evidence meets each of the insurer's criteria.
                - If the evidence does NOT meet the insurer's criteria, do not write a letter. Reply with one line that starts
                  with "NOT RECOMMENDED:" followed by the reason and the better next step.
                - Sign the letter as "Billing Department".""")
        @Agent(description = "Drafts the appeal letter", outputKey = "letter")
        String write(@V("claimSummary") String claimSummary, @V("evidence") String evidence);
    }

    // ---------- Checking (used on EVERY path): draft vs. the system's source records ----------

    public interface AppealChecker {
        @UserMessage("""
                You check a draft before a human biller reviews it. The draft is an appeal letter,
                a "NOT RECOMMENDED:" note or a "CORRECTED CLAIM:" suggestion.

                Source records - the ground truth from the system:
                {{sourceRecords}}

                Draft:
                {{letter}}

                List every statement in the draft that is NOT supported by the source records
                (including wrong dates, amounts, codes, modifiers or conclusions), one per line.
                If every statement is supported, reply with exactly: NONE""")
        @Agent(description = "Finds statements not backed by the source records", outputKey = "issues")
        String check(@V("sourceRecords") String sourceRecords, @V("letter") String letter);
    }

    public interface AppealReviser {
        @UserMessage("""
                Revise this draft. Remove or correct every unsupported statement listed below, using only the source records.
                Keep the same kind of draft and its first-line marker ("NOT RECOMMENDED:" or "CORRECTED CLAIM:") if it has one.

                Source records:
                {{sourceRecords}}

                Unsupported statements:
                {{issues}}

                Draft:
                {{letter}}

                Return only the revised draft.""")
        @Agent(description = "Fixes unsupported statements", outputKey = "letter")
        String revise(@V("sourceRecords") String sourceRecords, @V("issues") String issues, @V("letter") String letter);
    }

    // ---------- Coding path: suggest a corrected claim instead of an appeal ----------

    public interface CorrectedClaimAdvisor {
        @SystemMessage("""
                You are a medical coding specialist. Use the tools to read the claim, the insurer policy and the chart.
                Never suggest a higher-paying code - only correct what the documentation supports.""")
        @UserMessage("""
                This claim was denied for a coding problem: {{claimSummary}}

                In at most 6 lines explain: what is wrong, whether the chart documentation supports a correction
                (quote the date and source), and the exact correction to make before resubmitting.
                Start your answer with "CORRECTED CLAIM:".""")
        @Agent(description = "Suggests a corrected claim for coding denials", outputKey = "letter")
        String advise(@V("claimSummary") String claimSummary);
    }
}
