import { ElMessage, ElMessageBox } from 'element-plus'
import { API_BASE } from '../config/constants'
import { DEFAULT_LOADING_TEXT } from '../composables/useLoading'

let backendHealthy = true
let visibleRequestCount = 0
let loadingVisible = false
let loadingTimer = null
const loadingListeners = new Set()
const inflightGetRequests = new Map()
const DEFAULT_TIMEOUT_MS = 10000

function emitLoadingState(visible) {
  loadingVisible = visible
  loadingListeners.forEach((listener) => listener({ visible, text: DEFAULT_LOADING_TEXT }))
}

function beginLoading(options = {}) {
  if (options.silent === true || options.loading === false) {
    return () => {}
  }
  visibleRequestCount += 1
  if (visibleRequestCount === 1) {
    loadingTimer = setTimeout(() => {
      if (visibleRequestCount > 0) {
        emitLoadingState(true)
      }
    }, options.loadingDelayMs ?? 450)
  }
  return () => {
    visibleRequestCount = Math.max(0, visibleRequestCount - 1)
    if (visibleRequestCount === 0) {
      if (loadingTimer) {
        clearTimeout(loadingTimer)
        loadingTimer = null
      }
      if (loadingVisible) {
        emitLoadingState(false)
      }
    }
  }
}

export function onGlobalLoadingChange(listener) {
  loadingListeners.add(listener)
  listener({ visible: loadingVisible, text: DEFAULT_LOADING_TEXT })
  return () => loadingListeners.delete(listener)
}

export function isBackendHealthy() {
  return backendHealthy
}

export function hasActiveVisibleRequests() {
  return visibleRequestCount > 0
}

export function setBackendHealthy(value) {
  backendHealthy = value
}

export async function api(path, options = {}) {
  const {
    silent,
    loading,
    loadingDelayMs,
    timeoutMs = DEFAULT_TIMEOUT_MS,
    dedupe = true,
    headers = {},
    signal,
    body,
    method = 'GET',
    ...fetchOptions
  } = options
  const normalizedMethod = method.toUpperCase()
  const isFormData = body instanceof FormData
  const isDedupeable = dedupe && !body && (normalizedMethod === 'GET' || normalizedMethod === 'HEAD')
  const requestUrl = `${API_BASE}${path}`
  const requestKey = `${normalizedMethod}:${requestUrl}`
  const finishLoading = beginLoading({ silent, loading, loadingDelayMs })

  if (isDedupeable && inflightGetRequests.has(requestKey)) {
    return inflightGetRequests.get(requestKey).finally(finishLoading)
  }

  const controller = new AbortController()
  let timeoutId = null
  let timedOut = false
  if (timeoutMs > 0) {
    timeoutId = setTimeout(() => {
      timedOut = true
      controller.abort()
    }, timeoutMs)
  }
  if (signal) {
    if (signal.aborted) {
      controller.abort()
    } else {
      signal.addEventListener('abort', () => controller.abort(), { once: true })
    }
  }

  const requestPromise = runRequest()
  if (isDedupeable) {
    inflightGetRequests.set(requestKey, requestPromise)
  }

  return requestPromise
    .finally(() => {
      if (timeoutId) clearTimeout(timeoutId)
      if (isDedupeable && inflightGetRequests.get(requestKey) === requestPromise) {
        inflightGetRequests.delete(requestKey)
      }
      finishLoading()
    })

  async function runRequest() {
    let response
    try {
      response = await fetch(requestUrl, {
        ...fetchOptions,
        method: normalizedMethod,
        headers: {
          ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
          ...headers,
        },
        body: isFormData ? body : body ? JSON.stringify(body) : undefined,
        signal: controller.signal,
      })
    } catch (error) {
      if (timedOut) {
        const timeoutError = new Error('请求超时，请稍后重试')
        timeoutError.name = 'TimeoutError'
        if (!silent) ElMessage.error(timeoutError.message)
        throw timeoutError
      }
      if (error.name === 'AbortError') throw error
      backendHealthy = false
      if (!silent) {
        ElMessage.error(`后端不可达：${error.message || error}`)
      }
      throw error
    }
    backendHealthy = response.ok || response.status < 500
    const text = await response.text()
    let json = null
    try {
      json = text ? JSON.parse(text) : null
    } catch {
      // 非 JSON 响应
    }
    if (json && typeof json.code === 'number') {
      if (json.code !== 200) {
        const errorMsg = json.message || (json.code === 50004 ? '未知错误' : '请求失败')
        if (!silent) {
          if (json.code === 50004) {
            ElMessageBox.alert(errorMsg, '提示', { type: 'warning', confirmButtonText: '知道了' })
          } else {
            ElMessage.error(errorMsg)
          }
        }
        throw new Error(errorMsg)
      }
      return json.data
    }
    if (!response.ok) {
      const message = text || `请求失败：${response.status}`
      if (!silent) {
        ElMessage.error(message)
      }
      throw new Error(message)
    }
    if (!text) return null
    return json !== null ? json : text
  }
}

export async function pingBackend() {
  try {
    await api('/api/health', { silent: true })
    backendHealthy = true
  } catch (error) {
    backendHealthy = false
  }
}
