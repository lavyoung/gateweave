import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'

import {ApiRequestError, requestCenterApi} from '@/shared/http/center-api-client'

describe('requestCenterApi', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => {
    vi.stubEnv('VITE_GATEWEAVE_CENTER_API_BASE_URL', 'http://localhost:8080/api/v1')
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllEnvs()
    vi.unstubAllGlobals()
  })

  it('should return business data from a successful response', async () => {
    const data = { nodeId: 'gateway-node-01' }
    fetchMock.mockResolvedValue(jsonResponse({ success: true, code: 'COMMON-0000', message: '成功', data }))

    const result = await requestCenterApi<typeof data>('/gateway-nodes', {
      headers: { Authorization: 'Bearer token' },
    })

    expect(result).toEqual(data)
    const [requestUrl, requestInit] = fetchMock.mock.calls[0]!
    expect(requestUrl.toString()).toBe('http://localhost:8080/api/v1/gateway-nodes')
    const headers = new Headers(requestInit?.headers)
    expect(headers.get('Accept')).toBe('application/json')
    expect(headers.get('Authorization')).toBe('Bearer token')
  })

  it('should preserve the business error and request identifier', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(
        { success: false, code: 'GATEWAY-0001', message: '网关节点不存在', data: {} },
        { headers: { 'X-Request-Id': 'request-01' } },
      ),
    )

    await expect(requestCenterApi('/gateway-nodes')).rejects.toMatchObject({
      name: 'ApiRequestError',
      status: 200,
      code: 'GATEWAY-0001',
      message: '网关节点不存在',
      requestId: 'request-01',
    })
  })

  it('should reject an unsuccessful HTTP response', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({ success: false, code: 'COMMON-0003', message: '服务暂时不可用', data: {} }, { status: 503 }),
    )

    await expect(requestCenterApi('/gateway-nodes')).rejects.toMatchObject({
      status: 503,
      code: 'COMMON-0003',
      message: '服务暂时不可用',
    })
  })

  it('should reject a response that is not valid JSON', async () => {
    fetchMock.mockResolvedValue(
      new Response('service unavailable', {
        status: 502,
        headers: { 'X-Request-Id': 'request-02' },
      }),
    )

    await expect(requestCenterApi('/gateway-nodes')).rejects.toMatchObject({
      status: 502,
      code: 'CLIENT-INVALID-RESPONSE',
      requestId: 'request-02',
    })
  })

  it('should reject a JSON response that does not match the result contract', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ success: true, code: 'COMMON-0000', message: '成功' }))

    await expect(requestCenterApi('/gateway-nodes')).rejects.toMatchObject({
      status: 200,
      code: 'CLIENT-INVALID-RESPONSE',
      message: 'Center 返回的响应结构不正确',
    })
  })

  it('should convert a network failure to an API request error', async () => {
    fetchMock.mockRejectedValue(new TypeError('fetch failed'))

    await expect(requestCenterApi('/gateway-nodes')).rejects.toEqual(
      new ApiRequestError(0, 'CLIENT-NETWORK-ERROR', '无法连接 GateWeave Center', null),
    )
  })
})

function jsonResponse(body: unknown, init: ResponseInit = {}): Response {
  return new Response(JSON.stringify(body), {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init.headers,
    },
  })
}
