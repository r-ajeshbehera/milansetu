import { useState } from 'react'
import { useApi } from '../../api/useApi'
import type { CaseItem, CaseStatus, MEvent, Page } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { ErrorBox, Loading, PageHeader } from '../../components/ui'
import { CasesTable } from './DeskHome'

export default function CasesPage() {
  const { user } = useAuth()
  const staff = user?.role !== 'FAMILY'
  const events = useApi<MEvent[]>(staff ? '/events' : null)
  const [f, setF] = useState({ eventId: '', status: '' as CaseStatus | '', from: '', to: '' })
  const [page, setPage] = useState(0)
  const params = staff ? { ...Object.fromEntries(Object.entries(f).filter(([, v]) => v)), page, size: 25 } : { page, size: 25 }
  const { data, loading, error, reload } = useApi<Page<CaseItem>>('/cases', params)
  return (
    <div className="space-y-4">
      <PageHeader title={staff ? 'Event cases' : 'My cases'} />
      {staff && (
        <div className="card flex flex-wrap gap-3 items-end">
          <div><label className="label" htmlFor="ce">Event</label><select id="ce" className="input" value={f.eventId} onChange={(e) => setF({ ...f, eventId: e.target.value })}><option value="">All my events</option>{events.data?.map((ev) => <option key={ev.id} value={ev.id}>{ev.name}</option>)}</select></div>
          <div><label className="label" htmlFor="cs">Status</label><select id="cs" className="input" value={f.status} onChange={(e) => setF({ ...f, status: e.target.value as CaseStatus | '' })}><option value="">All</option>{(['REPORTED', 'MATCHED', 'AT_DESK', 'REUNITED', 'CLOSED'] as const).map((s) => <option key={s}>{s}</option>)}</select></div>
          <div><label className="label" htmlFor="cf">Opened from</label><input id="cf" type="date" className="input" value={f.from} onChange={(e) => setF({ ...f, from: e.target.value })} /></div>
          <div><label className="label" htmlFor="ct">to</label><input id="ct" type="date" className="input" value={f.to} onChange={(e) => setF({ ...f, to: e.target.value })} /></div>
        </div>
      )}
      <div className="card space-y-3">
        {loading && !data && <Loading />}{error && <ErrorBox message={error} onRetry={reload} />}
        {data && <CasesTable rows={data.content} />}
        {data && data.totalPages > 1 && <div className="flex justify-end gap-2 text-sm"><button className="btn-secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button><button className="btn-secondary" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Next</button></div>}
      </div>
    </div>
  )
}
