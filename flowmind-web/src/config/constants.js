import {
  Bell, Check, Checked, CircleClose, Close, Collection,
  Cpu, DataAnalysis, Delete, DocumentChecked, Guide,
  Operation, Plus, Promotion, Refresh, Search, Share,
  SwitchButton, UploadFilled, User,
} from '@element-plus/icons-vue'

export const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

export const fieldTypeOptions = [
  { label: '单行文本', value: 'TEXT' },
  { label: '多行文本', value: 'TEXTAREA' },
  { label: '数字', value: 'NUMBER' },
  { label: '金额', value: 'AMOUNT' },
  { label: '日期', value: 'DATE' },
  { label: '日期时间', value: 'DATETIME' },
  { label: '下拉选择', value: 'SELECT' },
  { label: '开关', value: 'BOOLEAN' },
]

export const controlTypeOptions = [
  ...fieldTypeOptions,
  { label: '附件上传', value: 'FILE' },
  { label: '明细表格', value: 'TABLE' },
  { label: '说明文本', value: 'INFO' },
]

export const controlPalette = [
  { label: '单行文本', componentType: 'TEXT', fieldKeyPrefix: 'text', placeholder: '请输入内容' },
  { label: '金额', componentType: 'AMOUNT', fieldKeyPrefix: 'amount', placeholder: '请输入金额', required: true },
  { label: '日期', componentType: 'DATE', fieldKeyPrefix: 'date', placeholder: '请选择日期' },
  { label: '下拉框', componentType: 'SELECT', fieldKeyPrefix: 'select', placeholder: '请选择', optionsJson: '[{"label":"选项一","value":"A"},{"label":"选项二","value":"B"}]' },
  { label: '附件上传', componentType: 'FILE', fieldKeyPrefix: 'attachment', placeholder: '上传制度附件、合同或发票' },
  { label: '明细表格', componentType: 'TABLE', fieldKeyPrefix: 'detail', placeholder: '维护多行明细数据', span: 24 },
  { label: '说明文本', componentType: 'INFO', fieldKeyPrefix: 'notice', placeholder: '请根据企业制度填写真实、完整的信息。', span: 24 },
]

export const sourceTypeOptions = [
  { label: '自定义字段', value: 'CUSTOM' },
  { label: '数据库字段', value: 'DB_COLUMN' },
  { label: '系统字段', value: 'SYSTEM' },
]

export const fieldPermissionOptions = [
  { label: '可编辑', value: 'EDITABLE' },
  { label: '只读', value: 'READONLY' },
  { label: '隐藏', value: 'HIDDEN' },
]

export const nodePalette = [
  { type: 'START', label: '开始节点', icon: Guide },
  { type: 'FORM_TASK', label: '表单任务', icon: DocumentChecked },
  { type: 'AI_RISK_CHECK', label: 'AI 风险检测', icon: Cpu },
  { type: 'AI_APPROVAL', label: 'AI 审批节点', icon: Cpu },
  { type: 'CONDITION', label: '条件路由', icon: Share },
  { type: 'APPROVAL', label: '人工审批', icon: Checked },
  { type: 'NOTIFY', label: '通知节点', icon: Bell },
  { type: 'END', label: '结束节点', icon: CircleClose },
]

export const menuIconMap = {
  dashboard: DataAnalysis, overview: DataAnalysis, instanceTrace: Operation,
  design: Share, workflowList: Share, workflowDesigner: Share,
  formList: Operation, fieldList: DocumentChecked, formDesigner: Operation,
  approval: Checked, todo: Checked, done: Check,
  startApproval: Promotion, runtimeForm: DocumentChecked,
  knowledge: Collection, documents: Collection, chunks: DocumentChecked,
  ragTest: Search, kbConfig: Operation,
  user: User, profile: User, roleList: Checked, userList: User, menuPermission: Operation,
}

export const sectionTitleMap = {
  dashboard: '运行总览', workflowList: '流程管理', designer: '流程设计',
  fields: '表单字段', formList: '表单管理', forms: '表单设计',
  launch: '发起审批', todo: '待审批', done: '已审批',
  runtimeForm: '审批表单运行态', instances: '实例追踪',
  myApplications: '我的申请',
  documents: '制度文档', chunks: 'Chunk 查看', ragTest: 'RAG 测试',
  kbConfig: '知识库配置', profile: '个人信息',
  roleList: '角色管理', userList: '用户管理', menuPermission: '菜单管理',
}

export const menuTreeProps = {
  label: 'menuName',
  children: 'children',
}