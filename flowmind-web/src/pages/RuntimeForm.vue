<template>
  <section class="page-grid two-column wide-left">
    <el-card shadow="never">
      <template #header>
        <div class="panel-header">
          <div>
            <span>运行态来源</span>
            <p class="panel-subtitle">选择待审批任务、已审批记录或流程实例，右侧会按节点表单权限渲染运行态页面。</p>
          </div>
          <el-button text :icon="Refresh" @click="refreshData">刷新</el-button>
        </div>
      </template>
      <el-tabs v-model="runtimeSourceTab">
        <el-tab-pane label="待审批" name="todo">
          <el-table :data="todoTasks" height="220" empty-text="暂无待审批任务">
            <el-table-column label="申请标题" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ taskInstanceTitle(row) }}</template>
            </el-table-column>
            <el-table-column prop="nodeName" label="节点" min-width="140" />
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button text type="primary" @click="openTaskRuntime(row)">处理</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="已审批" name="done">
          <el-table :data="doneTasks" height="220" empty-text="暂无已审批记录">
            <el-table-column label="申请标题" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ taskInstanceTitle(row) }}</template>
            </el-table-column>
            <el-table-column prop="nodeName" label="节点" min-width="140" />
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button text type="primary" @click="openDoneRuntime(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="流程实例" name="instance">
          <el-table :data="instances" height="220" empty-text="暂无流程实例">
            <el-table-column prop="title" label="实例标题" min-width="220" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button text type="primary" @click="openInstanceRuntime(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-card shadow="never">
      <template #header>运行态表单</template>
      <div v-if="selectedTask" class="approval-panel">
        <el-alert
          class="runtime-form-tip"
          type="info"
          show-icon
          :closable="false"
          :title="`当前任务：${taskInstanceTitle(selectedTask)} / ${selectedTask.nodeName || '-'}`"
        />
        <FormRenderer
          :fields="approvalFields"
          :model-value="approvalBusinessData"
          :readonly="selectedTask.status !== 'PENDING'"
          @update:model-value="(val) => Object.assign(approvalBusinessData, val)"
        />
        <template v-if="selectedTask.status === 'PENDING'">
          <el-input v-model="approvalComment" class="approval-comment" type="textarea" :rows="4" placeholder="填写审批意见" />
          <div class="approval-actions">
            <el-button type="success" :icon="Check" :loading="submitting" @click="completeTask('APPROVED')">同意</el-button>
            <el-button type="danger" :icon="Close" :loading="submitting" @click="completeTask('REJECTED')">拒绝</el-button>
            <el-button type="warning" :icon="Operation" :loading="submitting" @click="completeTask('RETURN')">退回</el-button>
            <el-button :icon="Share" :loading="submitting" @click="completeTask('TRANSFER')">转办</el-button>
          </div>
        </template>
      </div>
      <div v-else-if="selectedInstance" class="approval-panel">
        <el-alert
          class="runtime-form-tip"
          type="info"
          show-icon
          :closable="false"
          :title="`当前实例：${selectedInstance.title || selectedInstance.id}`"
        />
        <FormRenderer
          :fields="instanceFields"
          :model-value="instanceBusinessData"
          :readonly="true"
        />
      </div>
      <el-empty v-else description="请先在左侧选择一条运行态来源" />
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
import { reactive, ref } from 'vue'
import { Check, Close, Operation, Refresh, Share } from '@element-plus/icons-vue'
import { useSharedState } from '../composables/useSharedState'
import { useTaskCompletion } from '../composables/useTaskCompletion'
import { parseFormSchema, initBusinessData, taskInstanceTitle } from '../utils/helpers'
import { loadTodoPage } from '../api/task'
import { loadNotifications } from '../api/notification'
import { loadInstances } from '../api/instance'
import FormRenderer from '../components/FormRenderer.vue'

const { todoTasks, doneTasks, instances, currentUser, users } = useSharedState()

const runtimeSourceTab = ref('todo')
const selectedTask = ref(null)
const selectedInstance = ref(null)
const approvalFields = ref([])
const approvalBusinessData = reactive({})
const instanceFields = ref([])
const instanceBusinessData = reactive({})

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
    approvalFields.value = []
    Object.keys(approvalBusinessData).forEach((key) => delete approvalBusinessData[key])
  }
  await refreshData()
})

async function openTaskRuntime(task) {
  runtimeSourceTab.value = 'todo'
  selectedTask.value = task
  selectedInstance.value = null
  approvalComment.value = ''
  approvalFields.value = []
  Object.keys(approvalBusinessData).forEach((key) => delete approvalBusinessData[key])
  const { api } = await import('../api/index')
  const runtimeForm = await api(`/api/runtime-forms/tasks/${task.id}`)
  approvalFields.value = parseFormSchema(runtimeForm.schemaJson)
  Object.assign(approvalBusinessData, initBusinessData(approvalFields.value, runtimeForm.businessData || {}))
}

async function openDoneRuntime(task) {
  runtimeSourceTab.value = 'done'
  selectedTask.value = task
  selectedInstance.value = null
  approvalFields.value = []
  Object.keys(approvalBusinessData).forEach((key) => delete approvalBusinessData[key])
  const { api } = await import('../api/index')
  const runtimeForm = await api(`/api/runtime-forms/tasks/${task.id}`)
  approvalFields.value = parseFormSchema(runtimeForm.schemaJson)
  Object.assign(approvalBusinessData, initBusinessData(approvalFields.value, runtimeForm.businessData || {}))
}

async function openInstanceRuntime(instance) {
  runtimeSourceTab.value = 'instance'
  selectedTask.value = null
  selectedInstance.value = instance
  instanceFields.value = []
  Object.keys(instanceBusinessData).forEach((key) => delete instanceBusinessData[key])
  const { api } = await import('../api/index')
  const runtimeForm = await api(`/api/runtime-forms/instances/${instance.id}`)
  instanceFields.value = parseFormSchema(runtimeForm.schemaJson)
  Object.assign(instanceBusinessData, initBusinessData(instanceFields.value, runtimeForm.businessData || {}))
}

async function refreshData() {
  await Promise.all([loadTodoPage(), loadNotifications(currentUser.value), loadInstances()])
}
</script>