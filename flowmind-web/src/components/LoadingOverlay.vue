<template>
  <Teleport to="body">
    <Transition name="loading-fade">
      <div v-if="visible" class="loading-overlay" @click.prevent.stop>
        <div class="loading-spinner">
          <el-icon class="is-loading" :size="36"><Loading /></el-icon>
          <p v-if="text">{{ text }}</p>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { Loading } from '@element-plus/icons-vue'
import { DEFAULT_LOADING_TEXT } from '../composables/useLoading'

defineProps({
  visible: { type: Boolean, default: false },
  text: { type: String, default: DEFAULT_LOADING_TEXT },
})
</script>

<style scoped>
.loading-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(4px);
}

.loading-spinner {
  display: grid;
  gap: 16px;
  justify-items: center;
  padding: 32px 48px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 16px 40px rgba(22, 34, 52, 0.14);
}

.loading-spinner p {
  margin: 0;
  color: #65707f;
  font-size: 14px;
}

.loading-fade-enter-active,
.loading-fade-leave-active {
  transition: opacity 0.25s ease;
}

.loading-fade-enter-from,
.loading-fade-leave-to {
  opacity: 0;
}
</style>
