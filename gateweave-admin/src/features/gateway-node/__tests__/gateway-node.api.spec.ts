import {beforeEach, describe, expect, it, vi} from 'vitest'

import {queryGatewayNodePage} from '@/features/gateway-node/gateway-node.api'
import type {GatewayNodePage} from '@/features/gateway-node/gateway-node.model'
import {requestCenterApi} from '@/shared/http/center-api-client'

vi.mock('@/shared/http/center-api-client', () => ({
  requestCenterApi: vi.fn<(...arguments_: unknown[]) => Promise<unknown>>(),
}))

const requestCenterApiMock = vi.mocked(requestCenterApi)

describe('queryGatewayNodePage', () => {
  beforeEach(() => {
    requestCenterApiMock.mockReset()
  })

  it('should send only pagination parameters when filters are blank', async () => {
    const page = gatewayNodePage()
    requestCenterApiMock.mockResolvedValue(page)

    const result = await queryGatewayNodePage({
      nodeId: '   ',
      gatewayGroupId: '',
      pageNumber: 2,
      pageSize: 50,
    })

    expect(result).toBe(page)
    expect(requestCenterApiMock).toHaveBeenCalledWith('/gateway-nodes?pageNumber=2&pageSize=50')
  })

  it('should trim filters and preserve repeated status parameters', async () => {
    requestCenterApiMock.mockResolvedValue(gatewayNodePage())

    await queryGatewayNodePage({
      nodeId: ' node &/? ',
      gatewayGroupId: ' 默认分组 ',
      statuses: ['ONLINE', 'OFFLINE'],
      pageNumber: 1,
      pageSize: 20,
    })

    const requestPath = requestCenterApiMock.mock.calls[0]?.[0]
    expect(requestPath).toBeDefined()
    const parameters = new URL(requestPath!, 'http://localhost').searchParams
    expect(parameters.get('nodeId')).toBe('node &/?')
    expect(parameters.get('gatewayGroupId')).toBe('默认分组')
    expect(parameters.getAll('status')).toEqual(['ONLINE', 'OFFLINE'])
    expect(parameters.get('pageNumber')).toBe('1')
    expect(parameters.get('pageSize')).toBe('20')
  })
})

function gatewayNodePage(): GatewayNodePage {
  return {
    nodes: [],
    pageNumber: 1,
    pageSize: 20,
    totalElements: 0,
    totalPages: 0,
  }
}
