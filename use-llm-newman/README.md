# use-llm-newman

Newman test module for the APIs exposed by `use-llm-api`.

## What this covers

- `GET /api/v1/models` (with and without filters)
- `GET /api/v1/models/{modelId}`
- `POST /api/v1/models/search`
- `POST /api/v1/completions`
- `POST /api/v1/completions/stream`
- `POST /api/v1/chat/completions`
- `POST /api/v1/chat/completions/stream`
- `GET /api/v1/chat/conversations/{conversationId}/history`
- `DELETE /api/v1/chat/conversations/{conversationId}/messages`
- `DELETE /api/v1/chat/conversations/{conversationId}`

## Additional checks included

- Validation errors for missing required fields (`400` + standard error body)
- Conversation lifecycle checks (create, follow-up, history, clear, delete)
- Streaming endpoint checks (`text/event-stream` and non-empty stream payload)

## Prerequisites

- Node.js 18+
- npm
- `use-llm-api` running on `http://localhost:8080`

## Install

```bash
cd use-llm-newman
npm install
```

## Run tests

```bash
npm test
```

## Run with HTML report

```bash
npm run test:html
```

## Run via bash script

```bash
./scripts/run-newman.sh
```

To use a different Postman environment file:

```bash
./scripts/run-newman.sh ./postman/local.environment.json
```
