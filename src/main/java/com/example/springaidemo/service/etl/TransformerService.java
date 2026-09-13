package com.example.springaidemo.service.etl;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * "Transform" stage of the ETL pipeline: a whole PDF page is too big and too
 * unevenly shaped to embed well, so {@link TokenTextSplitter} cuts each
 * {@link Document} into smaller chunks sized by token count, splitting on
 * sentence/paragraph boundaries where it can.
 */
@Service
public class TransformerService {

    private final TokenTextSplitter splitter;

    public TransformerService(@Value("${app.rag.ingestion.chunk-size:500}") int chunkSize) {
        this.splitter = TokenTextSplitter.builder().withChunkSize(chunkSize).build();
    }

    /** Splits each input document into one or more embedding-sized chunks. */
    public List<Document> transform(List<Document> documents) {
        return splitter.apply(documents);
    }
}
