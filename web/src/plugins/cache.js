// 浏览器存储的两套薄封装，注册为 $cache 供组件调用。
// session 对应 sessionStorage（同标签页有效，关掉标签页即清空，适合临时筛选条件）；
// local 对应 localStorage（跨会话保留，需手动 remove，适合记住的布局/偏好）。
// set/get 只支持字符串，存对象请用 setJSON/getJSON；getJSON 没有 try/catch，
// 若该 key 下的内容不是合法 JSON 会直接抛错。浏览器禁用存储时 set 静默跳过、get 返回 null。
const sessionCache = {
  set (key, value) {
    if (!sessionStorage) {
      return
    }
    if (key != null && value != null) {
      sessionStorage.setItem(key, value)
    }
  },
  get (key) {
    if (!sessionStorage) {
      return null
    }
    if (key == null) {
      return null
    }
    return sessionStorage.getItem(key)
  },
  setJSON (key, jsonValue) {
    if (jsonValue != null) {
      this.set(key, JSON.stringify(jsonValue))
    }
  },
  getJSON (key) {
    const value = this.get(key)
    if (value != null) {
      return JSON.parse(value)
    }
    return null
  },
  remove (key) {
    sessionStorage.removeItem(key)
  }
}
// 本地持久化版本，结构与 sessionCache 完全一致，区别只在底层用 localStorage。
const localCache = {
  set (key, value) {
    if (!localStorage) {
      return
    }
    if (key != null && value != null) {
      localStorage.setItem(key, value)
    }
  },
  get (key) {
    if (!localStorage) {
      return null
    }
    if (key == null) {
      return null
    }
    return localStorage.getItem(key)
  },
  setJSON (key, jsonValue) {
    if (jsonValue != null) {
      this.set(key, JSON.stringify(jsonValue))
    }
  },
  getJSON (key) {
    const value = this.get(key)
    if (value != null) {
      return JSON.parse(value)
    }
    return null
  },
  remove (key) {
    localStorage.removeItem(key)
  }
}

export default {
  /**
   * 会话级缓存
   */
  session: sessionCache,
  /**
   * 本地缓存
   */
  local: localCache
}
