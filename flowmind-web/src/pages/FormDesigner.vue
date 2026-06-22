<template>
  <section class="form-designer-layout">
    <div class="form-designer-toolbar">
      <el-select
        class="form-selector"
        v-model="selectedFormId"
        filterable
        clearable
        placeholder="选择表单模板..."
        @change="onFormSelectChange"
      >
        <el-option
          v-for="form in forms"
          :key="form.id"
          :label="`${form.formName}（${form.formCode}）`"
          :value="form.id"
        />
      </el-select>
      <el-button text :icon="Plus" @click="newFormDefinition">新建</el-button>
      <el-button text :icon="Refresh" @click="refreshForms">刷新</el-button>
      <span class="toolbar-spacer" />
      <el-button :icon="Search" @click="openFormPreview">预览</el-button>
      <el-button type="primary" :icon="DocumentChecked" @click="saveFormDefinition">保存表单</el-button>
      <el-button :icon="SwitchButton" :disabled="!selectedFormId" @click="publishFormDefinition">发布</el-button>
      <el-button :icon="CircleClose" :disabled="!selectedFormId" @click="disableFormDefinition">停用</el-button>
    </div>

    <div class="form-meta-bar">
      <div class="form-meta-bar-row">
        <el-input v-model="formDefinitionForm.formName" placeholder="表单名称" />
        <el-input v-model="formDefinitionForm.formCode" placeholder="表单编码" />
        <el-input v-model="formDefinitionForm.category" placeholder="分类" />
        <el-select v-model="formDefinitionForm.status">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <el-button text size="small" @click="metaExpanded = !metaExpanded">
          {{ metaExpanded ? '收起' : '更多' }}
        </el-button>
      </div>
      <div v-show="metaExpanded" class="form-meta-bar-extra">
        <el-form-item label="表单说明">
          <el-input v-model="formDefinitionForm.description" type="textarea" :rows="2" placeholder="说明表单适用流程、填写要求和业务边界" />
        </el-form-item>
      </div>
    </div>

    <div class="form-designer-main">
      <el-card class="form-workbench-panel" shadow="never">
        <div
          class="form-canvas"
          :class="{ empty: formDesignerFields.length === 0, 'drop-active': canvasDragOver }"
          @dragover.prevent="canvasDragOver = true"
          @dragleave="canvasDragOver = false"
          @drop="dropFieldToForm"
        >
          <el-empty v-if="formDesignerFields.length === 0" description="从右侧控件库或字段库拖入内容，开始设计表单" />
          <div v-else class="form-preview-grid">
            <div
              v-for="(control, index) in formDesignerFields"
              :key="control.id"
              class="form-control-tile"
              :class="{ selected: selectedFormFieldId === control.id }"
              :style="{ gridColumn: control.span === 24 ? 'span 2' : 'span 1' }"
              @click="selectedFormFieldId = control.id"
            >
              <div class="control-title">
                <span>{{ control.label }}</span>
                <el-tag v-if="control.required" size="small" type="danger" effect="plain">必填</el-tag>
              </div>
              <div class="control-preview">
                <el-input v-if="['TEXT', 'NUMBER', 'AMOUNT'].includes(control.componentType)" :placeholder="control.placeholder || `请输入${control.label}`" :type="control.componentType === 'TEXT' ? 'text' : 'number'" />
                <el-input v-else-if="control.componentType === 'TEXTAREA'" type="textarea" :rows="3" :placeholder="control.placeholder || `请输入${control.label}`" />
                <el-date-picker v-else-if="['DATE', 'DATETIME'].includes(control.componentType)" :type="control.componentType === 'DATETIME' ? 'datetime' : 'date'" :placeholder="control.placeholder || '请选择'" />
                <el-select v-else-if="control.componentType === 'SELECT'" :placeholder="control.placeholder || '请选择'">
                  <el-option label="选项一" value="A" />
                  <el-option label="选项二" value="B" />
                </el-select>
                <el-upload v-else-if="control.componentType === 'FILE'" action="#" :auto-upload="false" :show-file-list="false">
                  <el-button :icon="UploadFilled">选择文件</el-button>
                </el-upload>
                <div v-else-if="control.componentType === 'TABLE'" class="detail-table-preview">
                  <div>物品/项目</div><div>数量</div><div>金额</div><div>备注</div>
                </div>
                <el-alert v-else-if="control.componentType === 'INFO'" type="info" :closable="false" :title="control.placeholder || control.label" />
                <el-switch v-else-if="control.componentType === 'BOOLEAN'" />
                <el-input v-else :placeholder="control.placeholder || `请输入${control.label}`" />
              </div>
              <div class="control-actions">
                <el-button :icon="ArrowUp" text size="small" :disabled="index === 0" @click.stop="moveFormControl(index, -1)" />
                <el-button :icon="ArrowDown" text size="small" :disabled="index === formDesignerFields.length - 1" @click.stop="moveFormControl(index, 1)" />
                <el-button :icon="Delete" text size="small" type="danger" @click.stop="removeFormControl(index)" />
              </div>
            </div>
          </div>
          <div class="canvas-actions">
            <el-button>撤销表单</el-button>
            <el-button type="primary">提交表单</el-button>
          </div>
        </div>
      </el-card>

      <el-card class="field-library-panel" shadow="never">
        <template #header>
          <div class="right-panel-tabs">
            <button class="right-panel-tab" :class="{ active: rightTab === 'controls' }" @click="rightTab = 'controls'">控件库</button>
            <button class="right-panel-tab" :class="{ active: rightTab === 'props' }" @click="rightTab = 'props'">属性配置</button>
          </div>
        </template>
        <template v-if="rightTab === 'controls'">
        <div class="sub-panel-title">通用控件</div>
        <div class="control-palette-grid">
          <button
            v-for="control in controlPalette"
            :key="control.componentType"
            class="control-palette-item"
            draggable="true"
            @dragstart="onControlDragStart(control, $event)"
            @click="addControlToForm(control)"
          >
            <span class="control-palette-icon">
              <component :is="controlIcon(control.componentType)" />
            </span>
            <span>
              <strong>{{ control.label }}</strong>
              <span>{{ fieldTypeLabel(control.componentType) }}</span>
            </span>
          </button>
        </div>

        <el-divider content-position="left">字段库</el-divider>
        <div class="field-library-list">
          <button
            v-for="field in enabledFields"
            :key="field.id"
            class="field-library-item"
            draggable="true"
            @dragstart="onFormFieldDragStart(field)"
            @click="addFieldToForm(field)"
          >
            <strong>{{ field.fieldName }}</strong>
            <span>{{ field.fieldKey }} · {{ fieldTypeLabel(field.fieldType) }}</span>
          </button>
          <el-empty v-if="enabledFields.length === 0" description="暂无启用字段，请先维护字段库" />
        </div>
        </template>

        <template v-if="rightTab === 'props'">
        <el-divider content-position="left">控件属性</el-divider>
        <el-form v-if="selectedFormField" label-position="top" class="compact-form">
          <el-form-item label="控件标题">
            <el-input v-model="selectedFormField.label" />
          </el-form-item>
          <el-form-item label="绑定字段">
            <el-select v-model="selectedFormField.fieldKey" filterable @change="syncSelectedControlField">
              <el-option v-for="field in fields" :key="field.fieldKey" :label="`${field.fieldName}（${field.fieldKey}）`" :value="field.fieldKey" />
            </el-select>
          </el-form-item>
          <el-form-item label="控件类型">
            <el-select v-model="selectedFormField.componentType">
              <el-option v-for="item in controlTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="占位提示">
            <el-input v-model="selectedFormField.placeholder" />
          </el-form-item>
          <el-form-item label="布局宽度">
            <el-radio-group v-model="selectedFormField.span">
              <el-radio-button :label="12">半行</el-radio-button>
              <el-radio-button :label="24">整行</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="是否必填">
            <el-switch v-model="selectedFormField.required" />
          </el-form-item>
          <el-form-item label="校验配置 JSON">
            <el-input v-model="selectedFormField.validationJson" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item v-if="selectedFormField.componentType === 'SELECT'" label="选项配置 JSON">
            <el-input v-model="selectedFormField.optionsJson" type="textarea" :rows="3" />
          </el-form-item>
        </el-form>
        <el-empty v-else description="请选择画布中的控件" />
        </template>
      </el-card>
    </div>

    <div class="form-reference-bar">
      <strong>引用此表单的节点：</strong>
      <template v-if="formReferences.length > 0">
        <span
          v-for="ref in formReferences"
          :key="`${ref.workflowId}-${ref.nodeId}`"
          class="form-reference-tag"
          @click="goToWorkflowNode(ref.workflowId)"
        >
          {{ ref.workflowName }} / {{ ref.nodeName }}
        </span>
      </template>
      <span v-else>此表单尚未被任何工作流节点引用</span>
    </div>

    <el-dialog v-model="formPreviewVisible" title="表单预览" width="720px">
      <el-form label-position="top" class="runtime-form-grid">
        <el-form-item
          v-for="control in formDesignerFields"
          :key="control.id"
          :label="control.label"
          :required="control.required"
          :style="{ gridColumn: control.span === 24 ? 'span 2' : 'span 1' }"
        >
          <el-input v-if="['TEXT', 'NUMBER', 'AMOUNT'].includes(control.componentType)" v-model="formPreviewData[control.fieldKey]" :type="control.componentType === 'TEXT' ? 'text' : 'number'" :placeholder="control.placeholder" />
          <el-input v-else-if="control.componentType === 'TEXTAREA'" v-model="formPreviewData[control.fieldKey]" type="textarea" :rows="3" :placeholder="control.placeholder" />
          <el-date-picker v-else-if="['DATE', 'DATETIME'].includes(control.componentType)" v-model="formPreviewData[control.fieldKey]" :type="control.componentType === 'DATETIME' ? 'datetime' : 'date'" :placeholder="control.placeholder || '选择日期'" />
          <el-select v-else-if="control.componentType === 'SELECT'" v-model="formPreviewData[control.fieldKey]" :placeholder="control.placeholder || '请选择'">
            <el-option v-for="option in parseControlOptions(control)" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
          <el-switch v-else-if="control.componentType === 'BOOLEAN'" v-model="formPreviewData[control.fieldKey]" />
          <el-upload v-else-if="control.componentType === 'FILE'" action="#" :auto-upload="false">
            <el-button :icon="UploadFilled">选择文件</el-button>
          </el-upload>
          <div v-else-if="control.componentType === 'TABLE'" class="detail-table-preview">
            <div>物品/项目</div><div>数量</div><div>金额</div><div>备注</div>
          </div>
          <el-alert v-else-if="control.componentType === 'INFO'" type="info" :closable="false" :title="control.placeholder || control.label" />
          <el-input v-else v-model="formPreviewData[control.fieldKey]" :placeholder="control.placeholder" />
        </el-form-item>
      </el-form>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, ArrowUp, Calendar, CircleClose, Delete, Document, DocumentChecked, Edit, Grid, InfoFilled, List, Money, Plus, Refresh, Search, Sort, Switch, SwitchButton, Upload, UploadFilled } from '@element-plus/icons-vue'
