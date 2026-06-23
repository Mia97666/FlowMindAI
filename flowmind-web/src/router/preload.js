import { api } from '../api/index'
import { routeComponentLoaders } from './routeComponents'

const NAVIGATION_REQUEST_OPTIONS = {
  loadingDelayMs: 0,
  timeoutMs: 10000,
}

function pageQuery(page = 1, size = 10, filters = {}) {
  const query = new URLSearchParams({ page: String(page - 1), size: String(size) })
  Object.entries(filters).forEach(([key, value]) => {
    if (value) query.set(key, value)
  })
  return query.toString()
}

function userPageQuery(user, page = 1, size = 10, filters = {}) {
  const query = new URLSearchParams({ assignee: user || 'admin', page: String(page - 1), size: String(size) })
  Object.entries(filters).forEach(([key, value]) => {
    if (value) query.set(key, value)
  })
  return query.toString()
}

function load(path) {
  return api(path, NAVIGATION_REQUEST_OPTIONS)
}

const routeDataLoaders = {
  overview: ({ currentUser }) => Promise.allSettled([
    load('/api/workflow-instances'),
    load(`/api/workflow-tasks/todo?assignee=${encodeURIComponent(currentUser || 'admin')}`),
    load(`/api/notifications?receiver=${encodeURIComponent(currentUser || 'admin')}`),
  ]),
  instanceTrace: () => load(`/api/workflow-instances/page?${pageQuery()}`),
  workflowList: () => load(`/api/workflows/page?${pageQuery()}`),
  workflowDesigner: () => load('/api/workflows'),
  fieldList: () => load(`/api/fields/page?${pageQuery()}`),
  formList: () => load(`/api/forms/page?${pageQuery()}`),
  formDesigner: () => Promise.allSettled([
    load('/api/forms'),
    load('/api/fields'),
  ]),
  startApproval: () => Promise.allSettled([
    load('/api/workflows'),
    load('/api/users'),
  ]),
  todo: ({ currentUser }) => load(`/api/workflow-tasks/todo/page?${userPageQuery(currentUser)}`),
  done: ({ currentUser }) => load(`/api/workflow-tasks/done/page?${userPageQuery(currentUser)}`),
  myApplications: ({ currentUser }) => {
    const query = new URLSearchParams({ starter: currentUser || 'admin', page: '0', size: '10' })
    return load(`/api/workflow-instances/my?${query.toString()}`)
  },
  documents: () => load(`/api/docs/page?${pageQuery()}`),
  chunks: () => load('/api/docs'),
  kbConfig: () => load('/api/knowledge/config'),
  userList: () => load(`/api/users/page?${pageQuery()}`),
  roleList: () => load(`/api/roles/page?${pageQuery()}`),
  menuPermission: () => Promise.allSettled([
    load('/api/menus/tree'),
    load('/api/menus/all-tree'),
    load('/api/roles'),
  ]),
}

export function preloadRoute(routeKey, context = {}) {
  const loaders = []
  const componentLoader = routeComponentLoaders[routeKey]
  const dataLoader = routeDataLoaders[routeKey]

  if (componentLoader) loaders.push(componentLoader())
  if (dataLoader) loaders.push(dataLoader(context))

  if (loaders.length === 0) return Promise.resolve()
  return Promise.allSettled(loaders)
}
