import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import {
  ArrowLeft, CheckCircle2, Clock, Copy, Hand, Lightbulb, Lock, MessageSquare, Play, RotateCcw, Send, ShieldAlert,
  Star, UserPlus, XCircle, AlertTriangle, HelpCircle, CircleCheckBig, Archive, Pencil, Info,
} from 'lucide-react'
import { ApiError, get, patch, post } from '@/lib/api'
import { useAuth, isStaff } from '@/lib/auth'
import type { AuditView, DuplicateView, Impact, IncidentDetail, NoteView, ResolutionSuggestion, ServiceView, TechnicianScoreView, Urgency, UserView } from '@/lib/types'
import { Button } from '@/components/ui/button'
import { Card, CardBody, CardHeader } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Field, Input, NativeSelect, Textarea } from '@/components/ui/input'
import { Dialog, DialogContent, DialogFooter } from '@/components/ui/dialog'
import { Skeleton, Switch, Tabs, TabsContent, TabsList, TabsTrigger, EmptyState } from '@/components/ui/misc'
import { CriticalityDots, PriorityBadge, SlaBadge, StatusBadge, useNow } from '@/components/domain'
import { countdown, dateTime, minutes, relativeTime, statusLabel, titleCase } from '@/lib/format'
import { cn } from '@/lib/utils'

type DialogKind = null | 'assign' | 'request-info' | 'provide-info' | 'resolve' | 'reopen' | 'close' | 'cancel' | 'feedback' | 'link' | 'edit'

