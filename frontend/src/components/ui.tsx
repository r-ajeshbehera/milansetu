import { useState, type ReactNode } from 'react'
import type { CaseStatus } from '../api/types'


export function Loading({ text = 'Loading…' }: { text?: string }) {
  return <div className="flex items-center gap-3 text-slate-500 py-8 justify-center"><span className="h-4 w-4 animate-spin rounded-full border-2 border-navy-700 border-t-transparent" />{text}</div>
}

export function ErrorBox({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800 flex items-center justify-between gap-4" role="alert">
      <span>{message}</span>
      {onRetry && <button className="btn-secondary" onClick={onRetry}>Retry</button>}
    </div>
  )
}

export function SuccessBox({ message }: { message: string }) {
  return <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-800" role="status">{message}</div>
}

export function Empty({ text }: { text: string }) {
  return <div className="text-center text-slate-500 py-10 text-sm">{text}</div>
}

export function StatCard({ label, value, hint, tone = 'navy' }: { label: string; value: ReactNode; hint?: string; tone?: 'navy' | 'red' | 'green' | 'amber' }) {
  const tones = { navy: 'text-navy-800', red: 'text-red-600', green: 'text-emerald-600', amber: 'text-amber-600' }
  return (
    <div className="card">
      <div className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</div>
      <div className={`mt-1 text-3xl font-bold ${tones[tone]}`}>{value}</div>
      {hint && <div className="mt-1 text-xs text-slate-500">{hint}</div>}
    </div>
  )
}

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-4 mb-6">
      <div>
        <h1 className="text-2xl font-bold text-navy-900">{title}</h1>
        {subtitle && <p className="text-sm text-slate-500 mt-1">{subtitle}</p>}
      </div>
      {actions && <div className="flex gap-2">{actions}</div>}
    </div>
  )
}

export function DemoBanner() {
  return (
    <div className="bg-amber-50 border-b border-amber-200 text-amber-900 text-xs text-center py-1.5 px-4">
      Portfolio project — <strong>demonstration data</strong> only. Not an officially approved emergency or child-safety system.
    </div>
  )
}

/** Accessible confirmation dialog used before irreversible actions. */
export function ConfirmButton({ label, title, message, onConfirm, className = 'btn-primary', disabled }: {
  label: string; title: string; message: string; onConfirm: () => Promise<void> | void; className?: string; disabled?: boolean
}) {
  const [open, setOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  return (
    <>
      <button className={className} disabled={disabled} onClick={() => setOpen(true)}>{label}</button>
      {open && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
          <div className="card max-w-md w-full">
            <h2 id="confirm-title" className="text-lg font-semibold text-navy-900">{title}</h2>
            <p className="text-sm text-slate-600 mt-2">{message}</p>
            <div className="mt-5 flex justify-end gap-2">
              <button className="btn-secondary" onClick={() => setOpen(false)} disabled={busy}>Cancel</button>
              <button className={className} disabled={busy} onClick={async () => {
                setBusy(true)
                try { await onConfirm() } finally { setBusy(false); setOpen(false) }
              }}>{busy ? 'Working…' : 'Confirm'}</button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}

export const fmtDate = (s: string | null | undefined) => (s ? new Date(s).toLocaleString() : '—')

const caseStyle: Record<CaseStatus, string> = {
  REPORTED: 'bg-blue-100 text-blue-800', MATCHED: 'bg-amber-100 text-amber-800', AT_DESK: 'bg-purple-100 text-purple-800',
  REUNITED: 'bg-emerald-100 text-emerald-800', CLOSED: 'bg-slate-200 text-slate-700',
}
export function CaseBadge({ status }: { status: CaseStatus }) {
  return <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${caseStyle[status]}`}>{status.replace('_', ' ')}</span>
}
export const CLOTHING = ['RED', 'ORANGE', 'YELLOW', 'GREEN', 'BLUE', 'PURPLE', 'PINK', 'WHITE', 'BLACK', 'GREY', 'BROWN', 'MULTI'] as const
export const swatch: Record<string, string> = {
  RED: '#dc2626', ORANGE: '#ea580c', YELLOW: '#eab308', GREEN: '#16a34a', BLUE: '#2563eb', PURPLE: '#9333ea', PINK: '#ec4899',
  WHITE: '#f8fafc', BLACK: '#0f172a', GREY: '#94a3b8', BROWN: '#92400e', MULTI: 'linear-gradient(90deg,#dc2626,#eab308,#2563eb)',
}
export function ColourPicker({ value, onChange }: { value: string; onChange: (c: string) => void }) {
  return (
    <div className="flex flex-wrap gap-2" role="radiogroup" aria-label="Clothing colour">
      {CLOTHING.map((c) => (
        <button type="button" key={c} role="radio" aria-checked={value === c} title={c} onClick={() => onChange(c)}
          className={`h-8 w-8 rounded-full border-2 ${value === c ? 'border-navy-900 ring-2 ring-navy-700/40' : 'border-slate-300'}`}
          style={{ background: swatch[c] }}><span className="sr-only">{c}</span></button>
      ))}
      <span className="text-xs text-slate-500 self-center">{value || 'choose'}</span>
    </div>
  )
}
