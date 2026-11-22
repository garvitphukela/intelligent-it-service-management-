import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Plus, X, Inbox, ChevronLeft, ChevronRight } from 'lucide-react'
import { get } from '@/lib/api'
import { useAuth } from '@/lib/auth'
import type { CategoryView, IncidentSummary, Page, ServiceView, UserView } from '@/lib/types'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input, NativeSelect } from '@/components/ui/input'
import { EmptyState } from '@/components/ui/misc'
import { IncidentTable } from '@/components/IncidentTable'
import { PageHeader } from '@/components/domain'
import { statusLabel } from '@/lib/format'

const STATUSES = ['NEW', 'CLASSIFIED', 'ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'AWAITING_INFO', 'RESOLVED', 'VERIFIED', 'CLOSED', 'CANCELLED']
const FILTER_KEYS = ['q', 'number', 'status', 'priority', 'categoryId', 'serviceId', 'technicianId', 'slaStatus', 'from', 'to', 'openOnly', 'unassigned', 'majorOnly']

export function IncidentsPage() {
  const { user } = useAuth()
  const employee = user?.role === 'EMPLOYEE'
  const [params, setParams] = useSearchParams()
  const [page, setPage] = useState(0)
  const filters = useMemo(() => Object.fromEntries(FILTER_KEYS.map((k) => [k, params.get(k) ?? ''])), [params])
  const set = (k: string, v: string) => { const p = new URLSearchParams(params); if (v) p.set(k, v); else p.delete(k); setParams(p, { replace: true }); setPage(0) }
  const active = FILTER_KEYS.filter((k) => filters[k]).length

  const { data: services } = useQuery({ queryKey: ['services'], queryFn: () => get<ServiceView[]>('/catalog/services') })
  const { data: categories } = useQuery({ queryKey: ['categories'], queryFn: () => get<CategoryView[]>('/catalog/categories') })
  const { data: techs } = useQuery({ queryKey: ['technicians'], queryFn: () => get<UserView[]>('/technicians'), enabled: !employee })
  const { data, isLoading, isFetching } = useQuery({
    queryKey: ['incidents', 'search', filters, page],
    queryFn: () => get<Page<IncidentSummary>>('/incidents', { ...filters, page, size: 20 }),
    placeholderData: keepPreviousData,
  })

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={employee ? 'My incidents' : 'All incidents'}
        description={employee ? 'Track everything you have reported.' : 'Search and filter across every incident in the organisation.'}
        actions={<Button asChild><Link to="/app/incidents/new"><Plus />{employee ? 'Report incident' : 'Log incident'}</Link></Button>}
      />

      <div className="flex flex-col gap-2">
        <div className="grid grid-cols-2 gap-2 md:grid-cols-4 xl:grid-cols-8">
          <Input className="col-span-2" placeholder="Search title or description…" value={filters.q} onChange={(e) => set('q', e.target.value)} />
          <Input placeholder="INC1024" value={filters.number} onChange={(e) => set('number', e.target.value)} className="font-mono" />
          <NativeSelect value={filters.status} onChange={(e) => set('status', e.target.value)}>
            <option value="">Any status</option>
            {STATUSES.map((s) => <option key={s} value={s}>{statusLabel(s)}</option>)}
          </NativeSelect>
          <NativeSelect value={filters.priority} onChange={(e) => set('priority', e.target.value)}>
            <option value="">Any priority</option>
            {['P1', 'P2', 'P3', 'P4'].map((p) => <option key={p}>{p}</option>)}
          </NativeSelect>
          <NativeSelect value={filters.categoryId} onChange={(e) => set('categoryId', e.target.value)}>
            <option value="">Any category</option>
            {categories?.map((c) => <option key={c.id} value={c.id}>{c.name.toLowerCase()}</option>)}
          </NativeSelect>
          <NativeSelect value={filters.serviceId} onChange={(e) => set('serviceId', e.target.value)}>
            <option value="">Any service</option>
            {services?.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </NativeSelect>
          <NativeSelect value={filters.slaStatus} onChange={(e) => set('slaStatus', e.target.value)}>
            <option value="">Any SLA</option>
            {['ON_TRACK', 'AT_RISK', 'BREACHED', 'MET', 'PAUSED'].map((s) => <option key={s} value={s}>{statusLabel(s)}</option>)}
          </NativeSelect>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {!employee && (
            <div className="w-48">
              <NativeSelect value={filters.technicianId} onChange={(e) => set('technicianId', e.target.value)}>
                <option value="">Any technician</option>
                {techs?.map((t) => <option key={t.id} value={t.id}>{t.fullName} ({t.code})</option>)}
              </NativeSelect>
            </div>
          )}
          <label className="flex items-center gap-1.5 text-[13px] text-muted">From <Input type="date" className="h-8 w-36" value={filters.from} onChange={(e) => set('from', e.target.value)} /></label>
          <label className="flex items-center gap-1.5 text-[13px] text-muted">To <Input type="date" className="h-8 w-36" value={filters.to} onChange={(e) => set('to', e.target.value)} /></label>
          {[['openOnly', 'Open only'], ...(employee ? [] : [['unassigned', 'Unassigned'], ['majorOnly', 'Major incidents']])].map(([k, label]) => (
            <button key={k} onClick={() => set(k, filters[k] ? '' : 'true')}
              className={`h-8 rounded-full border px-3 text-[13px] cursor-pointer transition-colors ${filters[k] ? 'border-brand bg-brand text-brand-fg' : 'border-border text-muted hover:border-border-strong hover:text-foreground'}`}>
              {label}
            </button>
          ))}
          {active > 0 && <Button variant="ghost" size="sm" onClick={() => { setParams({}, { replace: true }); setPage(0) }}><X />Clear {active}</Button>}
          <span className="ml-auto text-xs text-muted">{isFetching ? 'Loading…' : `${data?.totalElements ?? 0} incidents`}</span>
        </div>
      </div>

      <Card>
        {data && data.content.length === 0 && !isLoading ? (
          <EmptyState icon={<Inbox />} title="No incidents found" description={active ? 'Try removing some filters.' : 'Nothing has been reported yet.'} />
        ) : (
          <IncidentTable items={data?.content} loading={isLoading} showReporter={!employee} showTechnician />
        )}
        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-border px-4 py-2.5 text-xs text-muted">
            <span>Page {data.page + 1} of {data.totalPages}</span>
            <div className="flex gap-1">
              <Button variant="secondary" size="xs" disabled={page === 0} onClick={() => setPage(page - 1)}><ChevronLeft />Prev</Button>
              <Button variant="secondary" size="xs" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Next<ChevronRight /></Button>
            </div>
          </div>
        )}
      </Card>
    </div>
  )
}
