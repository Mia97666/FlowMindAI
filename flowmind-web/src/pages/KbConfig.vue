<template>
  <section class="page-grid">
    <el-card class="wide-panel" shadow="never">
      <template #header>
        <div class="panel-header">
          <span>知识库配置</span>
          <el-button type="primary" :icon="DocumentChecked" @click="saveConfig">保存配置</el-button>
        </div>
      </template>
      <el-alert
        class="runtime-form-tip"
        type="info"
        show-icon
        :closable="false"
        title="当前保留自研 RAG Pipeline，同时预留 RAGFlow Adapter；切换后前端会展示外部知识库连接参数。"
      />
      <el-form label-position="top" class="settings-grid">
        <el-form-item label="知识库名称">
          <el-input v-model="knowledgeConfig.name" />
        </el-form-item>
        <el-form-item label="适配器">
          <el-radio-group v-model="knowledgeConfig.adapterType">
            <el-radio-button label="SELF">自研</el-radio-button>
            <el-radio-button label="RAGFLOW">RAGFlow</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="TopK">
          <el-input-number v-model="knowledgeConfig.topK" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="最小相似度">
          <el-input-number v-model="knowledgeConfig.minScore" :min="0" :max="1" :step="0.05" />
        </el-form-item>
        <el-form-item label="Embedding 模型">
          <el-input v-model="knowledgeConfig.embeddingModel" />
        </el-form-item>
        <el-form-item label="Rerank 模型">
          <el-input v-model="knowledgeConfig.rerankModel" />
        </el-form-item>
        <el-form-item label="Chunk 大小">
          <el-input-number v-model="knowledgeConfig.chunkSize" :min="200" :max="2000" :step="100" />
        </el-form-item>
        <el-form-item label="Chunk 重叠">
          <el-input-number v-model="knowledgeConfig.chunkOverlap" :min="0" :max="500" :step="20" />
        </el-form-item>
        <el-form-item label="RAGFlow Adapter">
          <el-switch v-model="knowledgeConfig.ragFlowEnabled" />
        </el-form-item>
        <template v-if="knowledgeConfig.adapterType === 'RAGFLOW' || knowledgeConfig.ragFlowEnabled">
          <el-form-item label="RAGFlow 地址">
            <el-input v-model="knowledgeConfig.ragFlowEndpoint" placeholder="https://ragflow.example.com" />
          </el-form-item>
          <el-form-item label="Dataset ID">
            <el-input v-model="knowledgeConfig.ragFlowDatasetId" placeholder="企业制度库 Dataset" />
          </el-form-item>
          <el-form-item label="API Key">
            <el-input v-model="knowledgeConfig.ragFlowApiKey" type="password" show-password placeholder="后期对接时填写" />
          </el-form-item>
        </template>
      </el-form>
    </el-card>
  </section>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { DocumentChecked } from '@element-plus/icons-vue'
import { useSharedState } from '../composables/useSharedState'
import { saveKnowledgeConfig as saveKbConfig } from '../api/knowledge'

const { knowledgeConfig } = useSharedState()

async function saveConfig() {
  try {
    await saveKbConfig({ ...knowledgeConfig })
    ElMessage.success('知识库配置已保存')
  } catch {
    // 错误提示由 api() 统一处理
  }
}
</script>