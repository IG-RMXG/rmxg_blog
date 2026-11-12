// Element Plus 消息/弹窗的统一封装（$modal），把标题与按钮文案固定成中文，避免各页面重复写 options。
// 分四类：
//   msg* / notify*   轻提示，无返回值；
//   alert*           只有一个「确定」的强提示；
//   confirm / prompt 返回 Promise，用户点「取消」或关闭时 reject，
//                    所以调用方必须 catch（常写 .catch(() => {})），否则控制台会出现未捕获异常；
//   loading / closeLoading 全屏遮罩，单例，必须成对调用。
import { ElMessage, ElMessageBox, ElNotification, ElLoading } from 'element-plus'

// 遮罩单例：后一次 loading() 会覆盖前一次的引用，前一个实例就再也关不掉了，
// 所以约定同一时刻只开一个遮罩。
let loadingInstance

export default {
  // 消息提示
  msg(content) {
    ElMessage.info(content)
  },
  // 错误消息
  msgError(content) {
    ElMessage.error(content)
  },
  // 成功消息
  msgSuccess(content) {
    ElMessage.success(content)
  },
  // 警告消息
  msgWarning(content) {
    ElMessage.warning(content)
  },
  // 弹出提示
  alert(content) {
    ElMessageBox.alert(content, "系统提示")
  },
  // 错误提示
  alertError(content) {
    ElMessageBox.alert(content, "系统提示", { type: 'error' })
  },
  // 成功提示
  alertSuccess(content) {
    ElMessageBox.alert(content, "系统提示", { type: 'success' })
  },
  // 警告提示
  alertWarning(content) {
    ElMessageBox.alert(content, "系统提示", { type: 'warning' })
  },
  // 通知提示
  notify(content) {
    ElNotification.info(content)
  },
  // 错误通知
  notifyError(content) {
    ElNotification.error(content)
  },
  // 成功通知
  notifySuccess(content) {
    ElNotification.success(content)
  },
  // 警告通知
  notifyWarning(content) {
    ElNotification.warning(content)
  },
  // 确认窗体
  confirm(content) {
    return ElMessageBox.confirm(content, "系统提示", {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: "warning",
    })
  },
  // 提交内容
  prompt(content) {
    return ElMessageBox.prompt(content, "系统提示", {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: "warning",
    })
  },
  // 打开遮罩层
  loading(content) {
    loadingInstance = ElLoading.service({
      lock: true,
      text: content,
      background: "rgba(0, 0, 0, 0.7)",
    })
  },
  // 关闭遮罩层
  closeLoading() {
    loadingInstance.close()
  }
}
