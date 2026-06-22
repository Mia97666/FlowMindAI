<template>
  <section class="designer-layout">
    <div class="designer-toolbar">
      <div class="toolbar-primary">
        <el-button @click="newWorkflow">+ 默认流程</el-button>
        <el-button type="primary" :icon="DocumentChecked" @click="saveWorkflow">保存</el-button>
        <el-button :icon="Operation" @click="validateWorkflow">发布前校验</el-button>
        <el-button :icon="SwitchButton" @click="publishWorkflowAction" :disabled="!selectedWorkflowId">发布</el-button>
        <el-button :icon="CircleClose" @click="disableWorkflowAction" :disabled="!selectedWorkflowId">停用</el-button>
        <el-button :icon="Document" @click="openBpmnPreview">BPMN 预览</el-button>
        <el-button :icon="Upload" @click="bpmnImportVisible = true">导入 BPMN</el-button>
      </div>
    </div>

    <div class="designer-main">
      <el-card class="palette-panel" shadow="never">
        <template #header>节点库</template>
        <button v-for="item in nodePalette" :key="item.type" class="palette-item" @click="addNode(item)">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </button>
      </el-card>

      <div class="canvas-panel">
        <VueFlow
          v-model:nodes="flowNodes"
          v-model:edges="flowEdges"
          class="workflow-canvas"
          :default-viewport="{ zoom: 0.92 }"
          :fit-view-on-init="true"
          @node-click="onNodeClick"
          @connect="onConnect"
        >
          <Background pattern-color="#c9d3e5" :gap="18" />
          <Controls position="bottom-right" />
          <template #node-custom="{ id, data, selected }">
            <div class="flow-node" :class="[nodeClass(data.nodeType), { selected }]" @click="selectedNodeId = id">
              <Handle
                v-if="hasTargetHandle(data.nodeType)"
                class="node-handle node-handle-target"
                type="target"
                :position="Position.Left"
              />
              <div class="node-icon">
                <el-icon><component :is="nodeIcon(data.nodeType)" /></el-icon>
              </div>
              <div>
                <strong>{{ data.label }}</strong>
                <span>{{ nodeTypeLabel(data.nodeType) }}</span>
              </div>
              <Handle
                v-if="hasSourceHandle(data.nodeType)"
                class="node-handle node-handle-source"
                type="source"
                :position="Position.Right"
              />
            </div>
          </template>
        </VueFlow>
      </div>

      <el-card class="config-panel" shadow="never">
        <template #header>属性面板</template>
        <el-form label-position="top" class="compact-form">
          <el-form-item label="流程名称">
            <el-input v-model="workflowForm.name" placeholder="例如 采购审批流程" />
          </el-form-item>
          <el-form-item label="流程编码">
            <el-input v-model="workflowForm.code" placeholder="例如 PURCHASE_APPROVAL" />
          </el-form-item>
          <el-form-item label="流程描述">
            <el-input v-model="workflowForm.description" type="textarea" :rows="2" placeholder="说明适用场景和审批边界" />
          </el-form-item>
        </el-form>
        <el-divider content-position="left">设计校验</el-divider>
        <div class="workflow-summary">
          <div><strong>{{ flowNodes.length }}</strong><span>节点</span></div>
          <div><strong>{{ flowEdges.length }}</strong><span>连线</span></div>
          <div><strong>{{ forms.length }}</strong><span>可绑定表单</span></div>
        </div>
        <el-alert
          class="runtime-form-tip"
          type="info"
          show-icon
          :closable="false"
          title="节点属性已移到画布下方；点击节点后可配置审批人、AI策略、网关条件和字段权限。"
        />
      </el-card>
    </div>

    <el-card class="designer-bottom" shadow="never">
      <el-tabs v-model="designerActiveTab">
        <el-tab-pane label="节点属性" name="node">
          <div v-if="selectedNode" class="node-config node-config-wide">
            <div class="node-config-title">
              <strong>{{ selectedNode.data.label }} · {{ nodeTypeLabel(selectedNode.data.nodeType) }}</strong>
              <el-button text type="danger" :icon="Delete" @click="removeSelectedNode">删除节点</el-button>
            </div>
            <el-form label-position="top" class="node-property-grid">
              <el-form-item label="节点名称">
                <el-input v-model="selectedNode.data.label" />
              </el-form-item>
              <el-form-item label="节点类型">
                <el-select :model-value="selectedNode.data.nodeType" @change="(value) => confirmNodeTypeChange(selectedNode, value)">
                  <el-option v-for="item in nodePalette" :key="item.type" :label="item.label" :value="item.type" />
                </el-select>
              </el-form-item>
              <el-form-item label="节点 Key">
                <el-input :model-value="selectedNode.id" disabled />
              </el-form-item>
              <el-form-item v-if="nodeNeedsForm(selectedNode.data.nodeType)" label="绑定表单">
                <el-select v-model="selectedNode.data.config.formCode" filterable clearable placeholder="请选择节点处理表单" @change="onSelectedNodeFormChange">
                  <el-option v-for="form in forms" :key="form.formCode" :label="`${form.formName}（${form.formCode}）`" :value="form.formCode" />
                </el-select>
              </el-form-item>

              <template v-if="['APPROVAL', 'AI_APPROVAL', 'FORM_TASK'].includes(selectedNode.data.nodeType)">
                <el-form-item label="审批人类型">
                  <el-select v-model="selectedNode.data.config.assigneeType">
                    <el-option label="指定用户" value="USER" />
                    <el-option label="指定角色" value="ROLE" />
                    <el-option label="直属领导" value="MANAGER" />
                    <el-option label="部门负责人" value="DEPARTMENT_MANAGER" />
                    <el-option label="表单字段" value="FORM_FIELD" />
                  </el-select>
                </el-form-item>
                <el-form-item label="审批人/角色/字段值">
                  <el-select v-if="selectedNode.data.config.assigneeType === 'USER'" v-model="selectedNode.data.config.assigneeValue" filterable placeholder="请选择审批用户">
                    <el-option v-for="user in users" :key="user.username" :label="`${user.realName}（${user.username}）`" :value="user.username" />
                  </el-select>
                  <el-select v-else-if="selectedNode.data.config.assigneeType === 'ROLE'" v-model="selectedNode.data.config.assigneeValue" filterable placeholder="请选择审批角色">
                    <el-option v-for="role in roles" :key="role.code" :label="`${role.name}（${role.code}）`" :value="role.code" />
                  </el-select>
                  <el-select v-else-if="selectedNode.data.config.assigneeType === 'FORM_FIELD'" v-model="selectedNode.data.config.assigneeValue" filterable placeholder="请选择保存审批人的表单字段">
                    <el-option v-for="field in selectedNodeFormFields" :key="field.fieldKey" :label="`${field.label}（${field.fieldKey}）`" :value="field.fieldKey" />
                  </el-select>
                  <el-input v-else model-value="系统将自动解析审批人" disabled />
                </el-form-item>
                <el-form-item label="允许动作">
                  <el-input v-model="selectedNode.data.config.allowedActions" placeholder="同意、拒绝、退回、转办" />
                </el-form-item>
              </template>

              <template v-if="['AI_RISK_CHECK', 'AI_APPROVAL'].includes(selectedNode.data.nodeType)">
                <el-form-item label="AI 策略">
                  <el-input v-model="selectedNode.data.config.aiStrategy" placeholder="采购风险评分策略 V3" />
                </el-form-item>
                <el-form-item label="RAG 检索范围">
                  <el-input v-model="selectedNode.data.config.ragScope" placeholder="采购制度、财务制度" />
                </el-form-item>
                <el-form-item label="TopK">
                  <el-input-number v-model="selectedNode.data.config.topK" :min="1" :max="20" />
                </el-form-item>
                <el-form-item label="风险阈值">
                  <el-slider v-model="selectedNode.data.config.threshold" :min="0" :max="100" show-input />
                </el-form-item>
                <el-form-item label="自动通过最高分">
                  <el-input-number v-model="selectedNode.data.config.autoApproveMaxScore" :min="0" :max="100" />
                </el-form-item>
                <el-form-item label="自动审批人">
                  <el-input v-model="selectedNode.data.config.autoApproveActor" placeholder="例如 ai_approver" />
                </el-form-item>
                <el-form-item label="输出字段">
                  <el-input v-model="selectedNode.data.config.outputFields" placeholder="riskScore, riskLevel, riskReason" />
                </el-form-item>
                <el-form-item label="高风险通知人">
                  <el-input v-model="selectedNode.data.config.highRiskReceivers" placeholder="例如 finance,admin" />
                </el-form-item>
                <el-form-item class="node-property-wide" label="风险依据提示词">
                  <el-input v-model="selectedNode.data.config.prompt" type="textarea" :rows="3" />
                </el-form-item>
              </template>

              <template v-if="selectedNode.data.nodeType === 'CONDITION'">
                <el-form-item label="高风险条件">
                  <el-input v-model="selectedNode.data.config.highRiskCondition" placeholder="riskScore >= 70" />
                </el-form-item>
                <el-form-item label="默认路由">
                  <el-input v-model="selectedNode.data.config.defaultRoute" placeholder="manager_approve" />
                </el-form-item>
                <el-form-item label="表达式语言">
                  <el-select v-model="selectedNode.data.config.expressionLanguage">
                    <el-option label="SpEL" value="SpEL" />
                    <el-option label="JavaScript" value="JS" />
                    <el-option label="简单表达式" value="SIMPLE" />
                  </el-select>
                </el-form-item>
              </template>

              <template v-if="selectedNode.data.nodeType === 'NOTIFY'">
                <el-form-item label="接收人">
                  <el-input v-model="selectedNode.data.config.receivers" placeholder="starter,finance,admin" />
                </el-form-item>
                <el-form-item label="通知渠道">
                  <el-input v-model="selectedNode.data.config.channels" placeholder="站内信、邮件" />
                </el-form-item>
                <el-form-item label="模板编码">
                  <el-input v-model="selectedNode.data.config.templateCode" placeholder="APPROVAL_DONE_NOTICE" />
                </el-form-item>
                <el-form-item label="通知标题">
                  <el-input v-model="selectedNode.data.config.title" />
                </el-form-item>
                <el-form-item class="node-property-wide" label="通知内容">
                  <el-input v-model="selectedNode.data.config.content" type="textarea" :rows="3" />
                </el-form-item>
              </template>

              <template v-if="selectedNode.data.nodeType === 'END'">
                <el-form-item label="结束状态">
                  <el-select v-model="selectedNode.data.config.endStatus">
                    <el-option label="已完成" value="COMPLETED" />
                    <el-option label="已拒绝" value="REJECTED" />
                    <el-option label="已取消" value="CANCELED" />
                  </el-select>
                </el-form-item>
                <el-form-item label="归档策略">
                  <el-input v-model="selectedNode.data.config.archiveStrategy" placeholder="写入流程日志和表单快照" />
                </el-form-item>
              </template>
            </el-form>

            <div v-if="nodeAllowsFieldPermission(selectedNode.data.nodeType)" class="node-field-permission">
              <el-divider content-position="left">字段权限</el-divider>
              <el-alert
                v-if="!selectedNode.data.config.formCode"
                type="info"
                show-icon
                :closable="false"
                title="先绑定表单后，可配置该节点的字段可编辑、只读或隐藏。"
              />
              <el-table v-else :data="selectedNodeFormFields" size="small" max-height="260" empty-text="当前表单暂无字段">
                <el-table-column prop="label" label="字段" min-width="180" show-overflow-tooltip />
                <el-table-column prop="fieldKey" label="字段 Key" min-width="150" show-overflow-tooltip />
                <el-table-column label="权限" width="160">
                  <template #default="{ row }">
                    <el-select v-model="selectedNode.data.config.fieldPermissions[row.fieldKey]" size="small">
                      <el-option v-for="option in fieldPermissionOptions" :key="option.value" :label="option.label" :value="option.value" />
                    </el-select>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
          <el-empty v-else description="请选择画布节点" />
        </el-tab-pane>
        <el-tab-pane label="动态表单" name="form">
          <div class="table-toolbar">
            <el-button :icon="Plus" @click="addFormField">添加字段</el-button>
          </div>
          <el-table :data="formFields" row-key="key" empty-text="暂无表单字段">
            <el-table-column label="字段Key" min-width="150">
              <template #default="{ row }"><el-input v-model="row.key" /></template>
            </el-table-column>
            <el-table-column label="字段名称" min-width="160">
              <template #default="{ row }"><el-input v-model="row.label" /></template>
            </el-table-column>
            <el-table-column label="类型" width="150">
              <template #default="{ row }">
                <el-select v-model="row.type">
                  <el-option label="文本" value="TEXT" />
                  <el-option label="数字" value="NUMBER" />
                  <el-option label="金额" value="AMOUNT" />
                  <el-option label="日期" value="DATE" />
                  <el-option label="下拉" value="SELECT" />
                  <el-option label="多行文本" value="TEXTAREA" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="必填" width="90">
              <template #default="{ row }"><el-switch v-model="row.required" /></template>
            </el-table-column>
            <el-table-column label="提示" min-width="220">
              <template #default="{ row }"><el-input v-model="row.placeholder" /></template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="{ $index }">
                <el-button text type="danger" :icon="Delete" @click="formFields.splice($index, 1)" />
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="条件连线" name="edges">
          <el-table :data="flowEdges" row-key="id" empty-text="暂无连线">
            <el-table-column prop="source" label="来源" width="170" />
            <el-table-column prop="target" label="目标" width="170" />
            <el-table-column label="条件表达式" min-width="240">
              <template #default="{ row }">
                <el-input v-model="row.data.condition" placeholder="riskScore >= 70 或 default" />
              </template>
            </el-table-column>
            <el-table-column label="优先级" width="120">
              <template #default="{ row }">
                <el-input-number v-model="row.data.priority" :min="0" :max="999" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column label="标签" min-width="160">
              <template #default="{ row }">
                <el-input v-model="row.label" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button text type="danger" :icon="Delete" @click="removeEdge(row.id)" />
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </section>

  <el-dialog v-model="bpmnPreviewVisible" title="BPMN 2.0 预览" width="720px">
    <el-input
      v-model="bpmnXml"
      type="textarea"
      :rows="18"
      readonly
      class="bpmn-code"
    />
    <template #footer>
      <el-button @click="copyBpmnXml">复制</el-button>
      <el-button @click="bpmnPreviewVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="bpmnImportVisible" title="导入 BPMN 2.0 XML" width="720px">
    <el-alert
      type="info"
      show-icon
      :closable="false"
      title="支持 BPMN 2.0 基础元素：开始事件、结束事件、用户任务、服务任务、排他网关、顺序流。"
      class="bpmn-import-tip"
    />
    <el-input
      v-model="bpmnImportXml"
      type="textarea"
      :rows="18"
      placeholder="在此粘贴 BPMN 2.0 XML..."
      class="bpmn-code"
    />
    <template #footer>
      <el-button type="primary" @click="parseBpmnImport" :loading="bpmnParsing">解析并导入</el-button>
      <el-button @click="bpmnImportVisible = false">取消</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VueFlow, MarkerType, Handle, Position } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import {
  CircleClose, Delete, Document, DocumentChecked, Operation, Plus, Share, SwitchButton, Upload,
} from '@element-plus/icons-vue'
import { useSharedState } from '../composables/useSharedState'
import { nodePalette, fieldPermissionOptions } from '../config/constants'
import { parseFormSchema, parseJson, nodeTypeLabel, nodeClass, hasSourceHandle, hasTargetHandle, nodeNeedsForm, nodeAllowsFieldPermission } from '../utils/helpers'
import { buildBpmnXml, parseBpmnXml } from '../utils/workflowBpmn'
import { loadWorkflows as fetchWorkflows, createWorkflow, updateWorkflow, publishWorkflow, disableWorkflow } from '../api/workflow'

