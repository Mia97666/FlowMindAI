import { inject } from 'vue'

export const SharedStateKey = Symbol('SharedState')

export function useSharedState() {
  const state = inject(SharedStateKey)
  if (!state) {
    throw new Error('useSharedState() must be used within an App that provides SharedStateKey')
  }
  return state
}