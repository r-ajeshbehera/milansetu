import { useState } from 'react'
import { Bar, BarChart, CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { useApi } from '../../api/useApi'
import type { CaseItem, Dashboard, MEvent, Page } from '../../api/types'
import { ErrorBox, Loading, PageHeader, StatCard } from '../../components/ui'
import { CasesTable } from './DeskHome'

export default function PoliceDashboard() {
  const events = useApi<MEvent[]>('/events')
  const [f, setF] = useState({ eventId: '', from: '', to: '' })
  const eventId = f.eventId || String(events.data?.find((e) => e.active)?.id ?? '')
  const params = { ...Object.fromEntries(Object.entries({ ...f, eventId }).filter(([, v]) => v)) }
  const { data, loading, error, reload } = useApi<Dashboard>(eventId ? '/dashboard/summary' : null, params)
  const open = useApi<Page<CaseItem>>(eventId ? '/cases' : null, { eventId, status: 'REPORTED', size: 10 })
  return (
    <div className="space-y-6">
      <PageHeader title="Police control dashboard" subtitle="All figures come from live database queries." actions={<button className="btn-secondary" onClick={() => { void reload(); void open.reload() }}>Refresh</button>} />
      <div className="card flex flex-wrap gap-3 items-end">
        <div><label className="label" htmlFor="pe">Event</label><select id="pe" className="input" value={eventId} onChange={(e) => setF({ ...f, eventId: e.target.value })}>{events.data?.map((ev) => <option key={ev.id} value={ev.id}>{ev.name}</option>)}</select></div>
        <div><label className="label" htmlFor="pf">From</label><input id="pf" type="date" className="input" value={f.from} onChange={(e) => setF({ ...f, from: e.target.value })} /></div>
        <div><label className="label" htmlFor="pt">To</label><input id="pt" type="date" className="input" value={f.to} onChange={(e) => setF({ ...f, to: e.target.value })} /></div>
      </div>
      {loading && !data && <Loading />}{error && <ErrorBox message={error} onRetry={reload} />}
      {data && <>
        <div className="grid grid-cols-2 lg:grid-cols-6 gap-4">
          <StatCard label="Open cases" value={data.openCases} tone="amber" />
          <StatCard label="Reunited today" value={data.reunitedToday} tone="green" />
          <StatCard label="Missing reports" value={data.totalMissing} />
          <StatCard label="Found reports" value={data.totalFound} />
          <StatCard label="Awaiting confirmation" value={data.awaitingConfirmation} hint={`${data.awaitingHandover} awaiting handover`} tone="red" />
          <StatCard label="Avg. time to reunite" value={data.avgMinutesToReunite != null ? `${data.avgMinutesToReunite} min` : '—'} hint={`${data.staleCases} stale`} />
        </div>
        <div className="grid lg:grid-cols-2 gap-6">
          <div className="card"><h2 className="font-semibold text-navy-900 mb-3">Cases per day</h2>
            <ResponsiveContainer width="100%" height={240}><LineChart data={data.trend}><CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="date" tick={{ fontSize: 11 }} /><YAxis allowDecimals={false} /><Tooltip /><Legend />
              <Line dataKey="opened" name="Opened" stroke="#1f4f86" strokeWidth={2} /><Line dataKey="reunited" name="Reunited" stroke="#10b981" strokeWidth={2} /></LineChart></ResponsiveContainer></div>
          <div className="card"><h2 className="font-semibold text-navy-900 mb-3">Found reports by help desk</h2>
            <ResponsiveContainer width="100%" height={240}><BarChart data={data.byDesk} layout="vertical" margin={{ left: 40 }}><CartesianGrid strokeDasharray="3 3" horizontal={false} /><XAxis type="number" allowDecimals={false} /><YAxis type="category" dataKey="desk" width={150} tick={{ fontSize: 11 }} /><Tooltip />
              <Bar dataKey="foundReports" name="Found reports" fill="#1f4f86" radius={[0, 4, 4, 0]} /></BarChart></ResponsiveContainer></div>
        </div>
        <div className="card text-sm grid grid-cols-2 md:grid-cols-5 gap-2">{Object.entries(data.casesByStatus).map(([k, v]) => <div key={k} className="flex justify-between rounded bg-slate-50 px-3 py-2"><span>{k}</span><b>{v}</b></div>)}</div>
      </>}
      <div className="card"><h2 className="font-semibold text-navy-900 mb-3">Cases awaiting confirmation</h2>{open.data && <CasesTable rows={open.data.content} />}</div>
    </div>
  )
}
