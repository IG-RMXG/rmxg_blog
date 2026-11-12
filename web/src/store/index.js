// Pinia 根实例，main.js 中 app.use(store) 注册。
// createPinia 没有显式 import：vite/plugins/auto-import.js 配置了 unplugin-auto-import 的
// 'pinia' 预设，编译期会自动补 import；各 store 模块里的 defineStore 同理。
// 不要在这里手动补 import，否则会与自动导入重复声明。
const store = createPinia()

export default store