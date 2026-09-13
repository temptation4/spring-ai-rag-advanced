package com.example.springaidemo.service.etl;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.then;

/**
 * Pure unit test: all three collaborators are mocked, so this checks only the
 * orchestration - extract, then transform the extracted pages, then load the
 * transformed chunks - with no Ollama, PDF, or database involved.
 */
class IngestionServiceTest {

    private final DataLoaderService dataLoaderService = mock(DataLoaderService.class);
    private final TransformerService transformerService = mock(TransformerService.class);
    private final VectorStore vectorStore = mock(VectorStore.class);

    private final IngestionService ingestionService =
            new IngestionService(dataLoaderService, transformerService, vectorStore);

    @Test
    void ingest_runsExtractTransformLoadInOrderAndReportsCounts() {
        List<Document> pages = List.of(new Document("page 1"), new Document("page 2"));
        List<Document> chunks = List.of(new Document("chunk 1"), new Document("chunk 2"), new Document("chunk 3"));
        given(dataLoaderService.load()).willReturn(pages);
        given(transformerService.transform(pages)).willReturn(chunks);

        IngestionService.IngestionResult result = ingestionService.ingest();

        assertThat(result.pagesLoaded()).isEqualTo(2);
        assertThat(result.chunksStored()).isEqualTo(3);
        then(vectorStore).should().add(chunks);
    }
}
