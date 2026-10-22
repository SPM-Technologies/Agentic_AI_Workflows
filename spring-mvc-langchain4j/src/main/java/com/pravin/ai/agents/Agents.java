package com.pravin.ai.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * All agents used by the workflows, in one place.
 * Each agent = an interface with ONE job. LangChain4j writes the implementation.
 *  - {{name}} in the prompt is filled from the parameter marked @V("name")
 *  - outputKey = where the agent's answer is stored in the shared workflow state
 *    (the "AgenticScope"), so the next agent can read it by that name.
 */
public final class Agents {

    private Agents() { }

    // ---------- 6.3 Sequence + 6.4 Loop: write -> review -> score -> fix ----------

    public interface CodeWriter {
        @SystemMessage("You are a senior Java developer. Write simple, correct Java 21 code with short comments. Return only the code.")
        @UserMessage("Write Java code for this task: {{task}}")
        @Agent(description = "Writes Java code for a task", outputKey = "code")
        String write(@V("task") String task);
    }

    public interface CodeReviewer {
        @SystemMessage("You are a strict Java code reviewer. Be brief: list at most 5 concrete problems or suggestions.")
        @UserMessage("Review this Java code:\n\n{{code}}")
        @Agent(description = "Reviews Java code", outputKey = "review")
        String review(@V("code") String code);
    }

    public interface CodeScorer {
        @UserMessage("""
                Based on this code review, rate the code quality from 0.0 (very poor) to 1.0 (excellent).
                Reply with only the number.

                Review:
                {{review}}""")
        @Agent(description = "Scores code quality from 0.0 to 1.0", outputKey = "score")
        double score(@V("review") String review);
    }

    public interface CodeFixer {
        @SystemMessage("You are a senior Java developer. Return only the improved code.")
        @UserMessage("""
                Improve this Java code using the review below.

                Task: {{task}}

                Code:
                {{code}}

                Review:
                {{review}}""")
        @Agent(description = "Improves code using review feedback", outputKey = "code")
        String fix(@V("task") String task, @V("code") String code, @V("review") String review);
    }

    // ---------- 6.5a Parallel: three specialist reviewers at the same time ----------

    public interface SecurityReviewer {
        @UserMessage("In at most 4 bullet points, list SECURITY issues in this Java code (or say none):\n\n{{code}}")
        @Agent(description = "Security review", outputKey = "security")
        String review(@V("code") String code);
    }

    public interface PerformanceReviewer {
        @UserMessage("In at most 4 bullet points, list PERFORMANCE issues in this Java code (or say none):\n\n{{code}}")
        @Agent(description = "Performance review", outputKey = "performance")
        String review(@V("code") String code);
    }

    public interface ReadabilityReviewer {
        @UserMessage("In at most 4 bullet points, list READABILITY issues in this Java code (or say none):\n\n{{code}}")
        @Agent(description = "Readability review", outputKey = "readability")
        String review(@V("code") String code);
    }

    // ---------- 6.5b Routing: classify the question, then send it to the right expert ----------

    public enum Category { CODE, ORDER, GENERAL }

    public interface Router {
        @UserMessage("""
                Classify this request into exactly one category:
                CODE    = Java or programming questions
                ORDER   = questions about a customer order or its status
                GENERAL = anything else

                Request: {{question}}""")
        @Agent(description = "Classifies a request", outputKey = "category")
        Category classify(@V("question") String question);
    }

    public interface CodeExpert {
        @SystemMessage("You are a Java expert. Answer with a short explanation and a small code example.")
        @UserMessage("{{question}}")
        @Agent(description = "Answers Java questions", outputKey = "answer")
        String answer(@V("question") String question);
    }

    public interface OrderAgent {
        @SystemMessage("You are a customer support agent for an online shop. Use the tools to look up orders. Be polite and brief.")
        @UserMessage("{{question}}")
        @Agent(description = "Answers order questions using tools", outputKey = "answer")
        String answer(@V("question") String question);
    }

    public interface GeneralAgent {
        @SystemMessage("You are a friendly assistant. Answer briefly.")
        @UserMessage("{{question}}")
        @Agent(description = "Answers general questions", outputKey = "answer")
        String answer(@V("question") String question);
    }
}
