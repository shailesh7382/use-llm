import { useEffect, useMemo, useState } from 'react'
import './App.css'
import { apiRequest, streamSse } from './api'
import type {
  ChatResponse,
  CompletionResponse,
  ConversationSummary,
  LlmModel,
  Message,
  PromptRenderResponse,
  PromptTemplate,
  ReleaseNotesResponse,
} from './types'

type TabKey = 'models' | 'completions' | 'chat' | 'prompts' | 'release-notes'

const tabs: Array<{ key: TabKey; label: string }> = [
  { key: 'models', label: 'Models' },
  { key: 'completions', label: 'Completions' },
  { key: 'chat', label: 'Chat' },
  { key: 'prompts', label: 'Prompt Studio' },
  { key: 'release-notes', label: 'Release Notes' },
]

function parseJsonRecord(value: string, fallback: Record<string, string> = {}): Record<string, string> {
  try {
    const parsed = JSON.parse(value) as unknown
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      return Object.entries(parsed).reduce<Record<string, string>>((acc, [k, v]) => {
        acc[k] = String(v)
        return acc
      }, {})
    }
    return fallback
  } catch {
    return fallback
  }
}

function parseJsonTemplate(value: string): PromptTemplate {
  try {
    return JSON.parse(value) as PromptTemplate
  } catch {
    return {}
  }
}

function pretty(value: unknown): string {
  return JSON.stringify(value, null, 2)
}

