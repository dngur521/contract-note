import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // 화면에서 부르는 /api는 스프링 부트 서버로 넘긴다
    proxy: { '/api': 'http://localhost:8080' },
  },
})
