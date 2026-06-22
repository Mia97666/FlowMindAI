<template>
  <div v-if="riskPreview" class="risk-preview">
    <el-progress
      :percentage="riskPreview.riskScore || 0"
      :status="riskPreview.riskScore >= 70 ? 'exception' : 'success'"
    />
    <h3>{{ riskPreview.riskLevel }} · {{ riskPreview.decision }}</h3>
    <p>{{ riskPreview.riskReason }}</p>
    <p>{{ riskPreview.suggestion }}</p>
    <el-divider />
    <div class="source-list">
      <div
        v-for="source in riskPreview.sources || []"
        :key="`${source.documentId}-${source.chunkId}`"
        class="source-item"
      >
        <strong>{{ source.documentName || '制度来源' }}</strong>
        <span>{{ source.content }}</span>
      </div>
    </div>
  </div>
  <el-empty v-else description="暂无 AI 预检结果" />
</template>

<script setup>
defineProps({
  riskPreview: { type: Object, default: null },
})
</script>