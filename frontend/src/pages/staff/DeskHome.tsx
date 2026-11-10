import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useApi } from '../../api/useApi'
import type { CaseItem, CaseStatus, Page } from '../../api/types'
import { CaseBadge, Empty, ErrorBox, Loading, PageHeader, fmtDate } from '../../components/ui'

export function CasesTable({ rows }: { rows: CaseItem[] }) {
  if (rows.length === 0) return <Empty text="No cases match." />
  return (
    <div className="overflow-x-auto"><table className="table">
      <thead><tr><th>Case</th><th>Person</th><th>Type</th><th>Desk</th><th>Status</th><th>Opened</th></tr></thead>
      <tbody>{rows.map((c) => (
        <tr key={c.id} className={c.stale ? 'bg-amber-50' : ''}>
          <td><Link to={`/cases/${c.id}`} className="font-medium text-navy-700 hover:underline">#{c.id}</Link>{c.stale && <span className="ml-1 text-xs text-amber-700">stale</span>}</td>
          <td>{c.personName ?? (c.found ? `Found ${c.found.gender.toLowerCase()}, ~${c.found.approxAge}` : '—')}</td>
          <td className="text-xs">{c.missing && c.found ? 'Missing + found' : c.missing ? 'Missing report' : c.registeredMember ? 'Wristband scan' : 'Found (no wristband)'}</td>
          <td>{c.assignedDeskName ?? '—'}</td><td><CaseBadge status={c.status} /></td><td className="text-xs">{fmtDate(c.openedAt)}</td>
        </tr>))}</tbody></table></div>
  )
}

export default function DeskHome() {
  const [status, setStatus] = useState<CaseStatus | ''>('')
  const { data, loading, error, reload } = useApi<Page<CaseItem>>('/cases', { status: status || undefined, size: 50 })
  return (
    <div className="space-y-4">
      <PageHeader title="Help desk cases" subtitle="Open a missing-person case to see match suggestions."
        actions={<><select aria-label="Status" className="input w-44" value={status} onChange={(e) => setStatus(e.target.value as CaseStatus | '')}>
          <option value="">All statuses</option>{(['REPORTED', 'MATCHED', 'AT_DESK', 'REUNITED', 'CLOSED'] as const).map((s) => <option key={s}>{s}</option>)}</select>
          <button className="btn-secondary" onClick={reload}>Refresh</button></>} />
      <div className="card">{loading && !data && <Loading />}{error && <ErrorBox message={error} onRetry={reload} />}{data && <CasesTable rows={data.content} />}</div>
    </div>
  )
}
