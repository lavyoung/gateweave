import {createRouter, createWebHistory} from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/features/dashboard/DashboardView.vue'),
          meta: {
            title: '后台概览',
            showInMenu: true,
            order: 10,
          },
        },
        {
          path: 'gateway-nodes',
          name: 'gateway-nodes',
          component: () => import('@/features/gateway-node/GatewayNodeListView.vue'),
          meta: {
            title: '网关节点',
            showInMenu: true,
            order: 20,
          },
        },
        {
          path: ':pathMatch(.*)*',
          name: 'not-found',
          component: () => import('@/features/not-found/NotFoundView.vue'),
          meta: {
            title: '页面不存在',
          },
        },
      ],
    },
  ],
})

export default router
