<template>
  <section class="page-grid">
    <el-card class="wide-panel" shadow="never">
      <template #header>
        <div class="panel-header">
          <div>
            <span>流程管理</span>
            <p class="panel-subtitle">维护流程基础信息、版本状态和发起表单，设计动作会进入工作流设计器。</p>
          </div>
          <div class="table-toolbar">
            <el-button :icon="Refresh" @click="refreshWorkflows">刷新</el-button>
            <el-button type="primary" :icon="Plus" @click="createWorkflow">新增流程</el-button>
          </div>
        </div>
      </template>

      <div class="management-filter-bar workflow-filter-bar">
        <el-input v-model="filters.name" placeholder="流程名称" clearable />
        <el-input v-model="filters.code" placeholder="流程编码" clearable />
        <el-select v-model="filters.status" placeholder="状态" clearable>
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <el-input v-model="filters.category" placeholder="分类" clearable />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="workflowPageRows" height="560" row-key="id" empty-text="暂无流程定义">
        <el-table-column prop="name" label="流程名称" min-width="180" />
        <el-table-column prop="code" label="流程编码" min-width="180" />
        <el-table-column label="分类" width="110">
          <template #default="{ row }">{{ workflowCategory(row) }}</template>
        </el-table-column>
        <el-table-column label="当前版本" width="110">
          <template #default="{ row }">V{{ row.version || 1 }}</template>
        </el-table-column>
        <el-table-column label="发起表单" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ workflowStartFormLabel(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="workflowStatusTagType(row.status)" effect="plain">{{ workflowStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="170" :formatter="dateFormatter" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDesigner(row)">设计</el-button>
            <el-button text type="primary" @click="publishWorkflowFromList(row)">发布</el-button>
            <el-button text type="warning" @click="disableWorkflowFromList(row)">停用</el-button>
            <el-button text type="info" @click="showVersions(row)">版本</el-button>
          </template>
        </el-table-column>
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

    <el-dialog v-model="versionDialogVisible" title="版本信息" width="560px">
      <el-descriptions v-if="versionWorkflow" :column="1" border>
        <el-descriptions-item label="流程名称">{{ versionWorkflow.name }}</el-descriptions-item>
        <el-descriptions-item label="流程编码">{{ versionWorkflow.code }}</el-descriptions-item>
        <el-descriptions-item label="当前版本">V{{ versionWorkflow.version || 1 }}</el-descriptions-item>
        <el-descriptions-item label="发布版本 ID">{{ versionWorkflow.publishedVersionId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="workflowStatusTagType(versionWorkflow.status)" effect="plain">{{ workflowStatusLabel(versionWorkflow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ formatDateTime(versionWorkflow.publishedAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(versionWorkflow.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(versionWorkflow.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { useSharedState } from '../composables/useSharedState'
import { usePageableList } from '../composables/usePageableList'
import { workflowCategory, workflowStatusLabel, workflowStatusTagType, parseJson, dateFormatter, formatDateTime } from '../utils/helpers'
import { loadWorkflowPage as fetchWorkflowPage, publishWorkflow, disableWorkflow } from '../api/workflow'

const router = useRouter()
const { forms, workflowPageRows } = useSharedState()

const { filters, pagination, load, search, reset } = usePageableList(
  (page, size, f) => fetchWorkflowPage(page, size, f),
  {
    initialFilters: { name: '', code: '', status: '', category: '' },
    pageSize: 10,
    onResult: (rows) => { workflowPageRows.value = rows }
  }
)

const versionDialogVisible = ref(false)
const versionWorkflow = ref(null)

function workflowStartFormLabel(workflow) {
  const graph = parseJson(workflow?.definitionJson, {})
  const formCode = (graph.nodes || [])
    .map((node) => node.config?.formCode)
    .find((code) => code)
  if (!formCode) return '-'
  const form = forms.value.find((item) => item.formCode === formCode)
  return form ? `${form.formName}（${formCode}）` : formCode
}

function openDesigner(row) {
  router.push(`/workflows/designer/${row.id}`)
}

async function publishWorkflowFromList(row) {
  await publishWorkflow(row.id)
  await load()
}

async function disableWorkflowFromList(row) {
  await disableWorkflow(row.id)
  await load()
}

function showVersions(row) {
  versionWorkflow.value = row
  versionDialogVisible.value = true
}

function createWorkflow() {
  router.push('/workflows/designer')
}

onMounted(() => load())
</script>
