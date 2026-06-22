export function fieldTypeLabel(type) {
  const map = {
    TEXT: '单行文本', TEXTAREA: '多行文本', NUMBER: '数字', AMOUNT: '金额',
    DATE: '日期', DATETIME: '日期时间', SELECT: '下拉选择', BOOLEAN: '开关',
    FILE: '附件上传', TABLE: '明细表格', INFO: '说明文本',
  }
  return map[type] || type || '-'
}

export function sourceTypeLabel(type) {
  const map = { CUSTOM: '自定义', DB_COLUMN: '数据库', SYSTEM: '系统' }
  return map[type] || type || '-'
}

export function riskTagType(level) {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  if (level === 'LOW') return 'success'
  return 'info'
}

export function workflowStatusLabel(status) {
  const map = { DRAFT: '草稿', PUBLISHED: '已发布', DISABLED: '已停用' }
  return map[status] || status || '-'
}

export function workflowStatusTagType(status) {
  const map = { PUBLISHED: 'success', DRAFT: 'warning', DISABLED: 'info' }
  return map[status] || 'info'
}

export function formStatusLabel(status) {
  const map = { DRAFT: '草稿', PUBLISHED: '已发布', DISABLED: '已停用' }
  return map[status] || status || '-'
}

export function formStatusTagType(status) {
  const map = { PUBLISHED: 'success', DRAFT: 'warning', DISABLED: 'info' }
  return map[status] || 'info'
}

export function workflowCategory(row) {
  return row.category || '通用'
}

export function nodeTypeLabel(type) {
  const map = {
    START: '开始节点', END: '结束节点', FORM_TASK: '表单任务',
    APPROVAL: '人工审批', AI_RISK_CHECK: 'AI 风险检测',
    AI_APPROVAL: 'AI 审批', CONDITION: '条件路由', NOTIFY: '通知节点',
  }
  return map[type] || type || '-'
}

export function nodeClass(type) {
  const map = {
    START: 'node-start', END: 'node-end', FORM_TASK: 'node-form',
    APPROVAL: 'node-approval', AI_RISK_CHECK: 'node-ai',
    AI_APPROVAL: 'node-ai', CONDITION: 'node-gateway', NOTIFY: 'node-notify',
  }
  return map[type] || 'node-default'
}

export function formatDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (isNaN(d.getTime())) return value
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

export function dateFormatter(row, column, cellValue) {
  return formatDateTime(cellValue)
}

const ACTION_LABEL_MAP = {
  START: '发起',
  ENTER_NODE: '进入节点',
  AI_RISK_CHECK: 'AI风险评分',
  AI_AUTO_APPROVED: 'AI自动通过',
  AI_HUMAN_REVIEW_REQUIRED: '转人工复核',
  ROUTE_MATCHED: '路由命中',
  ROUTE_NOT_MATCHED: '路由未命中',
  CREATE_TASK: '创建待办',
  APPROVED: '人工同意',
  REJECTED: '人工拒绝',
  TRANSFER: '转办',
  COMPLETED: '流程完成',
  REJECTED_END: '拒绝结束',
}

export function actionLabel(action) {
  return ACTION_LABEL_MAP[action] || action || '-'
}

export function actionTagType(action) {
  if (['AI_AUTO_APPROVED', 'APPROVED', 'COMPLETED'].includes(action)) return 'success'
  if (['REJECTED', 'REJECTED_END'].includes(action)) return 'danger'
  if (['AI_HUMAN_REVIEW_REQUIRED', 'CREATE_TASK', 'TRANSFER'].includes(action)) return 'warning'
  if (['AI_RISK_CHECK', 'ROUTE_MATCHED'].includes(action)) return 'primary'
  return 'info'
}