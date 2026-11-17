import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import App from '@/App.vue'
import router from '@/app/router'

describe('App', () => {
  it('should redirect the root path to the dashboard', async () => {
    await router.push('/')
    await router.isReady()

    const wrapper = mount(App, {
      global: {
        plugins: [router],
      },
    })
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/dashboard')
    expect(wrapper.get('h1').text()).toBe('GateWeave 管理后台')
  })
})
