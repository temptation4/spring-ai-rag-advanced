# spring-ai-rag-advanced

An ETL pipeline that loads PDFs into a MariaDB vector store, and a modular RAG
service (`RetrievalAugmentationAdvisor`) that answers questions grounded on what
was loaded — Spring AI 2.0 on Spring Boot 4, local models via Ollama.

---

## Stack

| | |
|---|---|
| Spring AI | 2.0.0 |
| Spring Boot | 4.0.1 |
| Java | 21 |
| Models | Ollama — `llama3.2` (chat), `all-minilm` (embeddings) |
| Vector store | MariaDB 11.7+ native `VECTOR` type |
| Port | `8088` |

## Prerequisites

```bash
ollama pull llama3.2
ollama pull all-minilm

docker run -d --name mariadb1 -p 3308:3306 \
  -e MARIADB_ROOT_PASSWORD=password \
  -e MARIADB_DATABASE=spring_ai_yt \
  mariadb:11.8
```

## Run

```bash
SPRING_PROFILES_ACTIVE=mysql MYSQL_PASSWORD=password mvn spring-boot:run
mvn test   # 5 tests, no Ollama / DB needed (PDF reading is real, everything else mocked)
```

The datasource defaults to `jdbc:mariadb://localhost:3308/spring_ai_yt` and creates the
schema on first connect. Override with `MYSQL_URL` / `MYSQL_USER` / `MYSQL_PASSWORD`.
Must be a `jdbc:mariadb://` URL — the MySQL driver can't encode MariaDB's native
`VECTOR` type.

---

## ETL pipeline

Three classes, one per stage, orchestrated by `IngestionService`:

| Stage | Class | Does |
|---|---|---|
| **Extract** | `service/etl/DataLoaderService.java` | `PagePdfDocumentReader` reads every PDF matched by `app.rag.ingestion.location` (default `classpath:docs/*.pdf`) - one `Document` per page |
| **Transform** | `service/etl/TransformerService.java` | `TokenTextSplitter` cuts each page into smaller, embedding-sized chunks (`app.rag.ingestion.chunk-size`, default 500 tokens) |
| **Load** | `service/etl/IngestionService.java` | hands the chunks to `VectorStore.add(...)`, which embeds each one (`all-minilm`) and persists it to the `vector_store` table |

Triggered by:

```
POST /rag/ingest
```

```json
{"pagesLoaded": 1, "chunksStored": 1}
```

A sample PDF ships at `src/main/resources/docs/sample-spring-ai.pdf` so this works
out of the box - drop your own PDFs in `src/main/resources/docs/` and re-run.

---

## RAG implementation

`service/RagService.java` builds a `RetrievalAugmentationAdvisor` per request, one
Spring AI module per phase:

```java
RetrievalAugmentationAdvisor.builder()
    .queryTransformers(RewriteQueryTransformer..., TranslationQueryTransformer...)
    .queryExpander(MultiQueryExpander...)
    .documentRetriever(VectorStoreDocumentRetriever.builder()
            .vectorStore(vectorStore).topK(3).similarityThreshold(0.5).build())
    .documentJoiner(new ConcatenationDocumentJoiner())
    .queryAugmenter(ContextualQueryAugmenter.builder().allowEmptyContext(true).build())
    .order(-100)
    .build();
```

- **Query transformation** — `RewriteQueryTransformer` turns a chatty question into a
  search-friendly one; `TranslationQueryTransformer` normalizes it to English.
- **Query expansion** — `MultiQueryExpander` fans one question into several phrasings
  so recall doesn't hinge on exact wording.
- **Retrieval** — `VectorStoreDocumentRetriever` runs a similarity search per query
  (`similarityThreshold: 0.5` - keeps unrelated chunks out); `ConcatenationDocumentJoiner`
  merges the per-query results.
- **Generation** — `ContextualQueryAugmenter` folds the retrieved chunks into the
  prompt; `allowEmptyContext(true)` lets it still answer (gracefully) when nothing
  matched, instead of erroring.
- **Memory** — `MessageChatMemoryAdvisor` keeps each caller's history separate, keyed
  by the `X-User-Id` header.

Triggered by:

```
GET /rag/ask?question=...
Header: X-User-Id: <any id>
```

---

## Endpoints

| Method | Path | Notes |
|---|---|---|
| `POST` | `/rag/ingest` | runs the ETL pipeline once |
| `GET` | `/rag/ask?question=` | RAG answer with memory; requires header `X-User-Id: <id>` (missing → 400) |

`postman_collection.json` has both, in the right order.

### Quick check

```bash
curl -X POST http://localhost:8088/rag/ingest

curl -H "X-User-Id: alice" \
  "http://localhost:8088/rag/ask?question=What are the three stages of the ETL pipeline?"

curl -H "X-User-Id: alice" \
  "http://localhost:8088/rag/ask?question=Which one uses a VectorStore?"   # uses memory
```
