import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  //改讀根目錄(gigafix)的.env，且只有VITE_開頭的變數才會被打包進前端程式碼
  envDir: '../',
  server:{
    proxy:{
      "/api":{
        target:"http://localhost:8080",
        changeOrigin:true
      }
    }
  }
})
