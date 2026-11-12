/**
 * Vite 插件统一入口。
 *
 * vite.config.js 里一次性调用 createVitePlugins(env, command === 'build')，
 * 本文件决定「用哪些插件、以什么顺序注册」。顺序有意义：
 * 1) plugin-vue 负责 SFC 编译，必须最先；
 * 2) 自动导入 / script setup 扩展是编译期语法增强，紧随其后；
 * 3) svg-icon 注册图标集合；
 * 4) 压缩插件只在打包时追加，放最后不影响其它插件的转换结果。
 */
// 核心插件：解析 .vue 单文件组件（template / script / style 三段）
import vue from '@vitejs/plugin-vue'

// 本地插件工厂，各自返回一个或一组 Vite 插件
import createAutoImport from './auto-import'
import createSvgIcon from './svg-icon'
import createCompression from './compression'
import createSetupExtend from './setup-extend'

/**
 * 汇总项目所需的所有 Vite 插件。
 * @param {Record<string, string>} viteEnv loadEnv() 读取到的 .env.* 变量集合
 * @param {boolean} [isBuild=false] 是否为 vite build；dev 阶段为 false，
 *   用于关闭只对产物有意义的处理（图标 svgo 压缩、gzip/brotli 预压缩）
 * @returns {import('vite').PluginOption[]} 直接交给 vite 的 plugins 配置
 */
export default function createVitePlugins(viteEnv, isBuild = false) {
    // 数组顺序即插件注册顺序，后续注释说明每一项的职责
    const vitePlugins = [vue()]
    // 免 import 使用 ref / computed / useRouter / defineStore 等 API
    vitePlugins.push(createAutoImport())
    // 允许写 <script setup name="Xxx">，让组件名不依赖文件名
	vitePlugins.push(createSetupExtend())
    // 扫描 src/assets/icons/svg 并注册为 symbol 图标
    vitePlugins.push(createSvgIcon(isBuild))
    // 仅打包时按 VITE_BUILD_COMPRESS 追加压缩插件；空配置时展开为空，不会报错
	isBuild && vitePlugins.push(...createCompression(viteEnv))
    return vitePlugins
}
