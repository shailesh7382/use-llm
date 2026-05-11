# use-llm-newman

Newman test module for the APIs exposed by `use-llm-api`.

## What this covers

- `GET /api/v1/models`
- `GET /api/v1/models/{modelId}`
- `POST /api/v1/models/search`
- `POST /api/v1/completions`
- `POST /api/v1/chat/completions`
- `GET /api/v1/chat/conversations/{conversationId}/history`
- `DELETE /api/v1/chat/conversations/{conversationId}/messages`
- `DELETE /api/v1/chat/conversations/{conversationId}`

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

