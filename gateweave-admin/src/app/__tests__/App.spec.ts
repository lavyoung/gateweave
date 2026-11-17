import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import App from '@/App.vue'
import router from '@/app/router'

describe('App', () => {
  it('should redirect the root path to the dashboard', async () => {
    const wrapper = await mountApplication()

    expect(router.currentRoute.value.path).toBe('/dashboard')
    expect(wrapper.get('.dashboard h1').text()).toBe('GateWeave 管理后台')
  })

  it('should create navigation items from route metadata', async () => {
    const wrapper = await mountApplication()

    expect(wrapper.findAll('.el-menu-item').map((item) => item.text())).toEqual(['后台概览', '网关节点'])
  })

  it('should collapse and expand the sidebar locally', async () => {
    const wrapper = await mountApplication()
    const button = wrapper.get('[aria-label="切换侧边栏"]')

    await button.trigger('click')

    expect(wrapper.get('.brand').text()).toBe('GW')
    expect(button.text()).toBe('展开')
  })

  it('should render gateway node fixtures through the gateway node route', async () => {
    const wrapper = await mountApplication('/gateway-nodes')

    expect(router.currentRoute.value.path).toBe('/gateway-nodes')
    expect(wrapper.get('.gateway-node-page h2').text()).toBe('网关节点')
    expect(wrapper.text()).toContain('gateweave-engine-01')
    expect(wrapper.text()).toContain('默认网关节点二')
    expect(wrapper.findAll('.el-tag').map((tag) => tag.text())).toEqual(['在线', '离线'])
  })

  it('should render the not-found page for an unknown path', async () => {
    const wrapper = await mountApplication('/unknown-page')

    expect(router.currentRoute.value.name).toBe('not-found')
    expect(wrapper.get('.not-found-page h2').text()).toBe('页面不存在')
  })

  it('should return to the dashboard from the not-found page', async () => {
    const wrapper = await mountApplication('/unknown-page')

    await wrapper.get('.not-found-page .el-button').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/dashboard')
    expect(wrapper.get('.dashboard h1').text()).toBe('GateWeave 管理后台')
  })
})

async function mountApplication(path = '/') {
  await router.push(path)
  await router.isReady()

  const wrapper = mount(App, {
    global: {
      plugins: [router],
    },
  })
  await flushPromises()
  return wrapper
}
