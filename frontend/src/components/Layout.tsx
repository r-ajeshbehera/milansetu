import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useApi } from '../api/useApi'
import type { Role } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { DemoBanner } from './ui'

const links: { to: string; label: string; roles: Role[] }[] = [
  { to: '/family', label: 'Home', roles: ['FAMILY'] },
  { to: '/family/members', label: 'My members', roles: ['FAMILY'] },
  { to: '/family/report-missing', label: 'Report missing', roles: ['FAMILY'] },
  { to: '/cases', label: 'My cases', roles: ['FAMILY'] },
  { to: '/volunteer', label: 'Scan & register', roles: ['VOLUNTEER'] },
  { to: '/desk', label: 'Desk cases', roles: ['DESK'] },
  { to: '/desk/found', label: 'Register found person', roles: ['DESK'] },
  { to: '/police', label: 'Dashboard', roles: ['POLICE', 'ADMIN'] },
  { to: '/cases', label: 'Cases', roles: ['POLICE', 'ADMIN'] },
  { to: '/admin/events', label: 'Events & desks', roles: ['ADMIN'] },
  { to: '/admin/users', label: 'Users', roles: ['ADMIN'] },
  { to: '/notifications', label: 'Notifications', roles: ['FAMILY', 'VOLUNTEER', 'DESK', 'POLICE', 'ADMIN'] },
]

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const unread = useApi<{ unread: number }>(user ? '/notifications/unread-count' : null)
  if (!user) return null
  return (
    <div className="min-h-screen flex flex-col">
      <DemoBanner />
      <header className="bg-navy-900 text-white">
        <div className="mx-auto max-w-7xl px-4 py-3 flex flex-wrap items-center gap-x-6 gap-y-2">
          <div className="font-bold text-lg">Milan<span className="text-amber-300">Setu</span></div>
          <nav className="flex flex-wrap gap-1 text-sm flex-1">
            {links.filter((l) => l.roles.includes(user.role)).map((l) => (
              <NavLink key={l.to + l.label} to={l.to} end className={({ isActive }) =>
                `rounded-md px-3 py-1.5 ${isActive ? 'bg-navy-700' : 'text-slate-200 hover:bg-navy-800'}`}>
                {l.label}{l.to === '/notifications' && unread.data?.unread ? <span className="ml-1 rounded-full bg-amber-400 text-navy-900 px-1.5 text-xs font-bold">{unread.data.unread}</span> : null}
              </NavLink>
            ))}
          </nav>
          <div className="flex items-center gap-3 text-sm">
            <div className="text-right leading-tight"><div className="font-medium">{user.fullName}</div><div className="text-xs text-slate-300">{user.role}</div></div>
            <button className="rounded-md border border-white/30 px-3 py-1.5 hover:bg-navy-800" onClick={async () => { await logout(); navigate('/login') }}>Log out</button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-7xl w-full px-4 py-6 flex-1"><Outlet /></main>
    </div>
  )
}