export function IncidentDetailPage() {
  const { id } = useParams()
  const { user } = useAuth()
  const qc = useQueryClient()
  const staff = isStaff(user?.role)
  const [dialog, setDialog] = useState<DialogKind>(null)

  const { data: incident, isLoading, error } = useQuery({ queryKey: ['incident', id], queryFn: () => get<IncidentDetail>(`/incidents/${id}`), refetchInterval: 15000 })
  const { data: audit } = useQuery({ queryKey: ['incident', id, 'audit'], queryFn: () => get<AuditView[]>(`/incidents/${id}/audit`) })

  const action = useMutation({
    mutationFn: ({ path, body }: { path: string; body?: unknown }) => post<IncidentDetail>(`/incidents/${id}/${path}`, body),
    onSuccess: (data, vars) => {
      qc.setQueryData(['incident', id], data)
      qc.invalidateQueries({ queryKey: ['incident', id, 'audit'] })
      qc.invalidateQueries({ queryKey: ['incidents'] })
      qc.invalidateQueries({ queryKey: ['workload'] })
      setDialog(null)
      toast.success(successMessage(vars.path, data))
    },
    onError: (e: ApiError) => toast.error(e.message, { description: e.code }),
  })
  const run = (path: string, body?: unknown) => action.mutate({ path, body })

  if (isLoading) return <div className="flex flex-col gap-4"><Skeleton className="h-8 w-64" /><Skeleton className="h-40 w-full" /><Skeleton className="h-64 w-full" /></div>
  if (error || !incident) return <EmptyState icon={<XCircle />} title="Incident not available" description={(error as ApiError)?.message ?? 'Not found'} action={<Button variant="secondary" asChild><Link to="/app/incidents">Back to incidents</Link></Button>} />

  const a = new Set(incident.allowedActions)
  const busy = action.isPending

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-4 border-b border-border pb-5">
        <Link to="/app/incidents" className="flex w-fit items-center gap-1 text-[13px] text-muted hover:text-foreground"><ArrowLeft className="size-3.5" />Incidents</Link>
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <span className="font-mono text-sm text-muted">{incident.number}</span>
              <StatusBadge status={incident.status} />
              <PriorityBadge priority={incident.priority} showLabel />
              {incident.majorIncident && <Badge tone="red"><AlertTriangle className="size-3" />Major incident</Badge>}
              {incident.escalationLevel > 0 && <Badge tone="red"><ShieldAlert className="size-3" />Escalated · {incident.escalationLevelName}</Badge>}
              {incident.masterIncident && <Badge tone="outline"><Copy className="size-3" />Duplicate of <Link className="underline" to={`/app/incidents/${incident.masterIncident.id}`}>{incident.masterIncident.name}</Link></Badge>}
            </div>
            <h1 className="mt-2 text-2xl font-semibold tracking-tight">{incident.title}</h1>
            <p className="mt-1 text-[13px] text-muted">Reported by {incident.reporter.fullName} · {dateTime(incident.createdAt)} · updated {relativeTime(incident.updatedAt)}</p>
          </div>
          <div className="flex flex-wrap gap-2">
            {a.has('ACKNOWLEDGE') && <Button onClick={() => run('acknowledge')} loading={busy}><Hand />Acknowledge</Button>}
            {a.has('START') && <Button onClick={() => run('start')} loading={busy}><Play />Start work</Button>}
            {a.has('RESOLVE') && <Button onClick={() => setDialog('resolve')}><CheckCircle2 />Resolve</Button>}
            {a.has('PROVIDE_INFO') && <Button onClick={() => setDialog('provide-info')}><Send />Provide info</Button>}
            {a.has('VERIFY') && <Button onClick={() => run('verify')} loading={busy}><CircleCheckBig />Confirm fixed</Button>}
            {a.has('CLOSE') && <Button onClick={() => setDialog('close')}><Archive />Close</Button>}
            {a.has('FEEDBACK') && <Button variant="secondary" onClick={() => setDialog('feedback')}><Star />Rate</Button>}
            {a.has('REQUEST_INFO') && <Button variant="secondary" onClick={() => setDialog('request-info')}><HelpCircle />Request info</Button>}
            {a.has('ASSIGN') && <Button variant="secondary" onClick={() => setDialog('assign')}><UserPlus />{incident.technician ? 'Reassign' : 'Assign'}</Button>}
            {a.has('REOPEN') && <Button variant="secondary" onClick={() => setDialog('reopen')}><RotateCcw />Not fixed</Button>}
            {a.has('LINK_DUPLICATE') && staff && <Button variant="secondary" onClick={() => setDialog('link')}><Copy />Link duplicate</Button>}
            {a.has('EDIT') && <Button variant="ghost" onClick={() => setDialog('edit')}><Pencil />Edit</Button>}
            {a.has('CANCEL') && <Button variant="ghost" className="text-red" onClick={() => setDialog('cancel')}><XCircle />Cancel</Button>}
          </div>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <div className="flex min-w-0 flex-col gap-6">
          {incident.status === 'AWAITING_INFO' && !staff && (
            <div className="flex items-start gap-3 rounded-lg border border-amber/40 bg-amber-soft p-4 text-[13px]">
              <Info className="mt-0.5 size-4 shrink-0 text-amber" />
              <div className="flex-1"><p className="font-medium text-amber">The support engineer needs more information</p>
                <p className="mt-0.5 text-muted">{[...incident.notes].reverse().find((n) => n.type === 'INFO_REQUEST')?.text}</p></div>
              <Button size="sm" onClick={() => setDialog('provide-info')}>Reply</Button>
            </div>
          )}
          {incident.resolution && <ResolutionCard incident={incident} />}

          <Card>
            <CardHeader title="Description" />
            <CardBody><p className="whitespace-pre-wrap text-sm leading-relaxed">{incident.description}</p></CardBody>
          </Card>

          <Tabs defaultValue="activity">
            <TabsList>
              <TabsTrigger value="activity">Activity</TabsTrigger>
              <TabsTrigger value="history">Audit trail <span className="ml-1 font-mono text-xs text-subtle">{audit?.length ?? ''}</span></TabsTrigger>
            </TabsList>
            <TabsContent value="activity" className="pt-5"><Activity incident={incident} audit={audit ?? []} onComment={(text, internal) => run('notes', { text, internal })} busy={busy} staff={staff} /></TabsContent>
            <TabsContent value="history" className="pt-5"><AuditTable audit={audit ?? []} /></TabsContent>
          </Tabs>
        </div>

        <aside className="flex flex-col gap-4">
          <SlaPanel incident={incident} />
          <DetailsPanel incident={incident} />
          {staff && incident.status !== 'CLOSED' && incident.status !== 'CANCELLED' && <SuggestedFixes id={incident.id} />}
          {staff && <SimilarPanel id={incident.id} />}
          {incident.duplicates.length > 0 && (
            <Card>
              <CardHeader title="Linked duplicates" description="Resolved automatically with this incident" />
              <CardBody className="flex flex-wrap gap-1.5">{incident.duplicates.map((d) => <Link key={d.id} to={`/app/incidents/${d.id}`}><Badge tone="neutral" className="font-mono">{d.name}</Badge></Link>)}</CardBody>
            </Card>
          )}
        </aside>
      </div>

      <AssignDialog open={dialog === 'assign'} onClose={() => setDialog(null)} incident={incident} onAssign={(technicianId) => run('assign', { technicianId })} busy={busy} />
      <TextDialog open={dialog === 'request-info'} onClose={() => setDialog(null)} title="Request information" description="The reporter is notified and the resolution SLA clock pauses until they reply." label="Question for the reporter" submit="Send request" onSubmit={(message) => run('request-info', { message })} busy={busy} />
      <TextDialog open={dialog === 'provide-info'} onClose={() => setDialog(null)} title="Provide information" description="Your reply goes to the assigned engineer and the investigation resumes." label="Your answer" submit="Send reply" onSubmit={(message) => run('provide-info', { message })} busy={busy} />
      <TextDialog open={dialog === 'reopen'} onClose={() => setDialog(null)} title="Reopen incident" description="Tell the engineer what is still not working." label="What's still wrong?" submit="Reopen" onSubmit={(message) => run('reopen', { message })} busy={busy} />
      <TextDialog open={dialog === 'close'} onClose={() => setDialog(null)} title="Close incident" description="Closure is final. Add any closure notes." label="Closure notes" optional submit="Close incident" onSubmit={(message) => run('close', { message })} busy={busy} />
      <TextDialog open={dialog === 'cancel'} onClose={() => setDialog(null)} title="Cancel incident" description="Use this if the incident was reported by mistake or is no longer relevant." label="Reason" submit="Cancel incident" destructive onSubmit={(message) => run('cancel', { message })} busy={busy} />
      <ResolveDialog open={dialog === 'resolve'} onClose={() => setDialog(null)} id={incident.id} onSubmit={(body) => run('resolve', body)} busy={busy} />
      <FeedbackDialog open={dialog === 'feedback'} onClose={() => setDialog(null)} onSubmit={(body) => run('feedback', body)} busy={busy} />
      <LinkDialog open={dialog === 'link'} onClose={() => setDialog(null)} id={incident.id} onSubmit={(masterId) => run('link-duplicate', { masterId })} busy={busy} />
      <EditDialog open={dialog === 'edit'} onClose={() => setDialog(null)} incident={incident} />
    </div>
  )
}

