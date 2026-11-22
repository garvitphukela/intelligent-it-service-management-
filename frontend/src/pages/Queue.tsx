import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { ListTodo } from 'lucide-react'
import { get, post } from '@/lib/api'
import { useAuth } from '@/lib/auth'
import type { IncidentSummary, Page, UserView, WorkloadView } from '@/lib/types'
import { Card, CardHeader } from '@/components/ui/card'
import { EmptyState, Stat, Switch } from '@/components/ui/misc'
import { IncidentTable } from '@/components/IncidentTable'
import { PageHeader } from '@/components/domain'
import { Button } from '@/components/ui/button'
import { minutes } from '@/lib/format'
import { Meter } from './Dashboard'

export function QueuePage() {
  const { user, refresh } = useAuth()
  const qc = useQueryClient()
  const { data } = useQuery({ queryKey: ['workload', 'me'], queryFn: () => get<WorkloadView>('/technicians/me/workload'), refetchInterval: 20000 })
  const unassigned = useQuery({ queryKey: ['incidents', 'unassigned'], queryFn: () => get<Page<IncidentSummary>>('/incidents', { unassigned: true, openOnly: true, size: 10 }), enabled: user?.role === 'TEAM_LEAD' })
  const toggle = useMutation({
    mutationFn: (available: boolean) => post<UserView>('/technicians/me/availability', { available }),
    onSuccess: (u) => { qc.invalidateQueries({ queryKey: ['workload'] }); refresh(); toast.success(u.available ? 'You are available for new incidents' : 'Paused new assignments') },
  })
  const available = data?.technician.available ?? true
  const awaiting = data?.incidents.filter((i) => i.status === 'ASSIGNED').length ?? 0

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="My queue" description="Incidents assigned to you, most critical first."
        actions={<label className="flex h-9 items-center gap-2.5 rounded-md border border-border px-3 text-[13px]"><Switch checked={available} onCheckedChange={(v) => toggle.mutate(v)} />{available ? 'Available' : 'Unavailable'}</label>} />
      <Card className="grid grid-cols-2 sm:grid-cols-5 [&>*]:border-border sm:[&>*:not(:last-child)]:border-r max-sm:[&>*:nth-child(odd)]:border-r max-sm:[&>*:nth-child(-n+4)]:border-b">
        <div className="flex flex-col gap-2 p-4">
          <span className="text-[12px] text-muted">Workload</span>
          <span className="font-mono text-2xl font-semibold">{data?.active ?? '–'}<span className="text-base text-subtle">/{data?.maxWorkload ?? '–'}</span></span>
          {data && <Meter value={data.active / data.maxWorkload} tone={data.active >= data.maxWorkload ? 'red' : undefined} />}
        </div>
        <Stat label="Need acknowledgement" value={awaiting} tone={awaiting ? 'amber' : undefined} />
        <Stat label="Resolved (all time)" value={data?.resolvedTotal ?? '–'} />
        <Stat label="Avg resolution" value={minutes(data?.avgResolutionMinutes)} />
        <Stat label="Avg rating" value={data?.avgRating != null ? `${data.avgRating}/5` : '—'} />
      </Card>
      <Card>
        <CardHeader title="Assigned to me" description={`${data?.incidents.length ?? 0} active`} />
        {data && data.incidents.length === 0 ? <EmptyState icon={<ListTodo />} title="Queue is clear" description="New incidents matching your skills will be routed to you automatically." /> :
          <IncidentTable items={data?.incidents} loading={!data} showTechnician={false} />}
      </Card>
      {user?.role === 'TEAM_LEAD' && (
        <Card>
          <CardHeader title="Unassigned incidents" description="Waiting for an engineer" action={<Button variant="link" size="xs" asChild><Link to="/app/incidents?unassigned=true&openOnly=true">View all</Link></Button>} />
          {unassigned.data?.content.length === 0 ? <EmptyState title="Nothing waiting" /> : <IncidentTable items={unassigned.data?.content} loading={unassigned.isLoading} showTechnician={false} />}
        </Card>
      )}
    </div>
  )
}
