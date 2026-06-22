export function menuPathLabel(menu) {
  const parts = []
  let current = menu
  while (current) {
    parts.unshift(current.menuName || current.menuCode || '')
    current = current._parent || null
  }
  return parts.join(' / ')
}

export function flattenMenus(menus, parent = null) {
  return (menus || []).flatMap((menu) => {
    const item = { ...menu, _parent: parent }
    return [item, ...flattenMenus(menu.children || [], item)]
  })
}

export function taskInstanceTitle(task) {
  return task?.instanceTitle || task?.title || `任务 ${task?.id || ''}`
}

export function taskWorkflowName(task) {
  return task?.definitionName || task?.workflowName || '-'
}