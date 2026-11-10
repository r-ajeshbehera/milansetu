import axios, { AxiosError } from 'axios'
import type { ApiError } from './types'

const TOKEN_KEY = 'milansetu.token'

export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL ?? '/api' })

export const tokenStore = {
  get: () => { try { return sessionStorage.getItem(TOKEN_KEY) } catch { return null } },
  set: (t: string) => { try { sessionStorage.setItem(TOKEN_KEY, t) } catch { /* storage unavailable */ } },
  clear: () => { try { sessionStorage.removeItem(TOKEN_KEY) } catch { /* storage unavailable */ } },
}

api.interceptors.request.use((config) => {
  const token = tokenStore.get()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

let onUnauthorized: () => void = () => {}
export const setUnauthorizedHandler = (fn: () => void) => { onUnauthorized = fn }

api.interceptors.response.use(
  (r) => r,
  (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) onUnauthorized()
    return Promise.reject(error)
  },
)

/** Turns any request error into a readable message, including field validation errors. */
export function errorMessage(err: unknown): string {
  const e = err as AxiosError<ApiError>
  const data = e.response?.data
  if (data?.fieldErrors && Object.keys(data.fieldErrors).length) {
    return Object.entries(data.fieldErrors).map(([f, m]) => `${f}: ${m}`).join(' · ')
  }
  if (data?.message) return data.message
  if (e.response?.status === 429) return 'Too many requests. Please wait a minute.'
  return e.message || 'Something went wrong'
}

export function apiError(err: unknown): ApiError | undefined {
  return (err as AxiosError<ApiError>).response?.data
}