const route = useRoute()
const { workflows, users, roles, forms, fields } = useSharedState()

const selectedWorkflowId = ref(null)
const selectedNodeId = ref(null)
const designerActiveTab = ref('node')
const flowNodes = ref([])
const flowEdges = ref([])
const formFields = ref([])

const workflowForm = reactive({ id: null, code: '', name: '', description: '', version: 1, status: 'PUBLISHED' })

const quickEdge = reactive({ source: '', target: '', condition: '' })

const bpmnPreviewVisible = ref(false)
const bpmnImportVisible = ref(false)
const bpmnXml = ref('')
const bpmnImportXml = ref('')
const bpmnParsing = ref(false)

const selectedNode = computed(() => flowNodes.value.find((node) => node.id === selectedNodeId.value))

const connectableSourceNodes = computed(() => flowNodes.value.filter((node) => hasSourceHandle(node.data.nodeType)))
const connectableTargetNodes = computed(() => flowNodes.value.filter((node) => hasTargetHandle(node.data.nodeType) && node.id !== quickEdge.source))

const selectedNodeFormFields = computed(() => {
  const formCode = selectedNode.value?.data?.config?.formCode
  const form = forms.value.find((item) => item.formCode === formCode)
  return form ? parseFormSchema(form.schemaJson) : []
})

