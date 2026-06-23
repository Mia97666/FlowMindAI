<template>
  <section class="page-grid two-column">
    <el-card shadow="never">
      <template #header>RAG 测试</template>
      <el-form label-position="top">
        <el-form-item label="问题">
          <el-input v-model="ragQuestion" type="textarea" :rows="5" placeholder="例如 采购金额超过20万元需要谁审批？" />
        </el-form-item>
        <div class="rag-tuning-grid">
          <el-form-item label="TopK">
            <el-input-number v-model="knowledgeConfig.topK" :min="1" :max="20" />
          </el-form-item>
          <el-form-item label="最小相似度">
            <el-input-number v-model="knowledgeConfig.minScore" :min="0" :max="1" :step="0.05" />
          </el-form-item>
        </div>
        <el-form-item label="检索模式">
          <el-radio-group v-model="knowledgeConfig.retrievalMode">
            <el-radio-button label="LIGHT">轻量模式</el-radio-button>
            <el-radio-button label="HIGH_RECALL">高召回率模式</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <div v-if="knowledgeConfig.retrievalMode === 'HIGH_RECALL'" class="rag-tuning-grid">
          <el-form-item label="查询重写">
            <el-switch v-model="knowledgeConfig.queryRewriteEnabled" />
          </el-form-item>
          <el-form-item label="多查询扩展">
            <el-switch v-model="knowledgeConfig.multiQueryEnabled" />
          </el-form-item>
        </div>
        <el-form-item label="知识库适配器">
          <el-radio-group v-model="knowledgeConfig.adapterType">
            <el-radio-button label="SELF">自研 Pipeline</el-radio-button>
            <el-radio-button label="RAGFLOW">RAGFlow Adapter</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <div class="rag-actions">
          <el-button type="primary" :icon="Search" :loading="ragLoading" @click="askRag">检索制度</el-button>
        </div>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>回答与来源</template>
      <div v-if="ragAnswer" class="rag-answer">
        <strong>回答</strong>
        <p>{{ ragAnswer.answer }}</p>
        <el-divider content-position="left">来源引用</el-divider>
        <div class="source-list">
          <div v-for="source in ragAnswer.sources || []" :key="`${source.documentId}-${source.chunkId}`" class="source-item">
            <strong>{{ source.documentName }}</strong>
            <span>{{ source.content }}</span>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无检索结果" />
    </el-card>
  </section>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { useSharedState } from '../composables/useSharedState'
import { askRag as askRagApi } from '../api/knowledge'

const { knowledgeConfig } = useSharedState()

const ragQuestion = ref('采购金额超过20万元需要谁审批？')
const ragAnswer = ref(null)
const ragLoading = ref(false)

watch(
  () => knowledgeConfig.retrievalMode,
  (mode) => {
    if (mode !== 'HIGH_RECALL') {
      knowledgeConfig.queryRewriteEnabled = false
      knowledgeConfig.multiQueryEnabled = false
    }
  }
)

async function askRag() {
  ragLoading.value = true
  try {
    ragAnswer.value = await askRagApi(ragQuestion.value, {
      topK: knowledgeConfig.topK,
      minScore: knowledgeConfig.minScore,
      adapterType: knowledgeConfig.adapterType,
      retrievalMode: knowledgeConfig.retrievalMode,
      queryRewriteEnabled: knowledgeConfig.retrievalMode === 'HIGH_RECALL' && knowledgeConfig.queryRewriteEnabled,
      multiQueryEnabled: knowledgeConfig.retrievalMode === 'HIGH_RECALL' && knowledgeConfig.multiQueryEnabled,
    })
  } catch {
    // 异常已在 api 层弹窗处理
  } finally {
    ragLoading.value = false
  }
}
</script>
