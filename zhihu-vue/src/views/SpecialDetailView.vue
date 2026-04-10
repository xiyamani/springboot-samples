<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchSpecialById } from '../api/special'
import type { SpecialItem } from '../types/special'
import { axiosErrorMessage } from '../utils/httpError'
import { coverPlaceholder } from '../utils/coverPlaceholder'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const item = ref<SpecialItem | null>(null)

async function load(id: string) {
  if (!id) {
    item.value = null
    loading.value = false
    return
  }
  loading.value = true
  try {
    item.value = await fetchSpecialById(id)
    if (!item.value) ElMessage.warning('未找到该专题')
  } catch (e) {
    console.error(e)
    ElMessage.error(axiosErrorMessage(e, '加载详情失败'))
    item.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => load(String(route.params.id ?? '')))
watch(
  () => route.params.id,
  (id) => load(String(id ?? '')),
)
function back() {
  router.push({ name: 'special-list' })
}
function ph(t: string) {
  return coverPlaceholder(t, 800, 260)
}
</script>

<template>
  <div class="page">
    <el-button link type="primary" class="back" @click="back">← 返回列表</el-button>
    <el-skeleton :loading="loading" animated>
      <template #template>
        <el-skeleton-item variant="image" style="width: 100%; height: 200px" />
        <el-skeleton-item variant="h1" style="margin-top: 16px; width: 60%" />
        <el-skeleton-item variant="text" style="margin-top: 12px" />
      </template>
      <template #default>
        <template v-if="item">
          <div class="banner-wrap">
            <img
              class="banner"
              :src="item.cover || ph(item.title)"
              :alt="item.title"
              @error="(e) => ((e.target as HTMLImageElement).src = ph(item!.title))"
            />
          </div>
          <h1 class="title">{{ item.title }}</h1>
          <p class="meta">
            {{ item.updateLabel }} · {{ item.visitLabel }} · {{ item.followersLabel }}
          </p>
          <p v-if="item.introduction" class="intro">{{ item.introduction }}</p>
          <el-button round :class="item.isFollowing ? 'fol-on' : 'fol'">
            {{ item.isFollowing ? '已关注' : '关注专题' }}
          </el-button>
        </template>
        <el-empty v-else-if="!loading" description="无数据" />
      </template>
    </el-skeleton>
  </div>
</template>

<style scoped>
.page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px 20px 48px;
  box-sizing: border-box;
}
.back {
  margin-bottom: 16px;
}
.banner-wrap {
  border-radius: 6px;
  overflow: hidden;
  background: #f0f0f0;
}
.banner {
  display: block;
  width: 100%;
  height: 260px;
  object-fit: cover;
}
.title {
  margin: 20px 0 8px;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
}
.meta {
  margin: 0 0 16px;
  font-size: 14px;
  color: #8590a6;
}
.intro {
  margin: 0 0 20px;
  font-size: 15px;
  line-height: 1.6;
  color: #444;
}
.fol {
  --el-button-border-color: #0084ff;
  --el-button-text-color: #0084ff;
}
.fol-on {
  --el-button-text-color: #8590a6;
}
</style>
