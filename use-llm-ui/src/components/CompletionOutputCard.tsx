import { MarkdownContent } from './MarkdownContent'

interface Props {
  output: string
}

export function CompletionOutputCard({ output }: Props) {
  if (!output) {
    return <p className="muted">Run a completion to see output here.</p>
  }

  return (
    <div className="response-card">
      <div className="response-card-header">
        <div className="response-card-title">
          <span className="badge badge-ai">Output</span>
        </div>
      </div>
      <div className="response-content">
        <MarkdownContent content={output} />
      </div>
    </div>
  )
}