function nodeIcon(type) {
  return nodePalette.find((item) => item.type === type)?.icon || Operation
}

// Default data
function defaultGraph() {
  return {
    nodes: [
      { id: 'start', nodeType: 'START', name: '开始', position: { x: 40, y: 170 }, config: { formCode: 'PURCHASE_APPLY_FORM' } },
      { id: 'ai_risk', nodeType: 'AI_RISK_CHECK', name: 'AI 风险检测', position: { x: 280, y: 170 }, config: { formCode: 'PURCHASE_APPLY_FORM', aiStrategy: '采购制度风险评分策略', threshold: 70, autoApproveMaxScore: 30, autoApproveActor: 'ai_approver', highRiskReceivers: 'finance,admin' } },
      { id: 'risk_gateway', nodeType: 'CONDITION', name: '风险等级路由', position: { x: 540, y: 170 }, config: { defaultRoute: 'manager_approve' } },
      { id: 'manager_approve', nodeType: 'APPROVAL', name: '中风险直属领导审批', position: { x: 800, y: 70 }, config: { formCode: 'PURCHASE_APPLY_FORM', assigneeType: 'MANAGER', assigneeValue: '' } },
      { id: 'finance_approve', nodeType: 'APPROVAL', name: '高风险财务审批', position: { x: 800, y: 270 }, config: { formCode: 'PURCHASE_APPLY_FORM', assigneeType: 'ROLE', assigneeValue: 'FINANCE' } },
      { id: 'notify_done', nodeType: 'NOTIFY', name: '结果通知', position: { x: 1060, y: 170 }, config: { receivers: 'starter', title: '审批流转提醒', content: '流程 {title} 已完成当前审批路径，风险等级：{riskLevel}。' } },
      { id: 'end', nodeType: 'END', name: '结束', position: { x: 1320, y: 170 }, config: {} },
    ],
    edges: [
      { id: 'e_start_ai', source: 'start', target: 'ai_risk' },
      { id: 'e_ai_gateway', source: 'ai_risk', target: 'risk_gateway' },
      { id: 'e_high', source: 'risk_gateway', target: 'finance_approve', condition: 'riskLevel == "HIGH"', label: '高风险人工审批' },
      { id: 'e_low_auto', source: 'risk_gateway', target: 'notify_done', condition: 'riskLevel == "LOW"', label: '低风险AI自动通过' },
      { id: 'e_medium', source: 'risk_gateway', target: 'manager_approve', condition: 'default', label: '中风险人工审批' },
      { id: 'e_finance_notify', source: 'finance_approve', target: 'notify_done' },
      { id: 'e_manager_notify', source: 'manager_approve', target: 'notify_done' },
      { id: 'e_notify_end', source: 'notify_done', target: 'end' },
    ],
  }
}

