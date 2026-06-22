<template>
  <div class="data-table-wrap">
    <el-table
      v-bind="$attrs"
      :data="data"
      :height="height"
      empty-text="暂无数据"
      @current-change="$emit('current-change', $event)"
      @selection-change="$emit('selection-change', $event)"
    >
      <slot />
    </el-table>
    <div v-if="showPagination" class="table-pagination">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @current-change="$emit('page-change', $event)"
        @size-change="$emit('size-change', $event)"
      />
    </div>
  </div>
</template>

<script setup>
defineProps({
  data: { type: Array, default: () => [] },
  height: { type: [String, Number], default: undefined },
  showPagination: { type: Boolean, default: false },
  currentPage: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  total: { type: Number, default: 0 },
})

defineEmits(['current-change', 'selection-change', 'page-change', 'size-change'])
</script>