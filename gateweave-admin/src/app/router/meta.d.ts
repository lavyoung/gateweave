import 'vue-router'

export {}

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    showInMenu?: boolean
    order?: number
  }
}