function defaultFormFields() {
  return [
    { key: 'item', label: '采购物品', type: 'TEXT', required: true, placeholder: '例如 MacBook Pro' },
    { key: 'quantity', label: '采购数量', type: 'NUMBER', required: true, placeholder: '例如 30' },
    { key: 'amount', label: '采购金额', type: 'AMOUNT', required: true, placeholder: '例如 450000' },
    { key: 'purpose', label: '采购用途', type: 'TEXTAREA', required: true, placeholder: '说明采购背景和业务必要性' },
    { key: 'expectedDate', label: '期望到货时间', type: 'DATE', required: false, placeholder: '选择日期' },
  ]
}

function defaultNodeConfig(type) {
  const configMap = {
    START: { formCode: 'PURCHASE_APPLY_FORM', fieldPermissions: {} },
    APPROVAL: { formCode: 'PURCHASE_APPLY_FORM', assigneeType: 'USER', assigneeValue: 'lisi', allowedActions: '同意,拒绝,退回,转办', fieldPermissions: {} },
    FORM_TASK: { formCode: 'PURCHASE_APPLY_FORM', assigneeType: 'USER', assigneeValue: '', allowedActions: '提交', fieldPermissions: {} },
    AI_RISK_CHECK: { formCode: 'PURCHASE_APPLY_FORM', aiStrategy: '采购风险评分策略 V3', ragScope: '采购制度,财务制度,合规制度', topK: 8, threshold: 70, autoApproveMaxScore: 30, autoApproveActor: 'ai_approver', outputFields: 'riskScore,riskLevel,riskReason,sources', highRiskReceivers: 'finance,admin', prompt: '结合企业制度条款、表单数据和历史审批记录，输出风险评分、风险等级、解释依据和来源引用；低风险可自动通过，中高风险转人工复核。', fieldPermissions: {} },
    AI_APPROVAL: { formCode: 'PURCHASE_APPLY_FORM', assigneeType: 'ROLE', assigneeValue: 'AI_REVIEWER', allowedActions: '自动通过,转人工复核,退回补充', aiStrategy: 'AI 审批辅助策略 V1', ragScope: '企业制度库', topK: 8, threshold: 70, autoApproveMaxScore: 30, autoApproveActor: 'ai_approver', outputFields: 'approvalSuggestion,riskScore,riskReason,sources', highRiskReceivers: 'finance,admin', prompt: '根据制度检索结果和表单内容给出审批建议，高风险或依据不足时转人工复核。', fieldPermissions: {} },
    CONDITION: { highRiskCondition: 'riskScore >= 70', defaultRoute: 'manager_approve', expressionLanguage: 'SIMPLE' },
    NOTIFY: { receivers: 'starter', channels: '站内信,邮件', templateCode: 'APPROVAL_FLOW_NOTICE', title: '审批通知', content: '流程 {title} 已流转，风险等级：{riskLevel}。' },
    END: { endStatus: 'COMPLETED', archiveStrategy: '写入流程日志、表单快照和 AI 审计记录' },
  }
  return { fieldPermissions: {}, ...(configMap[type] || {}) }
}

