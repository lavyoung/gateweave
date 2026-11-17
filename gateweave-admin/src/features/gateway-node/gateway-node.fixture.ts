import type { GatewayNode } from '@/features/gateway-node/gateway-node.model'

export const gatewayNodeFixtures = [
  {
    nodeId: 'gateweave-engine-01',
    gatewayGroupId: 'default',
    nodeName: '默认网关节点一',
    advertisedAddress: 'http://192.168.3.127:7397',
    status: 'ONLINE',
    lastHeartbeatAt: '2026-11-17T02:20:00Z',
    currentConfigVersion: 3,
  },
  {
    nodeId: 'gateweave-engine-02',
    gatewayGroupId: 'default',
    nodeName: '默认网关节点二',
    advertisedAddress: 'http://192.168.3.128:7397',
    status: 'OFFLINE',
    lastHeartbeatAt: '2026-11-16T14:30:00Z',
    currentConfigVersion: 2,
  },
] satisfies readonly GatewayNode[]
