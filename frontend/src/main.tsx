import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Toaster } from 'sonner'
import './index.css'
import { AuthProvider } from '@/lib/auth'
import { RealtimeProvider } from '@/lib/realtime'
import { AppRoutes } from './routes'

const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 10_000, retry: 1, refetchOnWindowFocus: false } },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <RealtimeProvider>
            <AppRoutes />
            <Toaster position="bottom-right" toastOptions={{ className: '!bg-background !text-foreground !border-border !rounded-lg !shadow-lg', descriptionClassName: '!text-muted' }} />
          </RealtimeProvider>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