function successMessage(path: string, d: IncidentDetail) {
  const map: Record<string, string> = {
    acknowledge: 'Incident acknowledged', start: 'Investigation started', 'request-info': 'Information requested · SLA paused',
    'provide-info': 'Reply sent', resolve: 'Incident resolved', verify: 'Resolution confirmed', reopen: 'Incident reopened',
    close: 'Incident closed', cancel: 'Incident cancelled', notes: 'Comment added', feedback: 'Thanks for your feedback',
    'link-duplicate': 'Linked as duplicate', assign: `Assigned to ${d.technician?.fullName ?? ''}`,
  }
  return `${d.number}: ${map[path] ?? 'Updated'}`
}

// ------------------------------------------------------------------ side panels

function SlaPanel({ incident }: { incident: IncidentDetail }) {
  const now = useNow()
  const s = incident.sla
  const done = ['RESOLVED', 'VERIFIED', 'CLOSED', 'CANCELLED'].includes(incident.status)
  const row = (label: string, due?: string, at?: string, breached?: boolean) => {
    const ms = due ? new Date(due).getTime() - (at ? new Date(at).getTime() : now) : 0
    return (
      <div className="flex items-center justify-between py-2 text-[13px]">
        <span className="text-muted">{label}</span>
        {!due ? <span className="text-subtle">—</span> : at ? (
          <span className={cn('flex items-center gap-1.5 font-mono text-xs', breached ? 'text-red' : 'text-green')}>
            {breached ? <XCircle className="size-3.5" /> : <CheckCircle2 className="size-3.5" />}{dateTime(at)}
          </span>
        ) : s.pausedAt && label === 'Resolution' ? <span className="font-mono text-xs text-amber">paused</span> : (
          <span className={cn('font-mono text-xs tabular-nums', ms < 0 || breached ? 'text-red' : s.status === 'AT_RISK' ? 'text-amber' : 'text-foreground')}>{countdown(ms)}</span>
        )}
      </div>
    )
  }
  const pct = Math.min(100, Math.max(0, s.elapsedPercent))
  return (
    <Card>
      <CardHeader title={<span className="flex items-center gap-1.5"><Clock className="size-3.5" />Service level</span>} description={s.policyName} action={<SlaBadge status={s.status} />} />
      <CardBody className="pt-2">
        {!done && s.status !== 'NOT_APPLICABLE' && (
          <div className="mb-2 mt-1">
            <div className="h-1.5 overflow-hidden rounded-full bg-surface-2">
              <div className={cn('h-full rounded-full transition-all', s.status === 'BREACHED' || pct >= 100 ? 'bg-red' : s.status === 'AT_RISK' ? 'bg-amber' : 'bg-brand')} style={{ width: `${pct}%` }} />
            </div>
            <p className="mt-1 text-[11px] text-subtle">{Math.round(s.elapsedPercent)}% of the {s.respondedAt ? 'resolution' : 'response'} window used</p>
          </div>
        )}
        <div className="divide-y divide-border">
          {row('Response', s.responseDueAt, s.respondedAt, s.responseBreached)}
          {row('Resolution', s.resolutionDueAt, s.resolvedAt, s.resolutionBreached)}
        </div>
        <p className="mt-2 text-[11px] text-subtle">Targets {minutes(s.responseMinutes)} / {minutes(s.resolutionMinutes)}{s.timeScale !== 1 && ` · demo clock ×${s.timeScale}`}</p>
      </CardBody>
    </Card>
  )
}

