import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, errorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { DemoBanner, ErrorBox } from '../components/ui'

export default function Register() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [f, setF] = useState({ fullName: '', email: '', phone: '', password: '' })
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault(); setBusy(true); setError(null)
    try { await api.post('/auth/register', f); await login(f.email, f.password); navigate('/family') }
    catch (err) { setError(errorMessage(err)) } finally { setBusy(false) }
  }
  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-br from-navy-950 to-navy-700">
      <DemoBanner />
      <div className="flex-1 flex items-center justify-center p-4">
        <form onSubmit={submit} className="card w-full max-w-md space-y-4">
          <h1 className="text-xl font-bold text-navy-900">Family registration</h1>
          <p className="text-sm text-slate-500">Your phone number is never shown to volunteers or desk staff. Notifications reach you through MilanSetu.</p>
          {error && <ErrorBox message={error} />}
          <div><label className="label" htmlFor="n">Full name</label><input id="n" className="input" value={f.fullName} onChange={(e) => setF({ ...f, fullName: e.target.value })} required maxLength={120} /></div>
          <div><label className="label" htmlFor="e">Email</label><input id="e" type="email" className="input" value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} required /></div>
          <div><label className="label" htmlFor="p">Mobile number</label><input id="p" inputMode="numeric" pattern="[6-9][0-9]{9}" className="input" value={f.phone} onChange={(e) => setF({ ...f, phone: e.target.value })} required placeholder="10 digits" /></div>
          <div><label className="label" htmlFor="pw">Password (min 8)</label><input id="pw" type="password" minLength={8} className="input" value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} required /></div>
          <button className="btn-primary w-full" disabled={busy}>{busy ? 'Creating…' : 'Create family account'}</button>
          <p className="text-center text-sm"><Link to="/login" className="text-navy-700 hover:underline">Back to login</Link></p>
        </form>
      </div>
    </div>
  )
}
