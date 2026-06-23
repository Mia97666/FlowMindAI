<template>
  <section class="page-grid two-column wide-left">
    <el-card shadow="never">
      <template #header>
        <div class="panel-header">
          <div>
            <span>待审批</span>
            <p class="panel-subtitle">按当前用户展示待处理任务，审批按钮会在右侧打开运行态表单。</p>
          </div>
          <el-button text :icon="Refresh" @click="load">刷新</el-button>
        </div>
      </template>
      <div class="approval-filter-bar">
        <el-input v-model="filters.nodeName" placeholder="当前节点" clearable @keyup.enter="search" />
        <el-select v-model="filters.riskLevel" placeholder="风险等级" clearable>
          <el-option label="高风险" value="HIGH" />
          <el-option label="中风险" value="MEDIUM" />
          <el-option label="低风险" value="LOW" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-table :data="todoTasks" max-height="520" highlight-current-row empty-text="暂无待审批任务" v-loading="todoLoading" @current-change="selectTask">
        <el-table-column prop="id" label="任务ID" width="90" />
        <el-table-column label="申请标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ taskInstanceTitle(row) }}</template>
        </el-table-column>
        <el-table-column label="流程名称" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ taskWorkflowName(row) }}</template>
        </el-table-column>
        <el-table-column prop="nodeName" label="当前节点" min-width="150" />
        <el-table-column label="发起人" width="110">
          <template #default="{ row }">{{ taskStarter(row) }}</template>
        </el-table-column>
        <el-table-column label="风险" width="120">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row.riskLevel)" effect="plain">{{ row.riskLevel || '未评估' }} / {{ row.riskScore ?? '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关键字段" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ taskKeyFields(row) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="到达时间" min-width="170" :formatter="dateFormatter" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click.stop="selectTask(row)">审批</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="table-pagination"
        layout="total, sizes, prev, pager, next"
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        @size-change="search"
        @current-change="load"
      />
    </el-card>

    <el-card shadow="never">
      <template #header>审批表单运行态</template>
      <div v-if="approvalDetailMode === 'task' && selectedTask" class="approval-panel">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="申请标题">{{ taskInstanceTitle(selectedTask) }}</el-descriptions-item>
          <el-descriptions-item label="流程">{{ taskWorkflowName(selectedTask) }}</el-descriptions-item>
          <el-descriptions-item label="当前节点">{{ selectedTask.nodeName }}</el-descriptions-item>
          <el-descriptions-item label="AI风险">{{ selectedTask.riskLevel || '未评估' }} / {{ selectedTask.riskScore ?? '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">审批表单</el-divider>
        <FormRenderer
          :fields="approvalFields"
          :model-value="approvalBusinessData"
          @update:model-value="(val) => Object.assign(approvalBusinessData, val)"
        />
        <el-input v-model="approvalComment" class="approval-comment" type="textarea" :rows="4" placeholder="填写审批意见" />
        <div class="approval-actions">
          <el-button type="success" :icon="Check" :loading="submitting" @click="completeTask('APPROVED')">同意</el-button>
          <el-button type="danger" :icon="Close" :loading="submitting" @click="completeTask('REJECTED')">拒绝</el-button>
          <el-button type="warning" :icon="Operation" :loading="submitting" @click="openReturnDialog">退回</el-button>
          <el-button :icon="Share" :loading="submitting" @click="openTransferDialog">转办</el-button>
        </div>
      </div>
      <el-empty v-else description="请选择左侧待办任务" />
    </el-card>

    <el-dialog v-model="transferDialogVisible" title="转办" width="480px">
      <el-form label-position="top">
        <el-form-item label="转办目标">
          <el-select v-model="transferTargetAssignee" filterable placeholder="请选择转办目标用户">
            <el-option v-for="user in users" :key="user.username" :label="`${user.realName}（${user.username}）`" :value="user.username" />
          </el-select>
        </el-form-item>
        <el-form-item label="转办意见">
          <el-input v-model="transferComment" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="transferDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmTransfer">确认转办</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="returnDialogVisible" title="退回" width="480px">
      <el-form label-position="top">
        <el-form-item label="退回意见">
          <el-input v-model="returnComment" type="textarea" :rows="4" placeholder="退回必须填写意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="returnDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmReturn">确认退回</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Check, Close, Operation, Refresh, Search, Share } from '@element-plus/icons-vue'
import { useSharedState } from '../composables/useSharedState'
import { usePageableList } from '../composables/usePageableList'
import { useTaskCompletion } from '../composables/useTaskCompletion'
import { riskTagType, parseFormSchema, parseJson, dateFormatter, initBusinessData, taskInstanceTitle, taskWorkflowName } from '../utils/helpers'
import { loadTodoPage, loadDoneTasks } from '../api/task'
import { loadNotifications } from '../api/notification'
import { loadInstances } from '../api/instance'
import FormRenderer from '../components/FormRenderer.vue'

const { users, todoTasks, currentUser } = useSharedState()

const { filters, pagination, loading: todoLoading, load, search, reset } = usePageableList(
  (page, size, f) => loadTodoPage(currentUser.value, page, size, f),
  {
    initialFilters: { nodeName: '', riskLevel: '' },
    onResult: (rows) => { todoTasks.value = rows }
  }
)

const selectedTask = ref(null)
const approvalDetailMode = ref('empty')
const approvalRuntimeForm = ref(null)
const approvalFields = ref([])
const approvalBusinessData = reactive({})

const {
  submitting,
  approvalComment,
  transferDialogVisible,
  transferTargetAssignee,
  transferComment,
  returnDialogVisible,
  returnComment,
  completeTask,
  openTransferDialog,
  confirmTransfer,
  openReturnDialog,
  confirmReturn,
} = useTaskCompletion(selectedTask, approvalBusinessData, approvalFields, async (action) => {
  if (action !== 'TRANSFER') {
    selectedTask.value = null
    approvalDetailMode.value = 'empty'
    approvalRuntimeForm.value = null
    approvalFields.value = []
    Object.keys(approvalBusinessData).forEach((key) => delete approvalBusinessData[key])
  }
  await Promise.all([load(), loadDoneTasks(), loadInstances(), loadNotifications(currentUser.value)])
})

function taskStarter(task) {
  return task?.starter || task?.instanceStarter || '-'
}
function taskKeyFields(task) {
  return task?.keyFields || '-'
}

async function selectTask(task) {
  approvalDetailMode.value = task ? 'task' : 'empty'
  selectedTask.value = task
  approvalComment.value = ''
  approvalRuntimeForm.value = null
  approvalFields.value = []
  Object.keys(approvalBusinessData).forEach((key) => delete approvalBusinessData[key])
  if (!task) return
  const { api } = await import('../api/index')
  approvalRuntimeForm.value = await api(`/api/runtime-forms/tasks/${task.id}`)
  approvalFields.value = parseFormSchema(approvalRuntimeForm.value.schemaJson)
  const initData = initBusinessData(approvalFields.value, approvalRuntimeForm.value.businessData || {})
  Object.assign(approvalBusinessData, initData)
}

onMounted(() => load())
</script>
