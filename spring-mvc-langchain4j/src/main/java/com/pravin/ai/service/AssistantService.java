package com.pravin.ai.service;

import org.springframework.stereotype.Service;

import com.pravin.ai.config.ClaudeModel;
import com.pravin.ai.tools.ShopTools;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.tool.ToolExecution;

/**
 * The "brain" of the /ask page - Claude with tools (6.1) and memory (6.2).
 */
@Service
public class AssistantService {

    private final Assistant assistant; // null when no API key is configured

    // Spring passes in the shared Claude model and the tools (constructor injection)
    public AssistantService(ClaudeModel claudeModel, ShopTools shopTools) {
        if (!claudeModel.isConfigured()) {
            this.assistant = null;
            return;
        }

        this.assistant = AiServices.builder(Assistant.class)
                .chatModel(claudeModel.get())
                .tools(shopTools)           // 6.1: Claude may call these methods
                // 6.2: one memory per conversation id, keeping the last 20 messages
                .chatMemoryProvider(conversationId -> MessageWindowChatMemory.withMaxMessages(20))
                .build();
    }

    public String answer(String conversationId, String question) {
        if (assistant == null) {
            return ClaudeModel.NO_KEY_MESSAGE;
        }
        try {
            Result<String> result = assistant.chat(conversationId, question);
            return result.content() + toolsUsed(result);
        } catch (RuntimeException e) {
            return "Error calling Claude: " + e.getMessage();
        }
    }

    // "New chat" - forget everything said in this conversation
    public void clear(String conversationId) {
        if (assistant != null) {
            assistant.evictChatMemory(conversationId);
        }
    }

    // Shows which tools Claude called - helpful to see the agent "thinking with tools"
    private String toolsUsed(Result<String> result) {
        if (result.toolExecutions().isEmpty()) {
            return "\n\n[Tools used: none]";
        }
        StringBuilder sb = new StringBuilder("\n\n[Tools used]");
        for (ToolExecution execution : result.toolExecutions()) {
            sb.append("\n- ").append(execution.request().name())
              .append(execution.request().arguments().replaceAll("\\s*\\n\\s*", " ")) // JSON on one line
              .append(" -> ").append(execution.result());
        }
        return sb.toString();
    }
}
