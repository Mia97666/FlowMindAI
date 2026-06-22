export function hasSourceHandle(type) {
  return type !== 'END'
}

export function hasTargetHandle(type) {
  return type !== 'START'
}

export function nodeNeedsForm(type) {
  return ['START', 'APPROVAL', 'AI_APPROVAL', 'FORM_TASK', 'AI_RISK_CHECK'].includes(type)
}

export function nodeAllowsFieldPermission(type) {
  return ['APPROVAL', 'AI_APPROVAL', 'FORM_TASK'].includes(type)
}

export function ensureNodeConfig(node) {
  if (!node.data.config) {
    node.data.config = {}
  }
  const config = node.data.config
  if (node.data.nodeType === 'AI_RISK_CHECK' || node.data.nodeType === 'AI_APPROVAL') {
    config.threshold = config.threshold ?? 70
    config.highRiskReceivers = config.highRiskReceivers || 'finance,admin'
  }
  if (node.data.nodeType === 'CONDITION') {
    config.highRiskCondition = config.highRiskCondition || 'riskScore >= 70'
    config.defaultRoute = config.defaultRoute || ''
  }
  if (node.data.nodeType === 'NOTIFY') {
    config.receivers = config.receivers || 'starter'
    config.title = config.title || '审批流转提醒'
    config.content = config.content || '流程 {title} 已完成当前审批路径。'
  }
  if (!config.fieldPermissions) {
    config.fieldPermissions = {}
  }
}

export function generateId() {
  return `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}