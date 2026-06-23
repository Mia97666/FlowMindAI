import { api } from './index'

const AI_RISK_CHECK_TIMEOUT_MS = 45000

export function riskCheck(payload) {
  return api('/api/ai/risk-check', {
    method: 'POST',
    body: payload,
    timeoutMs: AI_RISK_CHECK_TIMEOUT_MS,
  })
}

export function loadAuditLogs(instanceId) {
  return api(`/api/ai/audit-logs/instance/${instanceId}`)
}
