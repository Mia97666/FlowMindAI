<template>
  <section class="page-grid">
    <el-card class="wide-panel" shadow="never">
      <template #header>
        <div class="panel-header">
          <span>个人信息</span>
          <div class="table-toolbar">
            <el-tag type="primary" effect="plain">{{ currentUserProfile?.status || 'ENABLED' }}</el-tag>
            <el-button type="primary" :icon="Edit" @click="openEditProfile">编辑资料</el-button>
          </div>
        </div>
      </template>
      <el-descriptions v-if="currentUserProfile" :column="2" border>
        <el-descriptions-item label="用户名">{{ currentUserProfile.username }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ currentUserProfile.realName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentUserProfile.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentUserProfile.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ currentUserProfile.department || '-' }}</el-descriptions-item>
        <el-descriptions-item label="岗位">{{ currentUserProfile.position || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户类型">{{ currentUserProfile.userType || 'NORMAL' }}</el-descriptions-item>
        <el-descriptions-item label="直属领导">{{ managerName(currentUserProfile.managerId) }}</el-descriptions-item>
        <el-descriptions-item label="角色" :span="2">
          <el-tag v-for="role in currentUserProfile.roles || []" :key="role.code" class="role-tag" effect="plain">{{ role.name }}（{{ role.code }}）</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无当前用户信息" />
    </el-card>

    <el-dialog v-model="editDialogVisible" title="编辑个人信息" width="760px">
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-position="top" class="field-dialog-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="profileForm.realName" placeholder="例如 张三" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profileForm.phone" placeholder="例如 13800000000" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" placeholder="例如 zhangsan@flowmind.local" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="部门" prop="department">
              <el-input v-model="profileForm.department" placeholder="例如 研发部" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="岗位" prop="position">
              <el-input v-model="profileForm.position" placeholder="例如 研发工程师" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveProfile">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import { useSharedState } from '../composables/useSharedState'
import { updateUser, loadUsers } from '../api/user'

const { users, currentUser } = useSharedState()

const profileFormRef = ref(null)
const editDialogVisible = ref(false)

const currentUserProfile = computed(() => users.value.find((user) => user.username === currentUser.value))

const profileForm = reactive({
  id: null, username: '', realName: '', phone: '', email: '', department: '', position: '',
})

const profileRules = {
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
}

function managerName(managerId) {
  if (!managerId) return '-'
  const manager = users.value.find((user) => user.id === managerId)
  return manager ? `${manager.realName}（${manager.username}）` : '-'
}

function openEditProfile() {
  const profile = currentUserProfile.value
  if (!profile) return
  Object.assign(profileForm, {
    id: profile.id,
    username: profile.username,
    realName: profile.realName || '',
    phone: profile.phone || '',
    email: profile.email || '',
    department: profile.department || '',
    position: profile.position || '',
  })
  editDialogVisible.value = true
}

async function saveProfile() {
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return
  const payload = {
    username: profileForm.username,
    realName: profileForm.realName,
    phone: profileForm.phone,
    email: profileForm.email,
    department: profileForm.department,
    position: profileForm.position,
  }
  await updateUser(profileForm.id, payload)
  editDialogVisible.value = false
  ElMessage.success('个人信息已更新')
  users.value = await loadUsers()
}
</script>