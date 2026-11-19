import type {GatewayNodePage, GatewayNodePageQuery} from '@/features/gateway-node/gateway-node.model'
import {requestCenterApi} from '@/shared/http/center-api-client'

/**
 * 分页查询 GateWeave Center 中维护的网关节点。
 *
 * @param query 节点筛选及分页条件
 * @returns 网关节点分页数据
 */
export function queryGatewayNodePage(query: GatewayNodePageQuery): Promise<GatewayNodePage> {
  const parameters = new URLSearchParams({
    pageNumber: String(query.pageNumber),
    pageSize: String(query.pageSize),
  })

  const nodeId = query.nodeId?.trim()
  if (nodeId) {
    parameters.set('nodeId', nodeId)
  }

  const gatewayGroupId = query.gatewayGroupId?.trim()
  if (gatewayGroupId) {
    parameters.set('gatewayGroupId', gatewayGroupId)
  }

  for (const status of query.statuses ?? []) {
    parameters.append('status', status)
  }

  return requestCenterApi<GatewayNodePage>(`/gateway-nodes?${parameters.toString()}`)
}
