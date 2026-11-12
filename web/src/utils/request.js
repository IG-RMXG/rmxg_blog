/**
 * axios 实例与全局拦截器，所有 api/*.js 的请求都经由这里发出。
 *
 * 对外契约：
 * - baseURL 取 VITE_APP_BASE_API（由 vite 代理转发到网关）；
 * - 请求头里 isToken: false 表示不带 token，repeatSubmit: false 表示关闭防重复提交；
 * - 响应成功时返回 res.data（后端 JSON body，含 code/msg/data），调用方通常再取 .data；
 * - code 非 200 由拦截器统一提示并 reject，业务代码不必重复弹窗；
 * - 文件下载走 download()，responseType 为 blob，需自行识别“错误 JSON”。
 */
import axios from 'axios'
import { ElNotification , ElMessageBox, ElMessage, ElLoading } from 'element-plus'
import { getToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { tansParams, blobValidate } from '@/utils/ruoyi'
import cache from '@/plugins/cache'
import { saveAs } from 'file-saver'
import useUserStore from '@/store/modules/user'

let downloadLoadingInstance
// 并发请求同时过期时只弹一次重新登录框，靠这个标记互斥
// 是否显示重新登录
export let isRelogin = { show: false }

// UAT 模式判定。这里不复用 @/utils/uat 的 isUat：uat.js 会 import @/api/uat，
// 而 api/uat 又 import 本文件，形成 request → uat → api/uat → request 的循环依赖。
// VITE_APP_ENV 是构建期静态替换的常量，直接读即可，效果等同。
const isUatMode = import.meta.env.VITE_APP_ENV === 'uat'

axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'
// 创建axios实例
const service = axios.create({
  // axios中请求配置有baseURL选项，表示请求URL公共部分
  baseURL: import.meta.env.VITE_APP_BASE_API,
  // 超时
  timeout: 10000
})

// request拦截器
service.interceptors.request.use(config => {
  // 是否需要设置 token
  const isToken = (config.headers || {}).isToken === false
  // 是否需要防止数据重复提交
  const isRepeatSubmit = (config.headers || {}).repeatSubmit === false
  if (getToken() && !isToken) {
    config.headers['Authorization'] = 'Bearer ' + getToken() // 让每个请求携带自定义token 请根据实际情况自行修改
  }
  // get请求映射params参数
  if (config.method === 'get' && config.params) {
    // tansParams 拼出来的串末尾多一个 &，这里用 slice(0,-1) 去掉；
    // 同时把 params 清空，避免 axios 再用 params 拼一遍
    let url = config.url + '?' + tansParams(config.params)
    url = url.slice(0, -1)
    config.params = {}
    config.url = url
  }
  if (!isRepeatSubmit && (config.method === 'post' || config.method === 'put')) {
    const requestObj = {
      url: config.url,
      data: typeof config.data === 'object' ? JSON.stringify(config.data) : config.data,
      time: new Date().getTime()
    }
    const requestSize = Object.keys(JSON.stringify(requestObj)).length // 请求数据大小
    const limitSize = 5 * 1024 * 1024 // 限制存放数据5M
    if (requestSize >= limitSize) {
      console.warn(`[${config.url}]: ` + '请求数据大小超出允许的5M限制，无法进行防重复提交验证。')
      return config
    }
    // 用 sessionStorage 记录最近一次提交（键名固定为 sessionObj），只与它比较
    const sessionObj = cache.session.getJSON('sessionObj')
    if (sessionObj === undefined || sessionObj === null || sessionObj === '') {
      cache.session.setJSON('sessionObj', requestObj)
    } else {
      const s_url = sessionObj.url                // 请求地址
      const s_data = sessionObj.data              // 请求数据
      const s_time = sessionObj.time              // 请求时间
      const interval = 1000                       // 间隔时间(ms)，小于此时间视为重复提交
      if (s_data === requestObj.data && requestObj.time - s_time < interval && s_url === requestObj.url) {
        const message = '数据正在处理，请勿重复提交'
        console.warn(`[${s_url}]: ` + message)
        return Promise.reject(new Error(message))
      } else {
        cache.session.setJSON('sessionObj', requestObj)
      }
    }
  }
  return config
}, error => {
    console.log(error)
    Promise.reject(error)
})

// 响应拦截器
service.interceptors.response.use(res => {
    // 未设置状态码则默认成功状态
    const code = res.data.code || 200
    // 获取错误信息
    const msg = errorCode[code] || res.data.msg || errorCode['default']
    // 二进制数据则直接返回
    if (res.request.responseType ===  'blob' || res.request.responseType ===  'arraybuffer') {
      return res.data
    }
    if (code === 401) {
      // 用户取消后停留在原页面，后续任意请求失败会再次触发此分支
      if (!isRelogin.show) {
        isRelogin.show = true
        ElMessageBox.confirm('登录状态已过期，您可以继续留在该页面，或者重新登录', '系统提示', { confirmButtonText: '重新登录', cancelButtonText: '取消', type: 'warning' }).then(() => {
          isRelogin.show = false
          useUserStore().logOut().then(() => {
            location.href = '/index'
          })
      }).catch(() => {
        isRelogin.show = false
      })
    }
      return Promise.reject('无效的会话，或者会话已过期，请重新登录。')
    } else if (code === 500) {
      ElMessage({ message: msg, type: 'error' })
      return Promise.reject(new Error(msg))
    } else if (code === 601) {
      // 601 为业务警告码：只弹警告，不跳转，但同样 reject，调用方仍需 catch
      ElMessage({ message: msg, type: 'warning' })
      return Promise.reject(new Error(msg))
    } else if (code !== 200) {
      ElNotification.error({ title: msg })
      return Promise.reject('error')
    } else {
      // 成功时返回后端 body 本身：调用方拿到的 res 已含 code/msg/data，业务数据在 res.data
      return  Promise.resolve(res.data)
    }
  },
  error => {
    console.log('err' + error)
    let { message } = error
    const status = error.response && error.response.status
    const body = error.response && error.response.data
    // UAT 模式下最常见的一种“500”其实是 vite 代理层的错误：
    // /uat-api 连不上 UAT 网关 8890（服务没起）时，代理直接回 500 且 body 为空，
    // 而真正的后端业务 500 一定带 {code,msg} JSON。两者必须分开，
    // 否则用户看到的是误导性的“系统接口500异常”，会去查后端代码。
    const isProxyError = isUatMode &&
      [500, 502, 503, 504].indexOf(status) !== -1 &&
      !(body && typeof body === 'object' && (body.msg || body.message))
    if (isProxyError) {
      message = 'UAT 后端服务未启动（网关 :8890 无响应），请先执行 scripts\\uat-up.ps1 -SkipBuild'
    } else if (message == "Network Error") {
      message = "后端接口连接异常"
    } else if (message.includes("timeout")) {
      message = "系统接口请求超时"
    } else if (message.includes("Request failed with status code")) {
      message = "系统接口" + message.substr(message.length - 3) + "异常"
    }
    ElMessage({ message: message, type: 'error', duration: (isProxyError ? 8 : 5) * 1000, showClose: isProxyError })
    // handled=true 表示提示语已由拦截器给出（且是改写后的可操作文案），
    // 调用方（如登录页 UAT 快捷进入）不要再用原始 axios 报错重复弹一次。
    if (isProxyError) {
      return Promise.reject(Object.assign(new Error(message), { handled: true }))
    }
    return Promise.reject(error)
  }
)

// 通用下载方法
// 以 form-urlencoded POST 表单参数，响应按 blob 取；后端出错时会返回 JSON 而不是文件流，
// 所以先 blobValidate 判断，是 JSON 就解析出 msg 提示用户。
export function download(url, params, filename, config) {
  downloadLoadingInstance = ElLoading.service({ text: "正在下载数据，请稍候", background: "rgba(0, 0, 0, 0.7)", })
  return service.post(url, params, {
    transformRequest: [(params) => { return tansParams(params) }],
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    responseType: 'blob',
    ...config
  }).then(async (data) => {
    const isBlob = blobValidate(data)
    if (isBlob) {
      const blob = new Blob([data])
      saveAs(blob, filename)
    } else {
      const resText = await data.text()
      const rspObj = JSON.parse(resText)
      const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode['default']
      ElMessage.error(errMsg)
    }
    downloadLoadingInstance.close()
  }).catch((r) => {
    console.error(r)
    ElMessage.error('下载文件出现错误，请联系管理员！')
    downloadLoadingInstance.close()
  })
}

export default service