function DetailsPanel({ incident }: { incident: IncidentDetail }) {
  const item = (label: string, value: React.ReactNode) => (
    <div className="flex items-start justify-between gap-3 py-2 text-[13px]"><span className="shrink-0 text-muted">{label}</span><span className="text-right">{value}</span></div>
  )
  return (
    <Card>
      <CardHeader title="Details" />
      <CardBody className="divide-y divide-border py-1">
        {item('Service', <span className="flex items-center gap-2">{incident.service.name}<CriticalityDots value={incident.service.businessCriticality} /></span>)}
        {item('Category', <span>{incident.category?.name.toLowerCase()}{incident.categoryAutoAssigned && <span className="ml-1 text-xs text-subtle">auto · {Math.round(incident.categoryConfidence * 100)}%</span>}</span>)}
        {item('Impact', titleCase(incident.impact))}
        {item('Urgency', titleCase(incident.urgency))}
        {item('Affected users', <span className="font-mono">{incident.affectedUsers.toLocaleString()}</span>)}
        {item('Assignee', incident.technician ? <span>{incident.technician.fullName} <span className="font-mono text-xs text-subtle">{incident.technician.code}</span><br /><span className="text-xs text-muted">{incident.technician.title}</span></span> : <span className="text-subtle">Unassigned</span>)}
        {item('Reporter', <span>{incident.reporter.fullName} <span className="font-mono text-xs text-subtle">{incident.reporter.code}</span></span>)}
        <div className="py-2 text-[13px]">
          <p className="text-muted">Priority rationale</p>
          <p className="mt-1 font-mono text-[11px] leading-relaxed text-subtle">{incident.priorityRationale}</p>
        </div>
        {incident.feedback && item('Feedback', <span className="flex items-center gap-0.5">{Array.from({ length: 5 }).map((_, i) => <Star key={i} className={cn('size-3.5', i < incident.feedback!.rating ? 'fill-foreground text-foreground' : 'text-border-strong')} />)}</span>)}
        {incident.closedAt && item('Closed', `${dateTime(incident.closedAt)} by ${incident.closedBy}`)}
        {incident.cancellationReason && item('Cancelled', incident.cancellationReason)}
      </CardBody>
    </Card>
  )
}

function ResolutionCard({ incident }: { incident: IncidentDetail }) {
  const r = incident.resolution!
  return (
    <Card className="border-green/40">
      <CardHeader title={<span className="flex items-center gap-1.5 text-green"><CheckCircle2 className="size-4" />Resolution</span>} description={`${r.resolvedBy} · ${dateTime(r.resolvedAt)}`} />
      <CardBody className="flex flex-col gap-3 text-sm">
        <p className="font-medium">{r.summary}</p>
        {r.rootCause && <div><p className="text-xs text-muted">Root cause</p><p className="mt-0.5">{r.rootCause}</p></div>}
        {r.steps && <div><p className="text-xs text-muted">Steps taken</p><p className="mt-0.5 whitespace-pre-wrap">{r.steps}</p></div>}
        {incident.closureNotes && <div><p className="text-xs text-muted">Closure notes</p><p className="mt-0.5">{incident.closureNotes}</p></div>}
      </CardBody>
    </Card>
  )
}

function SuggestedFixes({ id }: { id: number }) {
  const { data } = useQuery({ queryKey: ['intel', 'fixes', id], queryFn: () => get<ResolutionSuggestion[]>(`/incidents/${id}/suggested-resolutions`) })
  if (!data?.length) return null
  return (
    <Card>
      <CardHeader title={<span className="flex items-center gap-1.5"><Lightbulb className="size-3.5" />Suggested resolutions</span>} description="From similar resolved incidents" />
      <div className="divide-y divide-border">
        {data.map((r) => (
          <div key={r.incidentId} className="px-4 py-3 text-[13px]">
            <p>{r.summary}</p>
            {r.rootCause && <p className="mt-0.5 text-xs text-muted">Cause: {r.rootCause}</p>}
            <p className="mt-1 text-[11px] text-subtle"><Link to={`/app/incidents/${r.incidentId}`} className="underline">{r.incidentNumber}</Link> · {Math.round(r.similarity * 100)}% match</p>
          </div>
        ))}
      </div>
    </Card>
  )
}

