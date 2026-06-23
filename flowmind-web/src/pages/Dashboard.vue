<template>
  <section class="page-grid dashboard-grid">
    <el-card class="metric-card" shadow="never">
      <p>流程模板</p>
      <strong>{{ workflows.length }}</strong>
      <span>已配置 {{ enabledWorkflowCount }} 个可发起流程</span>
    </el-card>
    <el-card class="metric-card" shadow="never">
      <p>运行实例</p>
      <strong>{{ filteredInstances.length }}</strong>
      <span>运行中 {{ runningInstanceCount }} 个</span>
    </el-card>
    <el-card class="metric-card risk" shadow="never">
      <p>高风险申请</p>
      <strong>{{ highRiskInstanceCount }}</strong>
      <span>AI 自动标记并留痕</span>
    </el-card>
    <el-card class="metric-card" shadow="never">
      <p>我的待办</p>
      <strong>{{ todos.length }}</strong>
      <span>{{ currentUser }} 的待处理任务</span>
    </el-card>

    <el-card class="wide-panel" shadow="never">
      <template #header>
        <div class="panel-header">
          <span>实例趋势</span>
          <div class="table-toolbar">
            <el-select v-model="chartTimeRange" class="time-range-select" @change="updateCharts">
              <el-option label="近7天" value="7d" />
              <el-option label="近30天" value="30d" />
              <el-option label="全部" value="all" />
            </el-select>
          </div>
        </div>
      </template>
      <div ref="trendChartRef" class="dashboard-chart"></div>
    </el-card>

    <el-card class="wide-panel" shadow="never">
      <template #header>
        <div class="panel-header">
          <span>实例状态分布</span>
        </div>
      </template>
      <div ref="pieChartRef" class="dashboard-chart"></div>
    </el-card>

    <el-card class="wide-panel" shadow="never">
      <template #header>
        <div class="panel-header">
          <span>近期审批实例</span>
          <div class="table-toolbar">
            <el-select v-model="timeRange" class="time-range-select">
              <el-option label="今天" value="today" />
              <el-option label="本周" value="week" />
              <el-option label="本月" value="month" />
              <el-option label="全部" value="all" />
            </el-select>
            <el-button text :icon="Operation" @click="goToInstances">查看全部</el-button>
          </div>
        </div>
      </template>
      <el-table :data="filteredInstances.slice(0, 6)" height="316" empty-text="暂无流程实例">
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="definitionName" label="流程" width="150" />
        <el-table-column prop="starter" label="发起人" width="110" />
        <el-table-column label="风险" width="120">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row.riskLevel)" effect="plain">{{ row.riskLevel || '未评估' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" />
      </el-table>
    </el-card>

    <el-card class="wide-panel" shadow="never">
      <template #header>
        <div class="panel-header">
          <span>我的通知</span>
          <el-button text :icon="Bell" @click="loadNotifications">刷新通知</el-button>
        </div>
      </template>
      <div class="notification-list" style="max-height: 400px; overflow-y: auto;">
        <button
          v-for="message in notifications.slice(0, 8)"
          :key="message.id"
          class="notification-item"
          :class="{ unread: !message.readFlag }"
          @click="markNotificationRead(message)"
        >
          <strong>{{ message.title }}</strong>
          <span>{{ message.content }}</span>
        </button>
        <el-empty v-if="notifications.length === 0" description="暂无通知" />
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { computed, ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, Operation } from '@element-plus/icons-vue'
import { useSharedState } from '../composables/useSharedState'
import { riskTagType } from '../utils/helpers'
import { loadNotifications, markRead } from '../api/notification'

const router = useRouter()
const { workflows, instances, todos, notifications, currentUser } = useSharedState()

const timeRange = ref('all')
const chartTimeRange = ref('7d')
const trendChartRef = ref(null)
const pieChartRef = ref(null)
let trendChart = null
let pieChart = null
let echartsModule = null

async function loadEcharts() {
  if (!echartsModule) {
    echartsModule = await import('echarts')
  }
  return echartsModule
}