function normalizeNodeConfig(type, config = {}) {
  return {
    ...defaultNodeConfig(type),
    ...config,
    fieldPermissions: {
      ...(defaultNodeConfig(type).fieldPermissions || {}),
      ...(config.fieldPermissions || {}),
    },
  }
}

function normalizeEdge(edge) {
  const condition = edge.condition || edge.data?.condition || ''
  const priority = edge.priority ?? edge.data?.priority ?? 0
  return {
    id: edge.id || `edge_${edge.source}_${edge.target}`,
    source: edge.source,
    target: edge.target,
    type: 'smoothstep',
    label: edge.label || condition,
    animated: Boolean(condition && !['default', 'else', '默认'].includes(condition)),
    markerEnd: MarkerType.ArrowClosed,
    data: { condition, priority },
  }
}

function defaultPosition(index) {
  return { x: 80 + index * 210, y: 180 + (index % 2) * 120 }
}

function loadGraph(definitionJson) {
  const graph = parseJson(definitionJson, defaultGraph())
  flowNodes.value = (graph.nodes || []).map((node, index) => ({
    id: node.id, type: 'custom',
    position: node.position || defaultPosition(index),
    data: {
      label: node.name || node.label || node.id,
      nodeType: node.nodeType || node.type || 'APPROVAL',
      config: normalizeNodeConfig(node.nodeType || node.type || 'APPROVAL', node.config || {}),
    },
  }))
  flowEdges.value = (graph.edges || []).map((edge) => normalizeEdge(edge))
  resetQuickEdge()
}

