export const fallbackMenuTree = [
  {
    menuCode: 'dashboard', menuName: '运行总览', menuType: 'TOP',
    routeKey: 'dashboard', sortOrder: 10,
    children: [
      { menuCode: 'dashboard.overview', menuName: '运行总览', menuType: 'PAGE', routeKey: 'overview', sortOrder: 11 },
      { menuCode: 'dashboard.instances', menuName: '实例追踪', menuType: 'PAGE', routeKey: 'instanceTrace', sortOrder: 12 },
    ],
  },
  {
    menuCode: 'design', menuName: '流程设计', menuType: 'TOP',
    routeKey: 'design', sortOrder: 20,
    children: [
      {
        menuCode: 'design.workflow', menuName: '流程管理', menuType: 'GROUP',
        routeKey: 'workflowList', sortOrder: 21,
        children: [
          { menuCode: 'design.workflow.designer', menuName: '工作流设计', menuType: 'PAGE', routeKey: 'workflowDesigner', sortOrder: 22 },
        ],
      },
      {
        menuCode: 'design.form', menuName: '表单管理', menuType: 'GROUP',
        routeKey: 'formList', sortOrder: 23,
        children: [
          { menuCode: 'design.form.field', menuName: '表单字段管理', menuType: 'PAGE', routeKey: 'fieldList', sortOrder: 24 },
          { menuCode: 'design.form.designer', menuName: '表单设计', menuType: 'PAGE', routeKey: 'formDesigner', sortOrder: 25 },
        ],
      },
    ],
  },
  {
    menuCode: 'approval', menuName: '审批', menuType: 'TOP',
    routeKey: 'approval', sortOrder: 30,
    children: [
      { menuCode: 'approval.todo', menuName: '待审批', menuType: 'PAGE', routeKey: 'todo', sortOrder: 31 },
      { menuCode: 'approval.done', menuName: '已审批', menuType: 'PAGE', routeKey: 'done', sortOrder: 32 },
      { menuCode: 'approval.start', menuName: '发起审批', menuType: 'PAGE', routeKey: 'startApproval', sortOrder: 33 },
      { menuCode: 'approval.runtime', menuName: '审批表单运行态', menuType: 'PAGE', routeKey: 'runtimeForm', sortOrder: 34 },
      { menuCode: 'approval.myApplications', menuName: '我的申请', menuType: 'PAGE', routeKey: 'myApplications', sortOrder: 35 },
    ],
  },
  {
    menuCode: 'knowledge', menuName: '知识库', menuType: 'TOP',
    routeKey: 'knowledge', sortOrder: 40,
    children: [
      { menuCode: 'knowledge.documents', menuName: '制度文档', menuType: 'PAGE', routeKey: 'documents', sortOrder: 41 },
      { menuCode: 'knowledge.chunks', menuName: 'Chunk 查看', menuType: 'PAGE', routeKey: 'chunks', sortOrder: 42 },
      { menuCode: 'knowledge.rag', menuName: 'RAG 测试', menuType: 'PAGE', routeKey: 'ragTest', sortOrder: 43 },
      { menuCode: 'knowledge.config', menuName: '知识库配置', menuType: 'PAGE', routeKey: 'kbConfig', sortOrder: 44 },
    ],
  },
  {
    menuCode: 'user', menuName: '用户管理', menuType: 'TOP',
    routeKey: 'user', sortOrder: 50,
    children: [
      { menuCode: 'user.profile', menuName: '个人信息', menuType: 'PAGE', routeKey: 'profile', sortOrder: 51 },
      { menuCode: 'user.role', menuName: '角色管理', menuType: 'PAGE', routeKey: 'roleList', sortOrder: 52 },
      { menuCode: 'user.list', menuName: '用户管理', menuType: 'PAGE', routeKey: 'userList', sortOrder: 53 },
      { menuCode: 'user.menu', menuName: '菜单管理', menuType: 'PAGE', routeKey: 'menuPermission', sortOrder: 54 },
    ],
  },
]

export const routeSectionMap = {
  overview: { section: 'dashboard' },
  instanceTrace: { section: 'instances' },
  workflowDesigner: { section: 'designer' },
  workflowList: { section: 'workflowList' },
  fieldList: { section: 'fields' },
  formDesigner: { section: 'forms' },
  formList: { section: 'formList' },
  todo: { section: 'todo' },
  done: { section: 'done' },
  startApproval: { section: 'launch' },
  runtimeForm: { section: 'runtimeForm' },
  myApplications: { section: 'myApplications' },
  documents: { section: 'documents' },
  chunks: { section: 'chunks' },
  ragTest: { section: 'ragTest' },
  kbConfig: { section: 'kbConfig' },
  profile: { section: 'profile' },
  roleList: { section: 'roleList' },
  userList: { section: 'userList' },
  menuPermission: { section: 'menuPermission' },
}