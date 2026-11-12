/*
 * Vite 配置。这里用函数式导出，是因为需要在配置期拿到 mode（--mode 参数）
 * 与 command（serve / build）来做差异：代理目标、sourcemap、压缩插件都随之切换。
 *
 * 环境变量来自 loadEnv(mode, process.cwd())，即 web/.env.[mode]：
 *   yarn dev              → development
 *   yarn dev:uat          → uat（端口 81）
 *   yarn build:prod       → production
 *   yarn build:stage      → staging
 * 注意 mode 与 VITE_APP_ENV 是两个独立概念：前者决定加载哪个 env 文件，后者是给代码用的标识。
 */
import { defineConfig, loadEnv } from 'vite'
import path from 'path'
import createVitePlugins from './vite/plugins'

// https://vitejs.dev/config/
export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd())
  const { VITE_APP_ENV } = env
  // 上传图片回源：UAT 前端走 UAT 网关（图片落在 UAT blog-service 的 uploads 目录）
  const profileTarget = mode === 'uat' ? 'http://localhost:8890' : 'http://localhost:8889'
  // 代理目标没起时，vite 默认只打一行 AggregateError/ECONNREFUSED，
  // 很容易被当成“后端接口报错”去查代码。这里在 dev 终端补一句人话 + 修复命令。
  // 只对 UAT 模式的 8890 生效，避免干扰 DEV 的 8889。
  const uatProxyErrorHint = (prefix) => ({
    configure: (proxy) => {
      proxy.on('error', (err) => {
        if (mode !== 'uat') return
        console.error(`\n[vite] ${prefix} 代理到 UAT 网关 http://localhost:8890 失败：${err.code || err.message}`)
        console.error('[vite] UAT 后端没起？在仓库根目录执行：')
        console.error('[vite]   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\\uat-up.ps1 -SkipBuild\n')
      })
    }
  })
  return {
    // 部署生产环境和开发环境下的URL。
    // 默认情况下，vite 会假设你的应用是被部署在一个域名的根路径上
    // 例如 https://www.ruoyi.vip/。如果应用被部署在一个子路径上，你就需要用这个选项指定这个子路径。例如，如果你的应用被部署在 https://www.ruoyi.vip/admin/，则设置 baseUrl 为 /admin/。
    base: VITE_APP_ENV === 'production' ? '/' : '/',
    // 第二参数是构建态开关：只有 build 时才会按 .env 的 VITE_BUILD_COMPRESS 追加压缩插件
    // （生成 .gz / .br 预压缩产物，需 nginx 开启 gzip_static 之类才吃得到）；
    // dev 下压缩没有意义还会拖慢启动。插件注册顺序见 vite/plugins/index.js。
    plugins: createVitePlugins(env, command === 'build'),
    resolve: {
      // https://cn.vitejs.dev/config/#resolve-alias
      alias: {
        // 设置路径
        '~': path.resolve(__dirname, './'),
        // 设置别名
        '@': path.resolve(__dirname, './src')
      },
      // https://cn.vitejs.dev/config/#resolve-extensions
      extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
    },
    // 打包配置
    build: {
      // https://vite.dev/config/build-options.html
      // 构建产物不打 sourcemap：既减小体积，也避免把源码映射发布到线上。
      // 非 build 命令下取 'inline'，但 build.sourcemap 只对 build 生效，dev 的 sourcemap 由 dev server 自己处理。
      sourcemap: command === 'build' ? false : 'inline',
      outDir: 'dist',
      assetsDir: 'assets',
      // 单 chunk 告警阈值放宽到 2000KB（默认 500KB）：Element Plus 等依赖是全量引入的，
      // 按默认值构建会刷出大量「chunk larger than 500 kB」噪音，掩盖真正需要拆包的新问题。
      chunkSizeWarningLimit: 2000,
      rollupOptions: {
        output: {
          chunkFileNames: 'static/js/[name]-[hash].js',
          entryFileNames: 'static/js/[name]-[hash].js',
          assetFileNames: 'static/[ext]/[name]-[hash].[ext]'
        }
      }
    },
    // vite 相关配置
    server: {
      // 端口 80 是 DEV 的默认端口；dev:uat 脚本用 --port 81 覆盖，两套前端可以同时开着互不抢占。
      // host: true 监听 0.0.0.0，方便用局域网 IP / 手机真机访问；open 启动后自动打开浏览器。
      port: 80,
      host: true,
      open: true,
      proxy: {
        // https://cn.vitejs.dev/config/#server-proxy
        '/dev-api': {
          target: 'http://localhost:8889',
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/dev-api/, '')
        },
        // UAT 前端（yarn dev:uat，端口 81）专用代理：指向 UAT 网关 8890
        // 与 /dev-api 前缀区分，两套前端可同时运行互不干扰
        '/uat-api': {
          target: 'http://localhost:8890',
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/uat-api/, ''),
          ...uatProxyErrorHint('/uat-api')
        },
        // 上传文件静态资源（头像/博客图片）：UAT 模式回源 UAT 网关
        '/profile': {
          target: profileTarget,
          changeOrigin: true,
          ...uatProxyErrorHint('/profile')
        }
      }
    },
    // 自写 PostCSS 插件：在转换阶段删除所有 @charset 规则。
    // 原因：Element Plus 等第三方样式自带 @charset "UTF-8"，被 Vite 合并进同一个 CSS 文件后
    // 会落在非首行位置，从而触发「@charset must be the first rule in the file」告警。
    // 编码已由 index.html 的 meta charset 声明，删掉不影响中文显示。
    //fix:error:stdin>:7356:1: warning: "@charset" must be the first rule in the file
    css: {
      postcss: {
        plugins: [
          {
            postcssPlugin: 'internal:charset-removal',
            AtRule: {
              charset: (atRule) => {
                if (atRule.name === 'charset') {
                  atRule.remove()
                }
              }
            }
          }
        ]
      }
    }
  }
})
