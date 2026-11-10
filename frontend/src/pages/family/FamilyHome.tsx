import { Link } from 'react-router-dom'
import { useApi } from '../../api/useApi'
import type { CaseItem, Member, Notification, Page } from '../../api/types'
import { CaseBadge, Empty, ErrorBox, Loading, PageHeader, fmtDate } from '../../components/ui'

export default function FamilyHome() {
  const members = useApi<Member[]>('/members')
  const cases = useApi<Page<CaseItem>>('/cases', { size: 5 })
  const notes = useApi<Page<Notification>>('/notifications', { size: 5 })
  return (
    <div className="space-y-6">
      <PageHeader title="Family dashboard" subtitle="Register your children and elderly relatives before the festival, and print their wristbands."
        actions={<><Link to="/family/members" className="btn-secondary">+ Add member</Link><Link to="/family/report-missing" className="btn-danger">Report missing</Link></>} />
      <div className="grid lg:grid-cols-3 gap-6">
        <div className="card space-y-3">
          <h2 className="font-semibold text-navy-900">My members</h2>
          {members.loading && <Loading />}{members.error && <ErrorBox message={members.error} />}
          {members.data?.length === 0 && <Empty text="No members registered yet." />}
          <ul className="divide-y divide-slate-100">{members.data?.map((m) => (
            <li key={m.id} className="py-2 flex justify-between"><span>{m.fullName} <span className="text-xs text-slate-500">({m.age}, {m.gender.toLowerCase()})</span></span>
              <Link to="/family/members" className="text-xs text-navy-700 hover:underline">QR</Link></li>))}</ul>
        </div>
        <div className="card space-y-3">
          <h2 className="font-semibold text-navy-900">My cases</h2>
          {cases.data?.content.length === 0 && <Empty text="No cases. We hope it stays that way!" />}
          <ul className="divide-y divide-slate-100">{cases.data?.content.map((c) => (
            <li key={c.id} className="py-2 flex justify-between items-center"><Link to={`/cases/${c.id}`} className="text-navy-700 hover:underline">#{c.id} {c.personName}</Link><CaseBadge status={c.status} /></li>))}</ul>
        </div>
        <div className="card space-y-3">
          <h2 className="font-semibold text-navy-900">Latest notifications</h2>
          {notes.data?.content.length === 0 && <Empty text="No notifications." />}
          <ul className="space-y-2 text-sm">{notes.data?.content.map((n) => (
            <li key={n.id} className={n.read ? 'text-slate-500' : 'text-slate-800 font-medium'}>{n.message}<div className="text-xs text-slate-400">{fmtDate(n.createdAt)}</div></li>))}</ul>
          <Link to="/notifications" className="text-xs text-navy-700 hover:underline">All notifications →</Link>
        </div>
      </div>
    </div>
  )
}
