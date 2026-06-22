import { api } from './index'

export function loadFields(filters = {}) {
  const query = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => {
    if (value) query.set(key, value)
  })
  return api(`/api/fields${query.toString() ? `?${query.toString()}` : ''}`)
}

export function loadFieldPage(page = 1, size = 10, filters = {}) {
  const query = new URLSearchParams({ page: String(page - 1), size: String(size) })
  Object.entries(filters).forEach(([key, value]) => {
    if (value) query.set(key, value)
  })
  return api(`/api/fields/page?${query.toString()}`)
}

export function createField(payload) {
  return api('/api/fields', { method: 'POST', body: payload })
}

export function updateField(id, payload) {
  return api(`/api/fields/${id}`, { method: 'PUT', body: payload })
}

export function enableField(id) {
  return api(`/api/fields/${id}/enable`, { method: 'POST' })
}

export function disableField(id) {
  return api(`/api/fields/${id}/disable`, { method: 'POST' })
}

export function loadDbTables() {
  return api('/api/fields/database/tables')
}

export function loadDbColumns(tableName) {
  return api(`/api/fields/database/tables/${encodeURIComponent(tableName)}/columns`)
}

export function importDbFields(payload) {
  return api('/api/fields/import-db', { method: 'POST', body: payload })
}
