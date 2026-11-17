import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import DashboardView from '@/features/dashboard/DashboardView.vue'

describe('DashboardView', () => {
  it('should describe the control-plane capabilities', () => {
    const wrapper = mount(DashboardView)

    expect(wrapper.get('h1').text()).toBe('GateWeave 管理后台')
    expect(wrapper.get('p').text()).toContain('网关节点、服务目录、路由和配置发布')
  })
})
