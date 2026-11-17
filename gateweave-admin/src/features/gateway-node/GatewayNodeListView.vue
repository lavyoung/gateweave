<script setup lang="ts">
import { gatewayNodeFixtures } from '@/features/gateway-node/gateway-node.fixture'
import type { GatewayNodeStatus } from '@/features/gateway-node/gateway-node.model'

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

function statusPresentation(status: GatewayNodeStatus) {
  return statusPresentations[status]
}

const heartbeatFormatter = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'medium',
  timeStyle: 'medium',
  hour12: false,
})

function formatHeartbeat(value: string): string {
  return heartbeatFormatter.format(new Date(value))
}
</script>

<template>
  <section class="gateway-node-page">
    <header class="page-heading">
      <div>
        <h2>网关节点</h2>
        <p>查看控制面当前维护的网关算力节点及其配置同步状态。</p>
      </div>
    </header>

    <el-card class="node-card" shadow="never">
      <el-table :data="gatewayNodeFixtures" row-key="nodeId">
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
    </el-card>
  </section>
</template>

<style scoped>
.gateway-node-page {
  display: flex;
  flex-direction: column;
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

.node-card :deep(.el-card__body) {
  padding: 0;
}

.address {
  color: #606266;
  font-family: Consolas, Monaco, monospace;
}
</style>
