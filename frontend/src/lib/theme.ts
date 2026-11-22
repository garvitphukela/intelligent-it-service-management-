import { useEffect, useState } from 'react'

export type Theme = 'light' | 'dark'

export function useTheme(): [Theme, (t: Theme) => void] {
  const [theme, setTheme] = useState<Theme>(() => (document.documentElement.classList.contains('dark') ? 'dark' : 'light'))
  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark')
    try { localStorage.setItem('itsm-theme', theme) } catch { /* ignore */ }
  }, [theme])
  return [theme, setTheme]
}
