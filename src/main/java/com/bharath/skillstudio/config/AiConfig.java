package com.bharath.skillstudio.config;

import com.bharath.skillstudio.ai.LlmSupport;
import com.google.genai.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    LlmSupport llmSupport(
            @Value("${GEMINI_API_KEY:}") String geminiKey,
            @Value("${GEMINI_MODEL:gemini-3.8-flash}") String modelName) {
        if (geminiKey == null || geminiKey.isBlank() || "dummy".equals(geminiKey)) {
            log.info("No GEMINI_API_KEY. Tutor search uses the authored catalog.");
            return LlmSupport.disabled();
        }
        try {
            String model = modelName == null || modelName.isBlank() ? "gemini-3.8-flash" : modelName;
            Client genAiClient = Client.builder().apiKey(geminiKey).build();
            GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                    .model(model)
                    .temperature(0.2)
                    .build();
            GoogleGenAiChatModel chatModel = GoogleGenAiChatModel.builder()
                    .genAiClient(genAiClient)
                    .defaultOptions(options)
                    .build();
            ChatClient client = ChatClient.builder(chatModel)
                    .defaultAdvisors(new SimpleLoggerAdvisor())
                    .defaultSystem("You help a senior Java / Spring engineer learn from this classroom catalog.")
                    .build();
            log.info("Spring AI ChatClient enabled with Gemini model={}", model);
            return new LlmSupport("gemini", client, MessageWindowChatMemory.builder().maxMessages(16).build());
        } catch (Exception e) {
            log.warn("Could not start Gemini ChatClient: {}", e.getMessage());
            return LlmSupport.disabled();
        }
    }
}
