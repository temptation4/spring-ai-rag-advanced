package com.example.springaidemo.service.etl;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PathMatchingResourcePatternResolver} resolves {@code classpath:} patterns
 * with no Spring context needed, so this reads the real sample PDF under
 * {@code src/main/resources/docs/} end to end - no mocks, no Ollama, no database.
 */
class DataLoaderServiceTest {

    @Test
    void load_readsEveryPageOfEveryPdfUnderTheConfiguredLocation() {
        DataLoaderService loader = new DataLoaderService(
                new PathMatchingResourcePatternResolver(), "classpath:docs/*.pdf");

        List<Document> pages = loader.load();

        assertThat(pages).isNotEmpty();
        // PDFBox's layout-preserving extraction pads text with irregular runs of
        // spaces to mirror the page layout, so normalize before asserting on content.
        String normalized = pages.get(0).getText().replaceAll("\\s+", " ");
        assertThat(normalized).contains("Spring AI");
    }

    @Test
    void load_returnsEmptyListWhenNothingMatches() {
        DataLoaderService loader = new DataLoaderService(
                new PathMatchingResourcePatternResolver(), "classpath:docs/*.does-not-exist");

        assertThat(loader.load()).isEmpty();
    }
}
