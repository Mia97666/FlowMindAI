import { api } from './index'

export function loadMenuTree(username = 'admin') {
  return api(`/api/menus/users/${encodeURIComponent(username)}/tree`)
}

export function loadPermissionMenuTree() {
  return api('/api/menus/tree')
}

export function loadAllMenuTree() {
  return api('/api/menus/all-tree')
}

export function loadRoleMenus(roleId) {
  return api(`/api/menus/roles/${roleId}`)
}

export function saveRoleMenus(roleId, menuIds) {
  return api(`/api/menus/roles/${roleId}`, { method: 'PUT', body: { menuIds } })
}

export function createMenu(payload) {
  return api('/api/menus', { method: 'POST', body: payload })
}

export function updateMenu(id, payload) {
  return api(`/api/menus/${id}`, { method: 'PUT', body: payload })
}

export function disableMenu(id) {
  return api(`/api/menus/${id}/disable`, { method: 'POST' })
}

export function enableMenu(id) {
  return api(`/api/menus/${id}/enable`, { method: 'POST' })
}
