import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { Sparkles, Copy, Lightbulb, Gauge, ArrowLeft, AlertTriangle } from 'lucide-react'
import { ApiError, get, post } from '@/lib/api'
import type { Analysis, CategoryView, Impact, ServiceView, Urgency, IncidentDetail, DuplicateView } from '@/lib/types'
import { Button } from '@/components/ui/button'
import { Card, CardBody, CardHeader } from '@/components/ui/card'
import { Field, Input, NativeSelect, Textarea } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { CriticalityDots, PageHeader, PriorityBadge, StatusBadge } from '@/components/domain'
import { minutes } from '@/lib/format'
import { cn } from '@/lib/utils'

const IMPACTS: { v: Impact; label: string; hint: string }[] = [
  { v: 'LOW', label: 'Just me', hint: 'Single user, workaround exists' },
  { v: 'MEDIUM', label: 'My team', hint: 'A small group or department' },
  { v: 'HIGH', label: 'Many people', hint: 'Many users or a whole site' },
  { v: 'EXTENSIVE', label: 'Everyone', hint: 'Organisation-wide or customers' },
]
const URGENCIES: { v: Urgency; label: string; hint: string }[] = [
  { v: 'LOW', label: 'Can wait', hint: 'No deadline' },
  { v: 'MEDIUM', label: 'Slowed down', hint: 'Work is harder' },
  { v: 'HIGH', label: 'Blocked', hint: 'Cannot work' },
  { v: 'CRITICAL', label: 'Critical', hint: 'Money, safety or reputation' },
]

function useDebounced<T>(value: T, ms: number) {
  const [v, setV] = useState(value)
  useEffect(() => { const t = setTimeout(() => setV(value), ms); return () => clearTimeout(t) }, [value, ms])
  return v
}

