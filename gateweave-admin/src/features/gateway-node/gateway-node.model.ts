export type GatewayNodeStatus = 'ONLINE' | 'OFFLINE'

export interface GatewayNode {
  readonly nodeId: string
  readonly gatewayGroupId: string
  readonly nodeName: string
  readonly advertisedAddress: string
  readonly status: GatewayNodeStatus
  readonly lastHeartbeatAt: string
  readonly currentConfigVersion: number
}
