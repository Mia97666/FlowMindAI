import { api } from './index'

export function loadRoles() {
  return api('/api/roles')
}

export function loadRolePage(page = 1, size = 10, filters = {}) {
  const query = new URLSearchParams({ page: String(page - 1), size: String(size) })
  Object.entries(filters).forEach(([key, value]) => {
    if (value) query.set(key, value)
  })
  return api(`/api/roles/page?${query.toString()}`)
}

export function createRole(payload) {
  return api('/api/roles', { method: 'POST', body: payload })
}

export function updateRole(id, payload) {
  return api(`/api/roles/${id}`, { method: 'PUT', body: payload })
}
