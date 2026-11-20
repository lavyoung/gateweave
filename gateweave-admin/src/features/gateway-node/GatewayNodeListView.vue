<script setup lang="ts">
import {onMounted, reactive, ref} from 'vue'

import {queryGatewayNodePage} from '@/features/gateway-node/gateway-node.api'
import type {GatewayNode, GatewayNodeStatus} from '@/features/gateway-node/gateway-node.model'
import {ApiRequestError} from '@/shared/http/center-api-client'

const DEFAULT_PAGE_SIZE = 20

interface GatewayNodeFilters {
  nodeId: string
  gatewayGroupId: string
  statuses: GatewayNodeStatus[]
}

const filterForm = reactive<GatewayNodeFilters>(createEmptyFilters())
const activeFilters = ref<GatewayNodeFilters>(createEmptyFilters())
const nodes = ref<GatewayNode[]>([])
const loading = ref(false)
const errorMessage = ref('')
const pageNumber = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const totalElements = ref(0)

const statusPresentations = {
  ONLINE: {
    label: '在线',
    type: 'success',
  },
  OFFLINE: {
    label: '离线',
    type: 'info',
  },
} satisfies Record<GatewayNodeStatus, { label: string; type: 'success' | 'info' }>

const statusOptions: readonly GatewayNodeStatus[] = ['ONLINE', 'OFFLINE']

const heartbeatFormatter = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'medium',
  timeStyle: 'medium',
  hour12: false,
})

onMounted(() => {
  void loadGatewayNodes()
})

/**
 * 从 Center 加载当前页网关节点。
 */
async function loadGatewayNodes(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    const page = await queryGatewayNodePage({
      nodeId: activeFilters.value.nodeId,
      gatewayGroupId: activeFilters.value.gatewayGroupId,
      statuses: activeFilters.value.statuses,
      pageNumber: pageNumber.value,
      pageSize: pageSize.value,
    })

    nodes.value = [...page.nodes]
    pageNumber.value = page.pageNumber
    pageSize.value = page.pageSize
    totalElements.value = page.totalElements
  } catch (error: unknown) {
    nodes.value = []
    totalElements.value = 0
    errorMessage.value = resolveErrorMessage(error)
  } finally {
    loading.value = false
  }
}

/**
 * 应用当前筛选条件，并从第一页重新查询。
 */
function searchGatewayNodes(): void {
  activeFilters.value = {
    nodeId: filterForm.nodeId,
    gatewayGroupId: filterForm.gatewayGroupId,
    statuses: [...filterForm.statuses],
  }
  pageNumber.value = 1
  void loadGatewayNodes()
}

/**
 * 清空筛选条件，并重新查询第一页。
 */
function resetGatewayNodeFilters(): void {
  Object.assign(filterForm, createEmptyFilters())
  activeFilters.value = createEmptyFilters()
  pageNumber.value = 1
  void loadGatewayNodes()
}

/**
 * 切换节点列表页码。
 *
 * @param value 新页码
 */
function changePageNumber(value: number): void {
  pageNumber.value = value
  void loadGatewayNodes()
}

/**
 * 调整每页展示数量，并从第一页重新查询。
 *
 * @param value 新的每页数量
 */
function changePageSize(value: number): void {
  pageNumber.value = 1
  pageSize.value = value
  void loadGatewayNodes()
}

function statusPresentation(status: GatewayNodeStatus) {
  return statusPresentations[status]
}

function formatHeartbeat(value: string): string {
  return heartbeatFormatter.format(new Date(value))
}

function resolveErrorMessage(error: unknown): string {
  if (!(error instanceof ApiRequestError)) {
    return '加载网关节点失败，请稍后重试'
  }

  if (error.requestId) {
    return `${error.message}（请求标识：${error.requestId}）`
  }

  return error.message
}

function createEmptyFilters(): GatewayNodeFilters {
  return {
    nodeId: '',
    gatewayGroupId: '',
    statuses: [],
  }
}
</script>

<template>
  <section class="gateway-node-page">
    <header class="page-heading">
      <div>
        <h2>网关节点</h2>
        <p>查看控制面当前维护的网关算力节点及其配置同步状态。</p>
      </div>

      <el-button aria-label="刷新网关节点" :loading="loading" @click="loadGatewayNodes"> 刷新 </el-button>
    </header>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" @submit.prevent="searchGatewayNodes">
        <el-form-item label="节点标识">
          <el-input v-model="filterForm.nodeId" placeholder="请输入节点标识" clearable />
        </el-form-item>

        <el-form-item label="网关分组">
          <el-input v-model="filterForm.gatewayGroupId" placeholder="请输入网关分组" clearable />
        </el-form-item>

        <el-form-item label="节点状态">
          <el-select
            v-model="filterForm.statuses"
            class="status-filter"
            placeholder="全部状态"
            multiple
            clearable
            collapse-tags
          >
            <el-option
              v-for="status in statusOptions"
              :key="status"
              :label="statusPresentation(status).label"
              :value="status"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button native-type="submit" type="primary" :loading="loading">查询</el-button>
          <el-button :disabled="loading" @click="resetGatewayNodeFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-alert v-if="errorMessage" class="load-error" :title="errorMessage" type="error" show-icon :closable="false" />

    <el-card class="node-card" shadow="never">
      <el-table v-loading="loading" :data="nodes" row-key="nodeId" empty-text="暂无网关节点">
        <el-table-column prop="nodeId" label="节点标识" min-width="190" show-overflow-tooltip />
        <el-table-column prop="nodeName" label="节点名称" min-width="170" show-overflow-tooltip />
        <el-table-column prop="gatewayGroupId" label="网关分组" min-width="130" />

        <el-table-column label="对外地址" min-width="230" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="address">{{ row.advertisedAddress }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusPresentation(row.status).type">
              {{ statusPresentation(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="currentConfigVersion" label="配置版本" width="110" align="center" />

        <el-table-column label="最后心跳时间" min-width="190">
          <template #default="{ row }">
            {{ formatHeartbeat(row.lastHeartbeatAt) }}
          </template>
        </el-table-column>
      </el-table>

      <footer v-if="totalElements > 0" class="pagination">
        <el-pagination
          :current-page="pageNumber"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalElements"
          :disabled="loading"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="changePageNumber"
          @size-change="changePageSize"
        />
      </footer>
    </el-card>
  </section>
</template>

<style scoped>
.gateway-node-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.page-heading h2 {
  margin: 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.page-heading p {
  margin: 8px 0 0;
  color: #909399;
  font-size: 14px;
}

.load-error {
  flex: none;
}

.filter-card :deep(.el-card__body) {
  padding-bottom: 2px;
}

.filter-card :deep(.el-form-item) {
  margin-bottom: 16px;
}

.filter-card :deep(.el-input) {
  width: 220px;
}

.status-filter {
  width: 220px;
}

.node-card :deep(.el-card__body) {
  padding: 0;
}

.address {
  color: #606266;
  font-family: Consolas, Monaco, monospace;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid #ebeef5;
}
</style>
