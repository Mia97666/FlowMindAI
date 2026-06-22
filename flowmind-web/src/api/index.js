import { ElMessage } from 'element-plus'
import { API_BASE } from '../config/constants'

let backendHealthy = true

export function isBackendHealthy() {
  return backendHealthy
}

export function setBackendHealthy(value) {
  backendHealthy = value
}

export async function api(path, options = {}) {
  const isFormData = options.body instanceof FormData
  const silent = options.silent === true
  let response
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers: {
        ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
        ...(options.headers || {}),
      },
      body: isFormData ? options.body : options.body ? JSON.stringify(options.body) : undefined,
      signal: options.signal,
    })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    backendHealthy = false
    if (!silent) {
      ElMessage.error(`后端不可达：${error.message || error}`)
    }
    throw error
  }
  backendHealthy = response.ok || response.status < 500
  if (!response.ok) {
    const message = (await response.text()) || `请求失败：${response.status}`
    if (!silent) {
      ElMessage.error(message)
    }
    throw new Error(message)
  }
  const text = await response.text()
  if (!text) return null
  const json = JSON.parse(text)
  if (json && typeof json.code === 'number') {
    if (json.code !== 200) {
      if (!silent) {
        ElMessage.error(json.message || '请求失败')
      }
      throw new Error(json.message || '请求失败')
    }
    return json.data
  }
  return json
}

export async function pingBackend() {
  try {
    await api('/api/health', { silent: true })
    backendHealthy = true
  } catch (error) {
    backendHealthy = false
  }
}