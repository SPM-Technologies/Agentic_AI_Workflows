package com.pravin.appeal.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.pravin.appeal.agents.AppealAgents;
import com.pravin.appeal.agents.AppealAgents.DenialCategory;
import com.pravin.appeal.config.ClaudeModel;
import com.pravin.appeal.data.DemoData.DeniedClaim;
import com.pravin.appeal.tools.ClaimTools;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgentInvocation;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * The denied-claim appeal workflow:
 *
 *   1. classify the denial (routing)
 *   2a. CODING_ERROR      -> suggest a corrected claim
 *   2b. everything else   -> collect proof (tools) -> write letter (or "NOT RECOMMENDED")
 *   3. EVERY draft        -> check against the source records / revise (loop, max 3 rounds)
 *                         -> one final check if the last round ended on an unchecked revision
 *
 * It is built per claim, because the tools are bound to that one claim's patient.
 */
@Component
public class AppealWorkflow {

    /** One agent call, for the "steps" list on the review screen. */
    public record Step(String agent, String output) { }

    /** Everything the workflow produced. */
    public record Outcome(DenialCategory category, String evidence, String letter, String openIssues, List<Step> steps) { }

    private static final int MAX_CHECK_ROUNDS = 3;

    private final ClaudeModel claudeModel;

    public AppealWorkflow(ClaudeModel claudeModel) {
        this.claudeModel = claudeModel;
    }

    public Outcome run(DeniedClaim claim, ClaimTools tools) {
        ChatModel model = claudeModel.get();

        // Appeal path: proof -> letter -> check
        UntypedAgent appealPath = AgenticServices.sequenceBuilder()
                .subAgents(AgenticServices.agentBuilder(AppealAgents.EvidenceCollector.class)
                                   .chatModel(model).tools(tools).build(),
                           agent(AppealAgents.AppealWriter.class, model),
                           checkLoop(model),
                           finalCheck(model))
                .build();

        // Coding path: corrected-claim suggestion -> check
        UntypedAgent codingPath = AgenticServices.sequenceBuilder()
                .subAgents(AgenticServices.agentBuilder(AppealAgents.CorrectedClaimAdvisor.class)
                                   .chatModel(model).tools(tools).build(),
                           checkLoop(model),
                           finalCheck(model))
                .build();

        // Routing: only one path runs
        UntypedAgent paths = AgenticServices.conditionalBuilder()
                .subAgents(scope -> category(scope) == DenialCategory.CODING_ERROR, codingPath)
                .subAgents(scope -> category(scope) != DenialCategory.CODING_ERROR, appealPath)
                .build();

        UntypedAgent workflow = AgenticServices.sequenceBuilder()
                .subAgents(agent(AppealAgents.DenialClassifier.class, model), paths)
                .outputKey("letter")
                .build();

        ResultWithAgenticScope<String> result = workflow.invokeWithAgenticScope(Map.of(
                "denialCode", claim.denialCode(),
                "denialRemark", claim.denialRemark(),
                "claimSummary", summary(claim),
                "sourceRecords", tools.sourceRecords()));   // ground truth for the checker, read by code

        AgenticScope scope = result.agenticScope();
        String issues = String.valueOf(scope.readState("issues", "NONE"));
        return new Outcome(
                category(scope),
                String.valueOf(scope.readState("evidence", "")),
                String.valueOf(result.result()),
                isNone(issues) ? null : issues,          // still open after the final check -> shown as a warning
                steps(scope));
    }

    /** Check -> revise, until the checker finds nothing (checked after each step) or MAX_CHECK_ROUNDS is reached. */
    private static UntypedAgent checkLoop(ChatModel model) {
        return AgenticServices.loopBuilder()
                .subAgents(agent(AppealAgents.AppealChecker.class, model),
                           agent(AppealAgents.AppealReviser.class, model))
                .maxIterations(MAX_CHECK_ROUNDS)
                .exitCondition(scope -> noIssues(scope))
                .build();
    }

    /** If the loop ended on a revision (rounds used up), check that last revision once more. */
    private static UntypedAgent finalCheck(ChatModel model) {
        return AgenticServices.conditionalBuilder()
                .subAgents(scope -> !noIssues(scope), agent(AppealAgents.AppealChecker.class, model))
                .build();
    }

    private static String summary(DeniedClaim c) {
        return c.id() + " - " + c.service() + " (codes " + c.serviceCodes() + ") on " + c.serviceDate()
                + ", $" + c.amount() + ", insurer " + c.insurer() + ", denied with " + c.denialCode() + ": " + c.denialRemark();
    }

    private static DenialCategory category(AgenticScope scope) {
        return scope.readState("category", DenialCategory.OTHER);
    }

    private static boolean noIssues(AgenticScope scope) {
        return scope.hasState("issues") && isNone(String.valueOf(scope.readState("issues")));
    }

    private static boolean isNone(String issues) {
        return issues.trim().equalsIgnoreCase("NONE");
    }

    private static List<Step> steps(AgenticScope scope) {
        List<Step> steps = new ArrayList<>();
        for (AgentInvocation call : scope.agentInvocations()) {
            if (call.agentType().getEnclosingClass() == AppealAgents.class) { // skip the workflow wrappers
                steps.add(new Step(call.agentType().getSimpleName(), String.valueOf(call.output())));
            }
        }
        return steps;
    }

    private static <T> T agent(Class<T> agentType, ChatModel model) {
        return AgenticServices.agentBuilder(agentType).chatModel(model).build();
    }
}
