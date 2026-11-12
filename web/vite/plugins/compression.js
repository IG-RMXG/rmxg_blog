/**
 * Vite 插件：构建产物预压缩（vite-plugin-compression）。
 *
 * 只在 build（打包）阶段由 vite/plugins/index.js 按需挂载，dev 阶段不引入；
 * 是否启用及启用哪种算法由环境变量 VITE_BUILD_COMPRESS 决定
 * （目前 .env.production / .env.staging 均配置为 gzip）。
 * 产物是同目录下的 .gz / .br 副本，需要 Nginx 的 gzip_static / brotli 模块配合，
 * 否则浏览器仍会下载未压缩的原文件。
 */
import compression from 'vite-plugin-compression'

/**
 * 按环境变量创建压缩插件，可能返回 0~2 个插件（gzip / brotli）。
 * @param {Record<string, string>} env vite.config.js 中 loadEnv() 的返回值，
 *   这里只取 VITE_BUILD_COMPRESS；未配置时返回空数组，即不压缩。
 * @returns {import('vite').Plugin[]} 供 plugins 数组展开使用
 */
export default function createCompression(env) {
    // 取值形如 "gzip" / "gzip,brotli"；.env.development / .env.uat 未配置该变量，
    // 此时为 undefined，走下面的空数组分支（dev 阶段本来也不会挂载本插件）
    const { VITE_BUILD_COMPRESS } = env
    const plugin = []
    if (VITE_BUILD_COMPRESS) {
        // 支持逗号分隔的多算法配置，逐项判断以决定挂载几个压缩器
        const compressList = VITE_BUILD_COMPRESS.split(',')
        if (compressList.includes('gzip')) {
            // http://doc.ruoyi.vip/ruoyi-vue/other/faq.html#使用gzip解压缩静态文件
            plugin.push(
                compression({
                    // 生成 xxx.js.gz 同级副本；deleteOriginFile=false 保留原文件，
                    // 保证未开启 gzip_static 的服务器或本地 preview 仍可正常访问
                    ext: '.gz',
                    deleteOriginFile: false
                })
            )
        }
        if (compressList.includes('brotli')) {
            plugin.push(
                compression({
                    // Brotli 压缩率通常高于 gzip，但需服务端安装 brotli 模块才能生效；
                    // threshold（默认 1024 字节）以下的小文件会被插件跳过
                    ext: '.br',
                    algorithm: 'brotliCompress',
                    deleteOriginFile: false
                })
            )
        }
    }
    return plugin
}
