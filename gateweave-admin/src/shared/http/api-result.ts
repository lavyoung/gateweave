/**
 * 表示 GateWeave Center 返回的统一 HTTP 响应。
 *
 * T 是当前接口的业务数据类型。前端只依赖 JSON 协议，
 * 不直接依赖 gateweave-contract 中的 Java 类型。
 */
export interface ApiResult<T> {
  readonly success: boolean
  readonly code: string
  readonly message: string
  readonly data: T
}
