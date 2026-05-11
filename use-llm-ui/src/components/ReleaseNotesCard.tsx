import type { ReleaseNotesResponse } from '../types'
import { MarkdownContent } from './MarkdownContent'

interface Props {
  response: ReleaseNotesResponse
}

export function ReleaseNotesCard({ response }: Props) {
  return (
    <div className="response-card">
      <div className="response-card-header">
        <div className="response-card-title">
          <span className="badge badge-release">Release Notes</span>
          {response.model && <span className="response-model">{response.model}</span>}
        </div>
        {response.commitCount !== undefined && (
          <span className="badge badge-finish badge-success">{response.commitCount} commits</span>
        )}
      </div>

      {/* Meta */}
      <div className="response-meta" style={{ marginBottom: 12 }}>
        {response.branch && (
          <div className="meta-badge">
            <span className="meta-label">Branch</span>
            <span className="meta-value">{response.branch}</span>
          </div>
        )}
        {response.baseRef && (
          <div className="meta-badge">
            <span className="meta-label">Base ref</span>
            <span className="meta-value">{response.baseRef}</span>
          </div>
        )}
        {response.repoPath && (
          <div className="meta-badge">
            <span className="meta-label">Repo</span>
            <span className="meta-value" style={{ fontSize: 11 }}>{response.repoPath}</span>
          </div>
        )}
      </div>

      {/* Release notes markdown */}
      {response.releaseNotes && (
        <div className="response-content">
          <MarkdownContent content={response.releaseNotes} />
        </div>
      )}

      {/* Commits accordion */}
      {response.commits && response.commits.length > 0 && (
        <details className="commits-details">
          <summary>Analysed commits ({response.commits.length})</summary>
          <div className="commits-list">
            {response.commits.map((c, i) => (
              <div key={i} className="commit-row">
                <span className="commit-sha">{c.sha?.slice(0, 7) ?? '?'}</span>
                <div className="commit-info">
                  <span className="commit-msg">{c.message}</span>
                  {c.author && <span className="muted" style={{ fontSize: 11 }}>— {c.author}</span>}
                  {c.summary && <p className="commit-summary">{c.summary}</p>}
                </div>
              </div>
            ))}
          </div>
        </details>
      )}
    </div>
  )
}

