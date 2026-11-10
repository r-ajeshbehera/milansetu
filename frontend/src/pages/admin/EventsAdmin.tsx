import { useState, type FormEvent } from 'react'
import { api, errorMessage } from '../../api/client'
import { useApi } from '../../api/useApi'
import type { Desk, MEvent, Page, Staff, UserRow } from '../../api/types'
import { ConfirmButton, Empty, ErrorBox, Loading, PageHeader, SuccessBox } from '../../components/ui'

function EventDetail({ ev, onChanged }: { ev: MEvent; onChanged: () => void }) {
  const desks = useApi<Desk[]>(`/events/${ev.id}/desks`)
  const staff = useApi<Staff[]>(`/events/${ev.id}/staff`)
  const users = useApi<Page<UserRow>>('/users', { size: 100 })
  const [d, setD] = useState({ name: '', latitude: '', longitude: '', inChargeId: '' })
  const [s, setS] = useState({ userId: '', deskId: '' })
  const [err, setErr] = useState<string | null>(null)
  const staffUsers = users.data?.content.filter((u) => ['VOLUNTEER', 'DESK', 'POLICE'].includes(u.role) && u.active) ?? []

  async function addDesk(e: FormEvent) {
    e.preventDefault(); setErr(null)
    try { await api.post(`/events/${ev.id}/desks`, { name: d.name, latitude: Number(d.latitude), longitude: Number(d.longitude), inChargeId: d.inChargeId ? Number(d.inChargeId) : undefined }); setD({ name: '', latitude: '', longitude: '', inChargeId: '' }); await desks.reload(); await staff.reload() }
    catch (ex) { setErr(errorMessage(ex)) }
  }
  async function assign(e: FormEvent) {
    e.preventDefault(); setErr(null)
    try { await api.put(`/events/${ev.id}/staff/${s.userId}`, { deskId: s.deskId ? Number(s.deskId) : null }); setS({ userId: '', deskId: '' }); await staff.reload() }
    catch (ex) { setErr(errorMessage(ex)) }
  }
  return (
    <div className="card space-y-4">
      <div className="flex flex-wrap justify-between gap-2 items-center">
        <div><div className="font-semibold text-navy-900">{ev.name}</div><div className="text-xs text-slate-500">{ev.city} · {ev.startDate} → {ev.endDate}</div></div>
        <ConfirmButton label={ev.active ? 'Deactivate' : 'Activate'} className="btn-secondary" title={`${ev.active ? 'Deactivate' : 'Activate'} event?`}
          message={ev.active ? 'Scans and reports will be refused for an inactive event.' : 'Scanning and reporting become possible.'}
          onConfirm={async () => { await api.put(`/events/${ev.id}`, { ...ev, active: !ev.active }); onChanged() }} />
      </div>
      {err && <ErrorBox message={err} />}
      <div className="grid lg:grid-cols-2 gap-4">
        <div className="space-y-2">
          <h3 className="font-semibold text-sm">Help desks</h3>
          {desks.data?.length === 0 && <Empty text="No desks." />}
          <ul className="text-sm divide-y divide-slate-100">{desks.data?.map((x) => <li key={x.id} className="py-1.5">{x.name} <span className="text-xs text-slate-500">({Number(x.latitude).toFixed(4)}, {Number(x.longitude).toFixed(4)}) · in-charge: {x.inChargeName ?? '—'}</span></li>)}</ul>
          <form onSubmit={addDesk} className="grid grid-cols-2 gap-2">
            <input aria-label="Desk name" className="input col-span-2" placeholder="Desk name" value={d.name} onChange={(e) => setD({ ...d, name: e.target.value })} required />
            <input aria-label="Latitude" type="number" step="0.000001" className="input" placeholder="Latitude" value={d.latitude} onChange={(e) => setD({ ...d, latitude: e.target.value })} required />
            <input aria-label="Longitude" type="number" step="0.000001" className="input" placeholder="Longitude" value={d.longitude} onChange={(e) => setD({ ...d, longitude: e.target.value })} required />
            <select aria-label="Desk in-charge" className="input" value={d.inChargeId} onChange={(e) => setD({ ...d, inChargeId: e.target.value })}><option value="">In-charge (optional)</option>{staffUsers.filter((u) => u.role === 'DESK').map((u) => <option key={u.id} value={u.id}>{u.fullName}</option>)}</select>
            <button className="btn-primary">Add desk</button>
          </form>
        </div>
        <div className="space-y-2">
          <h3 className="font-semibold text-sm">Staff</h3>
          <ul className="text-sm divide-y divide-slate-100">{staff.data?.map((x) => (
            <li key={x.userId} className="py-1.5 flex justify-between"><span>{x.fullName} <span className="text-xs text-slate-500">{x.role}{x.deskId ? ` · desk ${desks.data?.find((dd) => dd.id === x.deskId)?.name ?? x.deskId}` : ''}</span></span>
              <button className="text-xs text-red-700 hover:underline" onClick={async () => { await api.delete(`/events/${ev.id}/staff/${x.userId}`); void staff.reload() }}>remove</button></li>))}</ul>
          <form onSubmit={assign} className="grid grid-cols-2 gap-2">
            <select aria-label="Staff user" className="input" value={s.userId} onChange={(e) => setS({ ...s, userId: e.target.value })} required><option value="">User…</option>{staffUsers.map((u) => <option key={u.id} value={u.id}>{u.fullName} ({u.role})</option>)}</select>
            <select aria-label="Home desk" className="input" value={s.deskId} onChange={(e) => setS({ ...s, deskId: e.target.value })}><option value="">No home desk</option>{desks.data?.map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}</select>
            <button className="btn-primary col-span-2">Assign to event</button>
          </form>
        </div>
      </div>
    </div>
  )
}

