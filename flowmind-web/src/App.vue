<template>
  <AppLayout />
  <LoadingOverlay :visible="loadingVisible" :text="loadingText" />
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, provide, reactive, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppLayout from './components/AppLayout.vue'
import LoadingOverlay from './components/LoadingOverlay.vue'
import { SharedStateKey } from './composables/useSharedState'
import { useLoading } from './composables/useLoading'
import { useDebounce } from './composables/useDebounce'
import { fallbackMenuTree } from './config/menuTree'
import { sectionTitleMap } from './config/constants'
import { workflowCategory, workflowStatusLabel, workflowStatusTagType, formStatusLabel, formStatusTagType, parseFormSchema, parseJson, parseControlOptions, riskTagType, flattenMenus, menuPathLabel } from './utils/helpers'
import { api as baseApi, hasActiveVisibleRequests, onGlobalLoadingChange } from './api/index'
import { preloadRoute } from './router/preload'
import { loadUsers, loadUserPage, createUser, updateUser, updateUserRoles } from './api/user'
import { loadRoles, loadRolePage, createRole, updateRole } from './api/role'
import { loadMenuTree, loadPermissionMenuTree, loadAllMenuTree, loadRoleMenus, saveRoleMenus, createMenu, updateMenu, disableMenu, enableMenu } from './api/menu'
import { loadWorkflows, loadWorkflowPage, createWorkflow, updateWorkflow, publishWorkflow, disableWorkflow } from './api/workflow'
import { loadInstances, loadInstancePage, loadInstanceLogs } from './api/instance'
import { loadTodos, loadTodoPage, loadDonePage, loadInstanceTasks, completeTask, loadTodoTasks, loadDoneTasks } from './api/task'
import { loadFields, loadFieldPage, createField, updateField, enableField, disableField, loadDbTables, loadDbColumns, importDbFields } from './api/field'
import { loadForms, loadFormPage, createForm, updateForm, deleteForm, publishForm, disableForm, loadRuntimeFormForInstance, loadRuntimeFormForTask, loadStartForm } from './api/form'
import { riskCheck, loadAuditLogs } from './api/ai'
import { loadDocuments, loadDocumentPage, uploadDocument, deleteDocument, loadDocumentChunks, loadChunkPage, loadKnowledgeConfig, saveKnowledgeConfig, askRag } from './api/knowledge'
import { loadNotifications, markRead } from './api/notification'

const router = useRouter()
const route = useRoute()
const { loadingVisible, loadingText, show: showLoading, hide: hideLoading } = useLoading()

// ---- Core state ----
const backendHealthy = ref(false)
const currentUser = ref('admin')
const globalSearchKeyword = ref('')
const debouncedSearchKeyword = useDebounce(globalSearchKeyword, 300)

// ---- Data ----
const workflows = ref([])
const users = ref([])
const roles = ref([])
const userPageRows = ref([])
const rolePageRows = ref([])
const workflowPageRows = ref([])
const formPageRows = ref([])
const fieldPageRows = ref([])
const documentPageRows = ref([])
const instancePageRows = ref([])
const chunkPageRows = ref([])
const instances = ref([])
const todos = ref([])
const todoTasks = ref([])
const doneTasks = ref([])
const notifications = ref([])
const documents = ref([])
const documentChunks = ref([])
const instanceTasks = ref([])
const auditLogs = ref([])
const menuTree = ref([])
const permissionMenuTree = ref([])
const menuManageTree = ref([])
const checkedMenuIds = ref([])
const fields = ref([])
const forms = ref([])
const dbTables = ref([])
const dbColumns = ref([])

// ---- Navigation ----
const activeTopCode = ref('dashboard')
const activeRouteKey = ref('overview')

// ---- Filters & Pagination ----
const todoFilters = reactive({ nodeName: '', riskLevel: '' })
const doneFilters = reactive({ nodeName: '', status: '', riskLevel: '' })
const todoPagination = reactive({ page: 1, size: 10, total: 0 })
const donePagination = reactive({ page: 1, size: 10, total: 0 })
const userFilters = reactive({ username: '', realName: '', department: '', status: '' })
const roleFilters = reactive({ code: '', name: '' })
const workflowFilters = reactive({ name: '', code: '', status: '', category: '' })
const userPagination = reactive({ page: 1, size: 10, total: 0 })
const rolePagination = reactive({ page: 1, size: 10, total: 0 })
const workflowPagination = reactive({ page: 1, size: 10, total: 0 })
const formPagination = reactive({ page: 1, size: 10, total: 0 })
const fieldPagination = reactive({ page: 1, size: 10, total: 0 })
const documentPagination = reactive({ page: 1, size: 10, total: 0 })
const instancePagination = reactive({ page: 1, size: 10, total: 0 })
const chunkPagination = reactive({ page: 1, size: 10, total: 0 })
const formFilters = reactive({ formName: '', formCode: '', status: '' })
const knowledgeConfig = reactive({
  name: 'FlowMind 知识库', adapterType: 'SELF', topK: 2, minScore: 0.55,
  retrievalMode: 'LIGHT', queryRewriteEnabled: false, multiQueryEnabled: false,
  embeddingModel: 'text-embedding-v3', rerankModel: 'bge-reranker-v2-m3',
  chunkSize: 800, chunkOverlap: 120, ragFlowEnabled: false,
  ragFlowEndpoint: '', ragFlowDatasetId: '', ragFlowApiKey: '',
})
const knowledgeDocFilters = reactive({ keyword: '', status: '' })
const chunkFilters = reactive({ keyword: '' })

