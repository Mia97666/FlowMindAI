import { api } from './index'

export function loadNotifications(receiver = 'admin') {
  return api(`/api/notifications?receiver=${encodeURIComponent(receiver)}`)
}

export function markRead(id) {
  return api(`/api/notifications/${id}/read`, { method: 'POST' })
}
