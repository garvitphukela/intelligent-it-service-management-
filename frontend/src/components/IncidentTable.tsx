import { Link } from 'react-router-dom'
import { AlertTriangle, Copy } from 'lucide-react'
import { Table, TD, TH, THead, TR } from '@/components/ui/table'
import { Skeleton } from '@/components/ui/misc'
import { PriorityBadge, SlaCountdown, StatusBadge } from './domain'
import { relativeTime } from '@/lib/format'
import type { IncidentSummary } from '@/lib/types'

export function IncidentTable({ items, loading, showReporter = true, showTechnician = true }: {
  items?: IncidentSummary[]; loading?: boolean; showReporter?: boolean; showTechnician?: boolean
}) {
  return (
    <Table>
      <THead>
        <tr>
          <TH className="w-24">ID</TH>
          <TH>Incident</TH>
          <TH>Priority</TH>
          <TH>Status</TH>
          <TH className="hidden md:table-cell">Service</TH>
          {showTechnician && <TH className="hidden lg:table-cell">Assignee</TH>}
          {showReporter && <TH className="hidden xl:table-cell">Reporter</TH>}
          <TH>SLA</TH>
          <TH className="hidden sm:table-cell text-right">Created</TH>
        </tr>
      </THead>
      <tbody>
        {loading && Array.from({ length: 6 }).map((_, i) => (
          <TR key={i}><TD colSpan={9}><Skeleton className="h-4 w-full" /></TD></TR>
        ))}
        {!loading && items?.map((i) => (
          <TR key={i.id} className="group hover:bg-surface">
            <TD className="font-mono text-xs text-muted">
              <Link to={`/app/incidents/${i.id}`} className="hover:text-foreground">{i.number}</Link>
            </TD>
            <TD className="max-w-[340px]">
              <Link to={`/app/incidents/${i.id}`} className="flex items-center gap-1.5 font-medium group-hover:underline underline-offset-4 decoration-border-strong">
                {i.majorIncident && <AlertTriangle className="size-3.5 shrink-0 text-red" aria-label="Major incident" />}
                {i.masterNumber && <Copy className="size-3.5 shrink-0 text-muted" aria-label={`Duplicate of ${i.masterNumber}`} />}
                <span className="truncate">{i.title}</span>
              </Link>
              <span className="text-xs text-muted">{i.category ? i.category.toLowerCase() : '—'}{i.escalationLevel > 0 && <span className="ml-1.5 text-red">· escalated L{i.escalationLevel}</span>}</span>
            </TD>
            <TD><PriorityBadge priority={i.priority} /></TD>
            <TD><StatusBadge status={i.status} /></TD>
            <TD className="hidden md:table-cell text-[13px] text-muted">{i.service}</TD>
            {showTechnician && <TD className="hidden lg:table-cell text-[13px]">{i.technicianName ? <span>{i.technicianName} <span className="font-mono text-xs text-subtle">{i.technicianCode}</span></span> : <span className="text-subtle">Unassigned</span>}</TD>}
            {showReporter && <TD className="hidden xl:table-cell text-[13px] text-muted">{i.reporterName}</TD>}
            <TD><SlaCountdown responseDueAt={i.responseDueAt} resolutionDueAt={i.resolutionDueAt} status={i.slaStatus} incidentStatus={i.status} compact /></TD>
            <TD className="hidden sm:table-cell text-right text-xs text-muted whitespace-nowrap">{relativeTime(i.createdAt)}</TD>
          </TR>
        ))}
      </tbody>
    </Table>
  )
}