function serializeGraph() {
  return {
    nodes: flowNodes.value.map((node) => ({
      id: node.id, nodeType: node.data.nodeType, name: node.data.label,
      config: node.data.config || {}, position: node.position,
    })),
    edges: flowEdges.value.map((edge) => ({
      id: edge.id, source: edge.source, target: edge.target,
      condition: edge.data?.condition || '', priority: edge.data?.priority ?? 0,
      label: edge.label || edge.data?.condition || '',
    })),
  }
}

function ensureNodeFormPermissions(node) {
  if (!node?.data) return
  node.data.config = normalizeNodeConfig(node.data.nodeType, node.data.config || {})
  const form = forms.value.find((item) => item.formCode === node.data.config.formCode)
  if (!form) return
  const permissions = node.data.config.fieldPermissions
  parseFormSchema(form.schemaJson).forEach((field) => {
    if (!permissions[field.fieldKey]) {
      permissions[field.fieldKey] = 'EDITABLE'
    }
  })
}

function onSelectedNodeFormChange() { ensureNodeFormPermissions(selectedNode.value) }

function selectWorkflow(id) {
  const workflow = workflows.value.find((item) => item.id === id)
  if (!workflow) return
  workflowForm.id = workflow.id
  workflowForm.code = workflow.code
  workflowForm.name = workflow.name
  workflowForm.description = workflow.description
  workflowForm.version = workflow.version || 1
  workflowForm.status = workflow.status || 'PUBLISHED'
  formFields.value = parseJson(workflow.formJson, { fields: defaultFormFields() }).fields || defaultFormFields()
  loadGraph(workflow.definitionJson)
}

function newWorkflow() {
  selectedWorkflowId.value = null
  workflowForm.id = null
  workflowForm.code = `CUSTOM_APPROVAL_${Date.now()}`
  workflowForm.name = '新的审批流程'
  workflowForm.description = '配置 AI 风险节点、条件路由和人工审批节点。'
  workflowForm.version = 1
  workflowForm.status = 'PUBLISHED'
  formFields.value = defaultFormFields()
  loadGraph(JSON.stringify(defaultGraph()))
  selectedNodeId.value = 'ai_risk'
  resetQuickEdge()
}

async function saveWorkflow() {
  const graph = serializeGraph()
  const payload = {
    code: workflowForm.code, name: workflowForm.name,
    description: workflowForm.description, version: workflowForm.version,
    status: workflowForm.status,
    formJson: JSON.stringify({ fields: formFields.value }),
    definitionJson: JSON.stringify(graph),
    bpmnXml: buildBpmnXml(graph, { processCode: workflowForm.code, processName: workflowForm.name }),
  }
  try {
    const saved = workflowForm.id
      ? await updateWorkflow(workflowForm.id, payload)
      : await createWorkflow(payload)
    ElMessage.success('流程已保存')
    workflowForm.id = saved.id
    selectedWorkflowId.value = saved.id
    await refreshWorkflows()
  } catch {
    // 错误提示由 api() 统一处理
  }
}

