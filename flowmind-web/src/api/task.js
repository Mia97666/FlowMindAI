import { api } from './index'

export function loadTodos(assignee = 'admin') {
  return api(`/api/workflow-tasks/todo?assignee=${encodeURIComponent(assignee)}`)
}

export function loadTodoPage(assignee = 'admin', page = 1, size = 10, filters = {}) {
  const query = new URLSearchParams({ assignee, page: String(page - 1), size: String(size) })
  Object.entries(filters).forEach(([key, value]) => {
    if (value) query.set(key, value)
  })
  return api(`/api/workflow-tasks/todo/page?${query.toString()}`)
}

export function loadDonePage(assignee = 'admin', page = 1, size = 10, filters = {}) {
  const query = new URLSearchParams({ assignee, page: String(page - 1), size: String(size) })
  Object.entries(filters).forEach(([key, value]) => {
    if (value) query.set(key, value)
  })
  return api(`/api/workflow-tasks/done/page?${query.toString()}`)
}

export function loadInstanceTasks(instanceId) {
  return api(`/api/workflow-tasks/instance/${instanceId}`)
}

export function completeTask(taskId, payload) {
  return api(`/api/workflow-tasks/${taskId}/complete`, { method: 'POST', body: payload })
}

export function loadDoneTasks(assignee = 'admin') {
  return api(`/api/workflow-tasks/done?assignee=${encodeURIComponent(assignee)}`)
}

export function loadTodoTasks(assignee = 'admin') {
  return api(`/api/workflow-tasks/todo?assignee=${encodeURIComponent(assignee)}`)
}
