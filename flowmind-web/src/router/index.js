import { createRouter, createWebHashHistory } from 'vue-router'
import { routeComponentLoaders } from './routeComponents'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', name: 'dashboard', component: routeComponentLoaders.overview, meta: { topCode: 'dashboard', routeKey: 'overview' } },
  { path: '/instances', name: 'instanceTrace', component: routeComponentLoaders.instanceTrace, meta: { topCode: 'dashboard', routeKey: 'instanceTrace' } },
  { path: '/workflows', name: 'workflowList', component: routeComponentLoaders.workflowList, meta: { topCode: 'design', routeKey: 'workflowList' } },
  { path: '/workflows/designer/:id?', name: 'workflowDesigner', component: routeComponentLoaders.workflowDesigner, meta: { topCode: 'design', routeKey: 'workflowDesigner' } },
  { path: '/fields', name: 'fieldList', component: routeComponentLoaders.fieldList, meta: { topCode: 'design', routeKey: 'fieldList' } },
  { path: '/forms', name: 'formList', component: routeComponentLoaders.formList, meta: { topCode: 'design', routeKey: 'formList' } },
  { path: '/forms/designer/:id?', name: 'formDesigner', component: routeComponentLoaders.formDesigner, meta: { topCode: 'design', routeKey: 'formDesigner' } },
  { path: '/approval/start', name: 'startApproval', component: routeComponentLoaders.startApproval, meta: { topCode: 'approval', routeKey: 'startApproval' } },
  { path: '/approval/todo', name: 'todo', component: routeComponentLoaders.todo, meta: { topCode: 'approval', routeKey: 'todo' } },
  { path: '/approval/done', name: 'done', component: routeComponentLoaders.done, meta: { topCode: 'approval', routeKey: 'done' } },
  { path: '/approval/runtime', name: 'runtimeForm', component: routeComponentLoaders.runtimeForm, meta: { topCode: 'approval', routeKey: 'runtimeForm' } },
  { path: '/approval/my-applications', name: 'myApplications', component: routeComponentLoaders.myApplications, meta: { topCode: 'approval', routeKey: 'myApplications' } },
  { path: '/knowledge/documents', name: 'documents', component: routeComponentLoaders.documents, meta: { topCode: 'knowledge', routeKey: 'documents' } },
  { path: '/knowledge/chunks', name: 'chunks', component: routeComponentLoaders.chunks, meta: { topCode: 'knowledge', routeKey: 'chunks' } },
  { path: '/knowledge/rag-test', name: 'ragTest', component: routeComponentLoaders.ragTest, meta: { topCode: 'knowledge', routeKey: 'ragTest' } },
  { path: '/knowledge/config', name: 'kbConfig', component: routeComponentLoaders.kbConfig, meta: { topCode: 'knowledge', routeKey: 'kbConfig' } },
  { path: '/profile', name: 'profile', component: routeComponentLoaders.profile, meta: { topCode: 'user', routeKey: 'profile' } },
  { path: '/users', name: 'userList', component: routeComponentLoaders.userList, meta: { topCode: 'user', routeKey: 'userList' } },
  { path: '/roles', name: 'roleList', component: routeComponentLoaders.roleList, meta: { topCode: 'user', routeKey: 'roleList' } },
  { path: '/menus', name: 'menuPermission', component: routeComponentLoaders.menuPermission, meta: { topCode: 'user', routeKey: 'menuPermission' } },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

export default router
