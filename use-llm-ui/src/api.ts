import type { ApiError } from './types'

async function parseError(response: Response): Promise<ApiError> {
  try {
    const body = (await response.json()) as { message?: string; type?: string; status?: number }
    return {
      status: body.status ?? response.status,
      type: body.type,
      message: body.message ?? `Request failed with status ${response.status}`,
    }
  } catch {
    return {
      status: response.status,
      message: `Request failed with status ${response.status}`,
    }
  }
}

export async function apiRequest<T>(
  baseUrl: string,
  path: string,
  init?: RequestInit,
): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
    ...init,
  })

  if (!response.ok) {
    const err = await parseError(response)
    throw new Error(err.message)
  }

  if (response.status === 204) {
    return null as T
  }

  return (await response.json()) as T
}

export async function streamSse<T>(
  baseUrl: string,
  path: string,
  body: unknown,
  onData: (event: T) => void,
): Promise<void> {
  const response = await fetch(`${baseUrl}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    const err = await parseError(response)
    throw new Error(err.message)
  }

  if (!response.body) {
    throw new Error('Streaming response body is unavailable')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() ?? ''

    for (const rawLine of lines) {
      const line = rawLine.trim()
      if (!line) continue

      const data = line.startsWith('data:') ? line.slice(5).trim() : line
      if (!data || data === '[DONE]') continue

      try {
        onData(JSON.parse(data) as T)
      } catch {
        // ignore non-json chunks
      }
    }
  }
}
