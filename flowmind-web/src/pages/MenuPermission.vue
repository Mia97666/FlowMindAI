<template>
  <section class="page-grid">
    <el-card class="wide-panel" shadow="never">
      <template #header>
        <div class="panel-header">
          <span>菜单管理</span>
          <div class="table-toolbar">
            <el-button :icon="Plus" @click="newRootMenu">新增顶级菜单</el-button>
            <el-button :disabled="!selectedMenuId" @click="newChildMenu">新增子菜单</el-button>
            <el-select v-model="selectedMenuRoleId" placeholder="选择角色" filterable @change="handleRoleChange">
              <el-option v-for="role in roles" :key="role.id" :label="`${role.name}（${role.code}）`" :value="role.id" />
            </el-select>
            <el-button type="primary" :icon="DocumentChecked" @click="handleSaveRoleMenus">保存授权</el-button>
          </div>
        </div>
      </template>
      <div class="menu-management-grid">
        <div class="menu-tree-panel">
          <div class="sub-panel-title">菜单层级</div>
          <el-input
            v-model="menuTreeFilter"
            placeholder="搜索菜单..."
            clearable
            :prefix-icon="Search"
            class="tree-filter-input"
          />
          <el-tree
            ref="manageTreeRef"
            :data="menuManageTree"
            :props="menuTreeProps"
            node-key="id"
            default-expand-all
            highlight-current
            :filter-node-method="filterMenuNode"
            class="permission-tree"
            @node-click="selectMenu"
          >
            <template #default="{ data }">
              <span class="menu-tree-node">
                <span>{{ data.menuName }}</span>
                <el-tag size="small" :type="data.status === 'ENABLED' ? 'success' : 'info'" effect="plain">
                  {{ data.menuType }}
                </el-tag>
              </span>
            </template>
          </el-tree>
        </div>

        <div class="menu-detail-panel">
          <div class="sub-panel-title">菜单详情</div>
          <el-form label-position="top" class="menu-form-grid">
            <el-form-item label="菜单名称">
              <el-input v-model="menuForm.menuName" placeholder="例如 表单设计" />
            </el-form-item>
            <el-form-item label="菜单编码">
              <el-input v-model="menuForm.menuCode" placeholder="例如 design.form.designer" />
            </el-form-item>
            <el-form-item label="父菜单">
              <el-select v-model="menuForm.parentId" clearable filterable placeholder="顶级菜单">
                <el-option v-for="menu in flatMenus" :key="menu.id" :label="menuPathLabel(menu)" :value="menu.id" :disabled="menu.id === menuForm.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="菜单类型">
              <el-select v-model="menuForm.menuType">
                <el-option label="顶部导航" value="TOP" />
                <el-option label="菜单分组" value="GROUP" />
                <el-option label="页面入口" value="PAGE" />
              </el-select>
            </el-form-item>
            <el-form-item label="路由 Key">
              <el-input v-model="menuForm.routeKey" placeholder="例如 formDesigner" />
            </el-form-item>
            <el-form-item label="权限编码">
              <el-input v-model="menuForm.permissionCode" placeholder="例如 form:design" />
            </el-form-item>
            <el-form-item label="排序号">
              <el-input-number v-model="menuForm.sortOrder" :min="0" :max="9999" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="menuForm.status">
                <el-option label="启用" value="ENABLED" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-form>
          <div class="approval-actions">
            <el-button type="primary" :icon="DocumentChecked" @click="saveMenu">保存菜单</el-button>
            <el-button v-if="menuForm.id && menuForm.status === 'ENABLED'" type="warning" @click="disableMenuAction">停用</el-button>
            <el-button v-if="menuForm.id && menuForm.status === 'DISABLED'" type="success" @click="enableMenuAction">启用</el-button>
          </div>
        </div>
      </div>

      <el-divider content-position="left">角色菜单授权</el-divider>
      <el-tree
        ref="menuTreeRef"
        :data="permissionMenuTree"
        :props="menuTreeProps"
        node-key="id"
        show-checkbox
        default-expand-all
        :default-checked-keys="checkedMenuIds"
        class="permission-tree"
      />
    </el-card>
  </section>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentChecked, Plus, Search } from '@element-plus/icons-vue'
