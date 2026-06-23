import { api } from './index'

export function loadNotifications(receiver = 'admin', options = {}) {
  return api(`/api/notifications?receiver=${encodeURIComponent(receiver)}`, options)
}

export function markRead(id) {
  return api(`/api/notifications/${id}/read`, { method: 'POST' })
}
