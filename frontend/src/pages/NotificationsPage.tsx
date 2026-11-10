import { api } from '../api/client'
import { useApi } from '../api/useApi'
import type { Notification, OutboxMessage, Page } from '../api/types'
import { Empty, ErrorBox, Loading, PageHeader, fmtDate } from '../components/ui'

export default function NotificationsPage() {
  const { data, loading, error, reload } = useApi<Page<Notification>>('/notifications', { size: 50 })
  const inbox = useApi<OutboxMessage[]>(import.meta.env.DEV ? '/dev/outbox' : null)
  return (
    <div className="space-y-6">
      <PageHeader title="Notifications" actions={<button className="btn-secondary" onClick={() => { void reload(); void inbox.reload() }}>Refresh</button>} />
      <div className="card">
        {loading && <Loading />}{error && <ErrorBox message={error} onRetry={reload} />}
        {data?.content.length === 0 && <Empty text="No notifications yet." />}
        <ul className="divide-y divide-slate-100">{data?.content.map((n) => (
          <li key={n.id} className="py-3 flex justify-between gap-4">
            <div><div className={n.read ? 'text-slate-500' : 'font-medium'}>{n.message}</div><div className="text-xs text-slate-400">{n.type} · {fmtDate(n.createdAt)}{n.caseId ? ` · case #${n.caseId}` : ''}</div></div>
            {!n.read && <button className="btn-secondary text-xs" onClick={async () => { await api.post(`/notifications/${n.id}/read`); void reload() }}>Mark read</button>}
          </li>))}</ul>
      </div>
      {import.meta.env.DEV && (
        <div className="card border-amber-300">
          <h2 className="font-semibold text-amber-900">Simulated e-mail inbox (development only)</h2>
          <p className="text-xs text-amber-800 mb-2">In production these messages go to your e-mail. This panel and its API exist only in the dev profile.</p>
          {inbox.error && <p className="text-xs text-slate-500">Not available ({inbox.error}).</p>}
          {inbox.data?.length === 0 && <Empty text="No simulated messages." />}
          <ul className="divide-y divide-amber-100 text-sm">{inbox.data?.map((m, i) => <li key={i} className="py-2"><b>{m.subject}</b><div>{m.body}</div><div className="text-xs text-slate-400">{fmtDate(m.sentAt)}</div></li>)}</ul>
        </div>
      )}
    </div>
  )
}