// ---- Computed ----
const navigationTree = computed(() => normalizeMenus(menuTree.value.length > 0 ? menuTree.value : fallbackMenuTree))
const topMenus = computed(() => navigationTree.value.filter((menu) => menu.menuType === 'TOP' || !menu.parentId))
const currentTopMenu = computed(() => topMenus.value.find((menu) => menu.menuCode === activeTopCode.value) || topMenus.value[0])
const unreadNotificationCount = computed(() => notifications.value.filter((message) => !message.readFlag).length)
const currentUserInitials = computed(() => {
  const user = users.value.find((item) => item.username === currentUser.value)
  const displayName = user?.realName || currentUser.value || 'FM'
  return String(displayName).slice(0, 2).toUpperCase()
})

const sectionTitle = computed(() => {
  const menu = findMenuByRouteKey(activeRouteKey.value)
  return menu?.menuName || sectionTitleMap[activeRouteKey.value] || sectionTitleMap[activeRouteKey.value] || ''
})

const sectionSubtitle = computed(() => {
  const map = {
    overview: `${instances.value.length} 个实例 · ${todos.value.length} 个待办 · ${notifications.value.length} 条通知`,
    workflowList: `${workflows.value.length} 个流程定义`,
    todo: `${currentUser.value} · ${todoPagination.total} 个待审批任务`,
    done: `${currentUser.value} · ${donePagination.total} 个已审批任务`,
    instances: `${instances.value.length} 个实例`,
    documents: `${documents.value.length} 份制度文档`,
    userList: `${userPagination.total} 个用户`,
    roleList: `${rolePagination.total} 个角色`,
    menuPermission: `${flattenMenus(menuManageTree.value).length} 个菜单节点 · ${roles.value.length} 个角色`,
    myApplications: `${currentUser.value} · ${userPagination.total} 个申请`,
  }
  return map[activeRouteKey.value] || ''
})

const globalSearchResults = computed(() => {
  const keyword = debouncedSearchKeyword.value.trim().toLowerCase()
  if (!keyword) return []
  const results = []
  flattenNavigationPages(navigationTree.value).forEach((menu) => {
    const title = menu.menuName || ''
    const haystack = `${title} ${menu.routeKey || ''} ${menu.menuCode || ''}`.toLowerCase()
    if (haystack.includes(keyword)) {
      results.push({ type: 'menu', typeLabel: '页面', key: menu.routeKey, title, description: menu.pathLabel || title, routeKey: menu.routeKey })
    }
  })
  workflows.value.forEach((workflow) => {
    const title = workflow.name || workflow.code || `流程 ${workflow.id}`
    const haystack = `${title} ${workflow.code || ''} ${workflow.category || ''}`.toLowerCase()
    if (haystack.includes(keyword)) {
      results.push({ type: 'workflow', typeLabel: '流程', key: workflow.id, title, description: `${workflow.code || '-'} · ${workflowStatusLabel(workflow.status)}`, routeKey: 'workflowDesigner', payload: workflow })
    }
  })
  ;[...todoTasks.value, ...doneTasks.value].forEach((task) => {
    const title = task.instanceTitle || task.title || `任务 ${task.id}`
    const haystack = `${title} ${task.nodeName || ''} ${task.assignee || ''} ${task.status || ''}`.toLowerCase()
    if (haystack.includes(keyword)) {
      results.push({ type: 'task', typeLabel: '任务', key: task.id, title, description: `${task.nodeName || '-'} · ${task.status || '-'}`, routeKey: task.status === 'PENDING' ? 'todo' : 'done', payload: task })
    }
  })
  documents.value.forEach((document) => {
    const title = document.originalFilename || `文档 ${document.id}`
    const haystack = `${title} ${document.createdAt || ''}`.toLowerCase()
    if (haystack.includes(keyword)) {
      results.push({ type: 'document', typeLabel: '知识库', key: document.id, title, description: `${document.chunkCount || 0} 个 Chunk`, routeKey: 'documents', payload: document })
    }
  })
  return results.slice(0, 8)
})

