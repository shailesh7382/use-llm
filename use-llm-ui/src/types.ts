export type Role = 'SYSTEM' | 'USER' | 'ASSISTANT'

export type Message = {
  role: Role
  content: string
  timestamp?: string
}

export type ConversationSummary = {
  conversationId: string
  createdAt: string
  updatedAt: string
  messageCount: number
}

export type LlmModel = {
  id: string
  object?: string
  created?: number
  owned_by?: string
  description?: string
  context_length?: number
}

export type CompletionChoice = {
  text?: string
  index?: number
  finish_reason?: string
}

export type Usage = {
  prompt_tokens?: number
  completion_tokens?: number
  total_tokens?: number
}

export type CompletionResponse = {
  id?: string
  object?: string
  created?: number
  model?: string
  choices?: CompletionChoice[]
  usage?: Usage
}

export type ChatResponse = {
  conversationId: string
  responseId?: string
  model?: string
  content?: string
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
  finishReason?: string
  memorySize?: number
  estimatedMemoryTokens?: number
  history?: Message[]
}

export type PromptVariable = {
  name: string
  description?: string
  defaultValue?: string
  required?: boolean
}

export type PromptExample = {
  userInput: string
  assistantOutput: string
  description?: string
}

export type PromptTemplate = {
  id?: string
  name?: string
  description?: string
  persona?: string
  systemPrompt?: string
  userPromptTemplate?: string
  outputFormat?: string
  examples?: PromptExample[]
  variables?: PromptVariable[]
  builtIn?: boolean
  createdAt?: string
  updatedAt?: string
}

export type PromptRenderResponse = {
  templateId: string
  templateName: string
  messages: Message[]
  resolvedVariables: Record<string, string>
  estimatedTokens: number
}

export type CommitAnalysis = {
  sha?: string
  message?: string
  author?: string
  summary?: string
}

export type ReleaseNotesResponse = {
  repoPath?: string
  branch?: string
  baseRef?: string
  model?: string
  commitCount?: number
  commits?: CommitAnalysis[]
  releaseNotes?: string
}

export type ApiError = {
  status?: number
  type?: string
  message: string
}
