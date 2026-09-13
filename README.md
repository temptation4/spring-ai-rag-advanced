# spring-ai-playground

A working tour of **Spring AI 2.0** on **Spring Boot 4** — every major feature wired up
as a small, runnable endpoint against **local models via Ollama** (no API keys, no cost).

It started as a chat demo; it now covers multi-model routing, prompt templates,
structured output, custom + built-in advisors, reactive streaming, chat memory
(in-memory **and** JDBC/MariaDB), embeddings, and **modular RAG** over a MariaDB
vector store.

> **Companion write-up — [Spring AI Field Guide](https://temptation4.github.io/spring-ai-playground/):**
> an illustrated walk-through of the same concepts (ChatClient, advisors, RAG, the
> four-phase modular RAG pipeline), with animated diagrams.

---

## Stack

| | |
|---|---|
| Spring AI | 2.0.0 |
| Spring Boot | 4.0.1 |
| Java | 21 |
| Models | Ollama — `llama3.2`, `qwen2.5:1.5b`, `all-minilm` (embeddings) |
| Vector store | MariaDB 11.7+ native `VECTOR` type (`mysql` profile) |
| Chat memory | in-memory (default) or JDBC → MariaDB (`mysql` profile) |
| Port | `8088` |

---

## Prerequisites

```bash
# 1. Ollama with the three models
ollama pull llama3.2
ollama pull qwen2.5:1.5b
ollama pull all-minilm

# 2. (only for the 'mysql' profile) MariaDB 11.7+ for the vector store + JDBC chat memory
docker run -d --name mariadb1 -p 3308:3306 \
  -e MARIADB_ROOT_PASSWORD=password \
  -e MARIADB_DATABASE=spring_ai_yt \
  mariadb:11.8
```

## Run

**Default** — in-memory chat memory; the vector-store endpoints return `503`:

```bash
mvn spring-boot:run
```

**`mysql` profile** — chat history + vector store persisted to MariaDB:

```bash
SPRING_PROFILES_ACTIVE=mysql MYSQL_PASSWORD=password mvn spring-boot:run
```

The datasource defaults to `jdbc:mariadb://localhost:3308/spring_ai_yt` and creates the
schema on first connect. Override with `MYSQL_URL` / `MYSQL_USER` / `MYSQL_PASSWORD`.
It **must** be a `jdbc:mariadb://` URL — the MySQL driver can't encode MariaDB's native
`VECTOR` type.

```bash
mvn test        # 19 tests, no Ollama / DB needed (models are mocked)
```

---

## What's inside

### Multi-model routing — `config/ModelConfig.java`
Three `ChatClient` beans over one auto-configured `ChatModel`, each pinning a different
Ollama model via `OllamaChatOptions.model(...)`:

| Bean (`@Qualifier`) | Model | Temp | For |
|---|---|---|---|
| `precise` | `llama3.2` | 0.2 | factual answers |
| `creative` | `qwen2.5:1.5b` | 0.9 | expressive answers |
| `rag` | `llama3.2` | 0.0 | grounded RAG answers, larger token budget |

### Advisors — `config/AdvisorConfig.java`, `advisor/TokenPrintAdvisor.java`
- **`TokenPrintAdvisor`** (custom) — implements both `CallAdvisor` and `StreamAdvisor`;
  logs the prompt, the reply and token usage around every call.
- **`SimpleLoggerAdvisor`** (built-in) — full request/response at `DEBUG`.
- **`SafeGuardAdvisor`** (built-in) — short-circuits before the model when the prompt
  contains a blocked word (`password`, `secret`, `api key`, `credit card`).
- Applied globally through a `ChatClientCustomizer`.

### Prompt templates — `service/InterviewService.java` + `src/main/resources/prompts/*.st`
`PromptTemplate` / `SystemPromptTemplate`, fluent `.param(...)`, templates loaded from
`.st` resource files, and Ollama JSON mode (`format: "json"`) for reliable structured
output.

### Structured output
`.call().entity(InterviewQuestion.class)` and
`.entity(new ParameterizedTypeReference<List<InterviewQuestion>>() {})`.

### Streaming — `controller/StreamController.java`, `service/StreamService.java`
`ChatClient.stream()` → `Flux<String>`; `spring-boot-starter-webflux` on the classpath,
still running on the servlet stack. One endpoint aggregates raw token fragments into
whole sentences with `bufferUntil(...)`.

### Chat memory — `config/ChatMemoryConfig.java`, `service/AiService.java`
`MessageChatMemoryAdvisor` + `MessageWindowChatMemory` (20 messages). Backend selected
by `app.chat-memory.type`:
- `inmemory` — `InMemoryChatMemoryRepository` (default)
- `jdbc` — `JdbcChatMemoryRepository` → `SPRING_AI_CHAT_MEMORY` table (auto-selected by
  the `mysql` profile)

The `X-User-Id` header on `GET /ai/ask` becomes the `conversation_id`, so every caller
gets an isolated history.

### Embeddings — `service/EmbeddingService.java`
`EmbeddingModel` (`all-minilm`, 384-dim). A hand-rolled cosine-similarity search over a
handful of in-memory docs — the "what a `VectorStore` does for you" teaching version.

### RAG over a MariaDB vector store — `service/VectorStoreService.java`
- `spring-ai-starter-vector-store-mariadb` → `MariaDBVectorStore`, table `vector_store`,
  384-dim, cosine distance, schema built on startup.
- `POST /ai/vector/save` embeds + stores plain strings as `Document`s.
- `GET /ai/vector/ask` answers grounded on the stored docs. The
  [Field Guide](https://temptation4.github.io/spring-ai-playground/) covers wiring the
  **modular** `RetrievalAugmentationAdvisor` pipeline
  (pre-retrieval → retrieval → post-retrieval → generation) on top of this.

### Tool calling — `tool/EmployeeTool.java`, `controller/ToolController.java`
An `@Tool`-annotated method the model invokes automatically inside one `.call()`.

---

## Endpoints

All under `http://localhost:8088`.

| Method | Path | Notes |
|---|---|---|
| `GET` | `/ai/ask?question=` | chat **with memory**; requires header `X-User-Id: <id>` |
| `GET` | `/ai/precise?question=` | `llama3.2`, temperature 0.2 |
| `GET` | `/ai/creative?question=` | `qwen2.5:1.5b`, temperature 0.9 |
| `GET` | `/ai/stream?question=` | `text/plain`, sentence-buffered stream |
| `GET` | `/ai/stream-raw?question=` | `text/plain`, raw token fragments |
| `GET` | `/ai/stream-sse?question=` | `text/event-stream` |
| `GET` | `/ai/interview?topic=` | one `InterviewQuestion` (structured output, JSON mode) |
| `GET` | `/ai/interviews?topics=` | `List<InterviewQuestion>` |
| `GET` | `/ai/explain?tech=&example=Java` | manual `Prompt` from `PromptTemplate` + `SystemPromptTemplate` |
| `GET` | `/ai/explain-file?tech=&example=Java` | same, template from `prompts/explain.st` |
| `GET` | `/ai/embed?text=` | raw 384-dim vector |
| `GET` | `/ai/similar?query=&topK=3` | cosine-ranked in-memory docs |
| `POST` | `/ai/vector/save` | body: `["text one","text two"]` — embed + store *(mysql profile)* |
| `GET` | `/ai/vector/search?query=&topK=3` | nearest stored docs *(mysql profile)* |
| `GET` | `/ai/vector/ask?question=` | RAG answer grounded on stored docs *(mysql profile)* |
| `GET` | `/ai/employee?question=` | model calls the `@Tool` method |

`postman_collection.json` has all of these ready to import.

### Quick check

```bash
# chat with memory
curl -H "X-User-Id: alice" "http://localhost:8088/ai/ask?question=my name is Alice"
curl -H "X-User-Id: alice" "http://localhost:8088/ai/ask?question=what is my name?"

# RAG (mysql profile)
curl -X POST http://localhost:8088/ai/vector/save -H 'Content-Type: application/json' \
  -d '["Tomatoes need daily light watering while establishing.","Wheat is sown in November and December in north India."]'
curl "http://localhost:8088/ai/vector/ask?question=when is wheat sown?"
```

---

## Notes

- The Java package is still `com.example.springaidemo` — it's a learning repo.
- `all-minilm` similarity scores are compressed (~0.7 for a strong match), so the RAG
  `similarityThreshold` is kept modest (0.5).
- No secrets in the repo: the MariaDB password comes from the `MYSQL_PASSWORD` env var.
