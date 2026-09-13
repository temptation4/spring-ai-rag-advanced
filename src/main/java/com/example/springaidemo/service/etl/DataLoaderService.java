package com.example.springaidemo.service.etl;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * "Extract" stage of the ETL pipeline: reads every PDF matched by
 * {@code app.rag.ingestion.location} (default {@code classpath:docs/*.pdf}) and
 * turns each page into a {@link Document} via {@link PagePdfDocumentReader}.
 *
 * <p>{@link ResourcePatternResolver} is what resolves the {@code *} wildcard -
 * Spring's {@code ApplicationContext} already implements it, so it's injectable
 * with no extra configuration.
 */
@Service
public class DataLoaderService {

    private final ResourcePatternResolver resourcePatternResolver;
    private final String location;

    public DataLoaderService(
            ResourcePatternResolver resourcePatternResolver,
            @Value("${app.rag.ingestion.location:classpath:docs/*.pdf}") String location) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.location = location;
    }

    /** One {@link Document} per PDF page, across every PDF matching the configured location. */
    public List<Document> load() {
        Resource[] resources;
        try {
            resources = resourcePatternResolver.getResources(location);
        }
        catch (IOException e) {
            throw new UncheckedIOException("Failed to resolve PDF resources at " + location, e);
        }

        List<Document> documents = new ArrayList<>();
        for (Resource resource : resources) {
            PagePdfDocumentReader reader = new PagePdfDocumentReader(resource, PdfDocumentReaderConfig.defaultConfig());
            documents.addAll(reader.get());
        }
        return documents;
    }
}
