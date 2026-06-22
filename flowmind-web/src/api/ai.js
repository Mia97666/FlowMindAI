import { api } from './index'

export function riskCheck(payload) {
  return api('/api/ai/risk-check', { method: 'POST', body: payload })
}

export function loadAuditLogs(instanceId) {
  return api(`/api/ai/audit-logs/instance/${instanceId}`)
}