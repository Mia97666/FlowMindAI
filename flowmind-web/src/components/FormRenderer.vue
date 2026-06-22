<template>
  <div class="runtime-form-grid">
    <el-form-item
      v-for="field in fields"
      :key="field.fieldKey || field.id"
      :label="field.label"
      :required="field.required && !readonly"
      :style="{ gridColumn: field.span === 24 ? 'span 2' : 'span 1' }"
    >
      <el-input
        v-if="['TEXT', 'NUMBER', 'AMOUNT'].includes(field.componentType)"
        :model-value="modelValue[field.fieldKey]"
        :type="field.componentType === 'TEXT' ? 'text' : 'number'"
        :placeholder="field.placeholder"
        :disabled="readonly || field.readOnly"
        @update:model-value="(val) => emit('update:modelValue', { ...modelValue, [field.fieldKey]: val })"
      />
      <el-input
        v-else-if="field.componentType === 'TEXTAREA'"
        :model-value="modelValue[field.fieldKey]"
        type="textarea"
        :rows="3"
        :placeholder="field.placeholder"
        :disabled="readonly || field.readOnly"
        @update:model-value="(val) => emit('update:modelValue', { ...modelValue, [field.fieldKey]: val })"
      />
      <el-date-picker
        v-else-if="['DATE', 'DATETIME'].includes(field.componentType)"
        :model-value="modelValue[field.fieldKey]"
        :type="field.componentType === 'DATETIME' ? 'datetime' : 'date'"
        :value-format="field.componentType === 'DATETIME' ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD'"
        :placeholder="field.placeholder || '选择日期'"
        :disabled="readonly || field.readOnly"
        @update:model-value="(val) => emit('update:modelValue', { ...modelValue, [field.fieldKey]: val })"
      />
      <el-select
        v-else-if="field.componentType === 'SELECT'"
        :model-value="modelValue[field.fieldKey]"
        :placeholder="field.placeholder || '请选择'"
        :disabled="readonly || field.readOnly"
        @update:model-value="(val) => emit('update:modelValue', { ...modelValue, [field.fieldKey]: val })"
      >
        <el-option
          v-for="option in parseControlOptions(field)"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      <el-switch
        v-else-if="field.componentType === 'BOOLEAN'"
        :model-value="modelValue[field.fieldKey]"
        :disabled="readonly || field.readOnly"
        @update:model-value="(val) => emit('update:modelValue', { ...modelValue, [field.fieldKey]: val })"
      />
      <el-upload
        v-else-if="field.componentType === 'FILE'"
        action="#"
        :auto-upload="false"
        :disabled="readonly || field.readOnly"
      >
        <el-button :icon="UploadFilled">选择文件</el-button>
      </el-upload>
      <div v-else-if="field.componentType === 'TABLE'" class="detail-table-preview">
        <div>物品/项目</div>
        <div>数量</div>
        <div>金额</div>
        <div>备注</div>
      </div>
      <el-alert
        v-else-if="field.componentType === 'INFO'"
        type="info"
        :closable="false"
        :title="field.placeholder || field.label"
      />
      <el-input
        v-else
        :model-value="modelValue[field.fieldKey]"
        :placeholder="field.placeholder"
        :disabled="readonly || field.readOnly"
        @update:model-value="(val) => emit('update:modelValue', { ...modelValue, [field.fieldKey]: val })"
      />
    </el-form-item>
  </div>
</template>

<script setup>
import { UploadFilled } from '@element-plus/icons-vue'
import { parseControlOptions } from '../utils/helpers'

defineProps({
  fields: { type: Array, default: () => [] },
  modelValue: { type: Object, default: () => ({}) },
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])
</script>