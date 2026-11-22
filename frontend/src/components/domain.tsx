import { useEffect, useState } from 'react'
import { Badge } from '@/components/ui/badge'
import { countdown, priorityLabel, slaLabel, statusLabel } from '@/lib/format'
import type { IncidentStatus, Priority, SlaStatus } from '@/lib/types'
import { cn } from '@/lib/utils'

export function useNow(intervalMs = 1000) {
  const [now, setNow] = useState(Date.now())
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), intervalMs)
    return () => clearInterval(id)
  }, [intervalMs])
  return now
}

export function PriorityBadge({ priority, showLabel }: { priority?: Priority; showLabel?: boolean }) {
  if (!priority) return <Badge tone="outline">—</Badge>
  const tone = priority === 'P1' ? 'red' : priority === 'P2' ? 'amber' : priority === 'P3' ? 'neutral' : 'outline'
  return (
    <Badge tone={tone} className="font-mono">
      {priority}{showLabel && <span className="font-sans font-normal">· {priorityLabel[priority]}</span>}
    </Badge>
  )
}

const statusTone: Record<IncidentStatus, 'neutral' | 'blue' | 'amber' | 'green' | 'outline' | 'red'> = {
  NEW: 'outline', CLASSIFIED: 'outline', ASSIGNED: 'neutral', ACKNOWLEDGED: 'blue', IN_PROGRESS: 'blue',
  AWAITING_INFO: 'amber', RESOLVED: 'green', VERIFIED: 'green', CLOSED: 'outline', CANCELLED: 'outline',
}

export function StatusBadge({ status }: { status: IncidentStatus }) {
  return <Badge tone={statusTone[status]} dot>{statusLabel(status)}</Badge>
}

export function SlaBadge({ status }: { status: SlaStatus }) {
  const tone = status === 'BREACHED' ? 'red' : status === 'AT_RISK' ? 'amber' : status === 'MET' ? 'green' : status === 'ON_TRACK' ? 'neutral' : 'outline'
  return <Badge tone={tone}>{slaLabel[status]}</Badge>
}

/** Live countdown to the relevant SLA deadline (response before acknowledgement, otherwise resolution). */
export function SlaCountdown({ responseDueAt, resolutionDueAt, status, incidentStatus, compact }: {
  responseDueAt?: string; resolutionDueAt?: string; status: SlaStatus; incidentStatus: IncidentStatus; compact?: boolean
}) {
  const now = useNow()
  if (status === 'MET' || status === 'NOT_APPLICABLE' || ['RESOLVED', 'VERIFIED', 'CLOSED', 'CANCELLED'].includes(incidentStatus)) {
    return <SlaBadge status={status} />
  }
  if (status === 'PAUSED') return <Badge tone="amber">Paused</Badge>
  const responding = ['NEW', 'CLASSIFIED', 'ASSIGNED'].includes(incidentStatus)
  const due = responding ? responseDueAt : resolutionDueAt
  if (!due) return <SlaBadge status={status} />
  const ms = new Date(due).getTime() - now
  const color = ms < 0 || status === 'BREACHED' ? 'text-red' : status === 'AT_RISK' ? 'text-amber' : 'text-muted'
  return (
    <span className={cn('inline-flex items-center gap-1.5 font-mono text-xs tabular-nums', color)} title={(responding ? 'Response' : 'Resolution') + ' due ' + new Date(due).toLocaleString()}>
      {!compact && <span className="font-sans text-[11px] text-subtle">{responding ? 'resp' : 'res'}</span>}
      {countdown(ms)}
    </span>
  )
}

export function CriticalityDots({ value }: { value: number }) {
  return (
    <span className="inline-flex gap-0.5" title={`Business criticality ${value}/5`}>
      {[1, 2, 3, 4, 5].map((i) => <span key={i} className={cn('h-2.5 w-1 rounded-sm', i <= value ? 'bg-foreground' : 'bg-border-strong')} />)}
    </span>
  )
}

export function LiveDot({ connected }: { connected: boolean }) {
  return (
    <span className="inline-flex items-center gap-1.5 text-[11px] text-muted">
      <span className={cn('size-1.5 rounded-full', connected ? 'bg-green animate-pulse-dot' : 'bg-subtle')} />
      {connected ? 'Live' : 'Offline'}
    </span>
  )
}

export function PageHeader({ title, description, actions }: { title: string; description?: React.ReactNode; actions?: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-3 border-b border-border pb-5 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
        {description && <p className="mt-1 text-sm text-muted">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  )
}
