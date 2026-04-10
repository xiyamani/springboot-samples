<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import type { SpecialItem } from '../types/special'
import { coverPlaceholder } from '../utils/coverPlaceholder'

const props = defineProps<{ item: SpecialItem }>()

const to = computed(() => ({
  name: 'special-detail' as const,
  params: { id: String(props.item.id ?? '') },
}))

const ph = () => coverPlaceholder(props.item.title)
</script>

<template>
  <RouterLink v-slot="{ navigate }" :to="to" custom>
    <li class="card-row" role="link" @click="navigate">
      <div class="cover-wrap">
        <img
          class="cover"
          :src="item.cover || ph()"
          :alt="item.title"
          loading="lazy"
          @error="(e) => ((e.target as HTMLImageElement).src = ph())"
        />
      </div>
      <div class="body">
        <h2 class="card-title">{{ item.title }}</h2>
        <p v-if="item.introduction" class="intro">{{ item.introduction }}</p>
        <p class="meta">
          <span>{{ item.updateLabel }}</span><span class="dot">·</span
          ><span>{{ item.visitLabel }}</span><span class="dot">·</span
          ><span>{{ item.followersLabel }}</span>
        </p>
        <el-button
          class="follow-btn"
          round
          :class="{ 'follow-btn--following': item.isFollowing }"
          @click.stop.prevent
        >
          {{ item.isFollowing ? '已关注' : '关注专题' }}
        </el-button>
      </div>
    </li>
  </RouterLink>
</template>

<style scoped>
.card-row {
  display: flex;
  gap: 20px;
  cursor: pointer;
  padding: 20px;
  margin-bottom: 12px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(18, 18, 18, 0.06);
  border: 1px solid #f0f0f0;
  list-style: none;
  transition: box-shadow 0.2s ease;
}
.card-row:hover {
  box-shadow: 0 4px 12px rgba(18, 18, 18, 0.08);
}
.cover-wrap {
  flex-shrink: 0;
  width: 200px;
  border-radius: 4px;
  overflow: hidden;
  background: #f6f6f6;
}
.cover {
  display: block;
  width: 100%;
  height: 120px;
  object-fit: cover;
}
.body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  text-align: left;
}
.card-title {
  margin: 0 0 10px;
  font-size: 18px;
  font-weight: 600;
  color: #121212;
  line-height: 1.35;
}
.intro {
  margin: 0 0 10px;
  font-size: 14px;
  color: #646464;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.meta {
  margin: 0 0 16px;
  font-size: 14px;
  color: #8590a6;
  line-height: 1.5;
}
.dot {
  margin: 0 6px;
  opacity: 0.7;
}
.follow-btn {
  --el-button-border-color: #0084ff;
  --el-button-text-color: #0084ff;
  --el-button-bg-color: #fff;
  --el-button-hover-text-color: #0084ff;
  --el-button-hover-bg-color: rgba(0, 132, 255, 0.06);
  --el-button-hover-border-color: #0084ff;
}
.follow-btn.follow-btn--following {
  --el-button-border-color: #ebebeb;
  --el-button-text-color: #8590a6;
  --el-button-bg-color: #f6f6f6;
  --el-button-hover-text-color: #8590a6;
  --el-button-hover-bg-color: #f0f0f0;
  --el-button-hover-border-color: #e0e0e0;
}
@media (max-width: 640px) {
  .card-row {
    flex-direction: column;
  }
  .cover-wrap {
    width: 100%;
  }
  .cover {
    height: 160px;
  }
}
</style>
