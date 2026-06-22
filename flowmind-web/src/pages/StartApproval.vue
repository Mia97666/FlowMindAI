<template>
  <section class="page-grid two-column">
    <el-card shadow="never">
      <template #header>提交审批申请</template>
      <el-form label-position="top">
        <el-form-item label="选择流程">
          <el-select v-model="launchForm.definitionId" filterable @change="prepareLaunchForm">
            <el-option v-for="workflow in enabledWorkflows" :key="workflow.id" :label="workflow.name" :value="workflow.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="发起人">
          <el-select v-model="launchForm.starter" filterable>
            <el-option v-for="user in users" :key="user.username" :label="`${user.realName}（${user.username}）`" :value="user.username" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请标题">
          <el-input v-model="launchForm.title" placeholder="例如 采购30台MacBook审批" />
        </el-form-item>

        <el-alert
          v-if="launchRuntimeForm"
          class="runtime-form-tip"
          type="info"
          show-icon
          :closable="false"
          :title="`当前表单：${launchRuntimeForm.formName || launchRuntimeForm.formCode}${launchRuntimeForm.fallback ? '（兼容旧表单）' : ''}`"
        />

        <FormRenderer
          :fields="launchFields"
          :model-value="launchForm.businessData"
          v-loading="formLoading"
          @update:model-value="(val) => launchForm.businessData = val"
        />

        <el-button type="primary" :icon="Promotion" :loading="submitting" @click="startWorkflow">提交审批</el-button>
        <el-button :icon="Cpu" @click="testRiskBeforeSubmit" :loading="riskTesting">AI 预检</el-button>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>AI 风险预检</template>
      <AiRiskPanel :risk-preview="riskPreview" />
    </el-card>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Cpu, Promotion } from '@element-plus/icons-vue'
import { useSharedState } from '../composables/useSharedState'
import { parseFormSchema, parseJson, initBusinessData, normalizeBusinessData } from '../utils/helpers'
import { riskCheck } from '../api/ai'
import { startWorkflow as startWorkflowApi } from '../api/workflow'
import { loadInstances } from '../api/instance'
import { loadTodoPage } from '../api/task'
import { loadNotifications } from '../api/notification'
import FormRenderer from '../components/FormRenderer.vue'
import AiRiskPanel from '../components/AiRiskPanel.vue'

const { workflows, users, currentUser } = useSharedState()
const router = useRouter()

const enabledWorkflows = computed(() => workflows.value.filter((item) => item.enabled))
const submitting = ref(false)
const riskTesting = ref(false)
const formLoading = ref(false)
const riskPreview = ref(null)
const launchRuntimeForm = ref(null)
const launchFields = ref([])

const launchForm = reactive({
  definitionId: null,
  starter: '',
  title: '',
  businessData: {},
})

async function prepareLaunchForm() {
  launchForm.title = ''
  launchForm.starter = currentUser.value
  riskPreview.value = null
  const workflow = workflows.value.find((item) => item.id === launchForm.definitionId)
  if (!workflow) {
    launchFields.value = []
    launchRuntimeForm.value = null
    launchForm.businessData = {}
    return
  }
  formLoading.value = true
  try {
    const { api } = await import('../api/index')
    launchRuntimeForm.value = await api(`/api/runtime-forms/workflows/${launchForm.definitionId}/start`, { silent: true })
    launchFields.value = parseFormSchema(launchRuntimeForm.value.schemaJson)
  } catch {
    ElMessage.warning('无法加载流程表单，使用内置表单')
    launchRuntimeForm.value = {
      formCode: `${workflow.code}_INLINE_FORM`,
      formName: `${workflow.name}内置表单`,
      fallback: true,
    }
    launchFields.value = parseFormSchema(workflow.formJson)
  }
  launchForm.title = `${workflow.name}申请`
  launchForm.businessData = initBusinessData(launchFields.value, {})
  formLoading.value = false
}

async function startWorkflow() {
  if (!launchForm.definitionId) {
    ElMessage.warning('请先选择流程')
    return
  }
  submitting.value = true
  try {
    const instance = await startWorkflowApi(launchForm.definitionId, {
      starter: launchForm.starter,
      title: launchForm.title,
      businessData: normalizeBusinessData(launchForm.businessData, launchFields.value),
    })
    ElMessage.success(`审批已提交，实例ID：${instance.id}`)
    await Promise.all([loadInstances(), loadTodoPage(), loadNotifications(currentUser.value)])
    resetLaunchForm()
    router.push('/approval/my-applications')
  } finally {
    submitting.value = false
  }
}

function resetLaunchForm() {
  launchForm.definitionId = null
  launchForm.starter = ''
  launchForm.title = ''
  launchForm.businessData = {}
  launchFields.value = []
  launchRuntimeForm.value = null
  riskPreview.value = null
}

async function testRiskBeforeSubmit() {
  const workflow = workflows.value.find((item) => item.id === launchForm.definitionId)
  riskTesting.value = true
  try {
    riskPreview.value = await riskCheck({
      workflowCode: workflow?.code || 'PURCHASE_APPROVAL',
      businessData: normalizeBusinessData(launchForm.businessData, launchFields.value),
    })
  } finally {
    riskTesting.value = false
  }
}
</script>