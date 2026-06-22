<template>
  <div class="search-bar">
    <div class="search-bar-fields">
      <slot />
    </div>
    <div class="search-bar-actions">
      <el-button type="primary" :icon="Search" @click="$emit('search')">查询</el-button>
      <el-button @click="$emit('reset')">重置</el-button>
      <slot name="extra" />
    </div>
  </div>
</template>

<script setup>
import { Search } from '@element-plus/icons-vue'

defineEmits(['search', 'reset'])
</script>