/**
 * Vite 插件：SVG 雪碧图（vite-plugin-svg-icons）。
 *
 * 把 src/assets/icons/svg 下的图标汇成一个 <symbol> 集合（虚拟模块 + 运行时注入
 * 页面 body，默认 inject: 'body-last'），业务代码通过
 * <svg><use xlink:href="#icon-xxx" /></svg> 引用（见 components/SvgIcon），
 * 好处是图标只随主包加载一次，不再有逐张 SVG 的网络请求。
 */
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import path from 'path'

/**
 * 创建 SVG 图标插件实例。
 * @param {boolean} isBuild 是否处于 vite build 阶段；为 true 时启用 svgo 压缩图标
 * @returns {import('vite').Plugin} 插件对象
 */
export default function createSvgIcon(isBuild) {
    return createSvgIconsPlugin({
        // 图标源目录；process.cwd() 是前端工程根 web/，因此该配置要求
        // vite 从 web/ 目录启动（package.json 的 dev / build:prod 均满足）
		iconDirs: [path.resolve(process.cwd(), 'src/assets/icons/svg')],
        // symbol id 生成规则：[dir] 对应子目录、[name] 为文件名，
        // 如 system/user.svg → #icon-system-user；当前图标都平铺在根目录，
        // 插件会把空的 [dir] 折叠掉，实际 id 形如 #icon-404
        symbolId: 'icon-[dir]-[name]',
        // dev 阶段保留原始 SVG 便于调试，打包时再交给 svgo 压缩
        svgoOptions: isBuild
    })
}
