<template>
  <section class="page-grid">
    <el-card class="wide-panel" shadow="never">
      <template #header>
        <div class="panel-header">
          <span>角色管理</span>
          <el-button type="primary" :icon="Plus" @click="openCreateRole">新增角色</el-button>
        </div>
      </template>
      <div class="management-filter-bar compact">
        <el-input v-model="filters.code" placeholder="角色编码" clearable @keyup.enter="search" />
        <el-input v-model="filters.name" placeholder="角色名称" clearable @keyup.enter="search" />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-table :data="rolePageRows" height="500" empty-text="暂无角色">
        <el-table-column prop="code" label="编码" width="160" />
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column prop="description" label="描述" min-width="220" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEditRole(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="table-pagination"
        layout="total, sizes, prev, pager, next"
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        @size-change="search"
        @current-change="load"
      />
    </el-card>

    <el-dialog v-model="roleDialogVisible" :title="roleDialogTitle" width="620px">
      <el-form label-position="top" class="field-dialog-form">
        <el-form-item label="角色编码">
          <el-input v-model="roleForm.code" placeholder="例如 FINANCE" />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input v-model="roleForm.name" placeholder="例如 财务人员" />
        </el-form-item>
        <el-form-item label="角色描述">
          <el-input v-model="roleForm.description" type="textarea" :rows="4" placeholder="说明角色职责和权限边界" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="roleDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveRole">保存角色</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { useSharedState } from '../composables/useSharedState'
import { usePageableList } from '../composables/usePageableList'
import { loadRolePage as fetchRolePage, createRole, updateRole } from '../api/role'

const { rolePageRows } = useSharedState()

const { filters, pagination, load, search, reset } = usePageableList(
  (page, size, f) => fetchRolePage(page, size, f),
  {
    initialFilters: { code: '', name: '' },
    onResult: (rows) => { rolePageRows.value = rows }
  }
)

const roleDialogVisible = ref(false)
const roleDialogMode = ref('create')
const roleDialogTitle = ref('新增角色')

const roleForm = reactive({ id: null, code: '', name: '', description: '' })

function resetRoleForm() {
  Object.assign(roleForm, { id: null, code: '', name: '', description: '' })
}

function openCreateRole() {
  roleDialogMode.value = 'create'
  roleDialogTitle.value = '新增角色'
  resetRoleForm()
  roleDialogVisible.value = true
}

function openEditRole(row) {
  roleDialogMode.value = 'edit'
  roleDialogTitle.value = '编辑角色'
  Object.assign(roleForm, {
    id: row.id, code: row.code, name: row.name, description: row.description || '',
  })
  roleDialogVisible.value = true
}

async function saveRole() {
  const payload = { code: roleForm.code, name: roleForm.name, description: roleForm.description }
  if (roleDialogMode.value === 'create') {
    await createRole(payload)
  } else {
    await updateRole(roleForm.id, payload)
  }
  roleDialogVisible.value = false
  ElMessage.success('角色已保存')
  await load()
}
</script>
