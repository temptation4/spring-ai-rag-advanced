package com.example.springaidemo.controller;

import com.example.springaidemo.service.etl.IngestionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /rag/ingest -> runs the ETL pipeline once (reads every PDF under
 * {@code app.rag.ingestion.location}, chunks it, embeds + stores it in the
 * vector store) and reports how much it processed. Needs the {@code mysql}
 * profile, same as the rest of {@code /rag/*} - that's what provides the
 * {@link org.springframework.ai.vectorstore.VectorStore} bean.
 */
@RestController
@RequestMapping("/rag")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    public IngestionService.IngestionResult ingest() {
        return ingestionService.ingest();
    }
}
