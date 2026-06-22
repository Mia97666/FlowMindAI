<template>
  <section class="page-grid two-column wide-left">
    <el-card shadow="never">
      <template #header>
        <div class="panel-header">
          <span>流程实例</span>
          <el-button text :icon="Refresh" @click="load">刷新</el-button>
        </div>
      </template>
      <el-table :data="instancePageRows" height="620" highlight-current-row empty-text="暂无流程实例" @current-change="selectInstance">
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="definitionName" label="流程" width="150" />
        <el-table-column prop="starter" label="发起人" width="100" />
        <el-table-column label="风险" width="120">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row.riskLevel)" effect="plain">{{ row.riskLevel || '未评估' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="currentNodeName" label="当前节点" min-width="180" />
      </el-table>
      <div class="table-pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>实例详情</template>
      <div v-if="selectedInstance" class="instance-detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="标题">{{ selectedInstance.title }}</el-descriptions-item>
          <el-descriptions-item label="流程">{{ selectedInstance.definitionName }} v{{ selectedInstance.definitionVersion || 1 }}</el-descriptions-item>
          <el-descriptions-item label="工作流版本ID">{{ selectedInstance.definitionVersionId || instanceRuntimeForm?.definitionVersionId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="表单版本ID">{{ instanceRuntimeForm?.formVersionId || selectedInstance.startFormVersionId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ selectedInstance.status }}</el-descriptions-item>
          <el-descriptions-item label="风险">{{ selectedInstance.riskLevel || '未评估' }} / {{ selectedInstance.riskScore ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="AI建议">{{ selectedInstance.aiSuggestion || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">业务表单</el-divider>
        <el-alert
          v-if="instanceRuntimeForm"
          class="runtime-form-tip"
          type="info"
          show-icon
          :closable="false"
          :title="`查看表单：${instanceRuntimeForm.formName || instanceRuntimeForm.formCode}${instanceRuntimeForm.fallback ? '（兼容旧表单）' : ''}`"
        />
        <FormRenderer
          :fields="instanceFields"
          :model-value="instanceBusinessData"
          :readonly="true"
        />

        <div v-if="canWithdrawInstance(selectedInstance)" class="instance-actions">
          <el-button type="warning" :icon="CircleClose" @click="withdrawInstance(selectedInstance)">撤回流程</el-button>
        </div>

        <el-divider content-position="left">运行日志</el-divider>
        <el-timeline>
          <el-timeline-item v-for="log in actionLogs" :key="log.id" :timestamp="formatDateTime(log.createdAt)">
            <div class="runtime-log-item">
              <div class="runtime-log-head">
                <el-tag :type="actionTagType(log.action)" effect="plain">{{ actionLabel(log.action) }}</el-tag>
                <strong>{{ log.nodeName || log.nodeId || '-' }}</strong>
                <span>{{ log.actor || 'SYSTEM' }}</span>
              </div>
              <p>{{ log.comment || '-' }}</p>
              <small>状态：{{ log.resultStatus || '-' }} · 任务ID：{{ log.taskId || '-' }}</small>
            </div>
          </el-timeline-item>
        </el-timeline>

        <el-divider content-position="left">AI 审计</el-divider>
        <div class="audit-list">
          <div v-for="log in auditLogs" :key="log.id" class="audit-item">
            <strong>{{ log.nodeId }} · {{ log.riskLevel }} · {{ log.riskScore }}</strong>
            <span>{{ log.riskReason }}</span>
            <small>{{ log.modelName }} · {{ log.durationMs }}ms</small>
          </div>
        </div>
      </div>
      <el-empty v-else description="请选择流程实例" />
    </el-card>
  </section>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { CircleClose, Refresh } from '@element-plus/icons-vue'
import { useSharedState } from '../composables/useSharedState'
import { usePageableList } from '../composables/usePageableList'
import { riskTagType, parseFormSchema, formatDateTime, actionLabel, actionTagType } from '../utils/helpers'
import { loadInstancePage as fetchInstancePage, loadInstanceLogs } from '../api/instance'
import { loadAuditLogs } from '../api/ai'
import FormRenderer from '../components/FormRenderer.vue'

const route = useRoute()
const { instances, instancePageRows, currentUser } = useSharedState()

const { pagination, load, search } = usePageableList(
  (page, size, f) => fetchInstancePage(page, size, f),
  {
    initialFilters: {},
    onResult: (rows) => { instancePageRows.value = rows }
  }
)

const selectedInstance = ref(null)
const instanceRuntimeForm = ref(null)
const instanceFields = ref([])
const instanceBusinessData = reactive({})
const actionLogs = ref([])
const auditLogs = ref([])

async function selectInstance(instance) {
  selectedInstance.value = instance
  instanceRuntimeForm.value = null
  instanceFields.value = []
  Object.keys(instanceBusinessData).forEach((key) => delete instanceBusinessData[key])
  actionLogs.value = []
  auditLogs.value = []
  if (!instance) return
  const { api } = await import('../api/index')
  try {
    instanceRuntimeForm.value = await api(`/api/runtime-forms/instances/${instance.id}`)
    instanceFields.value = parseFormSchema(instanceRuntimeForm.value.schemaJson)
    const initData = {}
    instanceFields.value.forEach((field) => {
      const key = field.fieldKey || field.key
      initData[key] = (instanceRuntimeForm.value.businessData || {})[key] ?? (field.componentType === 'BOOLEAN' ? false : '')
    })
    Object.assign(instanceBusinessData, initData)
  } catch {
    instanceRuntimeForm.value = null
  }
  actionLogs.value = await loadInstanceLogs(instance.id)
  auditLogs.value = await loadAuditLogs(instance.id)
}

function canWithdrawInstance(instance) {
  if (!instance) return false
  return instance.starter === currentUser.value && instance.status === 'RUNNING'
}

async function withdrawInstance(instance) {
  if (!instance) return
  try {
    await ElMessageBox.confirm(
      `确认撤回流程「${instance.title}」？撤回后所有待办将被取消。`,
      '撤回流程',
      { type: 'warning' },
    )
    const { api } = await import('../api/index')
    await api(`/api/workflow-instances/${instance.id}/withdraw`, { method: 'POST' })
    await load()
    selectedInstance.value = null
  } catch { /* cancelled */ }
}

onMounted(async () => {
  await load()
  const instanceId = route.query.id
  if (instanceId) {
    const id = Number(instanceId)
    const instance = instancePageRows.value.find((i) => i.id === id)
    if (instance) selectInstance(instance)
  }
})
</script>
