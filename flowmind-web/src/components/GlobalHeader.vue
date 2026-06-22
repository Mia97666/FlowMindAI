<template>
  <el-header class="global-header">
    <div class="brand">
      <img src="@/assets/images/审批流.png" alt="FlowMind AI" class="brand-logo" @error="onLogoError" />
      <div v-show="!logoFailed">
        <h1>FlowMind AI</h1>
        <p>智能审批流平台</p>
      </div>
    </div>

    <nav class="top-nav" aria-label="一级模块导航">
      <button
        v-for="menu in topMenus"
        :key="menu.id || menu.menuCode"
        class="top-nav-item"
        :class="{ active: activeTopCode === menu.menuCode }"
        type="button"
        @click="$emit('select-top-menu', menu)"
      >
        <el-icon><component :is="iconMap[menu.routeKey] || iconMap[menu.menuCode] || Operation" /></el-icon>
        <span>{{ menu.menuName }}</span>
      </button>
    </nav>

    <div class="global-tools">
      <div class="global-search-wrap">
        <el-input
          :model-value="searchKeyword"
          class="global-search"
          :prefix-icon="Search"
          placeholder="搜索流程、任务、制度文档"
          clearable
          @update:model-value="$emit('update:searchKeyword', $event)"
          @keyup.enter="$emit('search', searchKeyword)"
        />
        <div v-if="searchKeyword.trim()" class="global-search-panel">
          <button
            v-for="item in searchResults"
            :key="`${item.type}-${item.key}`"
            class="global-search-item"
            type="button"
            @mousedown.prevent="$emit('open-search-result', item)"
          >
            <span>{{ item.typeLabel }}</span>
            <strong>{{ item.title }}</strong>
            <small>{{ item.description }}</small>
          </button>
          <el-empty v-if="searchResults.length === 0" description="未找到匹配结果" :image-size="72" />
        </div>
      </div>
      <el-popover
        placement="bottom-end"
        :width="360"
        trigger="click"
        :visible="notificationVisible"
        @update:visible="notificationVisible = $event"
      >
        <template #reference>
          <el-badge :is-dot="unreadCount > 0" class="notification-badge">
            <el-button :icon="Bell">通知</el-button>
          </el-badge>
        </template>
        <div class="notification-popover">
          <div class="notification-popover-header">
            <strong>消息通知</strong>
            <el-button text type="primary" size="small" @click="handleMarkAllRead">全部已读</el-button>
          </div>
          <div v-if="notifications.length === 0" class="notification-empty">
            <el-empty description="暂无通知" :image-size="64" />
          </div>
          <button
            v-for="message in notifications.slice(0, 10)"
            :key="message.id"
            class="notification-item"
            :class="{ unread: !message.readFlag }"
            type="button"
            @click="handleMarkRead(message)"
          >
            <div class="notification-item-head">
              <strong>{{ message.title }}</strong>
              <el-tag v-if="!message.readFlag" size="small" type="danger">未读</el-tag>
            </div>
            <span>{{ message.content }}</span>
            <small>{{ formatDateTime(message.createdAt) }}</small>
          </button>
        </div>
      </el-popover>
      <el-tag v-if="backendHealthy" type="success" effect="plain">后端已连接</el-tag>
      <el-tag v-else type="warning" effect="plain">等待后端</el-tag>
      <div class="user-switcher">
        <el-avatar :size="34">{{ userInitials }}</el-avatar>
        <el-select
          :model-value="currentUser"
          size="small"
          filterable
          @change="$emit('update:currentUser', $event)"
        >
          <el-option
            v-for="user in users"
            :key="user.username"
            :label="`${user.realName}（${user.username}）`"
            :value="user.username"
          />
        </el-select>
      </div>
    </div>
  </el-header>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Bell, Operation, Search } from '@element-plus/icons-vue'
import { menuIconMap } from '../config/constants'
import { formatDateTime } from '../utils/helpers'

const props = defineProps({
  topMenus: { type: Array, default: () => [] },
  activeTopCode: { type: String, default: '' },
  currentUser: { type: String, default: 'admin' },
  users: { type: Array, default: () => [] },
  backendHealthy: { type: Boolean, default: false },
  unreadCount: { type: Number, default: 0 },
  searchKeyword: { type: String, default: '' },
  searchResults: { type: Array, default: () => [] },
  userInitials: { type: String, default: 'FM' },
  notifications: { type: Array, default: () => [] },
})

const emit = defineEmits(['select-top-menu', 'search', 'open-search-result', 'update:currentUser', 'update:searchKeyword', 'mark-read', 'mark-all-read'])

const iconMap = menuIconMap
const notificationVisible = ref(false)
const logoFailed = ref(false)

function onLogoError() {
  logoFailed.value = true
}

function handleMarkRead(message) {
  if (!message.readFlag) emit('mark-read', message.id)
}

function handleMarkAllRead() {
  emit('mark-all-read')
}
</script>