function SimilarPanel({ id }: { id: number }) {
  const { data } = useQuery({ queryKey: ['intel', 'similar', id], queryFn: () => get<DuplicateView[]>(`/incidents/${id}/similar`) })
  if (!data?.length) return null
  return (
    <Card>
      <CardHeader title={<span className="flex items-center gap-1.5"><Copy className="size-3.5" />Similar incidents</span>} description="Possible duplicates in the last 7 days" />
      <div className="divide-y divide-border">
        {data.map((d) => (
          <Link key={d.id} to={`/app/incidents/${d.id}`} className="flex items-center gap-2 px-4 py-2.5 text-[13px] hover:bg-surface">
            <span className="font-mono text-xs text-muted">{d.number}</span>
            <span className="flex-1 truncate">{d.title}</span>
            <span className={cn('font-mono text-xs', d.likelyDuplicate ? 'text-amber' : 'text-muted')}>{Math.round(d.similarity * 100)}%</span>
          </Link>
        ))}
      </div>
    </Card>
  )
}

// ------------------------------------------------------------------ activity

type TimelineItem = { kind: 'note'; at: string; note: NoteView } | { kind: 'audit'; at: string; audit: AuditView }

function Activity({ incident, audit, onComment, busy, staff }: { incident: IncidentDetail; audit: AuditView[]; onComment: (text: string, internal: boolean) => void; busy: boolean; staff: boolean }) {
  const [text, setText] = useState('')
  const [internal, setInternal] = useState(false)
  const items = useMemo<TimelineItem[]>(() => {
    const list: TimelineItem[] = [
      ...incident.notes.map((n) => ({ kind: 'note' as const, at: n.createdAt, note: n })),
      ...audit.filter((a) => a.action !== 'NOTE_ADDED').map((a) => ({ kind: 'audit' as const, at: a.timestamp, audit: a })),
    ]
    return list.sort((x, y) => new Date(x.at).getTime() - new Date(y.at).getTime())
  }, [incident.notes, audit])

  return (
    <div className="flex flex-col gap-4">
      <ol className="relative flex flex-col gap-0 pl-6 before:absolute before:left-[7px] before:top-2 before:bottom-2 before:w-px before:bg-border">
        {items.map((it, idx) => it.kind === 'audit' ? (
          <li key={'a' + it.audit.id + idx} className="relative py-1.5 text-[13px]">
            <span className="absolute -left-6 top-3 size-[15px] rounded-full border-2 border-background bg-border-strong" />
            <span className="text-muted">{it.audit.description}</span>
            <span className="ml-2 whitespace-nowrap text-[11px] text-subtle">{it.audit.actorName !== 'SYSTEM' ? it.audit.actorName.split(' (')[0] + ' · ' : ''}{relativeTime(it.at)}</span>
          </li>
        ) : (
          <li key={'n' + it.note.id} className="relative py-2">
            <span className={cn('absolute -left-6 top-4 size-[15px] rounded-full border-2 border-background', it.note.type === 'WORK_NOTE' ? 'bg-amber' : it.note.type === 'SYSTEM' ? 'bg-subtle' : 'bg-foreground')} />
            <div className={cn('rounded-lg border p-3', it.note.type === 'WORK_NOTE' ? 'border-amber/30 bg-amber-soft/50' : it.note.type === 'SYSTEM' ? 'border-dashed border-border' : 'border-border')}>
              <div className="mb-1 flex flex-wrap items-center gap-2 text-xs">
                <span className="font-medium">{it.note.authorName}</span>
                {it.note.type !== 'COMMENT' && <Badge tone={it.note.type === 'WORK_NOTE' ? 'amber' : it.note.type === 'INFO_REQUEST' ? 'blue' : 'outline'}>{it.note.type === 'WORK_NOTE' ? <><Lock className="size-2.5" />internal</> : statusLabel(it.note.type)}</Badge>}
                <span className="text-subtle">{dateTime(it.note.createdAt)}</span>
              </div>
              <p className="whitespace-pre-wrap text-[13px] leading-relaxed">{it.note.text}</p>
            </div>
          </li>
        ))}
      </ol>
      {incident.allowedActions.includes('COMMENT') && (
        <Card>
          <CardBody className="flex flex-col gap-3">
            <Textarea rows={3} value={text} onChange={(e) => setText(e.target.value)} placeholder={staff ? 'Add an update or investigation note…' : 'Add more details for the support team…'} />
            <div className="flex items-center justify-between">
              {staff ? (
                <label className="flex items-center gap-2 text-[13px] text-muted"><Switch checked={internal} onCheckedChange={setInternal} />Internal note (hidden from reporter)</label>
              ) : <span />}
              <Button size="sm" disabled={!text.trim()} loading={busy} onClick={() => { onComment(text.trim(), internal); setText('') }}><MessageSquare />{internal ? 'Add note' : 'Comment'}</Button>
            </div>
          </CardBody>
        </Card>
      )}
    </div>
  )
}

