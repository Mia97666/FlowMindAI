import { api } from './index'

export function loadUsers() {
  return api('/api/users')
}

export function loadUserPage(page = 1, size = 10, filters = {}) {
  const query = new URLSearchParams({ page: String(page - 1), size: String(size) })
  Object.entries(filters).forEach(([key, value]) => {
    if (value) query.set(key, value)
  })
  return api(`/api/users/page?${query.toString()}`)
}

export function createUser(payload) {
  return api('/api/users', { method: 'POST', body: payload })
}

export function updateUser(id, payload) {
  return api(`/api/users/${id}`, { method: 'PUT', body: payload })
}

export function updateUserRoles(userId, roleCodes) {
  return api(`/api/users/${userId}/roles`, { method: 'PUT', body: { roleCodes } })
}
