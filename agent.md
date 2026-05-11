# agent.md — use-llm Project Guide

## Project Overview

`use-llm` is a multi-module Spring Boot 3 AI application that integrates with any **OpenAI-compatible local LLM** (Ollama, LM Studio, LocalAI, etc.) to expose three capabilities via REST API:

- **Model Search** — list and filter available models
- **Autocomplete** — text completion with optional streaming (SSE)
- **Chat Completion** — stateful, memory-managed multi-turn chat with streaming

---

## Module Structure

```
use-llm/                          ← Parent Maven POM (com.usellm:use-llm:1.0.0-SNAPSHOT)
├── use-llm-core/                 ← Domain models and port interfaces (no Spring dependency)
├── use-llm-memory/               ← Conversation memory: JPA/H2 persistence + in-memory cache
├── use-llm-client/               ← OpenAI-compatible WebClient adapter (reactive, non-blocking)
└── use-llm-api/                  ← Spring Boot application entry point + REST controllers
```

### Module Dependencies

```
use-llm-api → use-llm-client → use-llm-core
use-llm-api → use-llm-memory → use-llm-core
```

---

## Key Design Decisions

### No Lombok
All classes use plain Java: manual getters/setters, explicit constructors, and nested static `Builder` classes. Do not introduce Lombok.

### Port/Adapter Architecture (Hexagonal)
- `com.usellm.core.port.LLMPort` — interface for LLM operations; implemented in `use-llm-client`
- `com.usellm.core.port.MemoryPort` — interface for conversation memory; implemented in `use-llm-memory`
- Services in `use-llm-api` depend only on the port interfaces, not concrete implementations

### Memory Strategies
Configured via `llm.memory.strategy` in `application.yml`:
| Strategy | Description |
|---|---|
| `TOKEN_AWARE` (default) | Fits as many recent messages as possible within `llm.memory.max-tokens` |
| `SLIDING_WINDOW` | Keeps the `llm.memory.max-messages` most recent messages |
| `ALL` | Returns all messages (use only for short conversations) |

The system prompt is always preserved regardless of strategy.

### Streaming
Both chat and autocomplete expose streaming endpoints via Spring WebFlux `Flux<T>` returning `text/event-stream`. The complete streamed response is persisted to memory after the stream ends.

---

## Build & Run

### Prerequisites
- Java 17+
- Maven 3.8+
- A local LLM server (Ollama, LM Studio, etc.)

### Build
```bash
mvn clean install -DskipTests
```

### Run
```bash
cd use-llm-api
mvn spring-boot:run
```

Default port: **8080**. Default LLM backend: `http://localhost:11434/v1` (Ollama).

### Run Tests
```bash
mvn test
```

---

## Configuration (`use-llm-api/src/main/resources/application.yml`)

```yaml
llm:
  client:
    base-url: http://localhost:11434/v1   # Change for LM Studio: http://localhost:1234/v1
    api-key: ollama
    default-model: llama3
    timeout-seconds: 120
  memory:
    strategy: TOKEN_AWARE                 # or SLIDING_WINDOW, ALL
    max-tokens: 4096
    max-messages: 20
    system-prompt: "You are a helpful AI assistant."
```

---

## REST API Quick Reference

### Models
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/models` | List models (optional `?query=&ownedBy=&limit=`) |
| `POST` | `/api/v1/models/search` | Search models (JSON body) |
| `GET` | `/api/v1/models/{modelId}` | Get model by ID |

### Autocomplete
| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/completions` | Text completion |
| `POST` | `/api/v1/completions/stream` | Streaming text completion (SSE) |

### Chat (Stateful with Memory)
| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/chat/completions` | Chat (provide `conversationId` for continuity) |
| `POST` | `/api/v1/chat/completions/stream` | Streaming chat (SSE) |
| `GET` | `/api/v1/chat/conversations/{id}/history` | Get conversation history |
| `DELETE` | `/api/v1/chat/conversations/{id}/messages` | Clear messages |
| `DELETE` | `/api/v1/chat/conversations/{id}` | Delete conversation |

---

## Important Implementation Notes

1. **JPA Scanning**: `UseLlmApplication` uses `@EnableJpaRepositories` and `@EntityScan` pointing to `com.usellm.memory.*` because the default Spring Boot scan only covers `com.usellm.api.*`.

2. **Reactive + MVC coexistence**: The app uses both Spring MVC (`spring-boot-starter-web`) and WebFlux (`spring-boot-starter-webflux`) so that reactive `Mono`/`Flux` return types work in MVC controllers.

3. **Cache invalidation**: `ConversationMemoryService` maintains a `ConcurrentHashMap` cache keyed by `conversationId`. The cache is cleared on `clearConversation` and `deleteConversation`.

4. **ObjectMapper in adapter**: `OpenAICompatibleAdapter` uses a shared `ObjectMapper` instance to parse SSE stream chunks.

5. **H2 Console** (dev): Available at `http://localhost:8080/h2-console` with JDBC URL `jdbc:h2:mem:llmdb`.