import { useSharedState } from '../composables/useSharedState'
import { controlPalette, controlTypeOptions } from '../config/constants'
import { parseFormSchema, parseControlOptions, parseJson, fieldTypeLabel } from '../utils/helpers'
import { loadForms as fetchForms, createForm, updateForm, publishForm, disableForm } from '../api/form'

const { forms, fields, workflows } = useSharedState()
const route = useRoute()
const router = useRouter()

const formFilters = reactive({ formName: '', status: '' })
const selectedFormId = ref(null)
const selectedFormFieldId = ref(null)
const formPreviewVisible = ref(false)
const formPreviewData = reactive({})
const formDesignerFields = ref([])
const draggingFormField = ref(null)
const draggingFormControl = ref(null)
const metaExpanded = ref(false)
const rightTab = ref('controls')
const canvasDragOver = ref(false)

const controlIconMap = {
  TEXT: Edit, NUMBER: Sort, AMOUNT: Money, DATE: Calendar,
  DATETIME: Calendar, SELECT: List, BOOLEAN: Switch, FILE: Upload,
  TEXTAREA: Document, TABLE: Grid, INFO: InfoFilled,
}
function controlIcon(componentType) {
  return controlIconMap[componentType] || Edit
}

const formDefinitionForm = reactive({
  id: null, formCode: '', formName: '', category: '', version: 1, status: 'DRAFT', description: '',
})

