// 当前登录用户的会话状态：token 持久化在 Cookie（见 utils/auth），角色与权限码由 getInfo() 拉取。
// permissions 形如 'blog:article:add'，供 $auth / v-hasPermi 判断；roles 供 v-hasRole 与 dynamicRoutes 过滤使用。
// 登录本身不拉用户信息：由 permission.js 的路由守卫在首次进入业务页时调用 getInfo()，
// 成功后再 generateRoutes() 挂载动态路由，这里只保存结果。
import router from '@/router'
import { ElMessageBox, } from 'element-plus'
import { login, logout, getInfo } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { isEmpty } from "@/utils/validate"
import defAva from '@/assets/images/profile.jpg'

const useUserStore = defineStore(
  'user',
  {
    // token 从 Cookie 取初值（刷新页面后仍保持登录）；其余字段在登录后由 getInfo 填充。
    // roles 为空数组表示「用户信息还没加载」，路由守卫据此决定是否补拉。
    state: () => ({
      token: getToken(),
      id: '',
      name: '',
      nickName: '',
      avatar: '',
      roles: [],
      permissions: []
    }),
    actions: {
      // 登录
      // 用户名先去空格再提交，密码与验证码原样传递（uuid 是验证码的标识）。
      // 后端返回 access_token（RuoYi 风格，字段名不是 token），这里同时写 Cookie 与内存；
      // 后续请求由 request.js 拦截器从 Cookie 取 token。失败直接 reject，由登录页负责提示。
      login(userInfo) {
        const username = userInfo.username.trim()
        const password = userInfo.password
        const code = userInfo.code
        const uuid = userInfo.uuid
        return new Promise((resolve, reject) => {
          login(username, password, code, uuid).then(res => {
            let data = res.data
            setToken(data.access_token)
            this.token = data.access_token
            resolve()
          }).catch(error => {
            reject(error)
          })
        })
      },
      // 获取用户信息
      // 拉取当前用户信息 + 角色 + 权限。注意返回结构：用户对象在 res.user（不是 res.data），
      // 角色与权限是同级数组，这里不解包 data，与其他接口的约定不同。
      // 角色为空时兜底 ['ROLE_DEFAULT']，防止出现「无角色即全放行」的越权。
      // 两个安全提示互斥：初始密码优先，其次密码过期；点「确定」跳个人中心的重置密码 tab。
      getInfo() {
        return new Promise((resolve, reject) => {
          getInfo().then(res => {
            const user = res.user
            const avatar = (isEmpty(user.avatar)) ? defAva : user.avatar
            if (res.roles && res.roles.length > 0) { // 验证返回的roles是否是一个非空数组
              this.roles = res.roles
              this.permissions = res.permissions
            } else {
              this.roles = ['ROLE_DEFAULT']
            }
            this.id = user.userId
            this.name = user.userName
            this.nickName = user.nickName
            this.avatar = avatar
            /* 初始密码提示 */
            if(res.isDefaultModifyPwd) {
              ElMessageBox.confirm('您的密码还是初始密码，请修改密码！',  '安全提示', {  confirmButtonText: '确定',  cancelButtonText: '取消',  type: 'warning' }).then(() => {
                router.push({ name: 'Profile', params: { activeTab: 'resetPwd' } })
              }).catch(() => {})
            }
            /* 过期密码提示 */
            if(!res.isDefaultModifyPwd && res.isPasswordExpired) {
              ElMessageBox.confirm('您的密码已过期，请尽快修改密码！',  '安全提示', {  confirmButtonText: '确定',  cancelButtonText: '取消',  type: 'warning' }).then(() => {
                router.push({ name: 'Profile', params: { activeTab: 'resetPwd' } })
              }).catch(() => {})
            }
            resolve(res)
          }).catch(error => {
            reject(error)
          })
        })
      },
      // 退出系统
      // 退出：先请求后端注销（让服务端 token 失效），成功后才清内存与 Cookie。
      // 接口失败则保留本地状态并 reject，避免「后端没退成功、本地却已清空」的不一致。
      logOut() {
        return new Promise((resolve, reject) => {
          logout(this.token).then(() => {
            this.token = ''
            this.roles = []
            this.permissions = []
            removeToken()
            resolve()
          }).catch(error => {
            reject(error)
          })
        })
      }
    }
  })

export default useUserStore
