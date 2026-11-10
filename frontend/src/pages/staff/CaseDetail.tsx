import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { api, apiError, errorMessage } from '../../api/client'
import { useApi } from '../../api/useApi'
import type { CaseItem, Desk, FoundReport, MatchSuggestion, MissingReport, TimelineEntry } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { CaseBadge, ConfirmButton, Empty, ErrorBox, Loading, PageHeader, SuccessBox, fmtDate, swatch } from '../../components/ui'

function Swatch({ c }: { c: string }) {
  return <span className="inline-flex items-center gap-1"><span className="h-3 w-3 rounded-full border" style={{ background: swatch[c] }} />{c}</span>
}

function Side({ title, m, f }: { title: string; m?: MissingReport; f?: FoundReport }) {
  if (!m && !f) return <div className="card"><h3 className="font-semibold text-navy-900">{title}</h3><Empty text="Not yet linked." /></div>
  return (
    <div className="card space-y-1 text-sm">
      <h3 className="font-semibold text-navy-900 mb-2">{title}</h3>
      {m && <><div><b>{m.personName}</b>, ~{m.approxAge} yrs, {m.gender.toLowerCase()}</div><div>Clothing: <Swatch c={m.clothingColor} /></div>
        <div>Last seen {fmtDate(m.lastSeenAt)} at {Number(m.lastSeenLat).toFixed(4)}, {Number(m.lastSeenLng).toFixed(4)}</div></>}
      {f && <><div>~{f.approxAge} yrs, {f.gender.toLowerCase()} {f.registeredMember && <span className="rounded bg-emerald-50 text-emerald-800 px-1.5 text-xs">wristband</span>}</div>
        <div>Clothing: <Swatch c={f.clothingColor} /></div><div>Found {fmtDate(f.foundAt)} · desk: {f.deskName}</div>
        {f.foundLat != null && <div>Location {Number(f.foundLat).toFixed(4)}, {Number(f.foundLng).toFixed(4)}</div>}
        {f.safeNotes && <div className="text-slate-600">Notes: {f.safeNotes}</div>}</>}
    </div>
  )
}

function Suggestions({ c, onDone }: { c: CaseItem; onDone: () => void }) {
  const { data, loading, error } = useApi<MatchSuggestion[]>(`/reports/missing/${c.missing!.id}/matches`)
  const [err, setErr] = useState<string | null>(null)
  if (loading) return <Loading text="Scoring found reports…" />
  if (error) return <ErrorBox message={error} />
  return (
    <div className="card space-y-3">
      <h2 className="font-semibold text-navy-900">Match suggestions (score ≥ 50)</h2>
      <p className="text-xs text-slate-500">Suggestions only. Compare details and talk to the family before confirming. Handover still needs the guardian's OTP.</p>
      {err && <ErrorBox message={err} />}
      {data?.length === 0 && <Empty text="No found report scores 50 or more yet." />}
      {data?.map((s) => (
        <div key={s.foundReportId} className="rounded-lg border border-slate-200 p-3 grid md:grid-cols-3 gap-3 items-start">
          <div><div className="text-3xl font-bold text-navy-800">{s.score}</div><div className="text-xs text-slate-500">{s.distanceKm} km · {s.minutesApart} min after last seen</div></div>
          <ul className="text-xs space-y-0.5">{s.factors.map((f) => <li key={f.factor} className={f.points ? 'text-slate-800' : 'text-slate-400'}><b>{f.factor}</b> +{f.points} — {f.detail}</li>)}</ul>
          <div className="space-y-2 text-sm">
            <Side title={`Found report #${s.foundReportId}`} f={s.found} />
            <ConfirmButton label="Confirm this match" className="btn-success w-full" title="Confirm match?"
              message="Only confirm after checking with the family. The guardian will receive a one-time code for the handover."
              onConfirm={async () => { try { await api.post(`/cases/${c.id}/confirm-match`, { foundReportId: s.foundReportId }); onDone() } catch (e) { setErr(errorMessage(e)) } }} />
          </div>
        </div>
      ))}
    </div>
  )
}

