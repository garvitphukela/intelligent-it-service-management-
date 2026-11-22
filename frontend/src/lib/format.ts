import type { IncidentStatus, Priority, SlaStatus } from './types'

export function relativeTime(iso?: string, now = Date.now()) {
  if (!iso) return '—'
  const diff = (now - new Date(iso).getTime()) / 1000
  const abs = Math.abs(diff)
  const fmt = (n: number, u: string) => `${Math.floor(n)}${u}`
  let s: string
  if (abs < 45) s = 'just now'
  else if (abs < 3600) s = fmt(abs / 60, 'm')
  else if (abs < 86400) s = fmt(abs / 3600, 'h')
  else s = fmt(abs / 86400, 'd')
  if (s === 'just now') return s
  return diff >= 0 ? `${s} ago` : `in ${s}`
}

export function dateTime(iso?: string) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

export function time(iso?: string) {
  if (!iso) return '—'
  return new Date(iso).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

export function countdown(ms: number) {
  const neg = ms < 0
  let s = Math.floor(Math.abs(ms) / 1000)
  const h = Math.floor(s / 3600); s -= h * 3600
  const m = Math.floor(s / 60); s -= m * 60
  const body = h > 0 ? `${h}h ${String(m).padStart(2, '0')}m` : `${m}:${String(s).padStart(2, '0')}`
  return neg ? `-${body}` : body
}

export function minutes(m?: number | null) {
  if (m === undefined || m === null) return '—'
  if (m < 60) return `${m.toFixed(m < 10 ? 1 : 0)}m`
  if (m < 1440) return `${(m / 60).toFixed(1)}h`
  return `${(m / 1440).toFixed(1)}d`
}

export const statusLabel = (s: IncidentStatus | string) => s.replace(/_/g, ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase())

export const priorityLabel: Record<Priority, string> = { P1: 'Critical', P2: 'High', P3: 'Medium', P4: 'Low' }

export const slaLabel: Record<SlaStatus, string> = {
  ON_TRACK: 'On track', AT_RISK: 'At risk', BREACHED: 'Breached', MET: 'Met', PAUSED: 'Paused', NOT_APPLICABLE: 'N/A',
}

export const titleCase = (s: string) => s.toLowerCase().replace(/(^|[\s_])\w/g, (c) => c.toUpperCase()).replace(/_/g, ' ')
