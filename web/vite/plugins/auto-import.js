/**
 * Vite 插件：自动导入（unplugin-auto-import）。
 *
 * 职责：让 .vue / .js 文件里直接使用 Vue、Vue Router、Pinia 的 API
 * （ref、computed、onMounted、useRouter、defineStore 等）而不必手写 import，
 * 由插件在编译期自动注入 import 语句。
 * 注意：这只是构建期语法糖，IDE 的类型提示/跳转依赖生成的 dts 声明文件；
 * 本项目为纯 JavaScript 且已关闭 dts，因此编辑器里可能出现"未定义"提示，属正常现象。
 */
import autoImport from 'unplugin-auto-import/vite'

/**
 * 创建自动导入插件实例。
 * @returns {import('vite').Plugin} 供 vite/plugins/index.js 汇总进 plugins 数组
 */
export default function createAutoImport() {
    return autoImport({
        // 自动导入来源白名单：仅这三类运行时 API，自研 utils / api 仍需显式 import
        imports: [
            'vue',
            'vue-router',
            'pinia'
        ],
        // 不生成 auto-imports.d.ts：纯 JS 项目用不到声明文件，也避免多出未跟踪文件
        dts: false
    })
}
