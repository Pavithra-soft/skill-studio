package com.bharath.skillstudio.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;

public record LlmSupport(String providerName, ChatClient chatClient, ChatMemory chatMemory) {

    public static LlmSupport disabled() {
        return new LlmSupport("none", null, null);
    }

    public boolean enabled() {
        return chatClient != null;
    }
}
