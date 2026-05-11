import type { ChatResponse } from '../types'
import { MarkdownContent } from './MarkdownContent'

interface Props {
  response: ChatResponse
}

function MetaBadge({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="meta-badge">
      <span className="meta-label">{label}</span>
      <span className="meta-value">{value}</span>
    </div>
  )
}

export function ChatResponseCard({ response }: Props) {
  const hasTokens =
    response.promptTokens !== undefined ||
    response.completionTokens !== undefined ||
    response.totalTokens !== undefined

  return (
    <div className="response-card">
      {/* Header row */}
      <div className="response-card-header">
        <div className="response-card-title">
          <span className="badge badge-ai">AI</span>
          <span className="response-model">{response.model ?? 'Unknown model'}</span>
        </div>
        {response.finishReason && (
          <span className={`badge badge-finish ${response.finishReason === 'stop' ? 'badge-success' : 'badge-warn'}`}>
            {response.finishReason}
          </span>
        )}
      </div>

      {/* Markdown content */}
      <div className="response-content">
        {response.content ? (
          <MarkdownContent content={response.content} />
        ) : (
          <span className="muted">No content yet.</span>
        )}
      </div>

      {/* Token & memory stats */}
      {hasTokens && (
        <div className="response-meta">
          {response.promptTokens !== undefined && (
            <MetaBadge label="Prompt tokens" value={response.promptTokens} />
          )}
          {response.completionTokens !== undefined && (
            <MetaBadge label="Completion tokens" value={response.completionTokens} />
          )}
          {response.totalTokens !== undefined && (
            <MetaBadge label="Total tokens" value={response.totalTokens} />
          )}
          {response.memorySize !== undefined && (
            <MetaBadge label="Memory messages" value={response.memorySize} />
          )}
          {response.estimatedMemoryTokens !== undefined && (
            <MetaBadge label="Memory tokens" value={response.estimatedMemoryTokens} />
          )}
        </div>
      )}

      {/* IDs row */}
      <div className="response-ids">
        <span className="id-chip" title="Conversation ID">
          💬 {response.conversationId}
        </span>
        {response.responseId && (
          <span className="id-chip" title="Response ID">
            🔑 {response.responseId}
          </span>
        )}
      </div>
    </div>
  )
}

