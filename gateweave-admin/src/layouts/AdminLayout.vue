<script setup lang="ts">
import {computed, ref} from 'vue'
import {RouterView, useRoute, useRouter} from 'vue-router'

const route = useRoute()
const router = useRouter()
const collapsed = ref(false)

const asideWidth = computed(() => (collapsed.value ? '64px' : '220px'))
const currentTitle = computed(() => route.meta.title ?? 'GateWeave 管理后台')
const menuItems = computed(() =>
  router
    .getRoutes()
    .filter((record) => record.meta.showInMenu && record.meta.title)
    .sort((left, right) => Number(left.meta.order ?? 0) - Number(right.meta.order ?? 0)),
)

function toggleSidebar(): void {
  collapsed.value = !collapsed.value
}
</script>

<template>
  <el-container class="admin-layout">
    <el-aside class="sidebar" :width="asideWidth">
      <div class="brand">{{ collapsed ? 'GW' : 'GateWeave' }}</div>

      <el-menu
        class="navigation"
        :collapse="collapsed"
        :default-active="route.path"
        background-color="#001529"
        text-color="#bfcbd9"
        active-text-color="#ffffff"
        router
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <span>{{ item.meta.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="workspace">
      <el-header class="header">
        <button class="collapse-button" type="button" aria-label="切换侧边栏" @click="toggleSidebar">
          {{ collapsed ? '展开' : '收起' }}
        </button>
        <h1>{{ currentTitle }}</h1>
      </el-header>

      <el-main class="content">
        <RouterView/>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.sidebar {
  overflow: hidden;
  background: #001529;
  transition: width 0.2s ease;
}

.brand {
  height: 64px;
  overflow: hidden;
  color: #ffffff;
  font-size: 20px;
  font-weight: 600;
  line-height: 64px;
  text-align: center;
  white-space: nowrap;
}

.navigation {
  border-right: 0;
}

.workspace {
  min-width: 0;
}

.header {
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 24px;
  background: #ffffff;
  border-bottom: 1px solid #ebeef5;
}

.header h1 {
  margin: 0 0 0 20px;
  color: #303133;
  font-size: 18px;
  font-weight: 500;
}

.collapse-button {
  padding: 6px 12px;
  color: #606266;
  cursor: pointer;
  background: transparent;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.collapse-button:hover {
  color: #409eff;
  border-color: #409eff;
}

.content {
  padding: 24px;
  background: #f5f7fa;
}
</style>