async function publishWorkflowAction() {
  await publishWorkflow(selectedWorkflowId.value)
  ElMessage.success('流程已发布')
  await refreshWorkflows()
}

async function disableWorkflowAction() {
  await disableWorkflow(selectedWorkflowId.value)
  ElMessage.success('流程已停用')
  await refreshWorkflows()
}

async function refreshWorkflows() {
  workflows.value = await fetchWorkflows()
}

function validateWorkflow() {
  const errors = []
  const nodes = flowNodes.value
  const edges = flowEdges.value

  const startNodes = nodes.filter((n) => n.data.nodeType === 'START')
  const endNodes = nodes.filter((n) => n.data.nodeType === 'END')
  const conditionNodes = nodes.filter((n) => n.data.nodeType === 'CONDITION')
  const formNodes = nodes.filter((n) => ['APPROVAL', 'AI_APPROVAL', 'FORM_TASK', 'AI_RISK_CHECK'].includes(n.data.nodeType))

  if (nodes.length === 0) errors.push('流程至少需要一个节点')
  if (startNodes.length === 0) errors.push('缺少开始节点')
  if (startNodes.length > 1) errors.push('只能有一个开始节点')
  if (endNodes.length === 0) errors.push('缺少结束节点')

  const nodeIds = new Set(nodes.map((n) => n.id))
  const hasIncoming = new Set(edges.map((e) => e.target))
  const hasOutgoing = new Set(edges.map((e) => e.source))

  nodes.forEach((node) => {
    if (node.data.nodeType !== 'START' && !hasIncoming.has(node.id)) {
      errors.push(`节点「${node.data.label}」没有任何入边`)
    }
    if (node.data.nodeType !== 'END' && !hasOutgoing.has(node.id)) {
      errors.push(`节点「${node.data.label}」没有任何出边`)
    }
  })

  conditionNodes.forEach((node) => {
    const outEdges = edges.filter((e) => e.source === node.id)
    if (outEdges.length < 2) {
      errors.push(`条件节点「${node.data.label}」至少需要两条出边（含默认路由）`)
    }
    const hasDefault = outEdges.some((e) => {
      const c = (e.data?.condition || e.condition || '').trim()
      return !c || c === 'default' || c === '默认'
    })
    if (!hasDefault) {
      errors.push(`条件节点「${node.data.label}」缺少默认路由（条件为空或 "default"）`)
    }
  })

  formNodes.forEach((node) => {
    if (!node.data.config?.formCode) {
      errors.push(`节点「${node.data.label}」未绑定表单`)
    }
  })

  const targetIds = new Set(edges.map((e) => e.target))
  const sourceIds = new Set(edges.map((e) => e.source))
  const referencedInEdges = new Set([...targetIds, ...sourceIds])
  nodes.forEach((node) => {
    if (node.data.nodeType !== 'START' && node.data.nodeType !== 'END' && !referencedInEdges.has(node.id)) {
      errors.push(`节点「${node.data.label}」孤立，未连接到任何连线`)
    }
  })

  if (errors.length > 0) {
    ElMessage.warning(`校验未通过：${errors.join('；')}`)
  } else {
    ElMessage.success('校验通过')
  }
}

function addNode(item) {
  const id = `${item.type.toLowerCase()}_${Date.now()}`
  flowNodes.value.push({
    id, type: 'custom',
    position: { x: 120 + flowNodes.value.length * 28, y: 120 + flowNodes.value.length * 18 },
    data: { label: item.label, nodeType: item.type, config: defaultNodeConfig(item.type) },
  })
  selectedNodeId.value = id
  if (!quickEdge.source && hasSourceHandle(item.type)) {
    quickEdge.source = id
  } else if (!quickEdge.target && hasTargetHandle(item.type) && quickEdge.source !== id) {
    quickEdge.target = id
  }
}

function removeSelectedNode() {
  if (!selectedNode.value) return
  const nodeId = selectedNode.value.id
  flowNodes.value = flowNodes.value.filter((node) => node.id !== nodeId)
  flowEdges.value = flowEdges.value.filter((edge) => edge.source !== nodeId && edge.target !== nodeId)
  selectedNodeId.value = null
}

