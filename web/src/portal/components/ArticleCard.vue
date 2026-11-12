<template>
  <router-link class="p-card p-card--hover p-article-card" :to="'/portal/article/' + article.articleId">
    <!-- 整卡可点：标题、封面任意位置都跳详情页，详情页再用路由里的 articleId 请求正文 -->
    <!-- 封面可选：列表接口没返回 coverUrl 时整块不渲染，不留空白占位；lazy 让首屏外的图延后加载 -->
    <div v-if="article.coverUrl" class="p-article-card__cover">
      <img :src="article.coverUrl" :alt="article.title" loading="lazy" />
    </div>
    <div class="p-article-card__body">
      <!-- 角标二选一：置顶优先于推荐，所以一篇最多只出现一个标记 -->
      <h2 class="p-article-card__title">
        <span v-if="article.isTop === '1'" class="p-flag p-flag--top">置顶</span>
        <span v-else-if="article.isRecommend === '1'" class="p-flag p-flag--rec">推荐</span>
        {{ article.title }}
      </h2>
      <p class="p-article-card__summary">{{ article.summary || '这篇文章还没有写摘要，点进去看看吧。' }}</p>

      <!-- 卡片里的标签是纯文字（不再嵌套 router-link，否则会与整卡跳转冲突），点击整卡仍进详情 -->
      <div v-if="tags.length" class="p-article-card__tags">
        <span v-for="t in tags" :key="t" class="p-tag"># {{ t }}</span>
      </div>

      <!-- 元信息逐项兜底：作者缺省「博主」、分类为空时不显示、各项计数缺省 0，避免出现空位 -->
      <div class="p-article-card__meta">
        <span>{{ article.authorNickName || '博主' }}</span>
        <span class="dot">{{ dateText }}</span>
        <span v-if="article.categoryName" class="dot">{{ article.categoryName }}</span>
        <span class="dot">阅读 {{ article.viewCount || 0 }}</span>
        <span class="dot">评论 {{ article.commentCount || 0 }}</span>
      </div>
    </div>
  </router-link>
</template>

<script setup name="PortalArticleCard">
/**
 * 门户文章卡片（首页列表项）。
 *
 * 纯展示组件：自己不请求接口、不处理点击（整卡是 router-link）；
 * 置顶/推荐只做角标提示，前端不参与排序，顺序完全由后端列表接口决定。
 */
import { computed } from 'vue'
import { parseTime } from '@/utils/ruoyi'

// 父级传入门户文章列表行（BlogArticle）：只读展示，字段缺失由模板里的兜底表达式负责
const props = defineProps({
  article: { type: Object, required: true }
})

// tagNames 是后端 GROUP_CONCAT 拼好的逗号分隔字符串（不是数组），这里切成数组；
// 最多取 4 个，避免标签多的卡片被撑高、把同一行的卡片错开。
const tags = computed(() => {
  const raw = props.article.tagNames
  return raw ? String(raw).split(',').filter(Boolean).slice(0, 4) : []
})

// 优先用发布时间；未发布过的文章没有 publishTime，回退创建时间。只精确到天，控制卡片信息密度。
const dateText = computed(() => {
  const t = props.article.publishTime || props.article.createTime
  return t ? parseTime(t, '{y}-{m}-{d}') : ''
})
</script>

<style scoped>
/* 角标字号明显小于标题，抬高 2px 让它的视觉中线与标题文字对齐，否则会显得下沉 */
.p-flag {
  display: inline-block;
  margin-right: 6px;
  padding: 0 6px;
  border-radius: 5px;
  font-size: 11.5px;
  font-weight: 600;
  line-height: 19px;
  vertical-align: 2px;
}

.p-flag--top {
  color: #d93026;
  background: #fdecec;
}

.p-flag--rec {
  color: #b26a00;
  background: #fff4e5;
}
</style>
