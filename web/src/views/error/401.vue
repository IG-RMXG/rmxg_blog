<template>
  <div class="errPage-container">
    <!-- 点击「返回」：有 noGoBack 参数时回首页，否则回退上一页，见 back() -->
    <el-button icon="arrow-left" class="pan-back-btn" @click="back">
      返回
    </el-button>
    <!-- 左侧 401 说明、右侧动图，各占 12/24 栅格 -->
    <el-row>
      <el-col :span="12">
        <h1 class="text-jumbo text-ginormous">
          401错误!
        </h1>
        <h2>您没有访问权限！</h2>
        <h6>对不起，您没有访问权限，请不要进行非法操作！您可以返回主页面</h6>
        <ul class="list-unstyled">
          <li class="link-type">
            <router-link to="/">
              回首页
            </router-link>
          </li>
        </ul>
      </el-col>
      <el-col :span="12">
        <img :src="errGif" width="313" height="428" alt="Girl has dropped her ice cream.">
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
/**
 * 401 无权限错误页（常量路由 /401，hidden，不进侧边栏）。
 * 纯静态展示，不请求接口；提供「返回」与「回首页」两个出口。
 */
import errImage from "@/assets/401_images/401.gif"

let { proxy } = getCurrentInstance()

// 图片地址后拼毫秒时间戳，强制浏览器重新拉取，避免缓存导致动图只播一次或显示旧图
const errGif = ref(errImage + "?" + +new Date())

/**
 * 返回上一页；带 noGoBack 查询参数时直接回首页，
 * 用于不希望用 go(-1) 回到来源页（例如来源页同样无权限）的场景。
 */
function back() {
  if (proxy.$route.query.noGoBack) {
    proxy.$router.push({ path: "/" })
  } else {
    proxy.$router.go(-1)
  }
}
</script>

<style lang="scss" scoped>
.errPage-container {
  width: 800px;
  max-width: 100%;
  margin: 100px auto;
  .pan-back-btn {
    background: #008489;
    color: #fff;
    border: none !important;
  }
  .pan-gif {
    margin: 0 auto;
    display: block;
  }
  .pan-img {
    display: block;
    margin: 0 auto;
    width: 100%;
  }
  .text-jumbo {
    font-size: 60px;
    font-weight: 700;
    color: #484848;
  }
  .list-unstyled {
    font-size: 14px;
    li {
      padding-bottom: 5px;
    }
    a {
      color: #008489;
      text-decoration: none;
      &:hover {
        text-decoration: underline;
      }
    }
  }
}
</style>
