<template>
  <view class="page">
    <view class="toolbar">
      <input
        v-model="keyword"
        class="input"
        type="text"
        placeholder="关键词，如：安全"
        confirm-type="search"
        @confirm="onSearch"
      />
      <button class="btn-search" size="mini" @click="onSearch">搜索</button>
    </view>

    <special-card
      v-for="it in list"
      :key="String(it.id ?? it.title)"
      :item="it"
    />

    <view v-if="loading && !list.length" class="tip">加载中…</view>
    <view v-else-if="footText" class="foot">{{ footText }}</view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad, onPullDownRefresh, onReachBottom } from "@dcloudio/uni-app";
import { fetchSpecialByTitle } from "@/api/special";
import type { SpecialItem } from "@/types/special";

const keyword = ref("");
const list = ref<SpecialItem[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const loading = ref(false);

const footText = computed(() => {
  if (loading.value && list.value.length) return "加载中…";
  if (!list.value.length || !total.value) return "";
  if (list.value.length >= total.value) return "— 没有更多了 —";
  return "上拉加载更多";
});

async function fetchPage(p: number, append: boolean) {
  if (loading.value) return;
  loading.value = true;
  try {
    const res = await fetchSpecialByTitle({
      title: keyword.value.trim() || "",
      pageNum: p,
      pageSize: pageSize.value,
    });
    if (append) {
      list.value = list.value.concat(res.list);
    } else {
      list.value = res.list;
    }
    total.value = res.total;
    pageNum.value = p;
  } catch (e) {
    console.error(e);
    uni.showToast({ title: "加载失败", icon: "none" });
    if (!append) {
      list.value = [];
      total.value = 0;
    }
  } finally {
    loading.value = false;
    uni.stopPullDownRefresh();
  }
}

function onSearch() {
  fetchPage(1, false);
}

onLoad(() => {
  fetchPage(1, false);
});

onPullDownRefresh(() => {
  fetchPage(1, false);
});

onReachBottom(() => {
  if (loading.value) return;
  if (list.value.length >= total.value) return;
  fetchPage(pageNum.value + 1, true);
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 40rpx;
  background: #f6f6f6;
}
.toolbar {
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 24rpx;
  background: #fff;
  margin-bottom: 8rpx;
}
.input {
  flex: 1;
  height: 72rpx;
  padding: 0 24rpx;
  border: 1px solid #eee;
  border-radius: 8rpx;
  font-size: 28rpx;
  margin-right: 16rpx;
}
.btn-search {
  background: #0084ff;
  color: #fff;
}
.tip,
.foot {
  text-align: center;
  font-size: 24rpx;
  color: #8590a6;
  padding: 24rpx;
}
</style>