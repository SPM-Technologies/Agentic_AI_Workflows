package com.pravin.appeal.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.stereotype.Component;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;

/**
 * One shared connection to Claude, used by every agent in the appeal workflow.
 * Same settings as the framework prototype (see its ClaudeModel for details).
 */
@Component
public class ClaudeModel {

    public static final String NO_KEY_MESSAGE =
            "ANTHROPIC_API_KEY is not set. Set it in the terminal, then run: docker compose up -d";

    private final ChatModel chatModel; // null when no API key is configured

    public ClaudeModel() {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            this.chatModel = null; // app still starts; pages show NO_KEY_MESSAGE
            return;
        }

        var builder = AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName("claude-opus-5-5")
                .maxTokens(16000)           // room for Claude's thinking + the answer
                .returnThinking(true)       // keep Claude's thinking blocks...
                .sendThinking(true)         // ...and send them back during tool calls
                .beta("server-side-fallback-2026-07-01,thinking-binding-controls-2026-08-01")
                .customParameters(Map.of(
                        // if Claude declines a request, the API retries it on a suitable model
                        "fallbacks", "default",
                        // adaptive = Claude decides how much to think;
                        // drop_block = ignore outdated thinking blocks instead of failing
                        "thinking", Map.of(
                                "type", "adaptive",
                                "block_binding", Map.of("prefix_mismatch_behavior", "drop_block"))))
                .timeout(Duration.ofMinutes(5));

        // Optional: another server address (used only for local testing; normally not set)
        String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }
        this.chatModel = builder.build();
    }

    public boolean isConfigured() {
        return chatModel != null;
    }

    public ChatModel get() {
        return chatModel;
    }
}