function AuditTable({ audit }: { audit: AuditView[] }) {
  return (
    <Card>
      <div className="overflow-x-auto">
        <table className="w-full text-[13px]">
          <thead><tr className="border-b border-border text-left text-xs text-muted">
            <th className="h-9 px-4 font-medium">Time</th><th className="px-3 font-medium">User</th><th className="px-3 font-medium">Action</th><th className="px-3 font-medium">State</th><th className="px-4 font-medium">Description</th>
          </tr></thead>
          <tbody>
            {audit.map((a) => (
              <tr key={a.id} className="border-b border-border last:border-0 align-top">
                <td className="whitespace-nowrap px-4 py-2.5 font-mono text-xs text-muted">{dateTime(a.timestamp)}</td>
                <td className="whitespace-nowrap px-3 py-2.5">{a.actorName}</td>
                <td className="px-3 py-2.5"><span className="font-mono text-[11px]">{a.action}</span></td>
                <td className="whitespace-nowrap px-3 py-2.5 font-mono text-[11px] text-muted">{a.previousState || a.newState ? `${a.previousState ?? '·'} → ${a.newState ?? '·'}` : '—'}</td>
                <td className="px-4 py-2.5 text-muted">{a.description}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  )
}

// ------------------------------------------------------------------ dialogs

function TextDialog({ open, onClose, title, description, label, submit, onSubmit, busy, optional, destructive }: {
  open: boolean; onClose: () => void; title: string; description: string; label: string; submit: string; onSubmit: (v: string) => void; busy: boolean; optional?: boolean; destructive?: boolean
}) {
  const [v, setV] = useState('')
  return (
    <Dialog open={open} onOpenChange={(o) => { if (!o) { onClose(); setV('') } }}>
      <DialogContent title={title} description={description}>
        <Field label={label}><Textarea rows={4} value={v} onChange={(e) => setV(e.target.value)} autoFocus /></Field>
        <DialogFooter>
          <Button variant="secondary" onClick={onClose}>Back</Button>
          <Button variant={destructive ? 'destructive' : 'default'} disabled={!optional && !v.trim()} loading={busy} onClick={() => onSubmit(v.trim())}>{submit}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function AssignDialog({ open, onClose, incident, onAssign, busy }: { open: boolean; onClose: () => void; incident: IncidentDetail; onAssign: (id: number | null) => void; busy: boolean }) {
  const { data: recs, isLoading } = useQuery({ queryKey: ['intel', 'recs', incident.id], queryFn: () => get<TechnicianScoreView[]>(`/incidents/${incident.id}/recommendations`), enabled: open })
  const { data: techs } = useQuery({ queryKey: ['technicians'], queryFn: () => get<UserView[]>('/technicians'), enabled: open })
  const [manual, setManual] = useState('')
  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent wide title={incident.technician ? 'Reassign incident' : 'Assign incident'} description="Smart recommendation ranks available engineers by skills, workload, experience and history with this service.">
        <div className="flex flex-col gap-4">
          <div className="overflow-hidden rounded-lg border border-border">
            {isLoading && <div className="p-4"><Skeleton className="h-16 w-full" /></div>}
            {recs?.length === 0 && <p className="p-4 text-[13px] text-muted">No engineer is currently available (all busy, unavailable or at max workload).</p>}
            {recs?.map((r, i) => {
              const current = incident.technician?.id === r.technician.id
              return (
                <div key={r.technician.id} className="flex items-center gap-3 border-b border-border px-4 py-3 last:border-0">
                  <span className="w-5 font-mono text-xs text-subtle">{i + 1}</span>
                  <div className="min-w-0 flex-1">
                    <p className="text-[13px] font-medium">{r.technician.fullName} <span className="font-mono text-xs text-subtle">{r.technician.code}</span> {i === 0 && <Badge tone="blue" className="ml-1">best match</Badge>}</p>
                    <p className="truncate text-xs text-muted">{r.reason}</p>
                    <div className="mt-1.5 flex h-1.5 w-full max-w-56 overflow-hidden rounded-full bg-surface-2">
                      {Object.entries(r.breakdown).map(([k, v], j) => <div key={k} title={`${k}: ${v}`} className={cn('h-full', ['bg-brand', 'bg-brand/70', 'bg-brand/45', 'bg-brand/25'][j % 4])} style={{ width: `${v * 100}%` }} />)}
                    </div>
                  </div>
                  <span className="font-mono text-sm tabular-nums">{Math.round(r.score * 100)}</span>
                  <Button size="sm" variant={i === 0 ? 'default' : 'secondary'} disabled={current} loading={busy} onClick={() => onAssign(r.technician.id)}>{current ? 'Current' : 'Assign'}</Button>
                </div>
              )
            })}
          </div>
          <p className="text-[11px] text-subtle">Bars: skill · workload · experience · service history</p>
          <div className="flex items-end gap-2">
            <Field label="Or pick anyone" className="flex-1">
              <NativeSelect value={manual} onChange={(e) => setManual(e.target.value)}>
                <option value="">Select technician…</option>
                {techs?.map((t) => <option key={t.id} value={t.id}>{t.fullName} ({t.code}) · {t.workload}/{t.maxWorkload}{t.available ? '' : ' · unavailable'}</option>)}
              </NativeSelect>
            </Field>
            <Button variant="secondary" disabled={!manual} loading={busy} onClick={() => onAssign(Number(manual))}>Assign</Button>
          </div>
        </div>
        <DialogFooter>
          <Button variant="secondary" onClick={onClose}>Close</Button>
          <Button variant="accent" loading={busy} onClick={() => onAssign(null)}>Auto-assign with active strategy</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function ResolveDialog({ open, onClose, id, onSubmit, busy }: { open: boolean; onClose: () => void; id: number; onSubmit: (b: { summary: string; rootCause: string; steps: string }) => void; busy: boolean }) {
  const [f, setF] = useState({ summary: '', rootCause: '', steps: '' })
  const { data: fixes } = useQuery({ queryKey: ['intel', 'fixes', id], queryFn: () => get<ResolutionSuggestion[]>(`/incidents/${id}/suggested-resolutions`), enabled: open })
  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent wide title="Resolve incident" description="The reporter is notified and asked to confirm the fix. Linked duplicates are resolved too.">
        {fixes && fixes.length > 0 && (
          <div className="mb-4 flex flex-col gap-1.5">
            <p className="text-xs text-muted">Use a known fix</p>
            <div className="flex flex-wrap gap-1.5">
              {fixes.map((x) => (
                <button key={x.incidentId} onClick={() => setF({ summary: x.summary, rootCause: x.rootCause ?? '', steps: x.steps ?? '' })}
                  className="max-w-full truncate rounded-full border border-border px-2.5 py-1 text-xs hover:border-brand cursor-pointer">
                  <Lightbulb className="mr-1 inline size-3" />{x.summary}
                </button>
              ))}
            </div>
          </div>
        )}
        <div className="flex flex-col gap-3">
          <Field label="Resolution summary"><Input value={f.summary} onChange={(e) => setF({ ...f, summary: e.target.value })} autoFocus /></Field>
          <Field label="Root cause"><Input value={f.rootCause} onChange={(e) => setF({ ...f, rootCause: e.target.value })} /></Field>
          <Field label="Steps taken"><Textarea rows={3} value={f.steps} onChange={(e) => setF({ ...f, steps: e.target.value })} /></Field>
        </div>
        <DialogFooter>
          <Button variant="secondary" onClick={onClose}>Back</Button>
          <Button disabled={!f.summary.trim()} loading={busy} onClick={() => onSubmit(f)}><CheckCircle2 />Mark resolved</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function FeedbackDialog({ open, onClose, onSubmit, busy }: { open: boolean; onClose: () => void; onSubmit: (b: { rating: number; comment: string }) => void; busy: boolean }) {
  const [rating, setRating] = useState(0)
  const [comment, setComment] = useState('')
  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent title="How did we do?" description="Your rating helps measure support quality.">
        <div className="flex justify-center gap-1 py-2">
          {[1, 2, 3, 4, 5].map((n) => (
            <button key={n} onClick={() => setRating(n)} className="cursor-pointer p-1" aria-label={`${n} stars`}>
              <Star className={cn('size-7 transition-colors', n <= rating ? 'fill-foreground text-foreground' : 'text-border-strong hover:text-muted')} />
            </button>
          ))}
        </div>
        <Field label="Comment (optional)"><Textarea rows={3} value={comment} onChange={(e) => setComment(e.target.value)} /></Field>
        <DialogFooter>
          <Button variant="secondary" onClick={onClose}>Later</Button>
          <Button disabled={!rating} loading={busy} onClick={() => onSubmit({ rating, comment })}>Submit</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function LinkDialog({ open, onClose, id, onSubmit, busy }: { open: boolean; onClose: () => void; id: number; onSubmit: (masterId: number) => void; busy: boolean }) {
  const { data } = useQuery({ queryKey: ['intel', 'similar', id], queryFn: () => get<DuplicateView[]>(`/incidents/${id}/similar`), enabled: open })
  const [manual, setManual] = useState('')
  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent wide title="Link as duplicate" description="This incident will follow the master incident and be resolved with it.">
        <div className="overflow-hidden rounded-lg border border-border">
          {data?.length === 0 && <p className="p-4 text-[13px] text-muted">No similar incidents detected. Enter a master incident number below.</p>}
          {data?.map((d) => (
            <div key={d.id} className="flex items-center gap-3 border-b border-border px-4 py-2.5 text-[13px] last:border-0">
              <span className="font-mono text-xs text-muted">{d.number}</span>
              <div className="min-w-0 flex-1"><p className="truncate">{d.title}</p><p className="text-[11px] text-subtle">{d.reasons.join(' · ')}</p></div>
              <span className="font-mono text-xs">{Math.round(d.similarity * 100)}%</span>
              <Button size="xs" variant="secondary" loading={busy} onClick={() => onSubmit(d.id)}>Link</Button>
            </div>
          ))}
        </div>
        <div className="mt-4 flex items-end gap-2">
          <Field label="Master incident number" className="flex-1"><Input className="font-mono" placeholder="INC1024" value={manual} onChange={(e) => setManual(e.target.value)} /></Field>
          <Button variant="secondary" disabled={!/\d+/.test(manual)} loading={busy} onClick={() => { const n = Number(manual.replace(/\D/g, '')); onSubmit(n > 1000 ? n - 1000 : n) }}>Link</Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}

function EditDialog({ open, onClose, incident }: { open: boolean; onClose: () => void; incident: IncidentDetail }) {
  const qc = useQueryClient()
  const { data: services } = useQuery({ queryKey: ['services'], queryFn: () => get<ServiceView[]>('/catalog/services'), enabled: open })
  const [f, setF] = useState({ title: incident.title, description: incident.description, serviceId: String(incident.service.id), impact: incident.impact, urgency: incident.urgency, affectedUsers: String(incident.affectedUsers) })
  const save = useMutation({
    mutationFn: () => patch<IncidentDetail>(`/incidents/${incident.id}`, { ...f, serviceId: Number(f.serviceId), affectedUsers: Number(f.affectedUsers) }),
    onSuccess: (d) => { qc.setQueryData(['incident', String(incident.id)], d); qc.invalidateQueries({ queryKey: ['incident', String(incident.id), 'audit'] }); toast.success('Incident updated', { description: `Priority ${d.priority}` }); onClose() },
    onError: (e: ApiError) => toast.error(e.message),
  })
  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent wide title="Edit incident" description="Changing impact, urgency, affected users or service recalculates priority and SLA.">
        <div className="flex flex-col gap-3">
          <Field label="Title"><Input value={f.title} onChange={(e) => setF({ ...f, title: e.target.value })} /></Field>
          <Field label="Description"><Textarea rows={4} value={f.description} onChange={(e) => setF({ ...f, description: e.target.value })} /></Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Service"><NativeSelect value={f.serviceId} onChange={(e) => setF({ ...f, serviceId: e.target.value })}>{services?.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}</NativeSelect></Field>
            <Field label="Affected users"><Input type="number" min={1} value={f.affectedUsers} onChange={(e) => setF({ ...f, affectedUsers: e.target.value })} /></Field>
            <Field label="Impact"><NativeSelect value={f.impact} onChange={(e) => setF({ ...f, impact: e.target.value as Impact })}>{['LOW', 'MEDIUM', 'HIGH', 'EXTENSIVE'].map((x) => <option key={x}>{x}</option>)}</NativeSelect></Field>
            <Field label="Urgency"><NativeSelect value={f.urgency} onChange={(e) => setF({ ...f, urgency: e.target.value as Urgency })}>{['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((x) => <option key={x}>{x}</option>)}</NativeSelect></Field>
          </div>
        </div>
        <DialogFooter>
          <Button variant="secondary" onClick={onClose}>Back</Button>
          <Button loading={save.isPending} onClick={() => save.mutate()}>Save changes</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
