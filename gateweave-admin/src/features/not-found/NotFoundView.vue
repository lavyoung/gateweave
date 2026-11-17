<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function returnToDashboard(): void {
  void router.push('/dashboard')
}
</script>

<template>
  <section class="not-found-page">
    <p class="status-code">404</p>
    <h2>页面不存在</h2>
    <p class="description">当前地址没有对应的管理功能，请检查访问路径。</p>
    <el-button type="primary" @click="returnToDashboard">返回后台概览</el-button>
  </section>
</template>

<style scoped>
.not-found-page {
  display: flex;
  min-height: 60vh;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.status-code {
  margin: 0;
  color: #409eff;
  font-size: 96px;
  font-weight: 700;
  line-height: 1;
}

.not-found-page h2 {
  margin: 24px 0 0;
  color: #303133;
  font-size: 24px;
}

.description {
  margin: 12px 0 24px;
  color: #909399;
}
</style>