const enabledFields = computed(() => fields.value.filter((field) => field.status === 'ENABLED'))

const selectedFormField = computed(() => formDesignerFields.value.find((field) => field.id === selectedFormFieldId.value))

const formReferences = computed(() => {
  if (!formDefinitionForm.formCode) return []
  const refs = []
  workflows.value.forEach((workflow) => {
    let graph = {}
    try {
      graph = typeof workflow.definitionJson === 'string'
        ? JSON.parse(workflow.definitionJson)
        : (workflow.definitionJson || {})
    } catch { /* ignore */ }
    const nodes = graph.nodes || []
    nodes.forEach((node) => {
      if (node.config?.formCode === formDefinitionForm.formCode) {
        refs.push({
          workflowId: workflow.id,
          workflowName: workflow.name || workflow.code || `流程 ${workflow.id}`,
          nodeId: node.id,
          nodeName: node.name || node.label || node.id,
        })
      }
    })
  })
  return refs
})

onMounted(async () => {
  try {
    await refreshForms()
    const idParam = route.params.id
    if (idParam) {
      selectFormDefinition(Number(idParam))
    }
  } catch {
    // 表单列表加载失败不影响页面渲染
  }
})

function newFormDefinition() {
  selectedFormId.value = null
  selectedFormFieldId.value = null
  Object.assign(formDefinitionForm, {
    id: null, formCode: `CUSTOM_FORM_${Date.now()}`, formName: '新的流程表单',
    category: '通用', version: 1, status: 'DRAFT',
    description: '从字段库拖入控件，配置校验、提示和布局。',
  })
  formDesignerFields.value = []
}

