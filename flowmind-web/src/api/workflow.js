import { api } from './index'

export function loadWorkflows() {
  return api('/api/workflows')
}

export function loadWorkflowPage(page = 1, size = 10, filters = {}) {
  const query = new URLSearchParams({ page: String(page - 1), size: String(size) })
  Object.entries(filters).forEach(([key, value]) => {
    if (value) query.set(key, value)
  })
  return api(`/api/workflows/page?${query.toString()}`)
}

export function createWorkflow(payload) {
  return api('/api/workflows', { method: 'POST', body: payload })
}

export function updateWorkflow(id, payload) {
  return api(`/api/workflows/${id}`, { method: 'PUT', body: payload })
}

export function publishWorkflow(id) {
  return api(`/api/workflows/${id}/publish`, { method: 'POST' })
}

export function disableWorkflow(id) {
  return api(`/api/workflows/${id}/disable`, { method: 'POST' })
}

export function enableWorkflow(id) {
  return api(`/api/workflows/${id}/enable`, { method: 'POST' })
}

export function preCheckWorkflow(id, payload) {
  return api(`/api/workflows/${id}/pre-check`, { method: 'POST', silent: true, body: payload })
}

export function startWorkflow(id, payload) {
  return api(`/api/workflows/${id}/start`, { method: 'POST', body: payload })
}