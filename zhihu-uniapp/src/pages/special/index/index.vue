<template>
  <view class="container">
    <text class="title">知乎 uni-app</text>
    <button @click="handleClick">点击测试</button>
  </view>
</template>

<script setup lang="ts">
const handleClick = () => {
  uni.showToast({
    title: 'Hello 知乎!',
    icon: 'success'
  })
}
</script>

<style>
.container {
  padding: 40rpx;
  text-align: center;
}

.title {
  font-size: 36rpx;
  font-weight: bold;
  color: #007aff;
  display: block;
  margin-bottom: 40rpx;
}
</style>