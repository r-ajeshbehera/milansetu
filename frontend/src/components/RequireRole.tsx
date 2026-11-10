import { Navigate, Outlet } from 'react-router-dom'
import { homePathFor, useAuth } from '../auth/AuthContext'
import type { Role } from '../api/types'
import { Loading } from './ui'

/** Frontend guard for convenience only. The backend enforces every permission again. */
export default function RequireRole({ roles }: { roles?: Role[] }) {
  const { user, ready } = useAuth()
  if (!ready) return <Loading />
  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to={homePathFor(user.role)} replace />
  return <Outlet />
}
