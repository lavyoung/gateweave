import type {ApiResult} from '@/shared/http/api-result'

const INVALID_RESPONSE_CODE = 'CLIENT-INVALID-RESPONSE'
const NETWORK_ERROR_CODE = 'CLIENT-NETWORK-ERROR'
const REQUEST_ID_HEADER = 'X-Request-Id'
/**
 * 表示管理后台调用 Center API 时发生的统一错误。
 *
 * HTTP 状态、业务错误码和请求标识被集中保留，页面不需要重复解析响应。
 */
export class ApiRequestError extends Error {
  readonly status: number
  readonly code: string
  readonly requestId: string | null

  constructor(status: number, code: string, message: string, requestId: string | null) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = status
    this.code = code
    this.requestId = requestId
  }
}

/**
 * 调用 GateWeave Center 并返回统一响应中的业务数据。
 *
 * @param path 相对于 Center API 根地址的资源路径
 * @param init Fetch 请求配置
 * @returns Center 返回的业务数据
 * @throws ApiRequestError 网络失败、响应格式错误或业务请求失败时抛出
 */
export async function requestCenterApi<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')

  let response: Response
  try {
    response = await fetch(resolveCenterApiUrl(path), {
      ...init,
      headers,
    })
  } catch {
    throw new ApiRequestError(0, NETWORK_ERROR_CODE, '无法连接 GateWeave Center', null)
  }

  const requestId = response.headers.get(REQUEST_ID_HEADER)
  let body: unknown
  try {
    body = await response.json()
  } catch {
    throw new ApiRequestError(response.status, INVALID_RESPONSE_CODE, 'Center 返回了无法解析的响应', requestId)
  }

  if (!isApiResult(body)) {
    throw new ApiRequestError(response.status, INVALID_RESPONSE_CODE, 'Center 返回的响应结构不正确', requestId)
  }

  if (!response.ok || !body.success) {
    throw new ApiRequestError(response.status, body.code, body.message, requestId)
  }

  return body.data as T
}

function resolveCenterApiUrl(path: string): URL {
  const configuredBaseUrl = import.meta.env.VITE_GATEWEAVE_CENTER_API_BASE_URL
  if (!configuredBaseUrl) {
    throw new Error('VITE_GATEWEAVE_CENTER_API_BASE_URL must be configured')
  }
  const baseUrl = new URL(`${configuredBaseUrl.replace(/\/+$/, '')}/`)
  return new URL(path.replace(/^\/+/, ''), baseUrl)
}

function isApiResult(value: unknown): value is ApiResult<unknown> {
  if (typeof value !== 'object' || value == null) {
    return false
  }
  const result = value as Record<string, unknown>
  return (
    typeof result.success === 'boolean' &&
    typeof result.code === 'string' &&
    typeof result.message === 'string' &&
    result.data !== null &&
    result.data !== undefined
  )
}
