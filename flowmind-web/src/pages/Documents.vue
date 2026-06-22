<template>
  <section class="page-grid">
    <div class="knowledge-metrics">
      <el-card class="metric-card compact" shadow="never">
        <p>制度文档</p>
        <strong>{{ documents.length }}</strong>
        <span>已入库文档总数</span>
      </el-card>
      <el-card class="metric-card compact" shadow="never">
        <p>Chunk 总数</p>
        <strong>{{ totalChunks }}</strong>
        <span>可用于制度检索</span>
      </el-card>
      <el-card class="metric-card compact" shadow="never">
        <p>RAG 模式</p>
        <strong>{{ knowledgeConfig.adapterType }}</strong>
        <span>{{ knowledgeConfig.ragFlowEnabled ? '外部知识库已启用' : '自研 Pipeline' }}</span>
      </el-card>
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="panel-header">
          <div>
            <span>制度文档</span>
            <p class="panel-subtitle">上传企业制度文档，解析后进入 RAG 检索与风险依据引用。</p>
          </div>
          <el-button :icon="Refresh" @click="refreshDocuments">刷新</el-button>
        </div>
      </template>
      <div class="management-filter-bar compact knowledge-filter-bar">
        <el-input v-model="filters.keyword" placeholder="文档名称" clearable @keyup.enter="search" />
        <el-select v-model="filters.status" placeholder="状态" clearable>
          <el-option label="READY" value="READY" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-upload
        drag
        action="#"
        :auto-upload="true"
        :http-request="handleUpload"
        :show-file-list="false"
        accept=".md,.txt"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖入制度文件或点击选择</div>
      </el-upload>
      <el-table :data="documentPageRows" height="360" empty-text="暂无制度文档">
        <el-table-column prop="originalFilename" label="文件名" min-width="220" />
        <el-table-column label="状态" width="110">
          <template #default>
            <el-tag type="success" effect="plain">READY</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="Chunk" width="100" />
        <el-table-column label="上传人" width="110">
          <template #default>admin</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" min-width="180" :formatter="dateFormatter" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openChunks(row)">查看Chunk</el-button>
            <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
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
  </section>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, UploadFilled } from '@element-plus/icons-vue'
import { useSharedState } from '../composables/useSharedState'
import { usePageableList } from '../composables/usePageableList'
import { dateFormatter } from '../utils/helpers'
import { loadDocumentPage as fetchDocumentPage, uploadDocument as uploadDoc, deleteDocument as deleteDoc } from '../api/knowledge'

const router = useRouter()
const { documents, documentPageRows, knowledgeConfig } = useSharedState()

const { filters, pagination, load, search, reset } = usePageableList(
  (page, size, f) => fetchDocumentPage(page, size, f),
  {
    initialFilters: { keyword: '', status: '' },
    onResult: (rows) => { documentPageRows.value = rows }
  }
)

const totalChunks = computed(() => documents.value.reduce((total, item) => total + (item.chunkCount || 0), 0))

async function handleUpload(req) {
  await uploadDoc(req.file)
  await load()
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除文档「${row.originalFilename}」？删除后不可恢复。`, '删除文档', { type: 'warning' })
  } catch {
    return
  }
  await deleteDoc(row.id)
  ElMessage.success('文档已删除')
  await load()
}

function openChunks(row) {
  router.push(`/knowledge/chunks?documentId=${row.id}`)
}

onMounted(() => load())
</script>
