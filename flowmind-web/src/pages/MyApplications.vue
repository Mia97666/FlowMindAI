<template>
  <section class="page-grid">
    <el-card class="wide-panel" shadow="never">
      <template #header>
        <div class="panel-header">
          <div>
            <span>我的申请</span>
            <p class="panel-subtitle">查看我发起的所有流程申请及其审批进度。</p>
          </div>
          <div class="table-toolbar">
            <el-button :icon="Refresh" @click="fetchData">刷新</el-button>
          </div>
        </div>
      </template>

      <SearchBar @search="handleSearch" @reset="handleReset">
        <el-input v-model="filters.title" placeholder="标题" clearable />
        <el-select v-model="filters.status" placeholder="状态" clearable>
          <el-option label="运行中" value="RUNNING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已拒绝" value="REJECTED" />
          <el-option label="已取消" value="CANCELED" />
        </el-select>
      </SearchBar>

      <DataTable
        :data="applications"
        :show-pagination="true"
        :current-page="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        height="520"
        @page-change="handlePageChange"
        @size-change="handleSizeChange"
      >
        <el-table-column prop="title" label="申请标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="definitionName" label="流程名称" min-width="160" />
        <el-table-column label="风险" width="120">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row.riskLevel)" effect="plain" size="small">
              {{ row.riskLevel || '未评估' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="instanceStatusTagType(row.status)" effect="plain" size="small">
              {{ instanceStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentNodeName" label="当前节点" min-width="160" />
        <el-table-column prop="createdAt" label="发起时间" min-width="170" :formatter="dateFormatter" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="viewDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </DataTable>
    </el-card>
  </section>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { useSharedState } from '../composables/useSharedState'
import { riskTagType, dateFormatter } from '../utils/helpers'
import { loadMyApplications } from '../api/instance'
import SearchBar from '../components/SearchBar.vue'
import DataTable from '../components/DataTable.vue'

const router = useRouter()
const { currentUser } = useSharedState()

const applications = ref([])
const filters = reactive({ title: '', status: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })

function instanceStatusLabel(status) {
  const map = { RUNNING: '运行中', COMPLETED: '已完成', REJECTED: '已拒绝', CANCELED: '已取消' }
  return map[status] || status || '-'
}

function instanceStatusTagType(status) {
  const map = { RUNNING: 'warning', COMPLETED: 'success', REJECTED: 'danger', CANCELED: 'info' }
  return map[status] || 'info'
}

async function fetchData() {
  const result = await loadMyApplications(currentUser.value, pagination.page, pagination.size, filters)
  applications.value = result.content || []
  pagination.total = result.totalElements || 0
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleReset() {
  filters.title = ''
  filters.status = ''
  pagination.page = 1
  fetchData()
}

function handlePageChange(page) {
  pagination.page = page
  fetchData()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  fetchData()
}

function viewDetail(row) {
  router.push(`/instances?id=${row.id}`)
}

onMounted(() => {
  fetchData()
})
</script>