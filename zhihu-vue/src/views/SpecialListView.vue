<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchSpecialByTitle } from '../api/special'
import type { SpecialItem } from '../types/special'
import { axiosErrorMessage } from '../utils/httpError'
import SpecialCard from '../components/SpecialCard.vue'

const titleQuery = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const list = ref<SpecialItem[]>([])
const total = ref(0)

const totalText = computed(() =>
  total.value > 0 ? `共有 ${total.value.toLocaleString('zh-CN')} 个专题` : '',
)

async function load() {
  loading.value = true
  try {
    const res = await fetchSpecialByTitle({
      title: titleQuery.value.trim() || '',
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    list.value = res.list
    total.value = res.total
  } catch (e) {
    console.error(e)
    ElMessage.error(axiosErrorMessage(e, '加载失败'))
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  pageNum.value = 1
  load()
}

function onSizeChange(s: number) {
  pageSize.value = s
  pageNum.value = 1
  load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <header class="head">
      <h1 class="title-all">全部专题</h1>
      <p v-if="totalText" class="sub">{{ totalText }}</p>
      <div class="toolbar">
        <el-input
          v-model="titleQuery"
          class="search"
          clearable
          placeholder="关键词，如：安全"
          @keyup.enter="onSearch"
        />
        <el-button type="primary" @click="onSearch">搜索</el-button>
      </div>
    </header>

    <el-skeleton :loading="loading" animated :count="3">
      <template #template>
        <div class="sk">
          <el-skeleton-item variant="image" style="width: 200px; height: 120px" />
          <div style="flex: 1; padding-left: 20px">
            <el-skeleton-item variant="h3" style="width: 50%" />
            <el-skeleton-item variant="text" style="margin-top: 16px" />
          </div>
        </div>
      </template>
      <template #default>
        <ul class="list">
          <SpecialCard v-for="it in list" :key="String(it.id ?? it.title)" :item="it" />
        </ul>
        <el-empty v-if="!loading && list.length === 0" description="暂无数据" />
      </template>
    </el-skeleton>

    <footer v-if="total > 0" class="pager">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="load"
        @size-change="onSizeChange"
      />
    </footer>
  </div>
</template>

<style scoped>
.page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 20px 48px;
  box-sizing: border-box;
}
.head {
  margin-bottom: 28px;
}
.title-all {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 600;
  color: #1a1a1a;
}
.sub {
  margin: 0 0 20px;
  font-size: 14px;
  color: #8590a6;
}
.toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.search {
  width: min(420px, 100%);
}
.list {
  margin: 0;
  padding: 0;
}
.sk {
  display: flex;
  padding: 20px;
  margin-bottom: 12px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
}
.pager {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}
</style>
