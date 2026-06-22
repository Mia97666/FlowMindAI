<template>
  <section class="page-grid">
    <el-card v-show="activeRouteKey === 'userList'" class="wide-panel" shadow="never">
      <template #header>
        <div class="panel-header">
          <span>用户管理</span>
          <el-button type="primary" :icon="Plus" @click="openCreateUser">新增用户</el-button>
        </div>
      </template>
      <div class="management-filter-bar">
        <el-input v-model="filters.username" placeholder="用户名" clearable @keyup.enter="search" />
        <el-input v-model="filters.realName" placeholder="姓名" clearable @keyup.enter="search" />
        <el-input v-model="filters.department" placeholder="部门" clearable @keyup.enter="search" />
        <el-select v-model="filters.status" placeholder="状态" clearable>
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-table :data="userPageRows" height="500" empty-text="暂无用户">
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column prop="realName" label="姓名" width="130" />
        <el-table-column prop="department" label="部门" width="130" />
        <el-table-column prop="position" label="岗位" min-width="160" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="plain">{{ row.status === 'ENABLED' ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="220">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role.code" class="role-tag" effect="plain">{{ role.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEditUser(row)">编辑</el-button>
            <el-button text type="primary" @click="openUserRoleDialog(row)">角色设置</el-button>
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

    <el-dialog v-model="userDialogVisible" :title="userDialogTitle" width="760px">
      <el-form label-position="top" class="field-dialog-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名">
              <el-input v-model="userForm.username" placeholder="例如 zhangsan" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名">
              <el-input v-model="userForm.realName" placeholder="例如 张三" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="userForm.phone" placeholder="例如 13800000000" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="userForm.email" placeholder="例如 zhangsan@flowmind.local" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="部门">
              <el-input v-model="userForm.department" placeholder="例如 研发部" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="岗位">
              <el-input v-model="userForm.position" placeholder="例如 研发工程师" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="直属领导">
              <el-select v-model="userForm.managerId" filterable clearable placeholder="请选择直属领导">
                <el-option v-for="user in users" :key="user.id" :label="`${user.realName}（${user.username}）`" :value="user.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="userForm.status">
                <el-option label="启用" value="ENABLED" />
                <el-option label="停用" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户类型">
              <el-select v-model="userForm.userType">
                <el-option label="普通用户" value="NORMAL" />
                <el-option label="AI 审批员" value="AI" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色">
              <el-select v-model="userForm.roleCodes" multiple filterable placeholder="请选择角色">
                <el-option v-for="role in roles" :key="role.code" :label="`${role.name}（${role.code}）`" :value="role.code" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="userDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveUser">保存用户</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="userRoleDialogVisible" title="用户角色设置" width="560px">
      <div v-if="roleEditingUser" class="role-assign-panel">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户">{{ roleEditingUser.realName }}（{{ roleEditingUser.username }}）</el-descriptions-item>
          <el-descriptions-item label="部门岗位">{{ roleEditingUser.department || '-' }} / {{ roleEditingUser.position || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-checkbox-group v-model="editingUserRoleCodes" class="role-check-list">
          <el-checkbox v-for="role in roles" :key="role.code" :label="role.code">
            {{ role.name }}（{{ role.code }}）
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="userRoleDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveUserRoles">保存角色</el-button>
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
import { loadUserPage as fetchUserPage, createUser, updateUser, updateUserRoles } from '../api/user'

const { users, roles, userPageRows, activeRouteKey } = useSharedState()

const { filters, pagination, loading, load, search, reset } = usePageableList(
  (page, size, f) => fetchUserPage(page, size, f),
  {
    initialFilters: { username: '', realName: '', department: '', status: '' },
    onResult: (rows) => { userPageRows.value = rows }
  }
)

const userDialogVisible = ref(false)
const userDialogMode = ref('create')
const userDialogTitle = ref('新增用户')
const userRoleDialogVisible = ref(false)
const roleEditingUser = ref(null)
const editingUserRoleCodes = ref([])

const userForm = reactive({
  id: null, username: '', realName: '', phone: '', email: '',
  department: '', position: '', managerId: null, status: 'ENABLED',
  userType: 'NORMAL', roleCodes: [],
})

function resetUserForm() {
  Object.assign(userForm, {
    id: null, username: '', realName: '', phone: '', email: '',
    department: '', position: '', managerId: null, status: 'ENABLED',
    userType: 'NORMAL', roleCodes: [],
  })
}

function openCreateUser() {
  userDialogMode.value = 'create'
  userDialogTitle.value = '新增用户'
  resetUserForm()
  userDialogVisible.value = true
}

function openEditUser(row) {
  userDialogMode.value = 'edit'
  userDialogTitle.value = '编辑用户'
  Object.assign(userForm, {
    id: row.id, username: row.username, realName: row.realName,
    phone: row.phone || '', email: row.email || '',
    department: row.department || '', position: row.position || '',
    managerId: row.managerId || null, status: row.status || 'ENABLED',
    userType: row.userType || 'NORMAL', roleCodes: (row.roles || []).map((r) => r.code),
  })
  userDialogVisible.value = true
}

async function saveUser() {
  const payload = {
    username: userForm.username, realName: userForm.realName,
    phone: userForm.phone, email: userForm.email,
    department: userForm.department, position: userForm.position,
    managerId: userForm.managerId, status: userForm.status,
    userType: userForm.userType, roleCodes: userForm.roleCodes,
  }
  if (userDialogMode.value === 'create') {
    await createUser(payload)
  } else {
    await updateUser(userForm.id, payload)
  }
  userDialogVisible.value = false
  ElMessage.success('用户已保存')
  await load()
}

function openUserRoleDialog(row) {
  roleEditingUser.value = row
  editingUserRoleCodes.value = (row.roles || []).map((r) => r.code)
  userRoleDialogVisible.value = true
}

async function saveUserRoles() {
  if (!roleEditingUser.value) return
  await updateUserRoles(roleEditingUser.value.id, editingUserRoleCodes.value)
  userRoleDialogVisible.value = false
  ElMessage.success('角色已更新')
  await load()
}
</script>
