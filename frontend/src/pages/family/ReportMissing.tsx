import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, errorMessage } from '../../api/client'
import { useApi } from '../../api/useApi'
import type { CaseItem, Gender, MEvent, Member } from '../../api/types'
import { ColourPicker, ErrorBox, PageHeader } from '../../components/ui'

const nowLocal = () => { const d = new Date(); d.setSeconds(0, 0); return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16) }

export default function ReportMissing() {
  const navigate = useNavigate()
  const members = useApi<Member[]>('/members')
  const events = useApi<MEvent[]>('/events', { active: true })
  const [f, setF] = useState({ memberId: '', personName: '', approxAge: '', gender: 'FEMALE' as Gender, clothingColor: '', lat: '', lng: '', lastSeenAt: nowLocal() })
  const [err, setErr] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  function pickMember(id: string) {
    const m = members.data?.find((x) => String(x.id) === id)
    setF({ ...f, memberId: id, personName: m?.fullName ?? f.personName, approxAge: m ? String(m.age) : f.approxAge, gender: m?.gender ?? f.gender })
  }
  function useMyLocation() {
    navigator.geolocation?.getCurrentPosition((p) => setF((x) => ({ ...x, lat: p.coords.latitude.toFixed(6), lng: p.coords.longitude.toFixed(6) })),
      () => setErr('Location permission denied. Enter the place on the map coordinates manually.'))
  }
  async function submit(e: FormEvent) {
    e.preventDefault(); setErr(null)
    if (!f.clothingColor) { setErr('Please choose the clothing colour.'); return }
    const member = members.data?.find((m) => String(m.id) === f.memberId)
    const eventId = member?.eventId ?? events.data?.[0]?.id
    setBusy(true)
    try {
      const res = await api.post<CaseItem>('/reports/missing', {
        eventId, memberId: f.memberId ? Number(f.memberId) : undefined, personName: f.personName, approxAge: Number(f.approxAge),
        gender: f.gender, clothingColor: f.clothingColor, lastSeenLat: Number(f.lat), lastSeenLng: Number(f.lng), lastSeenAt: f.lastSeenAt + ':00',
      })
      navigate(`/cases/${res.data.id}`)
    } catch (ex) { setErr(errorMessage(ex)) } finally { setBusy(false) }
  }
  return (
    <div className="max-w-2xl space-y-4">
      <PageHeader title="Report a missing person" subtitle="Also tell the nearest help desk or police booth in person." />
      {err && <ErrorBox message={err} />}
      <form onSubmit={submit} className="card space-y-4">
        <div><label className="label" htmlFor="mem">Registered member (optional)</label>
          <select id="mem" className="input" value={f.memberId} onChange={(e) => pickMember(e.target.value)}>
            <option value="">— not registered —</option>{members.data?.map((m) => <option key={m.id} value={m.id}>{m.fullName}</option>)}</select></div>
        <div className="grid sm:grid-cols-3 gap-3">
          <div className="sm:col-span-2"><label className="label" htmlFor="pn">Name</label><input id="pn" className="input" value={f.personName} onChange={(e) => setF({ ...f, personName: e.target.value })} required maxLength={120} /></div>
          <div><label className="label" htmlFor="aa">Approx. age</label><input id="aa" type="number" min={0} max={120} className="input" value={f.approxAge} onChange={(e) => setF({ ...f, approxAge: e.target.value })} required /></div>
        </div>
        <div><label className="label" htmlFor="g">Gender</label><select id="g" className="input" value={f.gender} onChange={(e) => setF({ ...f, gender: e.target.value as Gender })}><option value="FEMALE">Female</option><option value="MALE">Male</option><option value="OTHER">Other</option></select></div>
        <div><span className="label">Main clothing colour</span><ColourPicker value={f.clothingColor} onChange={(c) => setF({ ...f, clothingColor: c })} /></div>
        <div className="grid sm:grid-cols-3 gap-3 items-end">
          <div><label className="label" htmlFor="la">Last seen latitude</label><input id="la" type="number" step="0.000001" min={-90} max={90} className="input" value={f.lat} onChange={(e) => setF({ ...f, lat: e.target.value })} required /></div>
          <div><label className="label" htmlFor="lo">Longitude</label><input id="lo" type="number" step="0.000001" min={-180} max={180} className="input" value={f.lng} onChange={(e) => setF({ ...f, lng: e.target.value })} required /></div>
          <button type="button" className="btn-secondary" onClick={useMyLocation}>Use my location</button>
        </div>
        <div><label className="label" htmlFor="ls">Last seen at</label><input id="ls" type="datetime-local" max={nowLocal()} className="input" value={f.lastSeenAt} onChange={(e) => setF({ ...f, lastSeenAt: e.target.value })} required /></div>
        <button className="btn-danger w-full" disabled={busy}>{busy ? 'Submitting…' : 'Submit missing report'}</button>
      </form>
    </div>
  )
}