export function ReportIncidentPage() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [form, setForm] = useState({ title: '', description: '', serviceId: '', categoryId: '', impact: 'MEDIUM' as Impact, urgency: 'MEDIUM' as Urgency, affectedUsers: '1' })
  const [error, setError] = useState<ApiError | null>(null)
  const [duplicateWarning, setDuplicateWarning] = useState<string | null>(null)
  const [created, setCreated] = useState<{ incident: IncidentDetail; possibleDuplicates: DuplicateView[] } | null>(null)

  const { data: services } = useQuery({ queryKey: ['services'], queryFn: () => get<ServiceView[]>('/catalog/services') })
  const { data: categories } = useQuery({ queryKey: ['categories'], queryFn: () => get<CategoryView[]>('/catalog/categories') })

  const analysisInput = useDebounced({ title: form.title, description: form.description, serviceId: form.serviceId, categoryId: form.categoryId, impact: form.impact, urgency: form.urgency, affectedUsers: form.affectedUsers }, 450)
  const textLen = (analysisInput.title + analysisInput.description).trim().length
  const { data: analysis, isFetching: analysing } = useQuery({
    queryKey: ['intel', 'analyze', analysisInput],
    queryFn: () => post<Analysis>('/intelligence/analyze', {
      ...analysisInput,
      serviceId: analysisInput.serviceId ? Number(analysisInput.serviceId) : null,
      categoryId: analysisInput.categoryId ? Number(analysisInput.categoryId) : null,
      affectedUsers: Number(analysisInput.affectedUsers) || 1,
    }),
    enabled: textLen >= 6 || !!analysisInput.serviceId,
    placeholderData: (prev) => prev,
  })

  const create = useMutation({
    mutationFn: (force: boolean) => post<{ incident: IncidentDetail; possibleDuplicates: DuplicateView[] }>('/incidents', {
      title: form.title, description: form.description, serviceId: Number(form.serviceId) || null,
      categoryId: form.categoryId ? Number(form.categoryId) : null, impact: form.impact, urgency: form.urgency,
      affectedUsers: Number(form.affectedUsers) || 1, force,
    }),
    onSuccess: (r) => {
      qc.invalidateQueries({ queryKey: ['incidents'] })
      toast.success(`${r.incident.number} created`, { description: `Priority ${r.incident.priority} · ${r.incident.technician ? 'assigned to ' + r.incident.technician.fullName : 'queued for assignment'}` })
      setCreated(r)
    },
    onError: (e: ApiError) => {
      if (e.code === 'DUPLICATE_INCIDENT') setDuplicateWarning(e.message)
      else setError(e)
    },
  })

  function submit(e: FormEvent) {
    e.preventDefault()
    setError(null); setDuplicateWarning(null)
    create.mutate(false)
  }

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => setForm({ ...form, [k]: e.target.value })
  const service = services?.find((s) => String(s.id) === form.serviceId)
  const topCategory = analysis?.categories[0]

  if (created) return <CreatedView result={created} onAnother={() => { setCreated(null); setForm({ ...form, title: '', description: '' }) }} onOpen={() => navigate(`/app/incidents/${created.incident.id}`)} />

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Report an incident" description="Describe what's wrong. The system categorises, prioritises and routes it automatically."
        actions={<Button variant="secondary" asChild><Link to="/app/incidents"><ArrowLeft />Back</Link></Button>} />

      <div className="grid gap-6 lg:grid-cols-[1fr_380px]">
        <form onSubmit={submit} className="flex flex-col gap-5">
          <Card>
            <CardBody className="flex flex-col gap-4">
              <Field label="What's the problem?" error={error?.fieldErrors?.title}>
                <Input required minLength={5} maxLength={200} value={form.title} onChange={set('title')} placeholder="e.g. Payment application unavailable" />
              </Field>
              <Field label="Details" hint="What happened, when it started, error messages, who is affected." error={error?.fieldErrors?.description}>
                <Textarea required minLength={10} maxLength={4000} rows={5} value={form.description} onChange={set('description')} placeholder="Users are unable to complete transactions since 10:00. The checkout page shows a 503 error…" />
              </Field>
              <div className="grid gap-4 sm:grid-cols-2">
                <Field label="Affected service" error={error?.fieldErrors?.serviceId}>
                  <NativeSelect required value={form.serviceId} onChange={set('serviceId')}>
                    <option value="">Select a service…</option>
                    {services?.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                  </NativeSelect>
                </Field>
                <Field label="Category" hint={topCategory && !form.categoryId ? <>Auto-detect suggests <b className="text-foreground">{topCategory.name.toLowerCase()}</b></> : 'Leave on auto to let the system decide'}>
                  <NativeSelect value={form.categoryId} onChange={set('categoryId')}>
                    <option value="">Auto-detect</option>
                    {categories?.map((c) => <option key={c.id} value={c.id}>{c.name.toLowerCase()}</option>)}
                  </NativeSelect>
                </Field>
              </div>
            </CardBody>
          </Card>

          <Card>
            <CardBody className="flex flex-col gap-5">
              <Segmented label="Who is affected? (impact)" options={IMPACTS} value={form.impact} onChange={(v) => setForm({ ...form, impact: v as Impact })} />
              <Segmented label="How urgent is it?" options={URGENCIES} value={form.urgency} onChange={(v) => setForm({ ...form, urgency: v as Urgency })} />
              <Field label="Approximate number of affected users" className="max-w-xs">
                <Input type="number" min={1} max={1000000} value={form.affectedUsers} onChange={set('affectedUsers')} className="font-mono" />
              </Field>
            </CardBody>
          </Card>

          {duplicateWarning && (
            <div className="flex flex-col gap-3 rounded-lg border border-amber/40 bg-amber-soft p-4 text-[13px]">
              <p className="flex items-start gap-2 text-amber"><AlertTriangle className="mt-0.5 size-4 shrink-0" />{duplicateWarning}</p>
              <div className="flex gap-2"><Button size="sm" variant="secondary" type="button" onClick={() => create.mutate(true)} loading={create.isPending}>Submit anyway</Button></div>
            </div>
          )}
          {error && !error.fieldErrors && <p className="rounded-md border border-red/30 bg-red-soft px-3 py-2 text-[13px] text-red">{error.message}</p>}
          <div className="flex justify-end gap-2">
            <Button type="submit" loading={create.isPending} disabled={!form.serviceId}>Submit incident</Button>
          </div>
        </form>

        <aside className="flex flex-col gap-4 lg:sticky lg:top-20 lg:self-start">
          <Card>
            <CardHeader title={<span className="flex items-center gap-1.5"><Sparkles className="size-3.5" />Live analysis</span>}
              description="Updates as you type" action={analysing ? <span className="size-3.5 animate-spin rounded-full border-2 border-muted border-r-transparent" /> : null} />
            <CardBody className="flex flex-col gap-5 text-[13px]">
              <section>
                <p className="mb-2 flex items-center gap-1.5 text-xs font-medium text-muted"><Gauge className="size-3.5" />Predicted priority & SLA</p>
                {analysis?.priority ? (
                  <div className="flex flex-col gap-2">
                    <div className="flex items-center gap-2">
                      <PriorityBadge priority={analysis.priority.priority} showLabel />
                      <span className="font-mono text-xs text-muted">score {analysis.priority.score}</span>
                    </div>
                    <div className="grid grid-cols-2 gap-px overflow-hidden rounded-md border border-border bg-border">
                      <div className="bg-background p-2"><p className="text-[11px] text-muted">Response</p><p className="font-mono">{minutes(analysis.priority.responseMinutes)}</p></div>
                      <div className="bg-background p-2"><p className="text-[11px] text-muted">Resolution</p><p className="font-mono">{minutes(analysis.priority.resolutionMinutes)}</p></div>
                    </div>
                    <p className="text-[11px] leading-relaxed text-subtle">{analysis.priority.rationale}</p>
                  </div>
                ) : <p className="text-subtle">Pick a service to see the predicted priority.</p>}
                {service && <p className="mt-2 flex items-center gap-2 text-xs text-muted">{service.name} criticality <CriticalityDots value={service.businessCriticality} /></p>}
              </section>

              <section>
                <p className="mb-2 flex items-center gap-1.5 text-xs font-medium text-muted"><Sparkles className="size-3.5" />Suggested category</p>
                {analysis?.categories.length ? (
                  <div className="flex flex-col gap-1.5">
                    {analysis.categories.map((c, i) => (
                      <div key={c.id} className="flex items-center gap-2">
                        <span className={cn('w-28 truncate', i === 0 ? 'font-medium' : 'text-muted')}>{c.name.toLowerCase()}</span>
                        <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-surface-2"><div className="h-full rounded-full bg-brand" style={{ width: `${Math.max(4, c.confidence * 100)}%` }} /></div>
                        <span className="w-9 text-right font-mono text-xs text-muted">{Math.round(c.confidence * 100)}%</span>
                      </div>
                    ))}
                    {analysis.categories[0].matchedTerms.length > 0 && <p className="text-[11px] text-subtle">matched: {analysis.categories[0].matchedTerms.join(', ')}</p>}
                  </div>
                ) : <p className="text-subtle">Start describing the problem.</p>}
              </section>

              <section>
                <p className="mb-2 flex items-center gap-1.5 text-xs font-medium text-muted"><Copy className="size-3.5" />Possibly already reported</p>
                {analysis?.duplicates.length ? (
                  <div className="flex flex-col divide-y divide-border rounded-md border border-border">
                    {analysis.duplicates.slice(0, 4).map((d) => (
                      <Link key={d.id} to={`/app/incidents/${d.id}`} target="_blank" className="flex flex-col gap-0.5 px-2.5 py-2 hover:bg-surface">
                        <span className="flex items-center gap-2"><span className="font-mono text-xs text-muted">{d.number}</span>{d.likelyDuplicate && <Badge tone="amber">likely duplicate</Badge>}<span className="ml-auto font-mono text-xs">{Math.round(d.similarity * 100)}%</span></span>
                        <span className="truncate">{d.title}</span>
                      </Link>
                    ))}
                  </div>
                ) : <p className="text-subtle">No similar recent incidents.</p>}
              </section>

              <section>
                <p className="mb-2 flex items-center gap-1.5 text-xs font-medium text-muted"><Lightbulb className="size-3.5" />Known fixes</p>
                {analysis?.resolutions.length ? (
                  <div className="flex flex-col gap-2">
                    {analysis.resolutions.map((r) => (
                      <div key={r.incidentId} className="rounded-md border border-border p-2.5">
                        <p>{r.summary}</p>
                        <p className="mt-1 text-[11px] text-subtle">from {r.incidentNumber} · {Math.round(r.similarity * 100)}% similar</p>
                      </div>
                    ))}
                  </div>
                ) : <p className="text-subtle">No matching past resolutions yet.</p>}
              </section>
            </CardBody>
          </Card>
        </aside>
      </div>
    </div>
  )
}

