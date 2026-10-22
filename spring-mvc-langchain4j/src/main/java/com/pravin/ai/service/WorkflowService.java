package com.pravin.ai.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.pravin.ai.agents.Agents;
import com.pravin.ai.agents.Agents.Category;
import com.pravin.ai.config.ClaudeModel;
import com.pravin.ai.tools.ShopTools;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgentInvocation;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * 6.3 - 6.5: agentic workflows built from the small agents in {@link Agents}.
 * Every workflow shares one "AgenticScope" (a map of named values):
 * each agent reads its inputs from it and writes its answer back under its outputKey.
 */
@Service
public class WorkflowService {

    /** One agent call, shown on the page as a step. */
    public record Step(String agent, String output) { }

    /** Final output + every step that produced it. */
    public record WorkflowResult(String output, List<Step> steps) { }

    private final UntypedAgent sequence; // 6.3
    private final UntypedAgent loop;     // 6.4
    private final UntypedAgent parallel; // 6.5a
    private final UntypedAgent routing;  // 6.5b

    public WorkflowService(ClaudeModel claudeModel, ShopTools shopTools) {
        if (!claudeModel.isConfigured()) {
            sequence = loop = parallel = routing = null;
            return;
        }
        ChatModel model = claudeModel.get();

        // 6.3 SEQUENCE: writer -> reviewer (output of one is input of the next)
        sequence = AgenticServices.sequenceBuilder()
                .subAgents(agent(Agents.CodeWriter.class, model),
                           agent(Agents.CodeReviewer.class, model))
                .output(scope -> scope.readState("code") + "\n\n----- REVIEW -----\n" + scope.readState("review"))
                .build();

        // 6.4 LOOP (evaluator-optimizer): write once, then review -> score -> fix
        // until the score is good enough (>= 0.8) or 3 rounds are done
        UntypedAgent improveLoop = AgenticServices.loopBuilder()
                .subAgents(agent(Agents.CodeReviewer.class, model),
                           agent(Agents.CodeScorer.class, model),
                           agent(Agents.CodeFixer.class, model))
                .maxIterations(3)
                .exitCondition(scope -> scope.readState("score", 0.0) >= 0.8)
                .build();
        loop = AgenticServices.sequenceBuilder()
                .subAgents(agent(Agents.CodeWriter.class, model), improveLoop)
                .output(scope -> scope.readState("code") + "\n\n(final score: " + scope.readState("score") + ")")
                .build();

        // 6.5a PARALLEL: three reviewers run at the same time, then results are combined
        parallel = AgenticServices.parallelBuilder()
                .subAgents(agent(Agents.SecurityReviewer.class, model),
                           agent(Agents.PerformanceReviewer.class, model),
                           agent(Agents.ReadabilityReviewer.class, model))
                .output(scope -> "SECURITY\n" + scope.readState("security")
                        + "\n\nPERFORMANCE\n" + scope.readState("performance")
                        + "\n\nREADABILITY\n" + scope.readState("readability"))
                .build();

        // 6.5b ROUTING: the router picks a category, then only the matching expert runs
        UntypedAgent experts = AgenticServices.conditionalBuilder()
                .subAgents(scope -> scope.readState("category", Category.GENERAL) == Category.CODE,
                           agent(Agents.CodeExpert.class, model))
                .subAgents(scope -> scope.readState("category", Category.GENERAL) == Category.ORDER,
                           AgenticServices.agentBuilder(Agents.OrderAgent.class)
                                   .chatModel(model).tools(shopTools).build())
                .subAgents(scope -> scope.readState("category", Category.GENERAL) == Category.GENERAL,
                           agent(Agents.GeneralAgent.class, model))
                .build();
        routing = AgenticServices.sequenceBuilder()
                .subAgents(agent(Agents.Router.class, model), experts)
                .outputKey("answer")
                .build();
    }

    public WorkflowResult run(String pattern, String input) {
        if (sequence == null) {
            return new WorkflowResult(ClaudeModel.NO_KEY_MESSAGE, List.of());
        }
        try {
            return switch (pattern) {
                case "SEQUENCE" -> invoke(sequence, Map.of("task", input));
                case "LOOP"     -> invoke(loop, Map.of("task", input));
                case "PARALLEL" -> invoke(parallel, Map.of("code", input));
                case "ROUTING"  -> invoke(routing, Map.of("question", input));
                default -> new WorkflowResult("Unknown pattern: " + pattern, List.of());
            };
        } catch (RuntimeException e) {
            return new WorkflowResult("Error running workflow: " + e.getMessage(), List.of());
        }
    }

    // Runs a workflow and collects each agent call from the shared scope (for the step list)
    private WorkflowResult invoke(UntypedAgent workflow, Map<String, Object> input) {
        ResultWithAgenticScope<String> result = workflow.invokeWithAgenticScope(input);
        List<Step> steps = new ArrayList<>();
        for (AgentInvocation call : result.agenticScope().agentInvocations()) {
            if (call.agentType().getEnclosingClass() == Agents.class) { // skip the workflow wrappers
                steps.add(new Step(call.agentType().getSimpleName(), String.valueOf(call.output())));
            }
        }
        return new WorkflowResult(String.valueOf(result.result()), steps);
    }

    private static <T> T agent(Class<T> agentType, ChatModel model) {
        return AgenticServices.agentBuilder(agentType).chatModel(model).build();
    }
}
