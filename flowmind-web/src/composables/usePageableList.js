import { reactive, ref } from 'vue'

/**
 * 通用分页列表 composable。
 *
 * 封装了筛选、分页、加载状态的通用逻辑，
 * 消除各页面重复的 filter/search/reset/pagination 模式。
 *
 * @param {Function} fetchFn - 数据加载函数，签名为 (page, size, filters) => Promise
 * @param {Object} options
 * @param {Object} options.initialFilters - 初始筛选条件
 * @param {number} options.pageSize - 每页条数，默认 10
 * @param {Function} options.onResult - 结果回调，用于同步到共享状态
 */
export function usePageableList(fetchFn, options = {}) {
  const { initialFilters = {}, pageSize = 10, onResult } = options

  const filters = reactive({ ...initialFilters })
  const pagination = reactive({ page: 1, size: pageSize, total: 0 })
  const loading = ref(false)

  async function load(page) {
    if (page !== undefined) pagination.page = page
    loading.value = true
    try {
      const result = await fetchFn(pagination.page, pagination.size, { ...filters })
      const rows = result.records || result.content || result
      pagination.total = result.total || result.totalElements || (Array.isArray(rows) ? rows.length : 0)
      if (onResult) onResult(rows)
    } finally {
      loading.value = false
    }
  }

  function search() {
    return load(1)
  }

  function reset() {
    Object.assign(filters, initialFilters)
    return search()
  }

  return { filters, pagination, loading, load, search, reset }
}