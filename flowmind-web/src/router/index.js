import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', name: 'dashboard', component: () => import('../pages/Dashboard.vue'), meta: { topCode: 'dashboard', routeKey: 'overview' } },
  { path: '/instances', name: 'instanceTrace', component: () => import('../pages/InstanceTrace.vue'), meta: { topCode: 'dashboard', routeKey: 'instanceTrace' } },
  { path: '/workflows', name: 'workflowList', component: () => import('../pages/WorkflowList.vue'), meta: { topCode: 'design', routeKey: 'workflowList' } },
  { path: '/workflows/designer/:id?', name: 'workflowDesigner', component: () => import('../pages/WorkflowDesigner.vue'), meta: { topCode: 'design', routeKey: 'workflowDesigner' } },
  { path: '/fields', name: 'fieldList', component: () => import('../pages/FieldList.vue'), meta: { topCode: 'design', routeKey: 'fieldList' } },
  { path: '/forms', name: 'formList', component: () => import('../pages/FormList.vue'), meta: { topCode: 'design', routeKey: 'formList' } },
  { path: '/forms/designer/:id?', name: 'formDesigner', component: () => import('../pages/FormDesigner.vue'), meta: { topCode: 'design', routeKey: 'formDesigner' } },
  { path: '/approval/start', name: 'startApproval', component: () => import('../pages/StartApproval.vue'), meta: { topCode: 'approval', routeKey: 'startApproval' } },
  { path: '/approval/todo', name: 'todo', component: () => import('../pages/TodoList.vue'), meta: { topCode: 'approval', routeKey: 'todo' } },
  { path: '/approval/done', name: 'done', component: () => import('../pages/DoneList.vue'), meta: { topCode: 'approval', routeKey: 'done' } },
  { path: '/approval/runtime', name: 'runtimeForm', component: () => import('../pages/RuntimeForm.vue'), meta: { topCode: 'approval', routeKey: 'runtimeForm' } },
  { path: '/approval/my-applications', name: 'myApplications', component: () => import('../pages/MyApplications.vue'), meta: { topCode: 'approval', routeKey: 'myApplications' } },
  { path: '/knowledge/documents', name: 'documents', component: () => import('../pages/Documents.vue'), meta: { topCode: 'knowledge', routeKey: 'documents' } },
  { path: '/knowledge/chunks', name: 'chunks', component: () => import('../pages/Chunks.vue'), meta: { topCode: 'knowledge', routeKey: 'chunks' } },
  { path: '/knowledge/rag-test', name: 'ragTest', component: () => import('../pages/RagTest.vue'), meta: { topCode: 'knowledge', routeKey: 'ragTest' } },
  { path: '/knowledge/config', name: 'kbConfig', component: () => import('../pages/KbConfig.vue'), meta: { topCode: 'knowledge', routeKey: 'kbConfig' } },
  { path: '/profile', name: 'profile', component: () => import('../pages/Profile.vue'), meta: { topCode: 'user', routeKey: 'profile' } },
  { path: '/users', name: 'userList', component: () => import('../pages/UserList.vue'), meta: { topCode: 'user', routeKey: 'userList' } },
  { path: '/roles', name: 'roleList', component: () => import('../pages/RoleList.vue'), meta: { topCode: 'user', routeKey: 'roleList' } },
  { path: '/menus', name: 'menuPermission', component: () => import('../pages/MenuPermission.vue'), meta: { topCode: 'user', routeKey: 'menuPermission' } },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

export default router