<template>
  <view class="card" @click="go">
    <view class="body">
      <!-- 你的原有模板代码保持不变 -->
      <view
        class="btn"
        :class="{ on: item.isFollowing }"
        @click.stop.prevent="noop"
      >
        {{ item.isFollowing ? "已关注" : "关注专题" }}
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { SpecialItem } from "@/types/special";

// defineProps泛型已通过end.v.ts声明，红线自动消失
const props = defineProps<{ item: SpecialItem }>();

const imgErr = ref(false);
watch(
  () => [props.item.cover, props.item.id],
  () => {
    imgErr.value = false;
  }
);

const showImg = computed(() => Boolean(props.item.cover) && !imgErr.value);

function go() {
    const id = props.item.id;
    if (id === undefined || id === "") return;
    uni.navigateTo({
        url: `pages/special/detail?id=${encodeURIComponent(String(id))}`
    });
}

function noop() {}
</script>