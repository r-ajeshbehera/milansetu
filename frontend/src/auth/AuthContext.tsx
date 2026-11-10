import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, setUnauthorizedHandler, tokenStore } from '../api/client'
import type { LoginResponse, Profile as User, Role } from '../api/types'

interface AuthState {
  user: User | null
  ready: boolean
  login: (email: string, password: string) => Promise<User>
  logout: () => Promise<void>
  hasRole: (...roles: Role[]) => boolean
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    setUnauthorizedHandler(() => { tokenStore.clear(); setUser(null) })
    if (!tokenStore.get()) { setReady(true); return }
    api.get<User>('/users/me').then((r) => setUser(r.data)).catch(() => tokenStore.clear()).finally(() => setReady(true))
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const res = await api.post<LoginResponse>('/auth/login', { email, password })
    tokenStore.set(res.data.accessToken)
    setUser(res.data.user)
    return res.data.user
  }, [])

  const logout = useCallback(async () => {
    try { await api.post('/auth/logout') } catch { /* token may already be invalid */ }
    tokenStore.clear()
    setUser(null)
  }, [])

  const hasRole = useCallback((...roles: Role[]) => !!user && roles.includes(user.role), [user])

  const value = useMemo(() => ({ user, ready, login, logout, hasRole }), [user, ready, login, logout, hasRole])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}

export function homePathFor(role: Role): string {
  switch (role) {
    case 'FAMILY': return '/family'
    case 'VOLUNTEER': return '/volunteer'
    case 'DESK': return '/desk'
    case 'POLICE': return '/police'
    case 'ADMIN': return '/admin/events'
  }
}
