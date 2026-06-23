<template>
  <section class="page-grid two-column wide-left">
    <el-card shadow="never">
      <template #header>
        <div class="panel-header">
          <div>
            <span>已审批</span>
            <p class="panel-subtitle">查看当前用户已处理任务、审批动作、风险等级与历史表单。</p>
          </div>
          <el-button text :icon="Refresh" @click="load">刷新</el-button>
        </div>
      </template>
      <div class="approval-filter-bar">
        <el-input v-model="filters.nodeName" placeholder="审批节点" clearable @keyup.enter="search" />
        <el-select v-model="filters.status" placeholder="审批动作" clearable>
          <el-option label="已同意" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
        <el-select v-model="filters.riskLevel" placeholder="风险等级" clearable>
          <el-option label="高风险" value="HIGH" />
          <el-option label="中风险" value="MEDIUM" />
          <el-option label="低风险" value="LOW" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-table :data="doneTasks" max-height="520" empty-text="暂无已审批任务" v-loading="doneLoading">
        <el-table-column prop="id" label="任务ID" width="90" />
        <el-table-column label="申请标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ taskInstanceTitle(row) }}</template>
        </el-table-column>
        <el-table-column label="流程名称" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ taskWorkflowName(row) }}</template>
        </el-table-column>
        <el-table-column prop="nodeName" label="审批节点" min-width="150" />
        <el-table-column label="审批动作" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'APPROVED' ? 'success' : 'danger'" effect="plain">{{ row.status === 'APPROVED' ? '同意' : '拒绝' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="completedAt" label="审批时间" min-width="170" :formatter="dateFormatter" />
        <el-table-column label="风险" width="120">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row.riskLevel)" effect="plain">{{ row.riskLevel || '未评估' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="viewDoneTask(row)">查看</el-button>
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
      <template #header>审批记录</template>
      <div v-if="approvalDetailMode === 'task' && selectedTask" class="approval-panel">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="申请标题">{{ taskInstanceTitle(selectedTask) }}</el-descriptions-item>
          <el-descriptions-item label="审批节点">{{ selectedTask.nodeName }}</el-descriptions-item>
          <el-descriptions-item label="审批动作">{{ selectedTask.status }}</el-descriptions-item>
          <el-descriptions-item label="审批意见">{{ selectedTask.comment || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">表单快照</el-divider>
        <FormRenderer
          :fields="approvalFields"
          :model-value="approvalBusinessData"
          :readonly="true"
        />
      </div>
      <el-empty v-else description="请选择左侧已审批记录" />
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import { useSharedState } from '../composables/useSharedState'
import { usePageableList } from '../composables/usePageableList'
import { riskTagType, parseFormSchema, dateFormatter, initBusinessData, taskInstanceTitle, taskWorkflowName } from '../utils/helpers'
import { loadDonePage } from '../api/task'
import FormRenderer from '../components/FormRenderer.vue'

const { doneTasks, currentUser } = useSharedState()
const router = useRouter()
const route = useRoute()

const { filters, pagination, loading: doneLoading, load, search, reset } = usePageableList(
  (page, size, f) => loadDonePage(currentUser.value, page, size, f),
  {
    initialFilters: { nodeName: '', status: '', riskLevel: '' },
    onResult: (rows) => { doneTasks.value = rows }
  }
)

const selectedTask = ref(null)
const approvalDetailMode = ref('empty')
const approvalFields = ref([])
const approvalBusinessData = reactive({})

async function viewDoneTask(task) {
  const route = router.resolve({ path: '/approval/done', query: { taskId: task.id } })
  window.open(route.href, '_blank')
}

onMounted(async () => {
  await load()
  const taskId = route.query.taskId
  if (taskId) {
    const task = doneTasks.value.find((t) => t.id === Number(taskId))
    if (task) {
      approvalDetailMode.value = 'task'
      selectedTask.value = task
      Object.keys(approvalBusinessData).forEach((key) => delete approvalBusinessData[key])
      approvalFields.value = []
      const { api } = await import('../api/index')
      const runtimeForm = await api(`/api/runtime-forms/tasks/${task.id}`)
      approvalFields.value = parseFormSchema(runtimeForm.schemaJson)
      Object.assign(approvalBusinessData, initBusinessData(approvalFields.value, runtimeForm.businessData || {}))
    }
  }
})
</script>