export default function EventsAdmin() {
  const { data, loading, error, reload } = useApi<MEvent[]>('/events')
  const [f, setF] = useState({ name: '', city: '', startDate: '', endDate: '' })
  const [msg, setMsg] = useState<string | null>(null)
  const [err, setErr] = useState<string | null>(null)
  async function create(e: FormEvent) {
    e.preventDefault(); setMsg(null); setErr(null)
    try { await api.post('/events', { ...f, active: false }); setMsg('Event created (inactive). Add desks and staff, then activate it.'); setF({ name: '', city: '', startDate: '', endDate: '' }); await reload() }
    catch (ex) { setErr(errorMessage(ex)) }
  }
  return (
    <div className="space-y-4">
      <PageHeader title="Events, help desks and staff" />
      {msg && <SuccessBox message={msg} />}{err && <ErrorBox message={err} />}
      <form onSubmit={create} className="card grid md:grid-cols-5 gap-3 items-end">
        <div className="md:col-span-2"><label className="label" htmlFor="en">Event name</label><input id="en" className="input" placeholder="Rath Yatra (DEMO)" value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} required /></div>
        <div><label className="label" htmlFor="ec">City</label><input id="ec" className="input" value={f.city} onChange={(e) => setF({ ...f, city: e.target.value })} required /></div>
        <div><label className="label" htmlFor="es">Start</label><input id="es" type="date" className="input" value={f.startDate} onChange={(e) => setF({ ...f, startDate: e.target.value })} required /></div>
        <div><label className="label" htmlFor="ee">End</label><input id="ee" type="date" className="input" value={f.endDate} onChange={(e) => setF({ ...f, endDate: e.target.value })} required /></div>
        <button className="btn-primary md:col-span-5 w-fit">Create event</button>
      </form>
      {loading && <Loading />}{error && <ErrorBox message={error} onRetry={reload} />}
      {data?.map((ev) => <EventDetail key={ev.id} ev={ev} onChanged={reload} />)}
    </div>
  )
}
