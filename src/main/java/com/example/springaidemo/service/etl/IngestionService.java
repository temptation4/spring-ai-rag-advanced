package com.example.springaidemo.service.etl;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Runs the full Spring AI ETL pipeline once, end to end:
 *
 * <pre>
 *   Extract                Transform                 Load
 *   DataLoaderService  ->  TransformerService    ->  VectorStore (DocumentWriter)
 *   (PagePdfDocumentReader)  (TokenTextSplitter)       embeds + persists each chunk
 * </pre>
 *
 * {@link VectorStore} implements {@code DocumentWriter}, so {@code add(...)} is the
 * "Load" step: it embeds every chunk and stores it (MariaDB's {@code vector_store}
 * table when the {@code mysql} profile is active).
 */
@Service
public class IngestionService {

    private final DataLoaderService dataLoaderService;
    private final TransformerService transformerService;
    private final VectorStore vectorStore;

    public IngestionService(
            DataLoaderService dataLoaderService,
            TransformerService transformerService,
            VectorStore vectorStore) {
        this.dataLoaderService = dataLoaderService;
        this.transformerService = transformerService;
        this.vectorStore = vectorStore;
    }

    public IngestionResult ingest() {
        List<Document> pages = dataLoaderService.load();
        List<Document> chunks = transformerService.transform(pages);
        vectorStore.add(chunks);
        return new IngestionResult(pages.size(), chunks.size());
    }

    public record IngestionResult(int pagesLoaded, int chunksStored) {
    }
}
