import { ref } from 'vue'

export function useLoading() {
  const loadingVisible = ref(false)
  const loadingText = ref('加载中...')
  let loadingTimer = null

  function show(text = '加载中...') {
    loadingText.value = text
    loadingTimer = setTimeout(() => {
      loadingVisible.value = true
    }, 500)
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