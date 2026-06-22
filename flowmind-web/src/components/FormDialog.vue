<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    :width="width"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:modelValue', $event)"
    @closed="$emit('closed')"
  >
    <el-form :model="formData" :rules="rules" :label-width="labelWidth" ref="formRef">
      <slot :form-data="formData" />
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="loading" @click="$emit('confirm', formData)">{{ confirmText }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '表单' },
  width: { type: String, default: '560px' },
  labelWidth: { type: String, default: '100px' },
  formData: { type: Object, default: () => ({}) },
  rules: { type: Object, default: () => ({}) },
  loading: { type: Boolean, default: false },
  confirmText: { type: String, default: '确定' },
})

defineEmits(['update:modelValue', 'confirm', 'closed'])

const formRef = ref(null)

defineExpose({ formRef })
</script>