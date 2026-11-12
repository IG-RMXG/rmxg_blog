<template>
  <div class="p-side">
    <!-- 门户通用侧栏：博主卡片 / 热门文章 / 分类 / 标签 / 最新评论；除博主卡片外，某个区块没数据就整块不渲染 -->
    <!-- 博主卡片 -->
    <section class="p-card p-side__card p-side__author">
      <!-- 头像为空/加载失败时 el-avatar 回退显示插槽里的首字，不会出现破图 -->
      <el-avatar :size="64" :src="authorAvatar">
        {{ authorInitial }}
      </el-avatar>
      <h3>{{ authorName || '博主' }}</h3>
      <p>{{ site.subtitle || '记录、思考与分享' }}</p>
      <!-- 四项统计由站点接口聚合并返回，前端不再自己算，所以与列表分页、当前筛选都无关 -->
      <div class="p-side__stats">
        <div>
          <strong>{{ site.articleCount || 0 }}</strong>
          <span>文章</span>
        </div>
        <div>
          <strong>{{ site.categoryCount || 0 }}</strong>
          <span>分类</span>
        </div>
        <div>
          <strong>{{ site.commentCount || 0 }}</strong>
          <span>评论</span>
        </div>
        <div>
          <strong>{{ site.viewCount || 0 }}</strong>
          <span>浏览</span>
        </div>
      </div>
    </section>

    <!-- 热门文章 -->
    <section v-if="hot && hot.length" class="p-card p-side__card">
      <h4 class="p-section-title">热门文章</h4>
      <!-- 序号按接口返回顺序 1..n 展示，不参与排序；取几条由父级调用 listPortalHot 时的 limit 决定 -->
      <ul class="p-side__list">
        <li v-for="(item, index) in hot" :key="item.articleId">
          <router-link :to="'/portal/article/' + item.articleId">
            <span class="p-side__index">{{ index + 1 }}</span>{{ item.title }}
          </router-link>
          <span class="p-side__meta">阅读 {{ item.viewCount || 0 }}</span>
        </li>
      </ul>
    </section>

    <!-- 分类 -->
    <section v-if="categories && categories.length" class="p-card p-side__card">
      <h4 class="p-section-title">文章分类</h4>
      <div class="p-side__cats">
        <router-link
          v-for="c in categories"
          :key="c.categoryId"
          class="p-side__cat"
          :to="{ path: '/portal/home', query: { categoryId: c.categoryId } }"
        >
          <span>{{ c.categoryName }}</span>
          <em>{{ c.articleCount || 0 }}</em>
        </router-link>
      </div>
    </section>

    <!-- 标签列表同样只给有已发布文章的标签；点击回首页并用 query 传 tagId，与首页「URL 即筛选状态」的约定一致 -->
    <!-- 标签 -->
    <section v-if="tags && tags.length" class="p-card p-side__card">
      <h4 class="p-section-title">标签云</h4>
      <div class="p-side__tags">
        <router-link
          v-for="t in tags"
          :key="t.tagId"
          class="p-tag"
          :to="{ path: '/portal/home', query: { tagId: t.tagId } }"
        >
          # {{ t.tagName }}
        </router-link>
      </div>
    </section>

    <!-- 最新评论 -->
    <section v-if="comments && comments.length" class="p-card p-side__card">
      <h4 class="p-section-title">最新评论</h4>
      <!-- 昵称按「昵称 → 用户名 → 访客」三级回退；评论正文按纯文本输出，不渲染 Markdown/HTML -->
      <div class="p-side__list">
        <div v-for="c in comments" :key="c.commentId" class="p-side__comment">
          <p>{{ c.content }}</p>
          <span>
            {{ c.nickName || c.userName || '访客' }} ·
            <router-link class="p-link" :to="'/portal/article/' + c.articleId">{{ c.articleTitle }}</router-link>
          </span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup name="PortalSidebar">
/**
 * 门户通用侧栏（首页与文章详情页共用）。
 *
 * 组件本身不发请求：站点信息、分类、标签、热门文章、最新评论全部由父页面取好再传进来。
 * 首页会传全五项，详情页只传 site 与 hot，所以除博主卡片外每个区块都按「有数据才渲染」处理。
 */
import { computed } from 'vue'

// 五项数据都可选：默认给空对象/空数组，父级漏传时模板里不会因 undefined 报错
const props = defineProps({
  site: { type: Object, default: () => ({}) },
  categories: { type: Array, default: () => [] },
  tags: { type: Array, default: () => [] },
  hot: { type: Array, default: () => [] },
  comments: { type: Array, default: () => [] }
})

// site.author 是博主资料（nickName / avatar），站点接口可能不返回，取不到就交给模板兜底
const authorName = computed(() => (props.site.author && props.site.author.nickName) || '')
const authorAvatar = computed(() => (props.site.author && props.site.author.avatar) || undefined)
// 头像缺失时 el-avatar 渲染的占位文字：取昵称首字，没有昵称就用「博」
const authorInitial = computed(() => (authorName.value || '博').slice(0, 1))
</script>

<style scoped>
/* 序号列定宽 + tabular-nums，保证 10 号之后的两位数序号不会把标题挤得左右不齐 */
.p-side__index {
  display: inline-block;
  width: 16px;
  color: var(--p-text-3);
  font-size: 12px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
</style>