function onFormSelectChange(id) {
  if (id) {
    selectFormDefinition(id)
  } else {
    newFormDefinition()
  }
}

function selectFormDefinition(id) {
  const form = forms.value.find((item) => item.id === id)
  if (!form) return
  selectedFormId.value = id
  Object.assign(formDefinitionForm, {
    id: form.id, formCode: form.formCode, formName: form.formName,
    category: form.category || '', version: form.version || 1,
    status: form.status || 'DRAFT', description: form.description || '',
  })
  try {
    formDesignerFields.value = parseFormSchema(form.schemaJson)
  } catch {
    ElMessage.warning('表单 schema 解析失败，已重置为空表单')
    formDesignerFields.value = []
  }
  selectedFormFieldId.value = formDesignerFields.value[0]?.id || null
}

async function saveFormDefinition() {
  if (!formDefinitionForm.formCode || !formDefinitionForm.formName) {
    ElMessage.warning('表单编码和表单名称不能为空')
    return
  }
  const payload = {
    formCode: formDefinitionForm.formCode, formName: formDefinitionForm.formName,
    category: formDefinitionForm.category, version: formDefinitionForm.version,
    status: formDefinitionForm.status, description: formDefinitionForm.description,
    schemaJson: JSON.stringify({ fields: formDesignerFields.value }),
  }
  const saved = formDefinitionForm.id
    ? await updateForm(formDefinitionForm.id, payload)
    : await createForm(payload)
  selectedFormId.value = saved.id
  Object.assign(formDefinitionForm, {
    id: saved.id, formCode: saved.formCode, formName: saved.formName,
    category: saved.category || '', version: saved.version || 1,
    status: saved.status || 'DRAFT', description: saved.description || '',
  })
  ElMessage.success('表单已保存')
  await refreshForms()
}

async function publishFormDefinition() {
  if (!selectedFormId.value) { ElMessage.warning('请先保存表单'); return }
  const saved = await publishForm(selectedFormId.value)
  Object.assign(formDefinitionForm, {
    id: saved.id, status: saved.status || 'PUBLISHED',
  })
  ElMessage.success('表单已发布')
  await refreshForms()
}

