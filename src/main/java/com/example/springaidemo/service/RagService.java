package com.example.springaidemo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    /** all-minilm scores are compressed - 0.0 (accept everything) lets unrelated
     *  chunks leak into context for off-topic questions. */
    private static final double SIMILARITY_THRESHOLD = 0.5;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ChatMemory chatMemory;

    public RagService(ChatClient chatClient, VectorStore vectorStore, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.chatMemory = chatMemory;
    }

    /** {@code userId} (e.g. the X-User-Id header) becomes the conversation id, so each caller's history is isolated. */
    public String ask(String userId, String question) {

        var advisor = RetrievalAugmentationAdvisor.builder()
                .queryTransformers(
                        RewriteQueryTransformer.builder()
                                .chatClientBuilder(chatClient.mutate())
                                .build(),
                        TranslationQueryTransformer.builder()
                                .chatClientBuilder(chatClient.mutate())
                                .targetLanguage("english")
                                .build())
                .queryExpander(MultiQueryExpander.builder()
                        .chatClientBuilder(chatClient.mutate())
                        .build())
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .topK(3)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .build())
                .documentJoiner(new ConcatenationDocumentJoiner())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)   // still answer (with "I don't know") instead of erroring
                        .build())
                // ahead of the logging advisors (default order 0), so they log the augmented prompt
                .order(-100)
                .build();

        return chatClient
                .prompt()
                .advisors(advisor, MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                .user(question)
                .call()
                .content();
    }
}
