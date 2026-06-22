<template>
  <section class="page-grid">
    <el-card class="wide-panel" shadow="never">
      <template #header>
        <div class="panel-header">
          <div>
            <span>表单管理</span>
            <p class="panel-subtitle">维护流程可绑定的表单模板、版本状态和发布记录。</p>
          </div>
          <div class="table-toolbar">
            <el-button :icon="Refresh" @click="load">刷新</el-button>
            <el-button type="primary" :icon="Plus" @click="createForm">新增表单</el-button>
          </div>
        </div>
      </template>

      <div class="management-filter-bar form-management-filter-bar">
        <el-input v-model="filters.formName" placeholder="表单名称" clearable @keyup.enter="search" />
        <el-input v-model="filters.formCode" placeholder="表单编码" clearable />
        <el-select v-model="filters.status" placeholder="状态" clearable>
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="formPageRows" height="560" row-key="id" empty-text="暂无表单模板">
        <el-table-column prop="formName" label="表单名称" min-width="180" />
        <el-table-column prop="formCode" label="表单编码" min-width="190" />
        <el-table-column label="当前版本" width="110">
          <template #default="{ row }">V{{ row.version || 1 }}</template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column label="控件数" width="100">
          <template #default="{ row }">{{ parseFormSchema(row.schemaJson).length }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="formStatusTagType(row.status)" effect="plain">{{ formStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="170" :formatter="dateFormatter" />
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDesigner(row)">设计</el-button>
            <el-button text type="primary" @click="previewForm(row)">预览</el-button>
            <el-button text type="primary" @click="publishFormFromList(row)">发布</el-button>
            <el-button text type="primary" @click="copyForm(row)">复制新版本</el-button>
            <el-button text type="warning" @click="disableFormFromList(row)">停用</el-button>
            <el-button v-if="row.status === 'DRAFT'" text type="danger" @click="deleteDraftForm(row)">删除</el-button>
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

    <el-dialog v-model="previewVisible" :title="previewTitle" width="720px">
      <FormRenderer :fields="previewFields" :model-value="previewData" :readonly="true" />
    </el-dialog>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { useSharedState } from '../composables/useSharedState'
import { usePageableList } from '../composables/usePageableList'
import { parseFormSchema, formStatusLabel, formStatusTagType, dateFormatter } from '../utils/helpers'
import { loadFormPage as fetchFormPage, publishForm, disableForm, deleteForm, createForm as createFormApi } from '../api/form'
import FormRenderer from '../components/FormRenderer.vue'

const router = useRouter()
const { forms, formPageRows } = useSharedState()

const { filters, pagination, load, search, reset } = usePageableList(
  (page, size, f) => fetchFormPage(page, size, f),
  {
    initialFilters: { formName: '', formCode: '', status: '' },
    onResult: (rows) => { formPageRows.value = rows }
  }
)

const previewVisible = ref(false)
const previewFields = ref([])
const previewData = ref({})
const previewTitle = ref('')

function openDesigner(row) { router.push(`/forms/designer/${row.id}`) }
function createForm() { router.push('/forms/designer') }

function previewForm(row) {
  previewTitle.value = `${row.formName}（${row.formCode}）`
  previewFields.value = parseFormSchema(row.schemaJson)
  const data = {}
  previewFields.value.forEach((field) => {
    data[field.fieldKey] = field.componentType === 'BOOLEAN' ? false : ''
  })
  previewData.value = data
  previewVisible.value = true
}

async function copyForm(row) {
  try {
    await ElMessageBox.confirm(`确认复制「${row.formName}」为新版本？将生成 V${(row.version || 1) + 1} 草稿。`, '复制新版本', { type: 'info' })
  } catch { return }
  const payload = {
    formCode: row.formCode,
    formName: row.formName,
    category: row.category || '',
    version: (row.version || 1) + 1,
    status: 'DRAFT',
    description: row.description || '',
    schemaJson: row.schemaJson,
  }
  const saved = await createFormApi(payload)
  ElMessage.success(`已复制为 V${saved.version} 草稿`)
  await load()
}

async function deleteDraftForm(row) {
  try {
    await ElMessageBox.confirm(`确认删除草稿「${row.formName}」？删除后不可恢复。`, '删除草稿', { type: 'warning' })
  } catch { return }
  await deleteForm(row.id)
  ElMessage.success('草稿已删除')
  await load()
}

async function publishFormFromList(row) { await publishForm(row.id); await load() }
async function disableFormFromList(row) { await disableForm(row.id); await load() }

onMounted(() => load())
</script>
