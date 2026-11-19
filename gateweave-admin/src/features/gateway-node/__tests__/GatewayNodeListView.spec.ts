import {flushPromises, mount} from '@vue/test-utils'
import {beforeEach, describe, expect, it, vi} from 'vitest'

import {queryGatewayNodePage} from '@/features/gateway-node/gateway-node.api'
import type {GatewayNodePage} from '@/features/gateway-node/gateway-node.model'
import GatewayNodeListView from '@/features/gateway-node/GatewayNodeListView.vue'
import {ApiRequestError} from '@/shared/http/center-api-client'

vi.mock('@/features/gateway-node/gateway-node.api', () => ({
  queryGatewayNodePage: vi.fn<(...arguments_: unknown[]) => Promise<unknown>>(),
}))

const queryGatewayNodePageMock = vi.mocked(queryGatewayNodePage)

describe('GatewayNodeListView', () => {
  beforeEach(() => {
    queryGatewayNodePageMock.mockReset()
  })

  it('should load and render the first page of gateway nodes', async () => {
    queryGatewayNodePageMock.mockResolvedValue(gatewayNodePage())

    const wrapper = mount(GatewayNodeListView)
    await flushPromises()

    expect(queryGatewayNodePageMock).toHaveBeenCalledWith({ pageNumber: 1, pageSize: 20 })
    expect(wrapper.text()).toContain('gateway-node-01')
    expect(wrapper.text()).toContain('默认网关节点')
    expect(wrapper.text()).toContain('在线')
    expect(wrapper.find('.pagination').exists()).toBe(true)
  })

  it('should render an empty table without pagination', async () => {
    queryGatewayNodePageMock.mockResolvedValue(gatewayNodePage({ nodes: [], totalElements: 0, totalPages: 0 }))

    const wrapper = mount(GatewayNodeListView)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无网关节点')
    expect(wrapper.find('.pagination').exists()).toBe(false)
  })

  it('should render the API error with its request identifier', async () => {
    queryGatewayNodePageMock.mockRejectedValue(new ApiRequestError(503, 'COMMON-0003', '服务暂时不可用', 'request-01'))

    const wrapper = mount(GatewayNodeListView)
    await flushPromises()

    expect(wrapper.get('.load-error').text()).toContain('服务暂时不可用')
    expect(wrapper.get('.load-error').text()).toContain('request-01')
    expect(wrapper.find('.pagination').exists()).toBe(false)
  })

  it('should query the selected page number', async () => {
    queryGatewayNodePageMock
      .mockResolvedValueOnce(gatewayNodePage())
      .mockResolvedValueOnce(gatewayNodePage({ pageNumber: 2 }))
    const wrapper = mount(GatewayNodeListView)
    await flushPromises()

    wrapper.getComponent({ name: 'ElPagination' }).vm.$emit('current-change', 2)
    await flushPromises()

    expect(queryGatewayNodePageMock).toHaveBeenNthCalledWith(2, { pageNumber: 2, pageSize: 20 })
  })

  it('should return to the first page after changing the page size', async () => {
    queryGatewayNodePageMock
      .mockResolvedValueOnce(gatewayNodePage({ pageNumber: 3 }))
      .mockResolvedValueOnce(gatewayNodePage({ pageNumber: 1, pageSize: 50 }))
    const wrapper = mount(GatewayNodeListView)
    await flushPromises()

    wrapper.getComponent({ name: 'ElPagination' }).vm.$emit('size-change', 50)
    await flushPromises()

    expect(queryGatewayNodePageMock).toHaveBeenNthCalledWith(2, { pageNumber: 1, pageSize: 50 })
  })
})

function gatewayNodePage(overrides: Partial<GatewayNodePage> = {}): GatewayNodePage {
  return {
    nodes: [
      {
        nodeId: 'gateway-node-01',
        gatewayGroupId: 'default',
        nodeName: '默认网关节点',
        advertisedAddress: 'http://127.0.0.1:7397',
        status: 'ONLINE',
        lastHeartbeatAt: '2026-11-19T08:00:00Z',
        currentConfigVersion: 3,
      },
    ],
    pageNumber: 1,
    pageSize: 20,
    totalElements: 21,
    totalPages: 2,
    ...overrides,
  }
}
