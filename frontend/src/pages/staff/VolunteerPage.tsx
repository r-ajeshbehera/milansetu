import { useState, type FormEvent } from 'react'
import { api, errorMessage } from '../../api/client'
import { useApi } from '../../api/useApi'
import type { Desk, MEvent, ScanResponse } from '../../api/types'
import { ColourPicker, ErrorBox, PageHeader, SuccessBox } from '../../components/ui'
import FoundForm from './FoundForm'
import QrScanner from './QrScanner'

export default function VolunteerPage() {
  const [manual, setManual] = useState('')
  const [colour, setColour] = useState('')
  const [result, setResult] = useState<ScanResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const events = useApi<MEvent[]>('/events', { active: true })
  const desks = useApi<Desk[]>(events.data?.[0] ? `/events/${events.data[0].id}/desks` : null)

  function scan(token: string) {
    setError(null); setResult(null)
    const send = (lat?: number, lng?: number) => api.post<ScanResponse>(`/scan/${encodeURIComponent(token.trim())}`, { lat, lng, clothingColor: colour || undefined })
      .then((r) => setResult(r.data)).catch((e) => setError(errorMessage(e)))
    if (navigator.geolocation) navigator.geolocation.getCurrentPosition((p) => send(p.coords.latitude, p.coords.longitude), () => send(), { timeout: 4000 })
    else void send()
  }
  return (
    <div className="space-y-6">
      <PageHeader title="Volunteer" subtitle="Scan a wristband or register a found person. You will never see the family's contact details." />
      <div className="grid lg:grid-cols-2 gap-6">
        <div className="card space-y-4">
          <h2 className="font-semibold text-navy-900">1. Scan wristband</h2>
          <ol className="text-sm text-slate-600 list-decimal pl-5"><li>Allow camera access.</li><li>Hold the QR code inside the square.</li><li>Follow the instructions shown.</li></ol>
          <div><span className="label">Clothing colour you can see (optional)</span><ColourPicker value={colour} onChange={setColour} /></div>
          <QrScanner onScan={scan} />
          <form className="flex gap-2" onSubmit={(e: FormEvent) => { e.preventDefault(); if (manual) scan(manual) }}>
            <input aria-label="Manual QR code entry" className="input" placeholder="Manual entry: code printed under the QR" value={manual} onChange={(e) => setManual(e.target.value)} />
            <button className="btn-secondary">Submit</button>
          </form>
          {error && <ErrorBox message={error} />}
          {result && (
            <div className="space-y-2">
              <SuccessBox message={result.newCase ? `Case #${result.caseId} created. The family has been notified.` : `Already reported (case #${result.caseId}).`} />
              <div className="rounded-lg border-2 border-navy-700 p-4">
                <div className="text-2xl font-bold text-navy-900">{result.firstName}</div>
                <div className="text-sm text-slate-600">{result.approxAge} years · {result.gender.toLowerCase()}</div>
                <div className="mt-2 font-semibold">Take to: {result.desk.name}{result.desk.distanceKm != null ? ` (${result.desk.distanceKm} km)` : ''}</div>
                <p className="text-sm text-slate-700 mt-1">{result.instructions}</p>
              </div>
            </div>
          )}
        </div>
        <div className="space-y-6">
          <FoundForm />
          <div className="card"><h2 className="font-semibold text-navy-900 mb-2">Help desks</h2>
            <ul className="text-sm divide-y divide-slate-100">{desks.data?.filter((d) => d.active).map((d) => (
              <li key={d.id} className="py-1.5 flex justify-between"><span>{d.name}</span>
                <a className="text-navy-700 hover:underline text-xs" target="_blank" rel="noreferrer" href={`https://www.openstreetmap.org/?mlat=${d.latitude}&mlon=${d.longitude}#map=18/${d.latitude}/${d.longitude}`}>map</a></li>))}</ul></div>
        </div>
      </div>
    </div>
  )
}
