import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { completeTask as completeTaskApi } from '../api/task'
import { normalizeBusinessData } from '../utils/helpers'

/**
 * 审批任务完成操作 composable
 * 封装同意/拒绝/退回/转办的完整逻辑，供 TodoList 和 RuntimeForm 复用
 *
 * @param {Ref} selectedTask - 当前选中的任务
 * @param {Reactive} approvalBusinessData - 审批表单数据
 * @param {Ref} approvalFields - 审批表单字段定义
 * @param {Function} onAfterComplete - 完成后的回调，接收 action 参数
 */
export function useTaskCompletion(selectedTask, approvalBusinessData, approvalFields, onAfterComplete) {
  const submitting = ref(false)
  const approvalComment = ref('')
  const transferDialogVisible = ref(false)
  const transferTargetAssignee = ref('')
  const transferComment = ref('')
  const returnDialogVisible = ref(false)
  const returnComment = ref('')

  async function completeTaskAction(action, extras = {}) {
    if (!selectedTask.value) return
    submitting.value = true
    try {
      const payload = {
        action,
        comment: extras.comment !== undefined ? extras.comment : approvalComment.value,
        businessData: normalizeBusinessData(approvalBusinessData, approvalFields.value),
      }
      if (extras.targetAssignee) payload.targetAssignee = extras.targetAssignee
      await completeTaskApi(selectedTask.value.id, payload)
      const messages = { APPROVED: '审批已通过', REJECTED: '审批已拒绝', TRANSFER: '已转办给指定审批人' }
      ElMessage.success(messages[action] || '审批动作已提交')
      approvalComment.value = ''
      if (onAfterComplete) await onAfterComplete(action)
    } finally {
      submitting.value = false
    }
  }

  function completeTask(action) {
    if (action === 'TRANSFER') { openTransferDialog(); return }
    if (action === 'RETURN') { openReturnDialog(); return }
    completeTaskAction(action)
  }

  function openTransferDialog() {
    if (!selectedTask.value) { ElMessage.warning('请先选择一条待办任务'); return }
    transferTargetAssignee.value = ''
    transferComment.value = ''
    transferDialogVisible.value = true
  }

  async function confirmTransfer() {
    if (!transferTargetAssignee.value) { ElMessage.warning('请选择转办目标'); return }
    await completeTaskAction('TRANSFER', { targetAssignee: transferTargetAssignee.value, comment: transferComment.value })
    transferDialogVisible.value = false
  }

  function openReturnDialog() {
    if (!selectedTask.value) { ElMessage.warning('请先选择一条待办任务'); return }
    returnComment.value = ''
    returnDialogVisible.value = true
  }

  async function confirmReturn() {
    if (!returnComment.value || !returnComment.value.trim()) { ElMessage.warning('退回必须填写意见'); return }
    await completeTaskAction('REJECTED', { comment: `[退回] ${returnComment.value}` })
    returnDialogVisible.value = false
  }

  return {
    submitting,
    approvalComment,
    transferDialogVisible,
    transferTargetAssignee,
    transferComment,
    returnDialogVisible,
    returnComment,
    completeTask,
    completeTaskAction,
    openTransferDialog,
    confirmTransfer,
    openReturnDialog,
    confirmReturn,
  }
}