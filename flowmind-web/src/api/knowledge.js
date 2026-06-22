import { api } from './index'

export function loadDocuments() {
  return api('/api/docs')
}

export function loadDocumentPage(page = 1, size = 10, filters = {}) {
  const query = new URLSearchParams({ page: String(page - 1), size: String(size) })
  Object.entries(filters).forEach(([key, value]) => {
    if (value) query.set(key, value)
  })
  return api(`/api/docs/page?${query.toString()}`)
}

export function uploadDocument(file) {
  const formData = new FormData()
  formData.append('file', file)
  return api('/api/docs/upload', { method: 'POST', body: formData })
}

export function deleteDocument(id) {
  return api(`/api/docs/${id}`, { method: 'DELETE' })
}

export function loadDocumentChunks(documentId) {
  return api(`/api/docs/${documentId}/chunks`)
}

export function loadChunkPage(documentId, page = 1, size = 10) {
  return api(`/api/docs/${documentId}/chunks/page?page=${page - 1}&size=${size}`)
}

export function loadKnowledgeConfig() {
  return api('/api/knowledge/config', { silent: true })
}

export function saveKnowledgeConfig(payload) {
  return api('/api/knowledge/config', { method: 'POST', body: payload })
}

export function askRag(question, config) {
  return api('/api/chat/rag', { method: 'POST', body: { question, ...config } })
}