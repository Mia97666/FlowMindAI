import { ref, watch, onBeforeUnmount } from 'vue'

/**
 * 防抖 composable，延迟指定毫秒数后更新值
 * @param {Ref} source - 源响应式值
 * @param {number} delay - 延迟毫秒数，默认 300
 */
export function useDebounce(source, delay = 300) {
  const debounced = ref(source.value)
  let timer = null

  watch(source, (val) => {
    clearTimeout(timer)
    timer = setTimeout(() => {
      debounced.value = val
    }, delay)
  })

  onBeforeUnmount(() => {
    clearTimeout(timer)
  })

  return debounced
}