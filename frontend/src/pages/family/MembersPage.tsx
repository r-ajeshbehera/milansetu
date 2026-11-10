import { useEffect, useState, type FormEvent } from 'react'
import { api, errorMessage } from '../../api/client'
import { fetchBlobUrl } from '../../api/blob'
import { useApi } from '../../api/useApi'
import type { Gender, MEvent, Member } from '../../api/types'
import { ConfirmButton, Empty, ErrorBox, Loading, PageHeader, SuccessBox } from '../../components/ui'

function QrCard({ member, onChanged }: { member: Member; onChanged: () => void }) {
  const [src, setSrc] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)
  useEffect(() => {
    let url: string | null = null
    fetchBlobUrl(`/members/${member.id}/qr`).then((u) => { url = u; setSrc(u) }).catch((e) => setError(errorMessage(e)))
    return () => { if (url) URL.revokeObjectURL(url) }
  }, [member.id, version])

  async function printWristband() {
    const url = await fetchBlobUrl(`/members/${member.id}/wristband`)
    window.open(url, '_blank', 'noopener')
  }
  return (
    <div className="card flex flex-col sm:flex-row gap-4 items-center">
      {src ? <img src={src} alt={`QR wristband code for ${member.fullName}`} className="h-36 w-36 rounded border" /> : error ? <ErrorBox message={error} /> : <Loading text="" />}
      <div className="flex-1 space-y-1">
        <div className="font-semibold text-navy-900">{member.fullName}</div>
        <div className="text-sm text-slate-600">{member.age} years · {member.gender.toLowerCase()} · {member.eventName}</div>
        {member.notes && <div className="text-xs text-slate-500">Private note (only you see this): {member.notes}</div>}
        <div className="flex flex-wrap gap-2 pt-2">
          {src && <a href={src} download={`milansetu-${member.fullName.split(' ')[0].toLowerCase()}-qr.png`} className="btn-secondary">Download PNG</a>}
          <button className="btn-primary" onClick={printWristband}>Print wristband</button>
          <ConfirmButton label="Replace lost wristband" className="btn-secondary" title="Issue a new QR code?"
            message="The old wristband will stop working immediately. Print the new one."
            onConfirm={async () => { await api.post(`/members/${member.id}/qr/regenerate`); setVersion((v) => v + 1); onChanged() }} />
        </div>
      </div>
    </div>
  )
}

export default function MembersPage() {
  const { data, loading, error, reload } = useApi<Member[]>('/members')
  const events = useApi<MEvent[]>('/events')
  const [f, setF] = useState({ eventId: '', fullName: '', age: '', gender: 'FEMALE' as Gender, notes: '' })
  const [msg, setMsg] = useState<string | null>(null)
  const [err, setErr] = useState<string | null>(null)
  const eventId = f.eventId || String(events.data?.find((e) => e.active)?.id ?? events.data?.[0]?.id ?? '')

  async function add(e: FormEvent) {
    e.preventDefault(); setMsg(null); setErr(null)
    try {
      await api.post('/members', { eventId: Number(eventId), fullName: f.fullName, age: Number(f.age), gender: f.gender, notes: f.notes || undefined })
      setMsg(`${f.fullName} registered. Print the wristband below.`); setF({ ...f, fullName: '', age: '', notes: '' }); await reload()
    } catch (ex) { setErr(errorMessage(ex)) }
  }
  return (
    <div className="space-y-6">
      <PageHeader title="My members" subtitle="The QR code contains only a random code — no name, phone or address." />
      <form onSubmit={add} className="card grid md:grid-cols-5 gap-3 items-end">
        <div className="md:col-span-2"><label className="label" htmlFor="fn">Full name</label><input id="fn" className="input" value={f.fullName} onChange={(e) => setF({ ...f, fullName: e.target.value })} required maxLength={120} /></div>
        <div><label className="label" htmlFor="ag">Age</label><input id="ag" type="number" min={0} max={120} className="input" value={f.age} onChange={(e) => setF({ ...f, age: e.target.value })} required /></div>
        <div><label className="label" htmlFor="gn">Gender</label><select id="gn" className="input" value={f.gender} onChange={(e) => setF({ ...f, gender: e.target.value as Gender })}><option value="FEMALE">Female</option><option value="MALE">Male</option><option value="OTHER">Other</option></select></div>
        <div><label className="label" htmlFor="ev">Event</label><select id="ev" className="input" value={eventId} onChange={(e) => setF({ ...f, eventId: e.target.value })}>{events.data?.map((ev) => <option key={ev.id} value={ev.id}>{ev.name}</option>)}</select></div>
        <div className="md:col-span-4"><label className="label" htmlFor="nt">Private notes (optional, only you see them)</label><input id="nt" className="input" maxLength={500} value={f.notes} onChange={(e) => setF({ ...f, notes: e.target.value })} /></div>
        <button className="btn-primary">Register member</button>
      </form>
      {msg && <SuccessBox message={msg} />}{err && <ErrorBox message={err} />}
      {loading && <Loading />}{error && <ErrorBox message={error} onRetry={reload} />}
      {data?.length === 0 && <Empty text="No members yet." />}
      <div className="grid lg:grid-cols-2 gap-4">{data?.map((m) => <QrCard key={m.id} member={m} onChanged={reload} />)}</div>
    </div>
  )
}