function Segmented({ label, options, value, onChange }: { label: string; options: { v: string; label: string; hint: string }[]; value: string; onChange: (v: string) => void }) {
  return (
    <div className="flex flex-col gap-1.5">
      <span className="text-[13px] font-medium">{label}</span>
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
        {options.map((o) => (
          <button type="button" key={o.v} onClick={() => onChange(o.v)}
            className={cn('flex flex-col items-start rounded-md border px-3 py-2 text-left transition-colors cursor-pointer',
              value === o.v ? 'border-brand bg-brand-soft' : 'border-border hover:border-border-strong')}>
            <span className="text-[13px] font-medium">{o.label}</span>
            <span className="text-[11px] text-muted">{o.hint}</span>
          </button>
        ))}
      </div>
    </div>
  )
}

function CreatedView({ result, onAnother, onOpen }: { result: { incident: IncidentDetail; possibleDuplicates: DuplicateView[] }; onAnother: () => void; onOpen: () => void }) {
  const i = result.incident
  const steps = [
    { label: 'Created', value: i.number },
    { label: 'Classified', value: `${i.category?.name.toLowerCase()}${i.categoryAutoAssigned ? ` · auto ${Math.round(i.categoryConfidence * 100)}%` : ''}` },
    { label: 'Prioritised', value: <PriorityBadge priority={i.priority} showLabel /> },
    { label: 'SLA', value: `${i.sla.policyName} · respond ${minutes(i.sla.responseMinutes)}, resolve ${minutes(i.sla.resolutionMinutes)}` },
    { label: 'Assigned', value: i.technician ? `${i.technician.fullName} (${i.technician.code})` : 'Queued — no technician available' },
    { label: 'Notified', value: i.technician ? 'Technician alerted in-app, by email' + (i.priority === 'P1' ? ' and SMS' : '') : 'Team leads will pick it up' },
  ]
  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-6 py-6">
      <div>
        <p className="font-mono text-xs text-muted">{i.number}</p>
        <h1 className="mt-1 text-2xl font-semibold tracking-tight">Incident submitted</h1>
        <p className="mt-1 text-sm text-muted">{i.title}</p>
      </div>
      <Card>
        <ol className="divide-y divide-border">
          {steps.map((s, idx) => (
            <li key={s.label} className="flex items-center gap-4 px-4 py-3 text-[13px]">
              <span className="flex size-5 items-center justify-center rounded-full bg-brand font-mono text-[10px] text-brand-fg">{idx + 1}</span>
              <span className="w-24 text-muted">{s.label}</span>
              <span className="flex-1">{s.value}</span>
            </li>
          ))}
        </ol>
      </Card>
      {result.possibleDuplicates.length > 0 && (
        <Card>
          <CardHeader title="Similar incidents" description="Support staff may link yours to one of these if it's the same problem." />
          <div className="divide-y divide-border">
            {result.possibleDuplicates.slice(0, 3).map((d) => (
              <div key={d.id} className="flex items-center gap-3 px-4 py-2.5 text-[13px]">
                <span className="font-mono text-xs text-muted">{d.number}</span><span className="flex-1 truncate">{d.title}</span>
                <StatusBadge status={d.status} /><span className="font-mono text-xs">{Math.round(d.similarity * 100)}%</span>
              </div>
            ))}
          </div>
        </Card>
      )}
      <div className="flex gap-2">
        <Button onClick={onOpen}>Track incident</Button>
        <Button variant="secondary" onClick={onAnother}>Report another</Button>
      </div>
    </div>
  )
}
