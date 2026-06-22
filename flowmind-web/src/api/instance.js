import { api } from './index'

export function loadInstances() {
  return api('/api/workflow-instances')
}

export function loadInstancePage(page = 1, size = 10, filters = {}) {
  const query = new URLSearchParams({ page: String(page - 1), size: String(size) })
  Object.entries(filters).forEach(([key, value]) => {
    if (value) query.set(key, value)
  })
  return api(`/api/workflow-instances/page?${query.toString()}`)
}

export function loadInstanceLogs(id) {
  return api(`/api/workflow-instances/${id}/logs`)
}

export function loadMyApplications(starter = 'admin', page = 1, size = 10, filters = {}) {
  const query = new URLSearchParams({ starter, page: String(page - 1), size: String(size) })
  Object.entries(filters).forEach(([key, value]) => {
    if (value) query.set(key, value)
  })
  return api(`/api/workflow-instances/my?${query.toString()}`)
}