// ---- Menu helpers ----
function normalizeMenus(menus = []) {
  return menus.filter((menu) => menu.status !== 'DISABLED').map((menu) => ({ ...menu, children: normalizeMenus(menu.children || []) })).sort((first, second) => (first.sortOrder || 999) - (second.sortOrder || 999))
}

function visibleChildren(menu) {
  if (!menu?.children) return []
  return menu.children.filter((child) => child.status !== 'DISABLED')
}

function findMenuByRouteKey(routeKey, menus = navigationTree.value) {
  for (const menu of menus) {
    if (menu.routeKey === routeKey) return menu
    const matched = findMenuByRouteKey(routeKey, visibleChildren(menu))
    if (matched) return matched
  }
  return null
}

function flattenNavigationPages(menus = [], prefix = '') {
  return (menus || []).flatMap((menu) => {
    const label = prefix ? `${prefix} / ${menu.menuName}` : menu.menuName
    const current = menu.routeKey && menu.menuType !== 'TOP' ? [{ ...menu, pathLabel: label }] : []
    return [...current, ...flattenNavigationPages(visibleChildren(menu), label)]
  })
}

// ---- Navigation handlers ----
function handleSelectTopMenu(menu) {
  if (!menu) return
  activeTopCode.value = menu.menuCode
  const firstPage = findFirstPage(menu)
  if (firstPage?.routeKey) {
    router.push(routeKeyToPath(firstPage.routeKey))
  }
}

async function handleSideNavigate(routeKey) {
  const targetPath = routeKeyToPath(routeKey)
  showLoading(undefined, 0)
  const loadingFallbackTimer = setTimeout(() => {
    hideLoading()
  }, 12000)
  const preloadPromise = preloadRoute(routeKey, { currentUser: currentUser.value })
  try {
    await router.push(targetPath)
    await preloadPromise
  } finally {
    clearTimeout(loadingFallbackTimer)
    hideLoading()
  }
}

function handleUserChange(username) {
  currentUser.value = username
  refreshUserScopedData()
}

function routeKeyToPath(routeKey) {
  const route = router.getRoutes().find((r) => r.meta?.routeKey === routeKey)
  return materializeRoutePath(route?.path || '/dashboard')
}

function materializeRoutePath(path) {
  const normalized = String(path || '/dashboard')
    .replace(/\/:[^/]+\?/g, '')
    .replace(/\/:[^/]+/g, '')
  return normalized || '/'
}

function findFirstPage(menu) {
  if (!menu) return null
  if (menu.routeKey && menu.menuType !== 'TOP') return menu
  for (const child of visibleChildren(menu)) {
    const matched = findFirstPage(child)
    if (matched) return matched
  }
  return null
}

function jumpFirstSearchResult() {
  const first = globalSearchResults.value[0]
  if (first) openGlobalSearchResult(first)
}

async function openGlobalSearchResult(item) {
  if (!item) return
  globalSearchKeyword.value = ''
  if (item.type === 'workflow' && item.payload) {
    router.push(`/workflows/designer/${item.payload.id}`)
    return
  }
  if (item.type === 'task' && item.payload) {
    router.push(item.payload.status === 'PENDING' ? '/approval/todo' : '/approval/done')
    return
  }
  if (item.type === 'document' && item.payload) {
    knowledgeDocFilters.keyword = item.payload.originalFilename || ''
    router.push('/knowledge/documents')
    return
  }
  router.push(routeKeyToPath(item.routeKey))
}

// ---- Data loading ----
async function refreshAll() {
  const results = await Promise.allSettled([
    loadUsers(), loadWorkflows(), loadInstances(), loadTodos(currentUser.value),
    loadDoneTasks(currentUser.value), loadNotifications(currentUser.value), loadMenus(currentUser.value),
  ])
  const [usersR, workflowsR, instancesR, todosR, doneTasksR, notificationsR] = results
  if (usersR.status === 'fulfilled') users.value = usersR.value
  if (workflowsR.status === 'fulfilled') workflows.value = workflowsR.value
  if (instancesR.status === 'fulfilled') instances.value = instancesR.value
  if (todosR.status === 'fulfilled') {
    todos.value = todosR.value || []
    todoTasks.value = todosR.value || []
  }
  if (doneTasksR.status === 'fulfilled') doneTasks.value = doneTasksR.value || []
  if (notificationsR.status === 'fulfilled') notifications.value = notificationsR.value || []
}

