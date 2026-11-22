import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { toast } from 'sonner'
import { Activity as ActivityIcon, AlertTriangle, Radar, RefreshCw, TrendingUp } from 'lucide-react'
import { get, post } from '@/lib/api'
import { useAuth, isManagement } from '@/lib/auth'
import { useRealtime } from '@/lib/realtime'
import type { AuditView, Correlation, Count, Overview, ServiceStat, SlaCompliance, SlaRisk, TechnicianLoad, TrendPoint } from '@/lib/types'
import { Button } from '@/components/ui/button'
import { Card, CardBody, CardHeader } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { EmptyState, Skeleton, Stat } from '@/components/ui/misc'
import { Table, TD, TH, THead, TR } from '@/components/ui/table'
import { CriticalityDots, LiveDot, PageHeader, PriorityBadge, StatusBadge } from '@/components/domain'
import { minutes, relativeTime, time } from '@/lib/format'
import { cn } from '@/lib/utils'

export function DashboardPage() {
  const { user } = useAuth()
  const { connected, events } = useRealtime()
  const qc = useQueryClient()
  const opts = { refetchInterval: 30000 }
  const overview = useQuery({ queryKey: ['reports', 'overview'], queryFn: () => get<Overview>('/reports/overview'), ...opts })
  const trend = useQuery({ queryKey: ['reports', 'trend'], queryFn: () => get<TrendPoint[]>('/reports/trend', { days: 14 }), ...opts })
  const workload = useQuery({ queryKey: ['reports', 'workload'], queryFn: () => get<TechnicianLoad[]>('/reports/technician-workload'), ...opts })
  const services = useQuery({ queryKey: ['reports', 'services'], queryFn: () => get<ServiceStat[]>('/reports/service-analysis'), ...opts })
  const sla = useQuery({ queryKey: ['reports', 'sla'], queryFn: () => get<SlaCompliance[]>('/reports/sla-compliance'), ...opts })
  const breakdown = useQuery({ queryKey: ['reports', 'breakdown'], queryFn: () => get<Record<string, Count[]>>('/reports/breakdown'), ...opts })
  const correlations = useQuery({ queryKey: ['intel', 'correlations'], queryFn: () => get<Correlation[]>('/intelligence/correlations'), ...opts })
  const risks = useQuery({ queryKey: ['intel', 'risks'], queryFn: () => get<SlaRisk[]>('/intelligence/sla-risks'), ...opts })
  const runMonitor = useMutation({
    mutationFn: () => post<{ checked: number; breaches: number; warnings: number; escalations: number; majorIncidents: number }>('/admin/sla/run-monitor'),
    onSuccess: (r) => { toast.success('SLA monitor ran', { description: `${r.checked} checked · ${r.breaches} breaches · ${r.warnings} warnings · ${r.escalations} escalations · ${r.majorIncidents} major` }); qc.invalidateQueries() },
  })
  const recent = useQuery({ queryKey: ['reports', 'recent-audit'], queryFn: () => get<{ content: AuditView[] }>('/audit', { size: 15 }), ...opts })
  const o = overview.data
  // Live WebSocket events first, then recent history from the audit log (deduplicated)
  const feed = [
    ...events.map((e) => ({ key: e.at + e.type + e.incidentId, at: e.at, incidentId: e.incidentId, number: e.incidentNumber, type: e.type, description: e.description, live: true })),
    ...(recent.data?.content ?? []).map((a) => ({ key: 'a' + a.id, at: a.timestamp, incidentId: a.incidentId ?? 0, number: a.incidentNumber ?? '', type: a.action, description: a.description, live: false })),
  ].filter((x, i, arr) => arr.findIndex((y) => y.type === x.type && y.number === x.number && Math.abs(new Date(y.at).getTime() - new Date(x.at).getTime()) < 2000) === i).slice(0, 40)

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Operations" description={<span className="flex items-center gap-3">Service health across the organisation <LiveDot connected={connected} /></span>}
        actions={isManagement(user?.role) && <Button variant="secondary" onClick={() => runMonitor.mutate()} loading={runMonitor.isPending}><RefreshCw />Run SLA check</Button>} />

      <Card className="grid grid-cols-2 divide-border sm:grid-cols-4 xl:grid-cols-8 [&>*]:border-border [&>*:not(:last-child)]:border-r max-sm:[&>*:nth-child(2n)]:border-r-0 sm:max-xl:[&>*:nth-child(4n)]:border-r-0 max-xl:[&>*:nth-child(-n+4)]:border-b max-sm:[&>*:nth-child(-n+6)]:border-b">
        {o ? <>
          <Stat label="Total incidents" value={o.total} hint={`${o.createdToday} today`} />
          <Stat label="Open" value={o.open} hint={`${o.unassigned} unassigned`} />
          <Stat label="Critical open" value={o.critical} tone={o.critical ? 'red' : undefined} hint="P1" />
          <Stat label="Resolved" value={o.resolved} />
          <Stat label="SLA violations" value={o.slaViolations} tone={o.slaViolations ? 'red' : undefined} />
          <Stat label="SLA compliance" value={o.slaCompliancePercent != null ? `${o.slaCompliancePercent}%` : '—'} tone={(o.slaCompliancePercent ?? 100) >= 90 ? 'green' : 'amber'} />
          <Stat label="Avg response" value={minutes(o.avgResponseMinutes)} hint={`resolution ${minutes(o.avgResolutionMinutes)}`} />
          <Stat label="Satisfaction" value={o.avgRating != null ? `${o.avgRating}/5` : '—'} hint={`${o.majorIncidents} major open`} />
        </> : Array.from({ length: 8 }).map((_, i) => <div key={i} className="p-4"><Skeleton className="h-12" /></div>)}
      </Card>

      <div className="grid gap-6 xl:grid-cols-[1fr_380px]">
        <Card>
          <CardHeader title={<span className="flex items-center gap-1.5"><TrendingUp className="size-3.5" />Incident volume</span>} description="Created vs resolved per day, last 14 days"
            action={<div className="flex items-center gap-3 text-xs text-muted"><Legend color="bg-series-1" label="Created" /><Legend color="bg-series-2" label="Resolved" /></div>} />
          <CardBody className="h-72 pl-0">
            {trend.data ? <TrendChart data={trend.data} /> : <Skeleton className="ml-4 h-full" />}
          </CardBody>
        </Card>
        <Card className="flex flex-col">
          <CardHeader title={<span className="flex items-center gap-1.5"><ActivityIcon className="size-3.5" />Live activity</span>} description="Streams in over WebSocket" action={<LiveDot connected={connected} />} />
          <div className="max-h-72 flex-1 overflow-y-auto">
            {feed.length === 0 ? <p className="px-4 py-10 text-center text-[13px] text-muted">Waiting for events… create or update an incident to see it appear here.</p> : feed.map((e) => (
              <Link key={e.key} to={`/app/incidents/${e.incidentId}`} className="flex gap-3 border-b border-border px-4 py-2.5 last:border-0 hover:bg-surface animate-in">
                <span className="w-14 shrink-0 font-mono text-[11px] text-subtle">{time(e.at)}</span>
                <span className="min-w-0 flex-1">
                  <span className="flex items-center gap-1.5 text-xs"><span className="font-mono text-muted">{e.number}</span><span className="font-mono text-[10px] text-subtle">{e.type}</span>{e.live && <span className="size-1.5 rounded-full bg-green" title="Received live" />}</span>
                  <span className="mt-0.5 line-clamp-2 block text-[13px]">{e.description}</span>
                </span>
              </Link>
            ))}
          </div>
        </Card>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader title={<span className="flex items-center gap-1.5"><AlertTriangle className="size-3.5" />Correlated incidents</span>} description="Open incidents sharing a common upstream service" />
          {correlations.data?.length === 0 ? <EmptyState title="No correlated outages" description="Three or more related open incidents on a shared dependency will show up here." /> : (
            <div className="divide-y divide-border">
              {correlations.data?.map((c) => (
                <div key={c.rootService.id} className="px-4 py-3">
                  <div className="flex items-center gap-2">
                    <Badge tone="red">Major</Badge>
                    <span className="text-[13px] font-medium">Suspected root cause: {c.rootService.name}</span>
                  </div>
                  <p className="mt-1 text-xs text-muted">{c.explanation}</p>
                  <div className="mt-2 flex flex-wrap gap-1.5">
                    {c.incidents.map((i) => <Link key={i.id} to={`/app/incidents/${i.id}`}><Badge tone="neutral" className="font-mono hover:border-brand">{i.number} · {i.service}</Badge></Link>)}
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
        <Card>
          <CardHeader title={<span className="flex items-center gap-1.5"><Radar className="size-3.5" />SLA risk</span>} description="Predicted from elapsed time and historical resolution times" />
          {risks.data?.length === 0 ? <EmptyState title="Nothing at risk" description="Open incidents predicted to miss their SLA will be listed here." /> : (
            <div className="divide-y divide-border">
              {risks.data?.slice(0, 6).map((r) => (
                <Link key={r.incident.id} to={`/app/incidents/${r.incident.id}`} className="flex items-center gap-3 px-4 py-2.5 hover:bg-surface">
                  <span className="font-mono text-xs text-muted">{r.incident.number}</span>
                  <PriorityBadge priority={r.incident.priority} />
                  <span className="min-w-0 flex-1"><span className="block truncate text-[13px]">{r.incident.title}</span><span className="block truncate text-[11px] text-subtle">{r.reason}</span></span>
                  <Meter value={r.riskScore} tone={r.riskScore >= 0.8 ? 'red' : 'amber'} className="w-16" />
                  <span className="w-9 text-right font-mono text-xs">{Math.round(r.riskScore * 100)}</span>
                </Link>
              ))}
            </div>
          )}
        </Card>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader title="Technician workload" description="Active incidents against capacity" />
          <Table>
            <THead><tr><TH>Technician</TH><TH>Load</TH><TH className="text-right">Resolved</TH><TH className="text-right hidden sm:table-cell">Avg fix</TH><TH className="text-right hidden sm:table-cell">Rating</TH></tr></THead>
            <tbody>
              {workload.data?.map((t) => (
                <TR key={t.id}>
                  <TD><span className="flex items-center gap-2"><span className={cn('size-1.5 rounded-full', t.available ? 'bg-green' : 'bg-subtle')} title={t.available ? 'Available' : 'Unavailable'} /><span className="text-[13px]">{t.name}</span><span className="font-mono text-[11px] text-subtle">{t.code}</span></span></TD>
                  <TD><span className="flex items-center gap-2"><Meter value={t.active / t.maxWorkload} tone={t.active >= t.maxWorkload ? 'red' : t.active / t.maxWorkload >= 0.7 ? 'amber' : undefined} className="w-20" /><span className="font-mono text-xs">{t.active}/{t.maxWorkload}</span></span></TD>
                  <TD className="text-right font-mono text-xs">{t.resolved}</TD>
                  <TD className="text-right font-mono text-xs hidden sm:table-cell">{minutes(t.avgResolutionMinutes)}</TD>
                  <TD className="text-right font-mono text-xs hidden sm:table-cell">{t.avgRating ?? '—'}</TD>
                </TR>
              ))}
            </tbody>
          </Table>
        </Card>
        <Card>
          <CardHeader title="Service analysis" description="Incidents per IT service" />
          <Table>
            <THead><tr><TH>Service</TH><TH className="text-right">Total</TH><TH className="text-right">Open</TH><TH className="text-right">P1</TH><TH className="text-right">Breached</TH></tr></THead>
            <tbody>
              {services.data?.map((s) => (
                <TR key={s.id}>
                  <TD><span className="flex items-center gap-2 text-[13px]">{s.name}<CriticalityDots value={s.criticality} /></span></TD>
                  <TD className="text-right font-mono text-xs">{s.total}</TD>
                  <TD className="text-right font-mono text-xs">{s.open}</TD>
                  <TD className="text-right font-mono text-xs">{s.p1}</TD>
                  <TD className={cn('text-right font-mono text-xs', s.breached > 0 && 'text-red')}>{s.breached}</TD>
                </TR>
              ))}
            </tbody>
          </Table>
        </Card>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader title="SLA compliance by priority" description="Met vs breached, all time" />
          <div className="divide-y divide-border">
            {sla.data?.map((s) => (
              <div key={s.priority} className="flex items-center gap-4 px-4 py-3">
                <PriorityBadge priority={s.priority} />
                <div className="flex-1">
                  <Meter value={(s.compliancePercent ?? 0) / 100} tone={(s.compliancePercent ?? 100) < 85 ? 'amber' : 'green'} />
                  <p className="mt-1 text-[11px] text-subtle">{s.met} met · {s.breached} breached · {s.running} running · avg response {minutes(s.avgResponseMinutes)}</p>
                </div>
                <span className="w-14 text-right font-mono text-sm">{s.compliancePercent != null ? `${s.compliancePercent}%` : '—'}</span>
              </div>
            ))}
          </div>
        </Card>
        <Card>
          <CardHeader title="Incidents by category" />
          <CardBody className="flex flex-col gap-2">
            {(() => {
              const rows = breakdown.data?.category ?? []
              const max = Math.max(1, ...rows.map((r) => r.count))
              return rows.map((r) => (
                <div key={r.key} className="group flex items-center gap-3 text-[13px]" title={`${r.key}: ${r.count}`}>
                  <span className="w-28 truncate text-muted">{r.key.toLowerCase()}</span>
                  <div className="h-3.5 flex-1"><div className="h-full rounded-r bg-series-1 transition-opacity group-hover:opacity-80" style={{ width: `${(r.count / max) * 100}%` }} /></div>
                  <span className="w-8 text-right font-mono text-xs">{r.count}</span>
                </div>
              ))
            })()}
          </CardBody>
        </Card>
      </div>

      {isManagement(user?.role) && <RecentBreaches />}
    </div>
  )
}

function RecentBreaches() {
  const { data } = useQuery({ queryKey: ['incidents', 'breached'], queryFn: () => get<{ content: import('@/lib/types').IncidentSummary[] }>('/incidents', { slaStatus: 'BREACHED', size: 6, sort: 'updatedAt' }) })
  if (!data?.content.length) return null
  return (
    <Card>
      <CardHeader title="Recent SLA violations" action={<Button variant="link" size="xs" asChild><Link to="/app/incidents?slaStatus=BREACHED">View all</Link></Button>} />
      <div className="divide-y divide-border">
        {data.content.map((i) => (
          <Link key={i.id} to={`/app/incidents/${i.id}`} className="flex items-center gap-3 px-4 py-2.5 text-[13px] hover:bg-surface">
            <span className="font-mono text-xs text-muted">{i.number}</span><PriorityBadge priority={i.priority} />
            <span className="flex-1 truncate">{i.title}</span><StatusBadge status={i.status} />
            <span className="hidden text-xs text-muted sm:inline">{i.service}</span><span className="text-xs text-subtle">{relativeTime(i.updatedAt)}</span>
          </Link>
        ))}
      </div>
    </Card>
  )
}

function Legend({ color, label }: { color: string; label: string }) {
  return <span className="flex items-center gap-1.5"><span className={cn('h-0.5 w-3 rounded-full', color)} />{label}</span>
}

export function Meter({ value, tone, className }: { value: number; tone?: 'red' | 'amber' | 'green'; className?: string }) {
  const color = tone === 'red' ? 'bg-red' : tone === 'amber' ? 'bg-amber' : tone === 'green' ? 'bg-green' : 'bg-brand'
  return <div className={cn('h-1.5 overflow-hidden rounded-full bg-surface-2', className)}><div className={cn('h-full rounded-full', color)} style={{ width: `${Math.min(100, Math.max(2, value * 100))}%` }} /></div>
}

function TrendChart({ data }: { data: TrendPoint[] }) {
  const rows = data.map((d) => ({ ...d, label: new Date(d.date + 'T00:00:00').toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) }))
  return (
    <ResponsiveContainer width="100%" height="100%">
      <LineChart data={rows} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
        <CartesianGrid vertical={false} stroke="var(--border)" />
        <XAxis dataKey="label" tickLine={false} axisLine={false} tick={{ fontSize: 11, fill: 'var(--muted)' }} interval="preserveStartEnd" minTickGap={24} />
        <YAxis allowDecimals={false} tickLine={false} axisLine={false} tick={{ fontSize: 11, fill: 'var(--muted)' }} width={36} />
        <Tooltip cursor={{ stroke: 'var(--border-strong)' }} content={({ active, payload, label }) => active && payload?.length ? (
          <div className="rounded-md border border-border bg-background px-3 py-2 text-xs shadow-lg">
            <p className="mb-1 font-medium">{label}</p>
            {payload.map((p) => <p key={String(p.dataKey)} className="flex items-center gap-2"><span className="h-0.5 w-3 rounded-full" style={{ background: p.color }} /><span className="text-muted">{p.name}</span><span className="ml-auto font-mono">{p.value}</span></p>)}
            <p className="mt-1 text-muted">Breached <span className="font-mono text-foreground">{(payload[0].payload as TrendPoint).breached}</span></p>
          </div>
        ) : null} />
        <Line type="linear" dataKey="created" name="Created" stroke="var(--series-1)" strokeWidth={2} dot={false} activeDot={{ r: 4, strokeWidth: 2, stroke: 'var(--background)' }} />
        <Line type="linear" dataKey="resolved" name="Resolved" stroke="var(--series-2)" strokeWidth={2} dot={false} activeDot={{ r: 4, strokeWidth: 2, stroke: 'var(--background)' }} />
      </LineChart>
    </ResponsiveContainer>
  )
}
