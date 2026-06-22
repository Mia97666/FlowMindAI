<template>
  <section class="page-grid two-column">
    <el-card shadow="never">
      <template #header>制度文档</template>
      <el-table :data="filteredDocuments" height="520" highlight-current-row empty-text="暂无制度文档" @current-change="openDocumentChunks">
        <el-table-column prop="originalFilename" label="文档名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="chunkCount" label="Chunk" width="90" />
        <el-table-column prop="createdAt" label="上传时间" min-width="170" :formatter="dateFormatter" />
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="panel-header">
          <span>Chunk 查看</span>
          <el-button :icon="Refresh" :disabled="!selectedDocumentId" @click="load">重新解析</el-button>
        </div>
      </template>
      <div class="chunk-filter-bar">
        <el-input v-model="chunkFilters.keyword" :prefix-icon="Search" placeholder="按 Chunk 内容关键字过滤" clearable />
      </div>
      <el-alert
        v-if="selectedKnowledgeDocument"
        class="runtime-form-tip"
        type="info"
        show-icon
        :closable="false"
        :title="`当前文档：${selectedKnowledgeDocument.originalFilename}`"
      />
      <el-table :data="chunkPageRows" height="480" empty-text="请选择左侧文档查看 Chunk">
        <el-table-column prop="chunkIndex" label="序号" width="90" />
        <el-table-column prop="content" label="内容摘要" min-width="260" show-overflow-tooltip />
        <el-table-column prop="vectorId" label="向量ID" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" min-width="170" :formatter="dateFormatter" />
      </el-table>
      <div class="table-pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :disabled="!selectedDocumentId"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import { useSharedState } from '../composables/useSharedState'
import { usePageableList } from '../composables/usePageableList'
import { dateFormatter } from '../utils/helpers'
import { loadChunkPage as fetchChunkPage } from '../api/knowledge'

const route = useRoute()
const { documents, chunkPageRows, knowledgeDocFilters } = useSharedState()

const { pagination, load, search } = usePageableList(
  (page, size) => {
    if (!selectedDocumentId.value) return { records: [], total: 0 }
    return fetchChunkPage(selectedDocumentId.value, page, size)
  },
  {
    initialFilters: {},
    onResult: (rows) => { chunkPageRows.value = rows }
  }
)

const chunkFilters = reactive({ keyword: '' })
const selectedDocumentId = ref(null)
const selectedKnowledgeDocument = ref(null)

const filteredDocuments = computed(() => documents.value.filter((document) => {
  const keywordMatched = !knowledgeDocFilters.keyword || String(document.originalFilename || '').includes(knowledgeDocFilters.keyword)
  const statusMatched = !knowledgeDocFilters.status || knowledgeDocFilters.status === 'READY'
  return keywordMatched && statusMatched
}))

async function openDocumentChunks(row) {
  if (!row) return
  selectedDocumentId.value = row.id
  selectedKnowledgeDocument.value = row
  pagination.page = 1
  await load()
}

onMounted(async () => {
  const documentId = route.query.documentId
  if (documentId) {
    const id = Number(documentId)
    const doc = documents.value.find((d) => d.id === id)
    if (doc) openDocumentChunks(doc)
  }
})
</script>