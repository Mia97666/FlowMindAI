<template>
  <el-aside class="app-sidebar" :class="{ collapsed: collapsed }" :style="{ width: collapsed ? '64px' : '248px' }">
    <div class="sidebar-module">
      <span v-show="!collapsed">当前模块</span>
      <strong v-show="!collapsed">{{ currentTopMenu?.menuName || '运行总览' }}</strong>
    </div>

    <el-menu
      :key="activeTopCode"
      :default-active="activeRouteKey"
      :default-openeds="defaultOpenMenuCodes"
      :unique-opened="false"
      :collapse="collapsed"
      class="side-menu"
      @select="handleSelect"
    >
      <template v-for="menu in sideMenus" :key="menu.id || menu.menuCode">
        <el-sub-menu v-if="isMenuGroup(menu)" :index="menu.menuCode">
          <template #title>
            <el-icon><component :is="menuIcon(menu)" /></el-icon>
            <span>{{ menu.menuName }}</span>
          </template>
          <el-menu-item v-if="routeSectionMap[menu.routeKey]" :index="menu.routeKey">
            <el-icon><component :is="menuIcon(menu)" /></el-icon>
            <span>{{ menu.menuName }}</span>
          </el-menu-item>
          <el-menu-item
            v-for="child in visibleChildren(menu)"
            :key="child.id || child.menuCode"
            :index="child.routeKey"
          >
            <el-icon><component :is="menuIcon(child)" /></el-icon>
            <span>{{ child.menuName }}</span>
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item v-else :index="menu.routeKey">
          <el-icon><component :is="menuIcon(menu)" /></el-icon>
          <span>{{ menu.menuName }}</span>
        </el-menu-item>
      </template>
    </el-menu>

    <div class="sidebar-footer" v-show="!collapsed">
      <strong>在线演示效果，由 Mia 制作</strong>
    </div>
    <div class="sidebar-toggle-btn" @click="$emit('toggle-collapse')">
      <el-icon :size="18"><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
    </div>
  </el-aside>
</template>

<script setup>
import { computed } from 'vue'
import { DocumentChecked, Operation, Expand, Fold } from '@element-plus/icons-vue'
import { menuIconMap } from '../config/constants'
import { routeSectionMap } from '../config/menuTree'

const props = defineProps({
  navigationTree: { type: Array, default: () => [] },
  activeTopCode: { type: String, default: '' },
  activeRouteKey: { type: String, default: '' },
  collapsed: { type: Boolean, default: false },
})

const emit = defineEmits(['navigate', 'toggle-collapse'])

const topMenus = computed(() =>
  props.navigationTree.filter((menu) => menu.menuType === 'TOP' || !menu.parentId)
)

const currentTopMenu = computed(() =>
  topMenus.value.find((menu) => menu.menuCode === props.activeTopCode) || topMenus.value[0]
)

const sideMenus = computed(() => visibleChildren(currentTopMenu.value))

const defaultOpenMenuCodes = computed(() =>
  sideMenus.value.filter((menu) => isMenuGroup(menu)).map((menu) => menu.menuCode)
)

function visibleChildren(menu) {
  if (!menu?.children) return []
  return menu.children.filter((child) => child.status !== 'DISABLED')
}

function isMenuGroup(menu) {
  return menu?.menuType === 'GROUP' || (visibleChildren(menu).length > 0 && !routeSectionMap[menu.routeKey])
}

function menuIcon(menu) {
  return menuIconMap[menu?.routeKey] || menuIconMap[menu?.menuCode] || (isMenuGroup(menu) ? Operation : DocumentChecked)
}

function handleSelect(routeKey) {
  emit('navigate', routeKey)
}
</script>