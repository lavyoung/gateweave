import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { defineConfig } from 'vite'

const importElementPlusStyles = process.env.NODE_ENV !== 'test'

export default defineConfig({
  plugins: [
    vue(),
    Components({
      dts: 'src/app/components.d.ts',
      resolvers: [ElementPlusResolver({ importStyle: importElementPlusStyles ? 'css' : false })],
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
