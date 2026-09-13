package com.example.springaidemo.service.etl;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test - the token splitter needs no Spring context, no Ollama, no
 * database.
 */
class TransformerServiceTest {

    @Test
    void transform_splitsLongTextIntoMultipleChunks() {
        TransformerService transformer = new TransformerService(20); // tiny chunk size forces a split
        String longText = "Sentence number about the ETL pipeline stages. ".repeat(50);

        List<Document> chunks = transformer.transform(List.of(new Document(longText)));

        assertThat(chunks.size()).isGreaterThan(1);
    }

    @Test
    void transform_leavesShortTextAsOneChunk() {
        TransformerService transformer = new TransformerService(500);

        List<Document> chunks = transformer.transform(List.of(new Document("short text")));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getText()).isEqualTo("short text");
    }
}