function removeEdge(edgeId) {
  flowEdges.value = flowEdges.value.filter((edge) => edge.id !== edgeId)
}

function onNodeClick(event) {
  selectedNodeId.value = event.node.id
  ensureNodeFormPermissions(event.node)
}

function onConnect(params) {
  appendEdge({
    id: `edge_${params.source}_${params.target}_${Date.now()}`,
    source: params.source, target: params.target,
    condition: '', label: '',
  })
}

function createQuickEdge() {
  if (!quickEdge.source || !quickEdge.target) {
    ElMessage.warning('请选择来源节点和目标节点')
    return
  }
  if (quickEdge.source === quickEdge.target) {
    ElMessage.warning('来源节点和目标节点不能相同')
    return
  }
  appendEdge({
    id: `edge_${quickEdge.source}_${quickEdge.target}_${Date.now()}`,
    source: quickEdge.source, target: quickEdge.target,
    condition: quickEdge.condition, label: quickEdge.condition,
  })
  quickEdge.condition = ''
}

function appendEdge(edge) {
  const exists = flowEdges.value.some((item) => item.source === edge.source && item.target === edge.target)
  if (exists) { ElMessage.warning('这两个节点之间已经有连线'); return }
  flowEdges.value.push(normalizeEdge(edge))
}

function resetQuickEdge() {
  quickEdge.source = flowNodes.value.find((node) => hasSourceHandle(node.data.nodeType))?.id || ''
  quickEdge.target = flowNodes.value.find((node) => hasTargetHandle(node.data.nodeType) && node.id !== quickEdge.source)?.id || ''
  quickEdge.condition = ''
}

function addFormField() {
  formFields.value.push({
    key: `field_${Date.now()}`, label: '新字段', type: 'TEXT', required: false, placeholder: '请输入',
  })
}

function confirmNodeTypeChange(node, value) {
  node.data.nodeType = value
  node.data.config = normalizeNodeConfig(value, node.data.config || {})
}

function openBpmnPreview() {
  const graph = {
    nodes: flowNodes.value.map((n) => ({
      id: n.id, nodeType: n.data?.nodeType || 'APPROVAL',
      name: n.data?.label || n.id, position: n.position || { x: 0, y: 0 },
      config: n.data?.config || {},
    })),
    edges: flowEdges.value.map((e) => ({
      id: e.id, source: e.source, target: e.target,
      condition: e.data?.condition || '', label: e.label || '',
    })),
  }
  bpmnXml.value = buildBpmnXml(graph, {
    processCode: workflowForm.code || 'FLOWMIND_WORKFLOW',
    processName: workflowForm.name || 'FlowMind Workflow',
  })
  bpmnPreviewVisible.value = true
}

async function copyBpmnXml() {
  try {
    await navigator.clipboard.writeText(bpmnXml.value)
    ElMessage.success('BPMN XML 已复制到剪贴板')
  } catch {
    ElMessage.warning('复制失败，请手动选择文本复制')
  }
}

async function parseBpmnImport() {
  if (!bpmnImportXml.value.trim()) {
    ElMessage.warning('请先粘贴 BPMN 2.0 XML')
    return
  }
  bpmnParsing.value = true
  try {
    const graph = parseBpmnXml(bpmnImportXml.value)
    if (graph.nodes.length === 0) {
      ElMessage.warning('未解析到任何节点，请检查 XML 格式')
      return
    }
    flowNodes.value = graph.nodes.map((n) => ({
      id: n.id, type: 'custom',
      position: n.position || { x: 200, y: 100 },
      data: { label: n.name || n.id, nodeType: n.nodeType || 'APPROVAL', config: n.config || {} },
    }))
    flowEdges.value = graph.edges.map((e) => ({
      id: e.id, source: e.source, target: e.target,
      sourceHandle: 'bottom', targetHandle: 'top', type: 'smoothstep',
      label: e.label || '',
      data: { condition: e.condition || '', isDefault: !e.condition },
    }))
    selectedNodeId.value = null
    bpmnImportVisible.value = false
    ElMessage.success(`已导入 ${graph.nodes.length} 个节点、${graph.edges.length} 条连线`)
  } catch (err) {
    ElMessage.error(err.message || 'BPMN 解析失败')
  } finally {
    bpmnParsing.value = false
  }
}

onMounted(async () => {
  await refreshWorkflows()
  const workflowId = route.params.id
  if (workflowId) {
    const id = Number(workflowId)
    if (!isNaN(id) && workflows.value.find((w) => w.id === id)) {
      selectedWorkflowId.value = id
      selectWorkflow(id)
    }
  }
})
</script>
