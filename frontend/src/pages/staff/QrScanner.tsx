import { Html5Qrcode } from 'html5-qrcode'
import { useEffect, useRef, useState } from 'react'

/** Camera scanner (phone back camera when permitted). Calls onScan once per detected code. */
export default function QrScanner({ onScan }: { onScan: (text: string) => void }) {
  const [running, setRunning] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const scanner = useRef<Html5Qrcode | null>(null)
  const last = useRef<string>('')

  useEffect(() => () => { void scanner.current?.stop().catch(() => {}) }, [])

  async function start() {
    setError(null)
    try {
      scanner.current = new Html5Qrcode('qr-reader')
      await scanner.current.start({ facingMode: 'environment' }, { fps: 10, qrbox: 240 }, (text) => {
        if (text !== last.current) { last.current = text; onScan(text) }
      }, () => {})
      setRunning(true)
    } catch {
      setError('Camera not available or permission denied. Use manual entry below.')
    }
  }
  async function stop() {
    await scanner.current?.stop().catch(() => {})
    setRunning(false)
  }
  return (
    <div className="space-y-3">
      <div id="qr-reader" className="w-full max-w-sm aspect-square bg-slate-900 rounded-lg overflow-hidden" />
      {error && <p className="text-sm text-red-700">{error}</p>}
      {running ? <button className="btn-secondary" onClick={stop}>Stop camera</button> : <button className="btn-primary" onClick={start}>Start camera</button>}
    </div>
  )
}
