/**
 * Vite 插件：扩展 <script setup> 的能力（unplugin-vue-setup-extend-plus）。
 *
 * 作用：允许在 <script setup> 标签上直接写 name，例如
 * `<script setup name="User">`，从而显式指定组件名。
 * 为什么需要：<script setup> 默认以文件名推断组件名，而 keep-alive 的
 * include/exclude 是按组件名匹配的，靠文件名容易与实际业务名不一致。
 */
import setupExtend from 'unplugin-vue-setup-extend-plus/vite'

/**
 * 创建 setup 扩展插件实例。
 * @returns {import('vite').Plugin} 插件对象
 */
export default function createSetupExtend() {
    // 传空对象即使用插件默认配置（默认就开启了 name 解析）
    return setupExtend({})
}
