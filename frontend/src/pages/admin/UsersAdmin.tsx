import { useState, type FormEvent } from 'react'
import { api, errorMessage } from '../../api/client'
import { useApi } from '../../api/useApi'
import type { Page, Role, UserRow } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { ConfirmButton, ErrorBox, Loading, PageHeader, SuccessBox } from '../../components/ui'

export default function UsersAdmin() {
  const { user } = useAuth()
  const { data, loading, error, reload } = useApi<Page<UserRow>>('/users', { size: 100 })
  const [f, setF] = useState({ fullName: '', email: '', password: '', role: 'VOLUNTEER' as Role })
  const [msg, setMsg] = useState<string | null>(null)
  const [err, setErr] = useState<string | null>(null)
  async function create(e: FormEvent) {
    e.preventDefault(); setMsg(null); setErr(null)
    try { await api.post('/users', f); setMsg(`Created ${f.email}. Assign them to an event on the Events page.`); setF({ ...f, fullName: '', email: '', password: '' }); await reload() }
    catch (ex) { setErr(errorMessage(ex)) }
  }
  return (
    <div className="space-y-4">
      <PageHeader title="Users" subtitle="Families register themselves. Staff accounts are created here." />
      {msg && <SuccessBox message={msg} />}{err && <ErrorBox message={err} />}
      <form onSubmit={create} className="card grid md:grid-cols-5 gap-3 items-end">
        <div><label className="label" htmlFor="un">Name</label><input id="un" className="input" value={f.fullName} onChange={(e) => setF({ ...f, fullName: e.target.value })} required /></div>
        <div><label className="label" htmlFor="ue">Email</label><input id="ue" type="email" className="input" value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} required /></div>
        <div><label className="label" htmlFor="up">Temporary password</label><input id="up" type="password" minLength={8} className="input" value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} required /></div>
        <div><label className="label" htmlFor="ur">Role</label><select id="ur" className="input" value={f.role} onChange={(e) => setF({ ...f, role: e.target.value as Role })}>{(['VOLUNTEER', 'DESK', 'POLICE', 'ADMIN'] as const).map((r) => <option key={r}>{r}</option>)}</select></div>
        <button className="btn-primary">Create</button>
      </form>
      <div className="card overflow-x-auto">
        {loading && <Loading />}{error && <ErrorBox message={error} onRetry={reload} />}
        {data && <table className="table"><thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th /></tr></thead>
          <tbody>{data.content.map((u) => (
            <tr key={u.id}><td className="font-medium">{u.fullName}</td><td>{u.email}</td><td className="text-xs">{u.role}</td><td>{u.active ? 'Active' : 'Disabled'}</td>
              <td>{u.id !== user?.id && <ConfirmButton label={u.active ? 'Disable' : 'Enable'} className="btn-secondary" title="Change account status?" message={u.active ? 'The user is logged out immediately.' : 'The user can log in again.'}
                onConfirm={async () => { await api.patch(`/users/${u.id}`, { active: !u.active }); void reload() }} />}</td></tr>))}</tbody></table>}
      </div>
    </div>
  )
}
