<template>
  <router-link class="p-card p-card--hover p-article-card" :to="'/portal/article/' + article.articleId">
    <div v-if="article.coverUrl" class="p-article-card__cover">
      <img :src="article.coverUrl" :alt="article.title" loading="lazy" />
    </div>
    <div class="p-article-card__body">
      <h2 class="p-article-card__title">
        <span v-if="article.isTop === '1'" class="p-flag p-flag--top">置顶</span>
        <span v-else-if="article.isRecommend === '1'" class="p-flag p-flag--rec">推荐</span>
        {{ article.title }}
      </h2>
      <p class="p-article-card__summary">{{ article.summary || '这篇文章还没有写摘要，点进去看看吧。' }}</p>

      <div v-if="tags.length" class="p-article-card__tags">
        <span v-for="t in tags" :key="t" class="p-tag"># {{ t }}</span>
      </div>

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
import { computed } from 'vue'
import { parseTime } from '@/utils/ruoyi'

const props = defineProps({
  article: { type: Object, required: true }
})

const tags = computed(() => {
  const raw = props.article.tagNames
  return raw ? String(raw).split(',').filter(Boolean).slice(0, 4) : []
})

const dateText = computed(() => {
  const t = props.article.publishTime || props.article.createTime
  return t ? parseTime(t, '{y}-{m}-{d}') : ''
})
</script>

<style scoped>
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
