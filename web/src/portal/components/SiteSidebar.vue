<template>
  <div class="p-side">
    <!-- 博主卡片 -->
    <section class="p-card p-side__card p-side__author">
      <el-avatar :size="64" :src="authorAvatar">
        {{ authorInitial }}
      </el-avatar>
      <h3>{{ authorName || '博主' }}</h3>
      <p>{{ site.subtitle || '记录、思考与分享' }}</p>
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
import { computed } from 'vue'

const props = defineProps({
  site: { type: Object, default: () => ({}) },
  categories: { type: Array, default: () => [] },
  tags: { type: Array, default: () => [] },
  hot: { type: Array, default: () => [] },
  comments: { type: Array, default: () => [] }
})

const authorName = computed(() => (props.site.author && props.site.author.nickName) || '')
const authorAvatar = computed(() => (props.site.author && props.site.author.avatar) || undefined)
const authorInitial = computed(() => (authorName.value || '博').slice(0, 1))
</script>

<style scoped>
.p-side__index {
  display: inline-block;
  width: 16px;
  color: var(--p-text-3);
  font-size: 12px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
</style>
