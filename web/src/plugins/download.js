import axios from 'axios'
import { ElLoading, ElMessage } from 'element-plus'
import { saveAs } from 'file-saver'
import { getToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { blobValidate } from '@/utils/ruoyi'

// 文件下载插件（$download）。zip() 直接使用 axios 而不是项目封装的 @/utils/request，
// 因为需要拿到原始 blob：request.js 的响应拦截器会按 JSON 解包并统一处理 code，
// 二进制流会被它当成错误数据处理。所以这里手动拼 baseURL、手动带上 Authorization。
// 后端出错时返回的仍是 JSON（只是被包装成 blob），交由 blobValidate 判别后回读文本解析错误码。
const baseURL = import.meta.env.VITE_APP_BASE_API
// 下载遮罩实例。同一时刻只应存在一个下载任务，否则后一次会覆盖引用，前一个遮罩关不掉。
let downloadLoadingInstance

export default {
  // 下载后端打包好的 ZIP 流。url 为接口相对路径，直接拼在 baseURL 后面；
  // name 是落盘文件名，由调用方决定（通常带时间戳）。后端返回非 blob 时按错误 JSON 处理。
  zip(url, name) {
    var url = baseURL + url
    downloadLoadingInstance = ElLoading.service({ text: "正在下载数据，请稍候", background: "rgba(0, 0, 0, 0.7)", })
    axios({
      method: 'get',
      url: url,
      responseType: 'blob',
      headers: { 'Authorization': 'Bearer ' + getToken() }
    }).then((res) => {
      const isBlob = blobValidate(res.data)
      if (isBlob) {
        const blob = new Blob([res.data], { type: 'application/zip' })
        this.saveAs(blob, name)
      } else {
        this.printErrMsg(res.data)
      }
      downloadLoadingInstance.close()
    }).catch((r) => {
      console.error(r)
      ElMessage.error('下载文件出现错误，请联系管理员！')
      downloadLoadingInstance.close()
    })
  },
  saveAs(text, name, opts) {
    saveAs(text, name, opts)
  },
  // 后端异常时返回的是被包成 blob 的 JSON，需要先读回文本再取错误码；
  // 优先级：errorCode 映射表 → 后端 msg → errorCode.default。
  async printErrMsg(data) {
    const resText = await data.text()
    const rspObj = JSON.parse(resText)
    const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode['default']
    ElMessage.error(errMsg)
  }
}

