package com.example.springaidemo.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chat memory has two interchangeable storage backends. Pick one with
 * {@code app.chat-memory.type} (default {@code inmemory}).
 *
 * <ul>
 *   <li><b>inmemory</b> - {@link InMemoryChatMemoryRepository}: a
 *       ConcurrentHashMap. Fast, zero setup, but lost on restart and not
 *       shared across app instances.</li>
 *   <li><b>jdbc</b> - {@link JdbcChatMemoryRepository}: rows in a
 *       {@code SPRING_AI_CHAT_MEMORY} table. Survives restarts and is shared
 *       by every instance pointing at the same database. Auto-configured by
 *       {@code spring-ai-starter-model-chat-memory-repository-jdbc} against
 *       {@code spring.datasource.*} (an embedded H2 by default here).</li>
 * </ul>
 *
 * The JDBC starter always auto-configures a {@code JdbcChatMemoryRepository}
 * bean when H2 + a DataSource are on the classpath, so we never expose a second
 * {@code ChatMemoryRepository} bean here. Instead the {@link ChatMemory} bean
 * is defined twice, guarded by {@code @ConditionalOnProperty}, and each variant
 * wires the repository it needs. Exactly one is ever created.
 */
@Configuration
public class ChatMemoryConfig {

    private static final int MAX_MESSAGES = 20;

    /** Default backend: an in-process ConcurrentHashMap, nothing to set up. */
    @Bean
    @ConditionalOnProperty(name = "app.chat-memory.type", havingValue = "inmemory", matchIfMissing = true)
    ChatMemory inMemoryChatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(MAX_MESSAGES)
                .build();
    }

    /**
     * JDBC backend: reuses the {@code JdbcChatMemoryRepository} auto-configured
     * by {@code spring-ai-starter-model-chat-memory-repository-jdbc}. Enable
     * with {@code app.chat-memory.type=jdbc}.
     */
    @Bean
    @ConditionalOnProperty(name = "app.chat-memory.type", havingValue = "jdbc")
    ChatMemory jdbcChatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .maxMessages(MAX_MESSAGES)
                .build();
    }
}