import { useSharedState } from '../composables/useSharedState'
import { menuTreeProps } from '../config/constants'
import { flattenMenus, menuPathLabel } from '../utils/helpers'
import { loadMenuTree, loadPermissionMenuTree, loadAllMenuTree, loadRoleMenus, saveRoleMenus, createMenu, updateMenu, disableMenu, enableMenu } from '../api/menu'

const { roles, permissionMenuTree, menuManageTree, checkedMenuIds } = useSharedState()

const menuTreeRef = ref(null)
const manageTreeRef = ref(null)
const selectedMenuId = ref(null)
const selectedMenuRoleId = ref(null)
const menuEditMode = ref('create')
const menuTreeFilter = ref('')

const menuForm = reactive({
  id: null, parentId: null, menuCode: '', menuName: '', routeKey: '',
  menuType: 'PAGE', permissionCode: '', sortOrder: 0, status: 'ENABLED',
})

const flatMenus = computed(() => flattenMenus(menuManageTree.value))

function resetMenuForm(parentId) {
  Object.assign(menuForm, {
    id: null, parentId: parentId || null, menuCode: '', menuName: '', routeKey: '',
    menuType: 'PAGE', permissionCode: '', sortOrder: 0, status: 'ENABLED',
  })
}

function selectMenu(menu) {
  selectedMenuId.value = menu.id
  Object.assign(menuForm, {
    id: menu.id, parentId: menu.parentId || null, menuCode: menu.menuCode,
    menuName: menu.menuName, routeKey: menu.routeKey || '', menuType: menu.menuType,
    permissionCode: menu.permissionCode || '', sortOrder: menu.sortOrder || 0,
    status: menu.status || 'ENABLED',
  })
}

function newRootMenu() {
  menuEditMode.value = 'create'
  resetMenuForm(null)
}

function newChildMenu() {
  menuEditMode.value = 'create'
  resetMenuForm(selectedMenuId.value)
}

async function saveMenu() {
  if (!menuForm.menuCode || !menuForm.menuName) {
    ElMessage.warning('菜单编码和菜单名称不能为空')
    return
  }
  const payload = {
    parentId: menuForm.parentId, menuCode: menuForm.menuCode,
    menuName: menuForm.menuName, routeKey: menuForm.routeKey,
    menuType: menuForm.menuType, permissionCode: menuForm.permissionCode,
    sortOrder: menuForm.sortOrder, status: menuForm.status,
  }
  const saved = menuForm.id
    ? await updateMenu(menuForm.id, payload)
    : await createMenu(payload)
  ElMessage.success('菜单已保存')
  await loadAllMenuTree()
  selectMenu(saved)
}

async function disableMenuAction() {
  if (!menuForm.id) return
  await disableMenu(menuForm.id)
  ElMessage.success('菜单已停用')
  await loadAllMenuTree()
}

async function enableMenuAction() {
  if (!menuForm.id) return
  await enableMenu(menuForm.id)
  ElMessage.success('菜单已启用')
  await loadAllMenuTree()
}

async function handleRoleChange(roleId) {
  if (!roleId) return
  const ids = await loadRoleMenus(roleId)
  checkedMenuIds.value = Array.isArray(ids) ? ids : []
  menuTreeRef.value?.setCheckedKeys(checkedMenuIds.value)
}

async function handleSaveRoleMenus() {
  if (!selectedMenuRoleId.value) {
    ElMessage.warning('请先选择角色')
    return
  }
  const checkedKeys = menuTreeRef.value?.getCheckedKeys() || []
  const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys() || []
  await saveRoleMenus(selectedMenuRoleId.value, [...checkedKeys, ...halfCheckedKeys])
  ElMessage.success('角色菜单授权已保存')
}

watch(checkedMenuIds, (ids) => {
  if (menuTreeRef.value) {
    menuTreeRef.value.setCheckedKeys(ids || [])
  }
})

watch(menuTreeFilter, (val) => {
  manageTreeRef.value?.filter(val)
})

function filterMenuNode(value, data) {
  if (!value) return true
  return (data.menuName || '').toLowerCase().includes(value.toLowerCase())
}
</script>