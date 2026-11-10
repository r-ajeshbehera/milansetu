import { useCallback, useEffect, useState } from 'react'
import { api, errorMessage } from './client'

/** Small data-fetching hook: { data, loading, error, reload }. */
export function useApi<T>(url: string | null, params?: Record<string, unknown>) {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(Boolean(url))
  const [error, setError] = useState<string | null>(null)
  const key = JSON.stringify(params ?? {})

  const load = useCallback(async () => {
    if (!url) return
    setLoading(true)
    setError(null)
    try {
      const res = await api.get<T>(url, { params: JSON.parse(key) })
      setData(res.data)
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }, [url, key])

  useEffect(() => { void load() }, [load])
  return { data, loading, error, reload: load }
}
