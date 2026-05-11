import type { Message } from '../types'
import { MarkdownContent } from './MarkdownContent'

interface Props {
  messages: Message[]
}

const roleLabel: Record<string, string> = {
  USER: 'You',
  ASSISTANT: 'AI',
  SYSTEM: 'System',
}

const roleBadgeClass: Record<string, string> = {
  USER: 'badge-user',
  ASSISTANT: 'badge-ai',
  SYSTEM: 'badge-system',
}

export function ChatHistoryView({ messages }: Props) {
  if (messages.length === 0) {
    return <p className="muted">No history loaded.</p>
  }

  return (
    <div className="chat-history">
      {messages.map((msg, i) => (
        <div key={i} className={`chat-bubble chat-bubble-${msg.role.toLowerCase()}`}>
          <div className="chat-bubble-header">
            <span className={`badge ${roleBadgeClass[msg.role] ?? 'badge-system'}`}>
              {roleLabel[msg.role] ?? msg.role}
            </span>
            {msg.timestamp && (
              <span className="muted" style={{ fontSize: 11 }}>
                {new Date(msg.timestamp).toLocaleTimeString()}
              </span>
            )}
          </div>
          <div className="chat-bubble-body">
            {msg.role === 'ASSISTANT' ? (
              <MarkdownContent content={msg.content} />
            ) : (
              <p style={{ margin: 0, whiteSpace: 'pre-wrap' }}>{msg.content}</p>
            )}
          </div>
        </div>
      ))}
    </div>
  )
}