export default function CaseDetail() {
  const { id } = useParams()
  const { user } = useAuth()
  const { data: c, loading, error, reload } = useApi<CaseItem>(`/cases/${id}`)
  const timeline = useApi<TimelineEntry[]>(`/cases/${id}/timeline`)
  const desks = useApi<Desk[]>(c && (user?.role === 'POLICE' || user?.role === 'ADMIN') ? `/events/${c.eventId}/desks` : null)
  const [otp, setOtp] = useState('')
  const [note, setNote] = useState('')
  const [msg, setMsg] = useState<string | null>(null)
  const [err, setErr] = useState<string | null>(null)
  const refresh = () => { void reload(); void timeline.reload() }
  const act = async (path: string, body?: object, ok?: string) => {
    setErr(null); setMsg(null)
    try { await api.post(`/cases/${id}/${path}`, body); if (ok) setMsg(ok); refresh() }
    catch (e) { const b = apiError(e); setErr(errorMessage(e) + (b?.details?.attemptsLeft != null && b.code === 'OTP_INVALID' ? ` (${b.details.attemptsLeft} attempts left)` : '')) }
  }

  if (loading && !c) return <Loading />
  if (error) return <ErrorBox message={error} onRetry={reload} />
  if (!c) return null
  const desk = user?.role === 'DESK'
  const police = user?.role === 'POLICE' || user?.role === 'ADMIN'
  const family = user?.role === 'FAMILY'

  return (
    <div className="space-y-6">
      <PageHeader title={`Case #${c.id}${c.personName ? ` — ${c.personName}` : ''}`}
        subtitle={`Opened ${fmtDate(c.openedAt)} · desk: ${c.assignedDeskName ?? 'not assigned'}${c.matchScore != null ? ` · match score ${c.matchScore}` : ''}`}
        actions={<CaseBadge status={c.status} />} />
      {msg && <SuccessBox message={msg} />}{err && <ErrorBox message={err} />}
      {family && c.status !== 'REUNITED' && c.status !== 'CLOSED' && (
        <div className="card bg-blue-50 border-blue-100 text-sm text-blue-900">
          {c.status === 'REPORTED' && 'Your report is active. Help desks are checking found persons. Please also visit the nearest help desk.'}
          {c.status === 'MATCHED' && `A match was confirmed. Go to ${c.assignedDeskName ?? 'the help desk'} with the one-time code we sent you.`}
          {c.status === 'AT_DESK' && 'Your family member is at the desk. Tell the desk staff your one-time code.'}
        </div>
      )}
      <div className="grid md:grid-cols-2 gap-4">
        <Side title="Missing report" m={c.missing} />
        <Side title="Found report" f={c.found} />
      </div>

      {desk && c.status === 'REPORTED' && c.missing && !c.found && <Suggestions c={c} onDone={() => { setMsg('Match confirmed. OTP sent to the guardian.'); refresh() }} />}
      {desk && c.status === 'REPORTED' && c.found && c.registeredMember && (
        <div className="card flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm">Wristband case: the registered family is known. Confirm identity to notify the guardian and send the OTP.</p>
          <ConfirmButton label="Confirm identity" className="btn-success" title="Confirm identity?" message="The guardian receives a one-time handover code." onConfirm={() => act('confirm-match', {}, 'Confirmed. OTP sent to the guardian.')} />
        </div>
      )}
      {desk && c.status === 'MATCHED' && (
        <div className="card flex flex-wrap gap-2 items-center">
          <span className="text-sm flex-1">When the person and guardian are both at the desk, mark arrival, then verify the OTP.</span>
          <button className="btn-primary" onClick={() => act('arrived', undefined, 'Marked as at the desk.')}>Mark at desk</button>
          <button className="btn-secondary" onClick={() => act('otp', undefined, 'New OTP sent to the guardian.')}>Re-send OTP</button>
          <ConfirmButton label="Reject match" className="btn-danger" title="Reject this match?" message="The case returns to REPORTED and the pending OTP is cancelled." onConfirm={() => act('reject-match', { note: 'Rejected at desk' }, 'Match rejected.')} />
        </div>
      )}
      {desk && c.status === 'AT_DESK' && (
        <div className="card space-y-3 max-w-lg">
          <h2 className="font-semibold text-navy-900">Secure handover</h2>
          <p className="text-sm text-slate-600">Ask the guardian for the 6-digit code they received. Never read the code out yourself. After 5 wrong attempts the code locks.</p>
          <div className="flex gap-2">
            <input aria-label="One-time code" inputMode="numeric" maxLength={6} pattern="\d{6}" className="input text-2xl tracking-[0.5em] font-mono w-48" value={otp} onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))} />
            <button className="btn-success" disabled={otp.length !== 6} onClick={() => { void act('handover', { otp }, 'Reunited! Handover recorded.'); setOtp('') }}>Verify &amp; hand over</button>
          </div>
          <button className="btn-secondary" onClick={() => act('otp', undefined, 'New OTP sent to the guardian.')}>Issue new OTP</button>
        </div>
      )}
      {police && c.status !== 'CLOSED' && (
        <div className="card grid md:grid-cols-2 gap-4">
          <div className="space-y-2"><h3 className="font-semibold text-navy-900">Assign desk</h3>
            <select aria-label="Assign desk" className="input" defaultValue="" onChange={(e) => e.target.value && act('assign', { deskId: Number(e.target.value) }, 'Assigned.')}>
              <option value="">Choose desk…</option>{desks.data?.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}</select></div>
          <div className="space-y-2"><h3 className="font-semibold text-navy-900">Close case</h3>
            <input aria-label="Closing note" className="input" placeholder="Reason (required)" value={note} onChange={(e) => setNote(e.target.value)} />
            <ConfirmButton label="Close case" className="btn-danger" disabled={!note.trim()} title="Close this case?" message="Only close when the person is safe or the report was a mistake. This cannot be undone." onConfirm={() => act('close', { note }, 'Case closed.')} /></div>
        </div>
      )}
      {family && c.status === 'REPORTED' && c.missing && (
        <div className="card flex flex-wrap gap-2 items-center">
          <input aria-label="Reason" className="input flex-1" placeholder="e.g. We found her ourselves" value={note} onChange={(e) => setNote(e.target.value)} />
          <ConfirmButton label="Withdraw report" className="btn-secondary" disabled={!note.trim()} title="Withdraw missing report?" message="Only do this if the person is safely with you." onConfirm={() => act('close', { note }, 'Report withdrawn.')} />
        </div>
      )}

      <div className="card">
        <h2 className="font-semibold text-navy-900 mb-3">Timeline</h2>
        {timeline.data?.length === 0 && <Empty text="No events." />}
        <ol className="relative border-l border-slate-200 ml-2 space-y-4">
          {timeline.data?.map((t, i) => (
            <li key={i} className="ml-4"><span className="absolute -left-1.5 mt-1.5 h-3 w-3 rounded-full bg-navy-700" />
              <div className="text-sm"><CaseBadge status={t.status} /> <span className="text-slate-500 text-xs ml-2">{fmtDate(t.changedAt)} · {t.changedBy}{t.caseId !== c.id ? ` · (case #${t.caseId})` : ''}</span></div>
              {t.note && <div className="text-sm text-slate-700 mt-0.5">{t.note}</div>}</li>
          ))}
        </ol>
      </div>
    </div>
  )
}
