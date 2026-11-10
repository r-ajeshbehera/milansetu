import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, errorMessage } from '../../api/client'
import { useApi } from '../../api/useApi'
import type { CaseItem, Desk, Gender, MEvent } from '../../api/types'
import { ColourPicker, ErrorBox, SuccessBox } from '../../components/ui'

/** Found person WITHOUT a wristband (manual report). */
export default function FoundForm({ goToCase = false }: { goToCase?: boolean }) {
  const navigate = useNavigate()
  const events = useApi<MEvent[]>('/events', { active: true })
  const eventId = events.data?.[0]?.id
  const desks = useApi<Desk[]>(eventId ? `/events/${eventId}/desks` : null)
  const [f, setF] = useState({ deskId: '', approxAge: '', gender: 'MALE' as Gender, clothingColor: '', lat: '', lng: '', safeNotes: '' })
  const [msg, setMsg] = useState<string | null>(null)
  const [err, setErr] = useState<string | null>(null)

  function here() {
    navigator.geolocation?.getCurrentPosition((p) => setF((x) => ({ ...x, lat: p.coords.latitude.toFixed(6), lng: p.coords.longitude.toFixed(6) })),
      () => setErr('Location permission denied; enter coordinates or choose a desk.'))
  }
  async function submit(e: FormEvent) {
    e.preventDefault(); setMsg(null); setErr(null)
    if (!f.clothingColor) { setErr('Choose the clothing colour.'); return }
    const desk = desks.data?.find((d) => String(d.id) === f.deskId)
    try {
      const res = await api.post<CaseItem>('/reports/found', {
        eventId, deskId: f.deskId ? Number(f.deskId) : undefined, approxAge: Number(f.approxAge), gender: f.gender, clothingColor: f.clothingColor,
        foundLat: Number(f.lat || desk?.latitude), foundLng: Number(f.lng || desk?.longitude), safeNotes: f.safeNotes || undefined,
      })
      if (goToCase) navigate(`/cases/${res.data.id}`)
      setMsg(`Found report created (case #${res.data.id}). Take the person to ${res.data.assignedDeskName}.`)
      setF({ ...f, approxAge: '', clothingColor: '', safeNotes: '' })
    } catch (ex) { setErr(errorMessage(ex)) }
  }
  return (
    <form onSubmit={submit} className="card space-y-4">
      <h2 className="font-semibold text-navy-900">Register found person (no wristband)</h2>
      {msg && <SuccessBox message={msg} />}{err && <ErrorBox message={err} />}
      <div className="grid sm:grid-cols-2 gap-3">
        <div><label className="label" htmlFor="fa">Approx. age</label><input id="fa" type="number" min={0} max={120} className="input" value={f.approxAge} onChange={(e) => setF({ ...f, approxAge: e.target.value })} required /></div>
        <div><label className="label" htmlFor="fg">Gender</label><select id="fg" className="input" value={f.gender} onChange={(e) => setF({ ...f, gender: e.target.value as Gender })}><option value="MALE">Male</option><option value="FEMALE">Female</option><option value="OTHER">Other</option></select></div>
      </div>
      <div><span className="label">Clothing colour</span><ColourPicker value={f.clothingColor} onChange={(c) => setF({ ...f, clothingColor: c })} /></div>
      <div className="grid sm:grid-cols-3 gap-3 items-end">
        <div><label className="label" htmlFor="flat">Latitude</label><input id="flat" type="number" step="0.000001" className="input" value={f.lat} onChange={(e) => setF({ ...f, lat: e.target.value })} required={!f.deskId} /></div>
        <div><label className="label" htmlFor="flng">Longitude</label><input id="flng" type="number" step="0.000001" className="input" value={f.lng} onChange={(e) => setF({ ...f, lng: e.target.value })} required={!f.deskId} /></div>
        <button type="button" className="btn-secondary" onClick={here}>Use my location</button>
      </div>
      <div><label className="label" htmlFor="fd">Help desk (empty = nearest)</label>
        <select id="fd" className="input" value={f.deskId} onChange={(e) => setF({ ...f, deskId: e.target.value })}>
          <option value="">Nearest desk</option>{desks.data?.filter((d) => d.active).map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}</select></div>
      <div><label className="label" htmlFor="fn">Safe notes (no personal data)</label><input id="fn" className="input" maxLength={500} value={f.safeNotes} onChange={(e) => setF({ ...f, safeNotes: e.target.value })} placeholder="e.g. speaks Odia, says father sells flowers" /></div>
      <button className="btn-primary w-full" disabled={!eventId}>Register found person</button>
    </form>
  )
}