function App() {
  const [tab, setTab] = useState<TabKey>('models')
  const [baseUrl, setBaseUrl] = useState('http://localhost:8080')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const [models, setModels] = useState<LlmModel[]>([])
  const [modelQuery, setModelQuery] = useState('')
  const [ownedBy, setOwnedBy] = useState('')
  const [modelLimit, setModelLimit] = useState(20)
  const [selectedModel, setSelectedModel] = useState('')
  const [selectedModelResult, setSelectedModelResult] = useState<LlmModel | null>(null)

  const [completionPrompt, setCompletionPrompt] = useState('The capital of France is')
  const [completionTemp, setCompletionTemp] = useState(0.3)
  const [completionMaxTokens, setCompletionMaxTokens] = useState(128)
  const [completionOutput, setCompletionOutput] = useState('')

  const [conversations, setConversations] = useState<ConversationSummary[]>([])
  const [conversationId, setConversationId] = useState('')
  const [chatMessage, setChatMessage] = useState('Explain vector databases in simple terms.')
  const [chatTemp, setChatTemp] = useState(0.7)
  const [chatMaxTokens, setChatMaxTokens] = useState(512)
  const [chatResponse, setChatResponse] = useState<ChatResponse | null>(null)
  const [chatHistory, setChatHistory] = useState<Message[]>([])

  const [templates, setTemplates] = useState<PromptTemplate[]>([])
  const [templateFilter, setTemplateFilter] = useState<'all' | 'built-in' | 'custom'>('all')
  const [templateId, setTemplateId] = useState('')
  const [templatePayload, setTemplatePayload] = useState(
    pretty({
      id: 'custom-helper',
      name: 'Custom Helper',
      description: 'Helpful custom prompt',
      systemPrompt: 'You are a precise assistant.',
      userPromptTemplate: '{{task}}',
      variables: [{ name: 'task', required: true, description: 'Task to perform' }],
    }),
  )
  const [renderVariables, setRenderVariables] = useState(pretty({ task: 'Summarise clean architecture briefly' }))
  const [promptRenderOutput, setPromptRenderOutput] = useState<PromptRenderResponse | null>(null)
  const [promptChatOutput, setPromptChatOutput] = useState<ChatResponse | null>(null)

  const [repoPath, setRepoPath] = useState('/absolute/path/to/git/repo')
  const [branch, setBranch] = useState('feature/release-notes')
  const [baseRef, setBaseRef] = useState('main')
  const [releaseModel, setReleaseModel] = useState('')
  const [maxCommits, setMaxCommits] = useState(20)
  const [maxDiffCharacters, setMaxDiffCharacters] = useState(6000)
  const [releaseNotesOutput, setReleaseNotesOutput] = useState<ReleaseNotesResponse | null>(null)

  const apiBase = useMemo(() => baseUrl.replace(/\/$/, ''), [baseUrl])

  useEffect(() => {
    void refreshConversations()
    void listModelsGet()
    void listTemplates()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function run<T>(task: () => Promise<T>): Promise<T | undefined> {
    try {
      setBusy(true)
      setError('')
      return await task()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unexpected error')
      return undefined
    } finally {
      setBusy(false)
    }
  }

  async function listModelsGet() {
    await run(async () => {
      const params = new URLSearchParams()
      if (modelQuery) params.set('query', modelQuery)
      if (ownedBy) params.set('ownedBy', ownedBy)
      params.set('limit', String(modelLimit))
      const data = await apiRequest<LlmModel[]>(apiBase, `/api/v1/models?${params.toString()}`)
      setModels(data)
      if (!selectedModel && data.length > 0) {
        setSelectedModel(data[0].id)
      }
    })
  }

  async function searchModelsPost() {
    await run(async () => {
      const data = await apiRequest<LlmModel[]>(apiBase, '/api/v1/models/search', {
        method: 'POST',
        body: JSON.stringify({ query: modelQuery || undefined, ownedBy: ownedBy || undefined, limit: modelLimit }),
      })
      setModels(data)
    })
  }

  async function getModelById() {
    if (!selectedModel) return
    await run(async () => {
      const data = await apiRequest<LlmModel>(apiBase, `/api/v1/models/${encodeURIComponent(selectedModel)}`)
      setSelectedModelResult(data)
    })
  }

  async function runCompletion() {
    await run(async () => {
      const data = await apiRequest<CompletionResponse>(apiBase, '/api/v1/completions', {
        method: 'POST',
        body: JSON.stringify({
          model: selectedModel,
          prompt: completionPrompt,
          temperature: completionTemp,
          maxTokens: completionMaxTokens,
        }),
      })
      setCompletionOutput(data.choices?.[0]?.text ?? pretty(data))
    })
  }

  async function streamCompletion() {
    await run(async () => {
      setCompletionOutput('')
      await streamSse<CompletionResponse>(
        apiBase,
        '/api/v1/completions/stream',
        {
          model: selectedModel,
          prompt: completionPrompt,
          temperature: completionTemp,
          maxTokens: completionMaxTokens,
        },
        (event) => {
          const next = event.choices?.[0]?.text ?? ''
          setCompletionOutput((prev) => prev + next)
        },
      )
    })
  }

  async function refreshConversations() {
    await run(async () => {
      const data = await apiRequest<ConversationSummary[]>(apiBase, '/api/v1/chat/conversations?limit=50')
      setConversations(data)
      if (!conversationId && data.length > 0) {
        setConversationId(data[0].conversationId)
      }
    })
  }

  async function sendChat() {
    await run(async () => {
      const data = await apiRequest<ChatResponse>(apiBase, '/api/v1/chat/completions', {
        method: 'POST',
        body: JSON.stringify({
          conversationId: conversationId || undefined,
          model: selectedModel,
          message: chatMessage,
          temperature: chatTemp,
          maxTokens: chatMaxTokens,
        }),
      })
      setChatResponse(data)
      setConversationId(data.conversationId)
      await refreshConversations()
      await loadHistory(data.conversationId)
    })
  }

  async function streamChat() {
    await run(async () => {
      let localConversationId = conversationId
      setChatResponse(null)
      await streamSse<ChatResponse>(
        apiBase,
        '/api/v1/chat/completions/stream',
        {
          conversationId: conversationId || undefined,
          model: selectedModel,
          message: chatMessage,
          temperature: chatTemp,
          maxTokens: chatMaxTokens,
        },
        (event) => {
          localConversationId = event.conversationId
          setChatResponse((prev) => ({
            conversationId: event.conversationId,
            model: event.model,
            content: `${prev?.content ?? ''}${event.content ?? ''}`,
          }))
        },
      )

      if (localConversationId) {
        setConversationId(localConversationId)
        await refreshConversations()
        await loadHistory(localConversationId)
      }
    })
  }

  async function loadHistory(targetConversationId?: string) {
    const id = targetConversationId ?? conversationId
    if (!id) return
    await run(async () => {
      const data = await apiRequest<Message[]>(
        apiBase,
        `/api/v1/chat/conversations/${encodeURIComponent(id)}/history`,
      )
      setChatHistory(data)
    })
  }

  async function clearConversationMessages() {
    if (!conversationId) return
    await run(async () => {
      await apiRequest<{ status: string; conversationId: string }>(
        apiBase,
        `/api/v1/chat/conversations/${encodeURIComponent(conversationId)}/messages`,
        { method: 'DELETE' },
      )
      await loadHistory()
      await refreshConversations()
    })
  }

  async function deleteConversation() {
    if (!conversationId) return
    await run(async () => {
      await apiRequest<{ status: string; conversationId: string }>(
        apiBase,
        `/api/v1/chat/conversations/${encodeURIComponent(conversationId)}`,
        { method: 'DELETE' },
      )
      setConversationId('')
      setChatHistory([])
      await refreshConversations()
    })
  }

  async function listTemplates() {
    await run(async () => {
      const query =
        templateFilter === 'all'
          ? ''
          : `?builtIn=${templateFilter === 'built-in' ? 'true' : 'false'}`
      const data = await apiRequest<PromptTemplate[]>(apiBase, `/api/v1/prompts/templates${query}`)
      setTemplates(data)
      if (!templateId && data.length > 0 && data[0].id) {
        setTemplateId(data[0].id)
      }
    })
  }

  async function getTemplateById() {
    if (!templateId) return
    await run(async () => {
      const data = await apiRequest<PromptTemplate>(
        apiBase,
        `/api/v1/prompts/templates/${encodeURIComponent(templateId)}`,
      )
      setTemplatePayload(pretty(data))
    })
  }

  async function createTemplate() {
    await run(async () => {
      const payload = parseJsonTemplate(templatePayload)
      await apiRequest<PromptTemplate>(apiBase, '/api/v1/prompts/templates', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      await listTemplates()
    })
  }

  async function updateTemplate() {
    if (!templateId) return
    await run(async () => {
      const payload = parseJsonTemplate(templatePayload)
      await apiRequest<PromptTemplate>(apiBase, `/api/v1/prompts/templates/${encodeURIComponent(templateId)}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
      })
      await listTemplates()
    })
  }

  async function deleteTemplate() {
    if (!templateId) return
    await run(async () => {
      await apiRequest<void>(apiBase, `/api/v1/prompts/templates/${encodeURIComponent(templateId)}`, {
        method: 'DELETE',
      })
      await listTemplates()
    })
  }

  async function renderTemplate() {
    if (!templateId) return
    await run(async () => {
      const variables = parseJsonRecord(renderVariables)
      const data = await apiRequest<PromptRenderResponse>(apiBase, '/api/v1/prompts/render', {
        method: 'POST',
        body: JSON.stringify({ templateId, variables }),
      })
      setPromptRenderOutput(data)
    })
  }

  async function promptChat() {
    if (!templateId) return
    await run(async () => {
      const variables = parseJsonRecord(renderVariables)
      const data = await apiRequest<ChatResponse>(apiBase, '/api/v1/prompts/chat', {
        method: 'POST',
        body: JSON.stringify({
          templateId,
          variables,
          conversationId: conversationId || undefined,
          model: selectedModel || undefined,
          maxTokens: chatMaxTokens,
          temperature: chatTemp,
        }),
      })
      setPromptChatOutput(data)
      setConversationId(data.conversationId)
      await refreshConversations()
      await loadHistory(data.conversationId)
    })
  }

  async function generateReleaseNotes() {
    await run(async () => {
      const data = await apiRequest<ReleaseNotesResponse>(apiBase, '/api/v1/release-notes', {
        method: 'POST',
        body: JSON.stringify({
          repoPath,
          branch,
          baseRef: baseRef || undefined,
          model: releaseModel || selectedModel || undefined,
          maxCommits,
          maxDiffCharacters,
        }),
      })
      setReleaseNotesOutput(data)
    })
  }

  return (
    <div className="app-shell">
      <header className="top-bar">
        <div>
          <h1>use-llm Console</h1>
          <p>Advanced React 19 module for all REST and streaming endpoints.</p>
        </div>
        <label className="field compact">
          API Base URL
          <input value={baseUrl} onChange={(e) => setBaseUrl(e.target.value)} />
        </label>
      </header>

      <nav className="tabs" role="tablist" aria-label="Feature tabs">
        {tabs.map((entry) => (
          <button
            key={entry.key}
            type="button"
            className={tab === entry.key ? 'active' : ''}
            onClick={() => setTab(entry.key)}
          >
            {entry.label}
          </button>
        ))}
      </nav>

      <section className="status-bar">
        <span>Selected model: {selectedModel || 'None'}</span>
        {busy && <span className="busy">Processing...</span>}
      </section>

      {error && <div className="error-banner">{error}</div>}

      {tab === 'models' && (
        <section className="grid two">
          <article className="card">
            <h2>Model Explorer</h2>
            <div className="row">
              <label className="field">
                Query
                <input value={modelQuery} onChange={(e) => setModelQuery(e.target.value)} />
              </label>
              <label className="field">
                Owned By
                <input value={ownedBy} onChange={(e) => setOwnedBy(e.target.value)} />
              </label>
              <label className="field">
                Limit
                <input
                  type="number"
                  min={1}
                  value={modelLimit}
                  onChange={(e) => setModelLimit(Number(e.target.value))}
                />
              </label>
            </div>
            <div className="actions">
              <button type="button" onClick={listModelsGet}>GET /api/v1/models</button>
              <button type="button" onClick={searchModelsPost}>POST /api/v1/models/search</button>
            </div>
            <ul className="list">
              {models.map((model) => (
                <li key={model.id}>
                  <button
                    type="button"
                    className="link-button"
                    onClick={() => setSelectedModel(model.id)}
                  >
                    {model.id}
                  </button>
                  <span>{model.owned_by ?? 'unknown owner'}</span>
                </li>
              ))}
            </ul>
          </article>

          <article className="card">
            <h2>Get Model by ID</h2>
            <div className="row">
              <label className="field grow">
                Model ID
                <input value={selectedModel} onChange={(e) => setSelectedModel(e.target.value)} />
              </label>
              <button type="button" onClick={getModelById}>GET /api/v1/models/{'{modelId}'}</button>
            </div>
            <pre>{selectedModelResult ? pretty(selectedModelResult) : 'Select a model and fetch details.'}</pre>
          </article>
        </section>
      )}

      {tab === 'completions' && (
        <section className="grid two">
          <article className="card">
            <h2>Completions</h2>
            <label className="field">
              Prompt
              <textarea rows={8} value={completionPrompt} onChange={(e) => setCompletionPrompt(e.target.value)} />
            </label>
            <div className="row">
              <label className="field">
                Temperature
                <input
                  type="number"
                  step={0.1}
                  min={0}
                  max={2}
                  value={completionTemp}
                  onChange={(e) => setCompletionTemp(Number(e.target.value))}
                />
              </label>
              <label className="field">
                Max Tokens
                <input
                  type="number"
                  min={1}
                  value={completionMaxTokens}
                  onChange={(e) => setCompletionMaxTokens(Number(e.target.value))}
                />
              </label>
            </div>
            <div className="actions">
              <button type="button" onClick={runCompletion}>POST /api/v1/completions</button>
              <button type="button" onClick={streamCompletion}>POST /api/v1/completions/stream</button>
            </div>
          </article>
          <article className="card">
            <h2>Completion Output</h2>
            <pre>{completionOutput || 'Run completion to see output.'}</pre>
          </article>
        </section>
      )}

      {tab === 'chat' && (
        <section className="grid two">
          <article className="card">
            <h2>Stateful Chat</h2>
            <div className="row">
              <label className="field grow">
                Conversation ID
                <input value={conversationId} onChange={(e) => setConversationId(e.target.value)} />
              </label>
              <button type="button" onClick={refreshConversations}>GET /chat/conversations</button>
            </div>
            <div className="actions">
              <button type="button" onClick={loadHistory}>GET history</button>
              <button type="button" onClick={clearConversationMessages}>DELETE messages</button>
              <button type="button" onClick={deleteConversation}>DELETE conversation</button>
            </div>
            <label className="field">
              Message
              <textarea rows={6} value={chatMessage} onChange={(e) => setChatMessage(e.target.value)} />
            </label>
            <div className="row">
              <label className="field">
                Temperature
                <input
                  type="number"
                  step={0.1}
                  min={0}
                  max={2}
                  value={chatTemp}
                  onChange={(e) => setChatTemp(Number(e.target.value))}
                />
              </label>
              <label className="field">
                Max Tokens
                <input
                  type="number"
                  min={1}
                  value={chatMaxTokens}
                  onChange={(e) => setChatMaxTokens(Number(e.target.value))}
                />
              </label>
            </div>
            <div className="actions">
              <button type="button" onClick={sendChat}>POST /api/v1/chat/completions</button>
              <button type="button" onClick={streamChat}>POST /api/v1/chat/completions/stream</button>
            </div>
          </article>

          <article className="card">
            <h2>Conversation View</h2>
            <h3>Recent conversations</h3>
            <ul className="list compact-list">
              {conversations.map((conv) => (
                <li key={conv.conversationId}>
                  <button type="button" className="link-button" onClick={() => setConversationId(conv.conversationId)}>
                    {conv.conversationId}
                  </button>
                  <span>{conv.messageCount} msg</span>
                </li>
              ))}
            </ul>
            <h3>Latest response</h3>
            <pre>{chatResponse ? pretty(chatResponse) : 'No chat response yet.'}</pre>
            <h3>History</h3>
            <pre>{chatHistory.length > 0 ? pretty(chatHistory) : 'No history loaded.'}</pre>
          </article>
        </section>
      )}

      {tab === 'prompts' && (
        <section className="grid two">
          <article className="card">
            <h2>Template Management</h2>
            <div className="row">
              <label className="field">
                Built-in Filter
                <select
                  value={templateFilter}
                  onChange={(e) => setTemplateFilter(e.target.value as 'all' | 'built-in' | 'custom')}
                >
                  <option value="all">All</option>
                  <option value="built-in">Built-in</option>
                  <option value="custom">Custom</option>
                </select>
              </label>
              <button type="button" onClick={listTemplates}>GET /api/v1/prompts/templates</button>
            </div>
            <div className="row">
              <label className="field grow">
                Template ID
                <input value={templateId} onChange={(e) => setTemplateId(e.target.value)} />
              </label>
              <button type="button" onClick={getTemplateById}>GET /templates/{'{id}'}</button>
            </div>
            <label className="field">
              Template payload (JSON)
              <textarea rows={12} value={templatePayload} onChange={(e) => setTemplatePayload(e.target.value)} />
            </label>
            <div className="actions">
              <button type="button" onClick={createTemplate}>POST /templates</button>
              <button type="button" onClick={updateTemplate}>PUT /templates/{'{id}'}</button>
              <button type="button" onClick={deleteTemplate}>DELETE /templates/{'{id}'}</button>
            </div>
          </article>

          <article className="card">
            <h2>Render + Prompt Chat</h2>
            <label className="field">
              Variables (JSON)
              <textarea rows={8} value={renderVariables} onChange={(e) => setRenderVariables(e.target.value)} />
            </label>
            <div className="actions">
              <button type="button" onClick={renderTemplate}>POST /api/v1/prompts/render</button>
              <button type="button" onClick={promptChat}>POST /api/v1/prompts/chat</button>
            </div>
            <h3>Templates</h3>
            <pre>{templates.length > 0 ? pretty(templates) : 'No templates loaded.'}</pre>
            <h3>Render Result</h3>
            <pre>{promptRenderOutput ? pretty(promptRenderOutput) : 'No render output yet.'}</pre>
            <h3>Prompt Chat Result</h3>
            <pre>{promptChatOutput ? pretty(promptChatOutput) : 'No prompt chat output yet.'}</pre>
          </article>
        </section>
      )}

      {tab === 'release-notes' && (
        <section className="grid two">
          <article className="card">
            <h2>Release Notes Generator</h2>
            <label className="field">
              Repository Path
              <input value={repoPath} onChange={(e) => setRepoPath(e.target.value)} />
            </label>
            <div className="row">
              <label className="field">
                Branch
                <input value={branch} onChange={(e) => setBranch(e.target.value)} />
              </label>
              <label className="field">
                Base Ref
                <input value={baseRef} onChange={(e) => setBaseRef(e.target.value)} />
              </label>
            </div>
            <label className="field">
              Model (optional)
              <input value={releaseModel} onChange={(e) => setReleaseModel(e.target.value)} />
            </label>
            <div className="row">
              <label className="field">
                Max Commits
                <input
                  type="number"
                  min={1}
                  value={maxCommits}
                  onChange={(e) => setMaxCommits(Number(e.target.value))}
                />
              </label>
              <label className="field">
                Max Diff Characters
                <input
                  type="number"
                  min={100}
                  value={maxDiffCharacters}
                  onChange={(e) => setMaxDiffCharacters(Number(e.target.value))}
                />
              </label>
            </div>
            <button type="button" onClick={generateReleaseNotes}>POST /api/v1/release-notes</button>
          </article>

          <article className="card">
            <h2>Generated Release Notes</h2>
            <pre>{releaseNotesOutput ? pretty(releaseNotesOutput) : 'No release notes generated yet.'}</pre>
          </article>
        </section>
      )}
    </div>
  )
}

export default App
