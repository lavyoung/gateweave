export type GatewayNodeStatus = 'ONLINE' | 'OFFLINE'

/**
 * 表示管理后台展示的一条网关节点数据。
 */
export interface GatewayNode {
  readonly nodeId: string
  readonly gatewayGroupId: string
  readonly nodeName: string
  readonly advertisedAddress: string
  readonly status: GatewayNodeStatus
  readonly lastHeartbeatAt: string
  readonly currentConfigVersion: number
}

/**
 * 表示 Center 返回的一页网关节点。
 */
export interface GatewayNodePage {
  readonly nodes: readonly GatewayNode[]
  readonly pageNumber: number
  readonly pageSize: number
  readonly totalElements: number
  readonly totalPages: number
}

/**
 * 表示管理后台发起的网关节点分页查询。
 */
export interface GatewayNodePageQuery {
  readonly nodeId?: string
  readonly gatewayGroupId?: string
  readonly statuses?: readonly GatewayNodeStatus[]
  readonly pageNumber: number
  readonly pageSize: number
}