async function refreshUserScopedData() {
  const results = await Promise.allSettled([
    loadTodos(currentUser.value),
    loadTodoTasks(currentUser.value),
    loadDoneTasks(currentUser.value),
    loadNotifications(currentUser.value),
    loadMenus(currentUser.value),
  ])
  const [todosR, todoTasksR, doneTasksR, notificationsR, menusR] = results
  if (todosR.status === 'fulfilled') todos.value = todosR.value || []
  if (todoTasksR.status === 'fulfilled') todoTasks.value = todoTasksR.value || []
  if (doneTasksR.status === 'fulfilled') doneTasks.value = doneTasksR.value || []
  if (notificationsR.status === 'fulfilled') notifications.value = notificationsR.value || []
}

async function loadMenus(username = currentUser.value) {
  menuTree.value = await loadMenuTree(username)
}

// ---- Lifecycle ----
let healthTimer = null
let notificationTimer = null
let stopGlobalLoading = null

onMounted(async () => {
  stopGlobalLoading = onGlobalLoadingChange(({ visible, text }) => {
    if (visible) {
      showLoading(text, 0)
    } else {
      hideLoading()
    }
  })
  await refreshAll()
  try {
    await baseApi('/api/health', { silent: true })
    backendHealthy.value = true
  } catch {
    backendHealthy.value = false
  }
  healthTimer = setInterval(async () => {
    if (document.hidden || hasActiveVisibleRequests()) return
    try { await baseApi('/api/health', { silent: true, timeoutMs: 5000 }); backendHealthy.value = true } catch { backendHealthy.value = false }
  }, 30000)
  notificationTimer = setInterval(async () => {
    if (document.hidden || hasActiveVisibleRequests()) return
    try {
      const result = await loadNotifications(currentUser.value, { silent: true, timeoutMs: 5000 })
      if (result) notifications.value = result
    } catch { /* 静默刷新 */ }
  }, 60000)
})

onBeforeUnmount(() => {
  stopGlobalLoading?.()
  if (healthTimer) clearInterval(healthTimer)
  if (notificationTimer) clearInterval(notificationTimer)
})

// ---- Router sync ----
watch(() => route.meta, (meta) => {
  if (meta.topCode) activeTopCode.value = meta.topCode
  if (meta.routeKey) activeRouteKey.value = meta.routeKey
}, { immediate: true })

// ---- Provide shared state ----
provide(SharedStateKey, {
  // Core
  backendHealthy, currentUser, globalSearchKeyword, globalSearchResults,
  unreadNotificationCount, currentUserInitials,
  // Data
  workflows, users, roles, userPageRows, rolePageRows,
  workflowPageRows, formPageRows, fieldPageRows, documentPageRows,
  instancePageRows, chunkPageRows, instances,
  todos, todoTasks, doneTasks, notifications, documents, documentChunks,
  instanceTasks, auditLogs, menuTree, permissionMenuTree, menuManageTree,
  checkedMenuIds, fields, forms, dbTables, dbColumns,
  // Navigation
  navigationTree, activeTopCode, activeRouteKey, sectionTitle, sectionSubtitle,
  // Filters & Pagination
  todoFilters, doneFilters, todoPagination, donePagination,
  userFilters, roleFilters, workflowFilters, userPagination, rolePagination,
  workflowPagination, formPagination, fieldPagination,
  documentPagination, instancePagination, chunkPagination,
  formFilters, knowledgeConfig, knowledgeDocFilters, chunkFilters,
  // Handlers
  handleSelectTopMenu, handleSideNavigate, handleUserChange,
  jumpFirstSearchResult, openGlobalSearchResult,
  // Data loading
  refreshAll, refreshUserScopedData,
  loadUsers, loadUserPage, loadRoles, loadRolePage,
  loadWorkflows, loadWorkflowPage, loadInstances, loadInstancePage,
  loadTodos, loadTodoTasks, loadDoneTasks,
  loadNotifications, loadDocuments, loadDocumentPage,
  loadMenus, loadFields, loadFieldPage, loadForms, loadFormPage,
  loadKnowledgeConfig, loadDocumentChunks, loadChunkPage,
  loadInstanceTasks, loadAuditLogs,
  loadDbTables, loadDbColumns, loadMenuTree, loadPermissionMenuTree, loadAllMenuTree,
  loadRoleMenus, loadRuntimeFormForInstance, loadRuntimeFormForTask, loadStartForm,
  // Mutations
  createUser, updateUser, updateUserRoles, createRole, updateRole,
  createMenu, updateMenu, disableMenu, enableMenu, saveRoleMenus,
  createWorkflow, updateWorkflow, publishWorkflow, disableWorkflow,
  createField, updateField, enableField, disableField, importDbFields,
  createForm, updateForm, deleteForm, publishForm, disableForm,
  uploadDocument, deleteDocument, saveKnowledgeConfig,
  completeTask, riskCheck, askRag, markRead,
  showLoading, hideLoading,
})
</script>