function getTimeRangeStart() {
  const now = new Date()
  switch (timeRange.value) {
    case 'today': {
      const d = new Date(now.getFullYear(), now.getMonth(), now.getDate())
      return d.getTime()
    }
    case 'week': {
      const d = new Date(now)
      d.setDate(d.getDate() - d.getDay())
      d.setHours(0, 0, 0, 0)
      return d.getTime()
    }
    case 'month': {
      const d = new Date(now.getFullYear(), now.getMonth(), 1)
      return d.getTime()
    }
    default: return 0
  }
}

const filteredInstances = computed(() => {
  if (timeRange.value === 'all') return instances.value
  const start = getTimeRangeStart()
  return instances.value.filter((item) => {
    const t = item.createdAt ? new Date(item.createdAt).getTime() : 0
    return t >= start
  })
})

const enabledWorkflowCount = computed(() => workflows.value.filter((item) => item.enabled).length)
const runningInstanceCount = computed(() => filteredInstances.value.filter((item) => item.status === 'RUNNING').length)
const highRiskInstanceCount = computed(() => filteredInstances.value.filter((item) => item.riskLevel === 'HIGH').length)

function goToInstances() {
  router.push('/instances')
}

async function markNotificationRead(message) {
  if (!message.readFlag) {
    await markRead(message.id)
    await loadNotifications()
  }
}

function buildTrendData() {
  const now = new Date()
  let days = 30
  if (chartTimeRange.value === '7d') days = 7
  if (chartTimeRange.value === 'all') days = Math.max(30, Math.ceil((now - new Date(Math.min(...instances.value.map((i) => i.createdAt ? new Date(i.createdAt).getTime() : now.getTime())))) / 86400000))

  const dates = []
  const created = new Array(days).fill(0)
  const completed = new Array(days).fill(0)

  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(now)
    d.setDate(d.getDate() - i)
    dates.push(`${d.getMonth() + 1}/${d.getDate()}`)
  }

  instances.value.forEach((item) => {
    if (item.createdAt) {
      const t = new Date(item.createdAt)
      const diff = Math.floor((now - t) / 86400000)
      if (diff >= 0 && diff < days) created[days - 1 - diff]++
    }
    if (item.status === 'COMPLETED' && item.updatedAt) {
      const t = new Date(item.updatedAt)
      const diff = Math.floor((now - t) / 86400000)
      if (diff >= 0 && diff < days) completed[days - 1 - diff]++
    }
  })

  return { dates, created, completed }
}

function buildPieData() {
  const statusCount = {}
  instances.value.forEach((item) => {
    const s = item.status || 'UNKNOWN'
    statusCount[s] = (statusCount[s] || 0) + 1
  })
  return Object.entries(statusCount).map(([name, value]) => ({ name, value }))
}

async function initCharts() {
  const echarts = await loadEcharts()
  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value)
    updateTrendChart()
  }
  if (pieChartRef.value) {
    pieChart = echarts.init(pieChartRef.value)
    updatePieChart()
  }
}

function updateTrendChart() {
  if (!trendChart) return
  const { dates, created, completed } = buildTrendData()
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增实例', '已完成'], bottom: 0 },
    grid: { left: 40, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: 45 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '新增实例', type: 'line', data: created, smooth: true, itemStyle: { color: '#165dff' } },
      { name: '已完成', type: 'line', data: completed, smooth: true, itemStyle: { color: '#5ac8a5' } },
    ],
  })
}

function updatePieChart() {
  if (!pieChart) return
  const data = buildPieData()
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '45%'],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      data,
    }],
  })
}

function updateCharts() {
  updateTrendChart()
}

watch(instances, () => {
  nextTick(() => {
    updateTrendChart()
    updatePieChart()
  })
}, { deep: true })

onMounted(() => {
  nextTick(() => initCharts())
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  pieChart?.dispose()
})

function handleResize() {
  trendChart?.resize()
  pieChart?.resize()
}
</script>
