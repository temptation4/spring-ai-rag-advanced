package com.example.springaidemo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The single {@link ChatClient} used for RAG: llama3.2, temperature 0.2 so it
 * sticks close to the retrieved context, with enough token budget for a
 * grounded answer plus quoted context. It wraps the auto-configured
 * {@link ChatModel}; {@code OllamaChatOptions.model(...)} pins which model on
 * the Ollama server actually answers.
 */
@Configuration
public class ModelConfig {

    @Bean
    public ChatClient preciseClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultOptions(OllamaChatOptions.builder()
                        .model("llama3.2")
                        .temperature(0.2)
                        .maxTokens(300))   // Ollama num_predict; 20 truncates mid-sentence
                .build();
    }
}
