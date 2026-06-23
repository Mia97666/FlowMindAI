import { ref } from 'vue'

export const DEFAULT_LOADING_TEXT = '服务器资源紧张，正在努力加载中……'

export function useLoading() {
  const loadingVisible = ref(false)
  const loadingText = ref(DEFAULT_LOADING_TEXT)
  let loadingTimer = null

  function show(text = DEFAULT_LOADING_TEXT, delay = 500) {
    if (loadingTimer) {
      clearTimeout(loadingTimer)
      loadingTimer = null
    }
    loadingText.value = text
    loadingTimer = setTimeout(() => {
      loadingVisible.value = true
      loadingTimer = null
    }, delay)
  }

  function hide() {
    if (loadingTimer) {
      clearTimeout(loadingTimer)
      loadingTimer = null
    }
    loadingVisible.value = false
  }

  return { loadingVisible, loadingText, show, hide }
}