async function disableFormDefinition() {
  if (!selectedFormId.value) return
  const saved = await disableForm(selectedFormId.value)
  Object.assign(formDefinitionForm, { status: saved.status || 'DISABLED' })
  ElMessage.success('表单已停用')
  await refreshForms()
}

async function refreshForms() {
  forms.value = await fetchForms(formFilters)
}

function onFormFieldDragStart(field) { draggingFormField.value = field; draggingFormControl.value = null }
function onControlDragStart(control, event) {
  draggingFormControl.value = control
  draggingFormField.value = null
  if (event?.target) {
    event.target.classList.add('dragging')
    event.target.addEventListener('dragend', () => {
      event.target.classList.remove('dragging')
    }, { once: true })
  }
}

function dropFieldToForm() {
  canvasDragOver.value = false
  if (draggingFormControl.value) {
    addControlToForm(draggingFormControl.value)
    draggingFormControl.value = null
    return
  }
  if (!draggingFormField.value) return
  addFieldToForm(draggingFormField.value)
  draggingFormField.value = null
}

function addControlToForm(control) {
  const componentType = control.componentType || 'TEXT'
  const fieldKey = `${control.fieldKeyPrefix || componentType.toLowerCase()}_${Date.now()}`
  const next = {
    id: `${fieldKey}_control`, fieldKey, label: control.label, componentType,
    required: Boolean(control.required),
    span: control.span || (['TEXTAREA', 'TABLE', 'INFO', 'FILE'].includes(componentType) ? 24 : 12),
    placeholder: control.placeholder || '',
    validationJson: JSON.stringify({ required: Boolean(control.required) }),
    optionsJson: control.optionsJson || '',
  }
  formDesignerFields.value.push(next)
  selectedFormFieldId.value = next.id
}

function addFieldToForm(field) {
  if (formDesignerFields.value.some((item) => item.fieldKey === field.fieldKey)) {
    ElMessage.warning('该字段已在当前表单中')
    return
  }
  const validation = parseJson(field.validationJson, {})
  const next = {
    id: `${field.fieldKey}_${Date.now()}`, fieldKey: field.fieldKey,
    label: field.fieldName, componentType: field.fieldType || 'TEXT',
    required: Boolean(validation.required),
    span: field.fieldType === 'TEXTAREA' ? 24 : 12,
    placeholder: `请输入${field.fieldName}`,
    validationJson: field.validationJson || '{"required":false}',
    optionsJson: field.optionsJson || '',
  }
  formDesignerFields.value.push(next)
  selectedFormFieldId.value = next.id
}

function moveFormControl(index, direction) {
  const targetIndex = index + direction
  if (targetIndex < 0 || targetIndex >= formDesignerFields.value.length) return
  const nextFields = [...formDesignerFields.value]
  const [current] = nextFields.splice(index, 1)
  nextFields.splice(targetIndex, 0, current)
  formDesignerFields.value = nextFields
}

function removeFormControl(index) {
  const removed = formDesignerFields.value[index]
  formDesignerFields.value.splice(index, 1)
  if (selectedFormFieldId.value === removed?.id) {
    selectedFormFieldId.value = formDesignerFields.value[index]?.id || formDesignerFields.value[index - 1]?.id || null
  }
}

function syncSelectedControlField(fieldKey) {
  const field = fields.value.find((item) => item.fieldKey === fieldKey)
  if (!field || !selectedFormField.value) return
  selectedFormField.value.label = field.fieldName
  selectedFormField.value.componentType = field.fieldType || 'TEXT'
  selectedFormField.value.validationJson = field.validationJson || '{"required":false}'
  selectedFormField.value.optionsJson = field.optionsJson || ''
}

function openFormPreview() {
  if (formDesignerFields.value.length === 0) {
    ElMessage.warning('请先添加表单控件')
    return
  }
  Object.keys(formPreviewData).forEach((key) => { delete formPreviewData[key] })
  formDesignerFields.value.forEach((control) => {
    formPreviewData[control.fieldKey] = control.componentType === 'BOOLEAN' ? false : ''
  })
  formPreviewVisible.value = true
}

function goToWorkflowNode(workflowId) {
  router.push(`/workflows/designer/${workflowId}`)
}
</script>
