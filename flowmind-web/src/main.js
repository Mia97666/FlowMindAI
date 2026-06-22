import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import App from './App.vue'
import router from './router'
import './styles.css'

createApp(App)
  .use(ElementPlus, { locale: zhCn })
  .use(router)
  .mount('#app')