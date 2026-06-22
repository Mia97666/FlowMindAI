<template>
  <el-container class="app-shell">
    <GlobalHeader
      :top-menus="topMenus"
      :active-top-code="activeTopCode"
      :current-user="currentUser"
      :users="users"
      :backend-healthy="backendHealthy"
      :unread-count="unreadNotificationCount"
      :search-keyword="searchKeyword"
      :search-results="globalSearchResults"
      :user-initials="currentUserInitials"
      :notifications="notifications"
      @select-top-menu="handleSelectTopMenu"
      @search="jumpFirstSearchResult"
      @open-search-result="openGlobalSearchResult"
      @update:current-user="handleUserChange"
      @update:search-keyword="onSearchKeywordChange"
      @mark-read="handleMarkRead"
      @mark-all-read="handleMarkAllRead"
    />

    <el-container class="workspace-shell">
      <AppSidebar
        :navigation-tree="navigationTree"
        :active-top-code="activeTopCode"
        :active-route-key="activeRouteKey"
        :collapsed="sidebarCollapsed"
        @navigate="handleSideNavigate"
        @toggle-collapse="sidebarCollapsed = !sidebarCollapsed"
      />

      <el-container class="content-shell">
        <el-main class="app-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useSharedState } from '../composables/useSharedState'
import GlobalHeader from './GlobalHeader.vue'
import AppSidebar from './AppSidebar.vue'

const {
  navigationTree, activeTopCode, activeRouteKey, currentUser, users,
  backendHealthy, unreadNotificationCount, globalSearchKeyword, globalSearchResults,
  currentUserInitials, notifications,
  jumpFirstSearchResult, openGlobalSearchResult,
  handleSelectTopMenu, handleSideNavigate, handleUserChange,
  markRead, loadNotifications,
} = useSharedState()

const topMenus = computed(() =>
  navigationTree.value.filter((menu) => menu.menuType === 'TOP' || !menu.parentId)
)

const searchKeyword = ref(globalSearchKeyword.value)
const sidebarCollapsed = ref(false)

watch(globalSearchKeyword, (val) => { searchKeyword.value = val })

function onSearchKeywordChange(val) {
  searchKeyword.value = val
  globalSearchKeyword.value = val
}

async function handleMarkRead(id) {
  const msg = notifications.value.find((m) => m.id === id)
  if (msg) msg.readFlag = true
  await markRead(id)
  await loadNotifications(currentUser.value)
}

async function handleMarkAllRead() {
  notifications.value.forEach((m) => { m.readFlag = true })
  const unread = notifications.value.filter((m) => m.readFlag)
  await Promise.all(unread.map((m) => markRead(m.id)))
  await loadNotifications(currentUser.value)
}
</script>