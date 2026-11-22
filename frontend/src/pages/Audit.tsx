import { useState } from 'react'
import { Link } from 'react-router-dom'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { get } from '@/lib/api'
import type { AuditView, Page } from '@/lib/types'
import { Card } from '@/components/ui/card'
import { Input, NativeSelect } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Table, TD, TH, THead, TR } from '@/components/ui/table'
import { PageHeader } from '@/components/domain'
import { dateTime } from '@/lib/format'

const ACTIONS = ['INCIDENT_CREATED', 'INCIDENT_CLASSIFIED', 'PRIORITY_CALCULATED', 'PRIORITY_CHANGED', 'INCIDENT_ASSIGNED', 'INCIDENT_REASSIGNED', 'INCIDENT_ACKNOWLEDGED',
  'INCIDENT_WORK_STARTED', 'INCIDENT_INFO_REQUESTED', 'INCIDENT_INFO_PROVIDED', 'INCIDENT_RESOLVED', 'INCIDENT_VERIFIED', 'INCIDENT_REOPENED', 'INCIDENT_CLOSED', 'INCIDENT_CANCELLED',
  'NOTE_ADDED', 'SLA_APPROACHING', 'SLA_VIOLATED', 'INCIDENT_ESCALATED', 'DUPLICATE_LINKED', 'MAJOR_INCIDENT_DETECTED', 'FEEDBACK_SUBMITTED', 'INCIDENT_UPDATED']

export function AuditPage() {
  const [f, setF] = useState({ action: '', actor: '', incident: '' })
  const [page, setPage] = useState(0)
  const { data } = useQuery({ queryKey: ['audit', f, page], queryFn: () => get<Page<AuditView>>('/audit', { ...f, page, size: 50 }), placeholderData: keepPreviousData })
  const set = (k: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => { setF({ ...f, [k]: e.target.value }); setPage(0) }
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Audit log" description="Immutable history of every important action across all incidents." />
      <div className="grid gap-2 sm:grid-cols-3">
        <NativeSelect value={f.action} onChange={set('action')}><option value="">Any action</option>{ACTIONS.map((a) => <option key={a}>{a}</option>)}</NativeSelect>
        <Input placeholder="User name…" value={f.actor} onChange={set('actor')} />
        <Input placeholder="Incident (INC1024)" className="font-mono" value={f.incident} onChange={set('incident')} />
      </div>
      <Card>
        <Table>
          <THead><tr><TH>Time</TH><TH>Incident</TH><TH>User</TH><TH>Action</TH><TH className="hidden md:table-cell">State</TH><TH className="hidden lg:table-cell">Description</TH></tr></THead>
          <tbody>
            {data?.content.map((a) => (
              <TR key={a.id} className="align-top">
                <TD className="whitespace-nowrap font-mono text-xs text-muted">{dateTime(a.timestamp)}</TD>
                <TD className="font-mono text-xs">{a.incidentId ? <Link className="hover:underline" to={`/app/incidents/${a.incidentId}`}>{a.incidentNumber}</Link> : '—'}</TD>
                <TD className="whitespace-nowrap text-[13px]">{a.actorName}</TD>
                <TD className="font-mono text-[11px]">{a.action}</TD>
                <TD className="hidden md:table-cell whitespace-nowrap font-mono text-[11px] text-muted">{a.previousState || a.newState ? `${a.previousState ?? '·'} → ${a.newState ?? '·'}` : '—'}</TD>
                <TD className="hidden lg:table-cell max-w-lg py-2.5 text-[13px] text-muted">{a.description}</TD>
              </TR>
            ))}
          </tbody>
        </Table>
        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-border px-4 py-2.5 text-xs text-muted">
            <span>{data.totalElements} entries · page {data.page + 1} of {data.totalPages}</span>
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
