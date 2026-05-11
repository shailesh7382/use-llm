# use-llm-ui (React 19)

Advanced React 19 + Vite module for `use-llm` that provides a professional UI for **all** backend features:

- Model listing, search, and model-by-id lookup
- Completion (regular + streaming)
- Stateful chat (regular + streaming) with conversation history management
- Prompt template CRUD, render preview, and prompt-driven chat
- Release notes generation

## Run locally

```bash
cd use-llm-ui
npm install
npm run dev
```

Default UI URL: `http://localhost:5173`

## Backend API URL

The UI defaults to `http://localhost:8080` and exposes a configurable **API Base URL** field in the header.

## Build and lint

```bash
npm run lint
npm run build
```
