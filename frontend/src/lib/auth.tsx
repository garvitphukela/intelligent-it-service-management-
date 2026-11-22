import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { get, post, setUnauthorizedHandler, tokenStore } from './api'
import type { AuthResponse, Role, UserView } from './types'

interface AuthState {
  user: UserView | null
  loading: boolean
  login: (email: string, password: string) => Promise<UserView>
  register: (data: { fullName: string; email: string; password: string; phone?: string; department?: string }) => Promise<UserView>
  logout: () => void
  refresh: () => Promise<void>
  hasRole: (...roles: Role[]) => boolean
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserView | null>(null)
  const [loading, setLoading] = useState(true)
  const qc = useQueryClient()

  const logout = useCallback(() => {
    tokenStore.set(null)
    setUser(null)
    qc.clear()
  }, [qc])

  useEffect(() => { setUnauthorizedHandler(logout) }, [logout])

  const refresh = useCallback(async () => {
    if (!tokenStore.get()) { setLoading(false); return }
    try { setUser(await get<UserView>('/auth/me')) } catch { tokenStore.set(null); setUser(null) } finally { setLoading(false) }
  }, [])

  useEffect(() => { refresh() }, [refresh])

  const login = useCallback(async (email: string, password: string) => {
    const r = await post<AuthResponse>('/auth/login', { email, password })
    tokenStore.set(r.token)
    setUser(r.user)
    return r.user
  }, [])

  const register = useCallback(async (data: { fullName: string; email: string; password: string }) => {
    const r = await post<AuthResponse>('/auth/register', data)
    tokenStore.set(r.token)
    setUser(r.user)
    return r.user
  }, [])

  const hasRole = useCallback((...roles: Role[]) => !!user && roles.includes(user.role), [user])

  const value = useMemo(() => ({ user, loading, login, register, logout, refresh, hasRole }), [user, loading, login, register, logout, refresh, hasRole])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}

export const isStaff = (r?: Role) => !!r && r !== 'EMPLOYEE'
export const isManagement = (r?: Role) => r === 'ADMIN' || r === 'MANAGER'
export const isEngineer = (r?: Role) => r === 'TECHNICIAN' || r === 'TEAM_LEAD'
