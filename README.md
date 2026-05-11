# use-llm — Advanced Spring Boot AI Project

A sophisticated, production-ready multi-module Java Spring Boot project that integrates with any **OpenAI-compatible local LLM** (Ollama, LM Studio, LocalAI, etc.) to provide:

- 🔍 **Model Search** — list and filter available models
- ✏️ **Autocomplete** — text completion with streaming support
- 💬 **Chat Completion** — multi-turn chat with intelligent memory management

---

## Architecture

```
use-llm/                    ← Parent Maven POM
├── use-llm-core/           ← Domain models, port interfaces, exceptions
├── use-llm-memory/         ← Conversation memory (H2 JPA + in-memory cache)
├── use-llm-client/         ← OpenAI-compatible WebClient adapter
└── use-llm-api/            ← Spring Boot REST API (entry point)
```

---

## Prerequisites

- **Java 17+**
- **Maven 3.8+**
- A running OpenAI-compatible LLM server:
  - [Ollama](https://ollama.ai) (default: `http://localhost:11434/v1`)
  - [LM Studio](https://lmstudio.ai) (default: `http://localhost:1234/v1`)
  - [LocalAI](https://localai.io) or any compatible server

---

## Quick Start

### 1. Start your local LLM

```bash
# Ollama example
ollama serve
ollama pull llama3
```

### 2. Build the project

```bash
mvn clean install -DskipTests
```

### 3. Run the API

```bash
cd use-llm-api
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

---

## API Reference

### Model Search

#### List all models
```http
GET /api/v1/models
```

#### Search models
```http
GET /api/v1/models?query=llama&limit=10
```

#### Advanced search (POST)
```http
POST /api/v1/models/search
Content-Type: application/json

{
  "query": "llama",
  "ownedBy": "meta",
  "limit": 5
}
```

#### Get model by ID
```http
GET /api/v1/models/llama3
```

---

### Autocomplete

#### Text completion
```http
POST /api/v1/completions
Content-Type: application/json

{
  "model": "llama3",
  "prompt": "The capital of France is",
  "maxTokens": 50,
  "temperature": 0.3
}
```

#### Streaming completion (SSE)
```http
POST /api/v1/completions/stream
Content-Type: application/json

{
  "model": "llama3",
  "prompt": "Once upon a time",
  "maxTokens": 200
}
```

---

### Chat Completion with Memory

#### Chat (stateful, with memory)
```http
POST /api/v1/chat/completions
Content-Type: application/json

{
  "conversationId": "my-session-123",
  "model": "llama3",
  "message": "What is the speed of light?",
  "temperature": 0.7,
  "maxTokens": 1024
}
```
Omit `conversationId` to start a new conversation (auto-generated).

#### Streaming chat
```http
POST /api/v1/chat/completions/stream
Content-Type: application/json

{
  "conversationId": "my-session-123",
  "model": "llama3",
  "message": "Tell me more about that."
}
```

#### Get conversation history
```http
GET /api/v1/chat/conversations/{conversationId}/history
```

#### Clear conversation messages
```http
DELETE /api/v1/chat/conversations/{conversationId}/messages
```

#### Delete conversation
```http
DELETE /api/v1/chat/conversations/{conversationId}
```

---

### Release Notes

#### Generate release notes from a git branch
```http
POST /api/v1/release-notes
Content-Type: application/json

{
  "repoPath": "/absolute/path/to/git/repo",
  "branch": "feature/release-notes",
  "baseRef": "main",
  "model": "mistralai/devstral-small-2-2512",
  "maxCommits": 20,
  "maxDiffCharacters": 6000
}
```

The API reads commits from the requested git branch, analyzes each commit message and diff individually, and then uses the configured LLM to turn those commit analyses into polished markdown release notes. If `baseRef` is omitted, the API will try to compare the branch against the repository default branch (`origin/HEAD`, `main`, or `master`) and otherwise fall back to the latest commits on the branch.

For safety, the API only reads repositories under the configured `llm.release-notes.allowed-repo-roots` paths.

---

## Memory Management

The system uses three configurable memory strategies:

| Strategy | Description |
|----------|-------------|
| `TOKEN_AWARE` (default) | Includes as many recent messages as fit within the token budget |
| `SLIDING_WINDOW` | Keeps the N most recent messages |
| `ALL` | Includes all messages (not recommended for long conversations) |

Configuration in `application.yml`:
```yaml
llm:
  memory:
    strategy: TOKEN_AWARE     # SLIDING_WINDOW | TOKEN_AWARE | ALL
    max-tokens: 4096          # Max tokens for context
    max-messages: 20          # Max messages for sliding window
    system-prompt: "You are a helpful AI assistant."
```

Memory is persisted to an embedded H2 database and cached in-memory for fast access.

---

## Configuration

Edit `use-llm-api/src/main/resources/application.yml`:

```yaml
llm:
  client:
    base-url: http://localhost:11434/v1   # LLM server URL
    api-key: ollama                        # API key
    default-model: llama3                 # Default model
    timeout-seconds: 120
    max-retries: 3
  memory:
    strategy: TOKEN_AWARE
    max-tokens: 4096
    system-prompt: "You are a helpful AI assistant."
```

### For LM Studio
```yaml
llm:
  client:
    base-url: http://localhost:1234/v1
    api-key: lm-studio
    default-model: your-model-name
```

---

## Actuator / Health

```http
GET /actuator/health
GET /actuator/metrics
```

## H2 Console (Development)

```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:llmdb
Username: sa
Password: (empty)
```

---

## Running Tests

```bash
mvn test
```

---

## Tech Stack

- **Java 17**
- **Spring Boot 3.2**
- **Spring WebFlux** (reactive, non-blocking HTTP client + streaming)
- **Spring Web MVC** (REST controllers)
- **Spring Data JPA + H2** (persistent conversation memory)
- **Project Reactor** (Mono/Flux for async operations)
- **Jackson** (JSON serialization)
