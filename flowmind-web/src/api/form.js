import { api } from './index'

export function loadForms(filters = {}) {
  const query = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => {
    if (value) query.set(key, value)
  })
  return api(`/api/forms${query.toString() ? `?${query.toString()}` : ''}`)
}

export function loadFormPage(page = 1, size = 10, filters = {}) {
  const query = new URLSearchParams({ page: String(page - 1), size: String(size) })
  Object.entries(filters).forEach(([key, value]) => {
    if (value) query.set(key, value)
  })
  return api(`/api/forms/page?${query.toString()}`)
}

export function createForm(payload) {
  return api('/api/forms', { method: 'POST', body: payload })
}

export function updateForm(id, payload) {
  return api(`/api/forms/${id}`, { method: 'PUT', body: payload })
}

export function deleteForm(id) {
  return api(`/api/forms/${id}`, { method: 'DELETE', silent: true })
}

export function publishForm(id) {
  return api(`/api/forms/${id}/publish`, { method: 'POST' })
}

export function disableForm(id) {
  return api(`/api/forms/${id}/disable`, { method: 'POST' })
}

export function loadRuntimeFormForInstance(instanceId) {
  return api(`/api/runtime-forms/instances/${instanceId}`)
}

export function loadRuntimeFormForTask(taskId) {
  return api(`/api/runtime-forms/tasks/${taskId}`)
}

export function loadStartForm(definitionId) {
  return api(`/api/runtime-forms/workflows/${definitionId}/start`)
}
