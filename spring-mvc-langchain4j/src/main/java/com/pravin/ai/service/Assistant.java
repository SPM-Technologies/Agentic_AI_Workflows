package com.pravin.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.memory.ChatMemoryAccess;

/**
 * Just an interface - LangChain4j (AiServices) writes the implementation:
 * calling chat(...) sends the system message + earlier conversation + your text
 * to Claude and returns Claude's reply.
 * Result<String> = the reply text PLUS details such as which tools were called.
 * ChatMemoryAccess adds evictChatMemory(id), used by the "New chat" button.
 */
public interface Assistant extends ChatMemoryAccess {

    @SystemMessage("""
            You are a helpful assistant for a Java developer and a small online shop.
            Use the available tools for order status and the current date/time.
            Answer clearly and briefly.""")
    Result<String> chat(@MemoryId String conversationId, @UserMessage String userMessage);
}
