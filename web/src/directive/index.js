// 自定义指令注册入口。main.js 中直接调用 directive(app)（不是 app.use），
// 必须在 app.mount() 之前执行，否则首屏元素上绑定的指令不会生效。
//   v-hasRole / v-hasPermi  按钮级权限控制，无权限时把 DOM 移除
//   v-copyText              点击复制文本，可挂复制成功回调
import hasRole from './permission/hasRole'
import hasPermi from './permission/hasPermi'
import copyText from './common/copyText'

export default function directive(app){
  // 角色维度：v-hasRole="['admin']"，值必须是角色标识数组
  app.directive('hasRole', hasRole)
  // 权限维度：v-hasPermi="['blog:article:add']"，值必须是权限码数组
  app.directive('hasPermi', hasPermi)
  // 复制：v-copyText="text"，可再加 v-copyText:callback="fn" 接收复制结果
  app.directive('copyText', copyText)
}