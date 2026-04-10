<template>
  <view class="page">
    <view v-if="loading" class="tip">加载中…</view>
    <template v-else-if="item">
      <image
        v-if="bannerSrc"
        class="banner"
        :src="bannerSrc"
        mode="aspectFill"
        @error="imgErr = true"
      />
      <view v-else class="banner banner-ph">
        <text class="ph">{{ item.title.slice(0, 2) }}</text>
      </view>
      <view class="content">
        <text class="title">{{ item.title }}</text>
        <text class="meta"
          >{{ item.updateLabel }} · {{ item.visitLabel }} ·
          {{ item.followersLabel }}</text
        >
        <text v-if="item.introduction" class="intro">{{
          item.introduction
        }}</text>
        <view class="fol" :class="{ on: item.isFollowing }">
          {{ item.isFollowing ? "已关注" : "关注专题" }}
        </view>
      </view>
    </template>
    <view v-else class="tip">暂无数据</view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { fetchSpecialById } from "@/api/special";
import type { SpecialItem } from "@/types/special";

const id = ref("");
const item = ref<SpecialItem | null>(null);
const loading = ref(true);
const imgErr = ref(false);

const bannerSrc = computed(() => {
  if (!item.value?.cover || imgErr.value) return "";
  return String(item.value.cover);
});

watch(
  () => item.value?.cover,
  () => {
    imgErr.value = false;
  },
);

async function load() {
  if (!id.value) {
    item.value = null;
    loading.value = false;
    return;
  }
  loading.value = true;
  try {
    item.value = await fetchSpecialById(id.value);
    if (!item.value) {
      uni.showToast({ title: "未找到专题", icon: "none" });
    }
  } catch (e) {
    console.error(e);
    uni.showToast({ title: "加载失败", icon: "none" });
    item.value = null;
  } finally {
    loading.value = false;
  }
}

onLoad((q) => {
  id.value = q?.id ? decodeURIComponent(String(q.id)) : "";
  load();
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f6f6f6;
}
.banner {
  width: 100%;
  height: 360rpx;
  background: #e8e8e8;
}
.banner-ph {
  display: flex;
  align-items: center;
  justify-content: center;
}
.ph {
  font-size: 48rpx;
  color: #8590a6;
}
.content {
  padding: 32rpx;
  background: #fff;
  margin-top: 16rpx;
}
.title {
  font-size: 40rpx;
  font-weight: 600;
  color: #1a1a1a;
}
.meta {
  display: block;
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #8590a6;
}
.intro {
  display: block;
  margin-top: 24rpx;
  font-size: 28rpx;
  line-height: 1.6;
  color: #444;
}
.fol {
  display: inline-block;
  margin-top: 32rpx;
  padding: 12rpx 40rpx;
  font-size: 28rpx;
  border-radius: 999rpx;
  border: 1px solid #0084ff;
  color: #0084ff;
}
.fol.on {
  border-color: #ebebeb;
  color: #8590a6;
  background: #f6f6f6;
}
.tip {
  padding: 48rpx;
  text-align: center;
  color: #8590a6;
}
</style>