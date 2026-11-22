export class ApiError extends Error {
  status: number
  code: string
  fieldErrors?: Record<string, string>
  constructor(status: number, code: string, message: string, fieldErrors?: Record<string, string>) {
    super(message)
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

const TOKEN_KEY = 'itsm-token'

export const tokenStore = {
  get(): string | null {
    try { return localStorage.getItem(TOKEN_KEY) } catch { return null }
  },
  set(t: string | null) {
    try { t ? localStorage.setItem(TOKEN_KEY, t) : localStorage.removeItem(TOKEN_KEY) } catch { /* storage unavailable */ }
  },
}

let onUnauthorized: () => void = () => {}
export function setUnauthorizedHandler(fn: () => void) { onUnauthorized = fn }

export async function api<T>(path: string, init: { method?: string; body?: unknown; params?: Record<string, unknown> } = {}): Promise<T> {
  let url = '/api' + path
  if (init.params) {
    const qs = new URLSearchParams()
    for (const [k, v] of Object.entries(init.params)) {
      if (v === undefined || v === null || v === '') continue
      if (Array.isArray(v)) v.forEach((x) => qs.append(k, String(x)))
      else qs.append(k, String(v))
    }
    const s = qs.toString()
    if (s) url += '?' + s
  }
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (init.body !== undefined) headers['Content-Type'] = 'application/json'
  const token = tokenStore.get()
  if (token) headers.Authorization = 'Bearer ' + token
  let res: Response
  try {
    res = await fetch(url, { method: init.method ?? (init.body !== undefined ? 'POST' : 'GET'), headers, body: init.body !== undefined ? JSON.stringify(init.body) : undefined })
  } catch {
    throw new ApiError(0, 'NETWORK_FAILURE', 'Cannot reach the server. Check your connection and try again.')
  }
  const text = await res.text()
  const data = text ? safeJson(text) : null
  if (!res.ok) {
    if (res.status === 401 && token) onUnauthorized()
    throw new ApiError(res.status, data?.code ?? 'ERROR', data?.message ?? res.statusText, data?.fieldErrors)
  }
  return data as T
}

function safeJson(t: string) {
  try { return JSON.parse(t) } catch { return null }
}

export const get = <T,>(path: string, params?: Record<string, unknown>) => api<T>(path, { params })
export const post = <T,>(path: string, body?: unknown) => api<T>(path, { method: 'POST', body: body ?? {} })
export const put = <T,>(path: string, body?: unknown) => api<T>(path, { method: 'PUT', body })
export const patch = <T,>(path: string, body?: unknown) => api<T>(path, { method: 'PATCH', body })
export const del = <T,>(path: string) => api<T>(path, { method: 'DELETE' })
