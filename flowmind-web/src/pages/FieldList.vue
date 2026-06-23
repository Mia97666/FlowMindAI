<template>
  <section class="page-grid">
    <el-card class="wide-panel" shadow="never">
      <template #header>
        <div class="panel-header">
          <div>
            <span>流程表单字段管理</span>
            <p class="panel-subtitle">统一维护可绑定到表单控件的字段，支持自定义字段和数据库已有字段导入。</p>
          </div>
          <div class="table-toolbar">
            <el-button :icon="Refresh" @click="load">刷新</el-button>
            <el-button :icon="UploadFilled" @click="openDbImportDialog">导入数据库字段</el-button>
            <el-button type="primary" :icon="Plus" @click="openCreateField">新增自定义字段</el-button>
          </div>
        </div>
      </template>

      <div class="field-filter-bar">
        <el-input v-model="filters.fieldKey" placeholder="字段 Key" clearable />
        <el-input v-model="filters.fieldName" placeholder="字段名称" clearable />
        <el-select v-model="filters.fieldType" placeholder="字段类型" clearable>
          <el-option v-for="item in fieldTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.sourceType" placeholder="字段来源" clearable>
          <el-option v-for="item in sourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.status" placeholder="状态" clearable>
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="fieldPageRows" max-height="560" row-key="id" empty-text="暂无字段定义">
        <el-table-column prop="fieldKey" label="字段 Key" min-width="160" />
        <el-table-column prop="fieldName" label="字段名称" min-width="150" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ fieldTypeLabel(row.fieldType) }}</template>
        </el-table-column>
        <el-table-column label="来源" width="130">
          <template #default="{ row }">
            <el-tag effect="plain">{{ sourceTypeLabel(row.sourceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="数据库映射" min-width="220">
          <template #default="{ row }">
            <span v-if="row.sourceTable || row.sourceColumn">{{ row.sourceTable || '-' }}.{{ row.sourceColumn || '-' }}</span>
            <span v-else class="muted-text">未绑定</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="plain">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEditField(row)">编辑</el-button>
            <el-button v-if="row.status === 'ENABLED'" text type="warning" @click="disableFieldRow(row)">停用</el-button>
            <el-button v-else text type="success" @click="enableFieldRow(row)">启用</el-button>
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

    <el-dialog v-model="fieldDialogVisible" :title="fieldDialogTitle" width="720px">
      <el-form label-position="top" class="field-dialog-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="字段名称">
              <el-input v-model="fieldForm.fieldName" placeholder="例如 采购金额" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字段 Key">
              <el-input v-model="fieldForm.fieldKey" placeholder="例如 amount" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="字段类型">
              <el-select v-model="fieldForm.fieldType">
                <el-option v-for="item in fieldTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字段来源">
              <el-select v-model="fieldForm.sourceType">
                <el-option v-for="item in sourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="fieldForm.sourceType === 'DB_COLUMN'" :gutter="16">
          <el-col :span="12">
            <el-form-item label="来源表">
              <el-input v-model="fieldForm.sourceTable" placeholder="例如 purchase_order" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源列">
              <el-input v-model="fieldForm.sourceColumn" placeholder="例如 amount" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="默认值">
              <el-input v-model="fieldForm.defaultValue" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="fieldForm.status">
                <el-option label="启用" value="ENABLED" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="选项配置 JSON">
          <el-input v-model="fieldForm.optionsJson" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="字段说明">
          <el-input v-model="fieldForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="fieldDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveField">保存字段</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="dbImportDialogVisible" title="导入数据库字段" width="720px">
      <el-form label-position="top">
        <el-form-item label="选择数据库表">
          <el-select v-model="selectedDbTable" filterable placeholder="请选择数据库表" @change="loadDbColumnsByTable">
            <el-option v-for="table in dbTables" :key="table" :label="table" :value="table" />
          </el-select>
        </el-form-item>
        <el-table :data="dbColumns" height="320" @selection-change="onDbColumnSelectionChange">
          <el-table-column type="selection" width="55" />
          <el-table-column prop="columnName" label="列名" min-width="180" />
          <el-table-column prop="suggestedFieldKey" label="建议字段 Key" min-width="180" />
          <el-table-column prop="suggestedFieldName" label="建议字段名称" min-width="180" />
          <el-table-column prop="suggestedFieldType" label="建议类型" width="120" />
        </el-table>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dbImportDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="dbImportLoading" @click="importSelectedDbFields">导入选中字段</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, Search, UploadFilled } from '@element-plus/icons-vue'
import { useSharedState } from '../composables/useSharedState'
import { usePageableList } from '../composables/usePageableList'
import { fieldTypeOptions, sourceTypeOptions } from '../config/constants'
import { fieldTypeLabel, sourceTypeLabel } from '../utils/helpers'
import {
  loadFieldPage as fetchFieldPage,
  createField,
  updateField,
  enableField as enableFieldApi,
  disableField as disableFieldApi,
  loadDbTables as fetchDbTables,
  loadDbColumns as fetchDbColumns,
  importDbFields as importDbFieldsApi,
} from '../api/field'

const { fields, fieldPageRows } = useSharedState()

const { filters, pagination, load, search, reset } = usePageableList(
  (page, size, f) => fetchFieldPage(page, size, f),
  {
    initialFilters: { fieldKey: '', fieldName: '', fieldType: '', sourceType: '', status: '' },
    onResult: (rows) => { fieldPageRows.value = rows }
  }
)

const fieldDialogVisible = ref(false)
const fieldDialogMode = ref('create')
const fieldForm = reactive({
  id: null, fieldKey: '', fieldName: '', fieldType: 'TEXT', sourceType: 'CUSTOM',
  sourceTable: '', sourceColumn: '', defaultValue: '', optionsJson: '', validationJson: '{"required":false}',
  description: '', status: 'ENABLED',
})
const dbImportDialogVisible = ref(false)
const selectedDbTable = ref('')
const dbTables = ref([])
const dbColumns = ref([])
const selectedDbColumnNames = ref([])

const dbImportLoading = ref(false)

const fieldDialogTitle = ref('新增自定义字段')

function resetFieldForm() {
  Object.assign(fieldForm, {
    id: null, fieldKey: '', fieldName: '', fieldType: 'TEXT', sourceType: 'CUSTOM',
    sourceTable: '', sourceColumn: '', defaultValue: '', optionsJson: '', validationJson: '{"required":false}',
    description: '', status: 'ENABLED',
  })
}

function openCreateField() {
  fieldDialogMode.value = 'create'
  fieldDialogTitle.value = '新增自定义字段'
  resetFieldForm()
  fieldDialogVisible.value = true
}

function openEditField(row) {
  fieldDialogMode.value = 'edit'
  fieldDialogTitle.value = '编辑字段定义'
  Object.assign(fieldForm, {
    id: row.id, fieldKey: row.fieldKey, fieldName: row.fieldName,
    fieldType: row.fieldType || 'TEXT', sourceType: row.sourceType || 'CUSTOM',
    sourceTable: row.sourceTable || '', sourceColumn: row.sourceColumn || '',
    defaultValue: row.defaultValue || '', optionsJson: row.optionsJson || '',
    validationJson: row.validationJson || '{"required":false}',
    description: row.description || '', status: row.status || 'ENABLED',
  })
  fieldDialogVisible.value = true
}

async function saveField() {
  if (!fieldForm.fieldKey || !fieldForm.fieldName) {
    ElMessage.warning('字段 Key 和字段名称不能为空')
    return
  }
  const payload = {
    fieldKey: fieldForm.fieldKey, fieldName: fieldForm.fieldName,
    fieldType: fieldForm.fieldType, sourceType: fieldForm.sourceType,
    sourceTable: fieldForm.sourceType === 'DB_COLUMN' ? fieldForm.sourceTable : '',
    sourceColumn: fieldForm.sourceType === 'DB_COLUMN' ? fieldForm.sourceColumn : '',
    defaultValue: fieldForm.defaultValue, optionsJson: fieldForm.optionsJson,
    validationJson: fieldForm.validationJson, description: fieldForm.description,
    status: fieldForm.status,
  }
  if (fieldDialogMode.value === 'create') {
    await createField(payload)
  } else {
    await updateField(fieldForm.id, payload)
  }
  fieldDialogVisible.value = false
  ElMessage.success('字段定义已保存')
  await load()
}

onMounted(() => load())

async function enableFieldRow(row) {
  await enableFieldApi(row.id)
  await refreshFields()
}

async function disableFieldRow(row) {
  await disableFieldApi(row.id)
  await refreshFields()
}

async function openDbImportDialog() {
  dbImportDialogVisible.value = true
  selectedDbTable.value = ''
  dbColumns.value = []
  selectedDbColumnNames.value = []
  dbTables.value = await fetchDbTables()
}

async function loadDbColumnsByTable(tableName) {
  dbColumns.value = tableName ? await fetchDbColumns(tableName) : []
  selectedDbColumnNames.value = []
}

function onDbColumnSelectionChange(rows) {
  selectedDbColumnNames.value = rows.map((row) => row.columnName)
}

async function importSelectedDbFields() {
  if (!selectedDbTable.value || selectedDbColumnNames.value.length === 0) {
    ElMessage.warning('请先选择数据库表和字段')
    return
  }
  dbImportLoading.value = true
  try {
    const selectedColumns = dbColumns.value.filter((column) => selectedDbColumnNames.value.includes(column.columnName))
    await importDbFieldsApi({
      sourceTable: selectedDbTable.value,
      columns: selectedColumns.map((column) => ({
        sourceColumn: column.columnName,
        fieldKey: column.suggestedFieldKey,
        fieldName: column.suggestedFieldName,
        fieldType: column.suggestedFieldType,
      })),
    })
    dbImportDialogVisible.value = false
    ElMessage.success('数据库字段已导入')
    await refreshFields()
  } finally {
    dbImportLoading.value = false
  }
}
</script>
