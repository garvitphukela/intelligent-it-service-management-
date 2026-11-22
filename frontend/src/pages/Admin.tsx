import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { Plus, Pencil, Trash2, Check, Mail, MessageSquare, Bell } from 'lucide-react'
import { ApiError, del, get, post, put } from '@/lib/api'
import type { CategoryView, NotificationView, Page, Priority, Role, ServiceView, SettingsView, SlaPolicyView, UserView } from '@/lib/types'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Field, Input, NativeSelect, Textarea } from '@/components/ui/input'
import { Dialog, DialogContent, DialogFooter } from '@/components/ui/dialog'
import { Switch } from '@/components/ui/misc'
import { Table, TD, TH, THead, TR } from '@/components/ui/table'
import { CriticalityDots, PageHeader, PriorityBadge } from '@/components/domain'
import { dateTime, minutes, titleCase } from '@/lib/format'
import { cn } from '@/lib/utils'

function useSave<T>(fn: (body: T) => Promise<unknown>, keys: string[][], ok: string, after?: () => void) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: () => { keys.forEach((k) => qc.invalidateQueries({ queryKey: k })); toast.success(ok); after?.() },
    onError: (e: ApiError) => toast.error(e.message, { description: e.fieldErrors ? Object.entries(e.fieldErrors).map(([k, v]) => `${k}: ${v}`).join(', ') : e.code }),
  })
}

// ============================================================ users

const ROLES: Role[] = ['EMPLOYEE', 'TECHNICIAN', 'TEAM_LEAD', 'ADMIN', 'MANAGER']
const SKILLS = ['HARDWARE', 'SOFTWARE', 'NETWORK', 'DATABASE', 'CLOUD', 'SECURITY', 'AUTHENTICATION', 'APPLICATION', 'STORAGE']

export function UsersPage() {
  const [role, setRole] = useState<string>('')
  const [editing, setEditing] = useState<UserView | 'new' | null>(null)
  const { data } = useQuery({ queryKey: ['admin', 'users', role], queryFn: () => get<UserView[]>('/admin/users', { role }) })
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Users & technicians" description="Manage accounts, roles, skills and capacity." actions={<Button onClick={() => setEditing('new')}><Plus />New user</Button>} />
      <div className="flex flex-wrap gap-1.5">
        {['', ...ROLES].map((r) => (
          <button key={r} onClick={() => setRole(r)} className={cn('h-8 rounded-full border px-3 text-[13px] cursor-pointer', role === r ? 'border-brand bg-brand text-brand-fg' : 'border-border text-muted hover:text-foreground')}>{r ? titleCase(r) : 'All'}</button>
        ))}
      </div>
      <Card>
        <Table>
          <THead><tr><TH>Code</TH><TH>Name</TH><TH>Role</TH><TH className="hidden md:table-cell">Skills</TH><TH className="hidden sm:table-cell">Load</TH><TH>Status</TH><TH /></tr></THead>
          <tbody>
            {data?.map((u) => (
              <TR key={u.id} className="hover:bg-surface">
                <TD className="font-mono text-xs text-muted">{u.code}</TD>
                <TD><p className="text-[13px] font-medium">{u.fullName}</p><p className="text-xs text-muted">{u.email}</p></TD>
                <TD><Badge tone={u.role === 'EMPLOYEE' ? 'outline' : 'neutral'}>{u.title}</Badge></TD>
                <TD className="hidden md:table-cell"><div className="flex max-w-xs flex-wrap gap-1">{u.skills?.map((s) => <Badge key={s} tone="outline">{s.toLowerCase()}</Badge>) ?? <span className="text-subtle">—</span>}</div></TD>
                <TD className="hidden sm:table-cell font-mono text-xs">{u.maxWorkload ? `${u.workload ?? 0}/${u.maxWorkload}` : '—'}{u.experienceYears ? <span className="text-subtle"> · {u.experienceYears}y</span> : null}</TD>
                <TD>{u.active ? (u.available === false ? <Badge tone="amber">Unavailable</Badge> : <Badge tone="green" dot>Active</Badge>) : <Badge tone="outline">Deactivated</Badge>}</TD>
                <TD className="text-right"><Button variant="ghost" size="icon-sm" onClick={() => setEditing(u)} aria-label="Edit"><Pencil /></Button></TD>
              </TR>
            ))}
          </tbody>
        </Table>
      </Card>
      {editing && <UserDialog user={editing === 'new' ? null : editing} onClose={() => setEditing(null)} />}
    </div>
  )
}

function UserDialog({ user, onClose }: { user: UserView | null; onClose: () => void }) {
  const [f, setF] = useState({
    role: (user?.role ?? 'EMPLOYEE') as Role, fullName: user?.fullName ?? '', email: user?.email ?? '', password: '', phone: user?.phone ?? '', department: user?.department ?? '',
    skills: new Set(user?.skills ?? []), experienceYears: String(user?.experienceYears ?? 2), maxWorkload: String(user?.maxWorkload ?? 5),
    active: user?.active ?? true, available: user?.available ?? true,
  })
  const tech = f.role === 'TECHNICIAN' || f.role === 'TEAM_LEAD'
  const keys = [['admin', 'users'], ['technicians'], ['reports']]
  const create = useSave(() => post('/admin/users', { ...f, skills: [...f.skills], experienceYears: Number(f.experienceYears), maxWorkload: Number(f.maxWorkload) }), keys, 'User created', onClose)
  const update = useSave(() => put(`/admin/users/${user!.id}`, {
    fullName: f.fullName, phone: f.phone, department: f.department, active: f.active, available: tech ? f.available : undefined,
    skills: tech ? [...f.skills] : undefined, experienceYears: tech ? Number(f.experienceYears) : undefined, maxWorkload: tech ? Number(f.maxWorkload) : undefined,
    newPassword: f.password || undefined,
  }), keys, 'User updated', onClose)
  const toggleSkill = (s: string) => { const n = new Set(f.skills); n.has(s) ? n.delete(s) : n.add(s); setF({ ...f, skills: n }) }
  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent wide title={user ? `Edit ${user.fullName}` : 'New user'} description={user ? `${user.code} · ${user.title}` : 'Staff accounts are created here; employees can also self-register.'}>
        <div className="grid gap-3 sm:grid-cols-2">
          {!user && <Field label="Role"><NativeSelect value={f.role} onChange={(e) => setF({ ...f, role: e.target.value as Role })}>{ROLES.map((r) => <option key={r} value={r}>{titleCase(r)}</option>)}</NativeSelect></Field>}
          <Field label="Full name"><Input value={f.fullName} onChange={(e) => setF({ ...f, fullName: e.target.value })} /></Field>
          {!user && <Field label="Email"><Input type="email" value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} /></Field>}
          <Field label={user ? 'Reset password' : 'Password'} hint="8+ chars, letters and digits"><Input type="password" value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} placeholder={user ? 'Leave blank to keep' : ''} /></Field>
          <Field label="Phone"><Input value={f.phone} onChange={(e) => setF({ ...f, phone: e.target.value })} /></Field>
          <Field label="Department"><Input value={f.department} onChange={(e) => setF({ ...f, department: e.target.value })} /></Field>
        </div>
        {tech && (
          <div className="mt-4 flex flex-col gap-3 border-t border-border pt-4">
            <Field label="Skills"><div className="flex flex-wrap gap-1.5">{SKILLS.map((s) => (
              <button key={s} type="button" onClick={() => toggleSkill(s)} className={cn('h-7 rounded-full border px-2.5 text-xs cursor-pointer', f.skills.has(s) ? 'border-brand bg-brand text-brand-fg' : 'border-border text-muted hover:text-foreground')}>{s.toLowerCase()}</button>
            ))}</div></Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Experience (years)"><Input type="number" min={0} max={50} value={f.experienceYears} onChange={(e) => setF({ ...f, experienceYears: e.target.value })} /></Field>
              <Field label="Max concurrent incidents"><Input type="number" min={1} max={50} value={f.maxWorkload} onChange={(e) => setF({ ...f, maxWorkload: e.target.value })} /></Field>
            </div>
          </div>
        )}
        {user && (
          <div className="mt-4 flex flex-wrap gap-6 border-t border-border pt-4 text-[13px]">
            <label className="flex items-center gap-2"><Switch checked={f.active} onCheckedChange={(v) => setF({ ...f, active: v })} />Account active</label>
            {tech && <label className="flex items-center gap-2"><Switch checked={f.available} onCheckedChange={(v) => setF({ ...f, available: v })} />Available for assignment</label>}
          </div>
        )}
        <DialogFooter>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button loading={create.isPending || update.isPending} onClick={() => (user ? update.mutate(undefined) : create.mutate(undefined))}>{user ? 'Save' : 'Create user'}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ============================================================ services

export function ServicesPage() {
  const [editing, setEditing] = useState<ServiceView | 'new' | null>(null)
  const { data } = useQuery({ queryKey: ['admin', 'services'], queryFn: () => get<ServiceView[]>('/admin/services') })
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="IT services" description="Business services, their criticality and dependencies (used for priority and correlation)." actions={<Button onClick={() => setEditing('new')}><Plus />New service</Button>} />
      <Card>
        <Table>
          <THead><tr><TH>Service</TH><TH>Criticality</TH><TH className="hidden sm:table-cell text-right">Users</TH><TH className="hidden md:table-cell">Depends on</TH><TH className="hidden lg:table-cell">Owner</TH><TH /></tr></THead>
          <tbody>
            {data?.map((s) => (
              <TR key={s.id} className="hover:bg-surface">
                <TD><p className="text-[13px] font-medium">{s.name} {!s.active && <Badge tone="outline">retired</Badge>}</p><p className="max-w-sm truncate text-xs text-muted">{s.description}</p></TD>
                <TD><span className="flex items-center gap-2"><CriticalityDots value={s.businessCriticality} /><span className="font-mono text-xs">{s.businessCriticality}</span></span></TD>
                <TD className="hidden sm:table-cell text-right font-mono text-xs">{s.userBase.toLocaleString()}</TD>
                <TD className="hidden md:table-cell"><div className="flex flex-wrap gap-1">{s.dependsOn.length ? s.dependsOn.map((d) => <Badge key={d.id} tone="outline">{d.name}</Badge>) : <span className="text-subtle">—</span>}</div></TD>
                <TD className="hidden lg:table-cell text-[13px] text-muted">{s.ownerTeam}</TD>
                <TD className="text-right"><Button variant="ghost" size="icon-sm" onClick={() => setEditing(s)} aria-label="Edit"><Pencil /></Button></TD>
              </TR>
            ))}
          </tbody>
        </Table>
      </Card>
      {editing && <ServiceDialog service={editing === 'new' ? null : editing} all={data ?? []} onClose={() => setEditing(null)} />}
    </div>
  )
}

function ServiceDialog({ service, all, onClose }: { service: ServiceView | null; all: ServiceView[]; onClose: () => void }) {
  const [f, setF] = useState({ name: service?.name ?? '', description: service?.description ?? '', businessCriticality: String(service?.businessCriticality ?? 3), userBase: String(service?.userBase ?? 100), ownerTeam: service?.ownerTeam ?? '', active: service?.active ?? true, deps: new Set(service?.dependsOn.map((d) => d.id) ?? []) })
  const body = () => ({ ...f, businessCriticality: Number(f.businessCriticality), userBase: Number(f.userBase), dependsOnIds: [...f.deps] })
  const keys = [['admin', 'services'], ['services']]
  const save = useSave(() => service ? put(`/admin/services/${service.id}`, body()) : post('/admin/services', body()), keys, service ? 'Service updated' : 'Service created', onClose)
  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent wide title={service ? `Edit ${service.name}` : 'New IT service'}>
        <div className="grid gap-3 sm:grid-cols-2">
          <Field label="Name"><Input value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} /></Field>
          <Field label="Owner team"><Input value={f.ownerTeam} onChange={(e) => setF({ ...f, ownerTeam: e.target.value })} /></Field>
          <Field label="Description" className="sm:col-span-2"><Textarea rows={2} value={f.description} onChange={(e) => setF({ ...f, description: e.target.value })} /></Field>
          <Field label="Business criticality (1–5)"><NativeSelect value={f.businessCriticality} onChange={(e) => setF({ ...f, businessCriticality: e.target.value })}>{[1, 2, 3, 4, 5].map((n) => <option key={n}>{n}</option>)}</NativeSelect></Field>
          <Field label="User base"><Input type="number" min={0} value={f.userBase} onChange={(e) => setF({ ...f, userBase: e.target.value })} /></Field>
        </div>
        <Field label="Depends on" className="mt-4" hint="Circular dependencies are rejected."><div className="flex flex-wrap gap-1.5">
          {all.filter((s) => s.id !== service?.id).map((s) => (
            <button key={s.id} type="button" onClick={() => { const n = new Set(f.deps); n.has(s.id) ? n.delete(s.id) : n.add(s.id); setF({ ...f, deps: n }) }}
              className={cn('h-7 rounded-full border px-2.5 text-xs cursor-pointer', f.deps.has(s.id) ? 'border-brand bg-brand text-brand-fg' : 'border-border text-muted hover:text-foreground')}>{s.name}</button>
          ))}
        </div></Field>
        {service && <label className="mt-4 flex items-center gap-2 text-[13px]"><Switch checked={f.active} onCheckedChange={(v) => setF({ ...f, active: v })} />Active</label>}
        <DialogFooter><Button variant="secondary" onClick={onClose}>Cancel</Button><Button loading={save.isPending} onClick={() => save.mutate(undefined)}>Save</Button></DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ============================================================ categories

export function CategoriesPage() {
  const [editing, setEditing] = useState<CategoryView | 'new' | null>(null)
  const { data } = useQuery({ queryKey: ['admin', 'categories'], queryFn: () => get<CategoryView[]>('/admin/categories') })
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Incident categories" description="Keywords drive auto-categorisation; the required skill drives assignment. New categories need no code changes." actions={<Button onClick={() => setEditing('new')}><Plus />New category</Button>} />
      <Card>
        <Table>
          <THead><tr><TH>Category</TH><TH>Required skill</TH><TH className="hidden md:table-cell">Keywords</TH><TH /></tr></THead>
          <tbody>
            {data?.map((c) => (
              <TR key={c.id} className="hover:bg-surface">
                <TD><p className="text-[13px] font-medium">{c.name.toLowerCase()} {!c.active && <Badge tone="outline">inactive</Badge>}</p><p className="text-xs text-muted">{c.description}</p></TD>
                <TD><Badge tone="neutral" className="font-mono">{c.requiredSkill}</Badge></TD>
                <TD className="hidden md:table-cell"><p className="max-w-md truncate text-xs text-muted">{c.keywords.join(', ')}</p></TD>
                <TD className="text-right"><Button variant="ghost" size="icon-sm" onClick={() => setEditing(c)} aria-label="Edit"><Pencil /></Button></TD>
              </TR>
            ))}
          </tbody>
        </Table>
      </Card>
      {editing && <CategoryDialog category={editing === 'new' ? null : editing} onClose={() => setEditing(null)} />}
    </div>
  )
}

function CategoryDialog({ category, onClose }: { category: CategoryView | null; onClose: () => void }) {
  const [f, setF] = useState({ name: category?.name ?? '', description: category?.description ?? '', requiredSkill: category?.requiredSkill ?? '', keywords: category?.keywords.join(', ') ?? '', active: category?.active ?? true })
  const body = () => ({ ...f, keywords: f.keywords.split(',').map((k) => k.trim()).filter(Boolean) })
  const save = useSave(() => category ? put(`/admin/categories/${category.id}`, body()) : post('/admin/categories', body()), [['admin', 'categories'], ['categories']], 'Category saved', onClose)
  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent title={category ? `Edit ${category.name.toLowerCase()}` : 'New category'}>
        <div className="flex flex-col gap-3">
          <div className="grid grid-cols-2 gap-3">
            <Field label="Name"><Input value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} /></Field>
            <Field label="Required skill" hint="Defaults to the name"><Input value={f.requiredSkill} onChange={(e) => setF({ ...f, requiredSkill: e.target.value })} /></Field>
          </div>
          <Field label="Description"><Input value={f.description} onChange={(e) => setF({ ...f, description: e.target.value })} /></Field>
          <Field label="Keywords" hint="Comma separated. Multi-word phrases allowed."><Textarea rows={3} value={f.keywords} onChange={(e) => setF({ ...f, keywords: e.target.value })} /></Field>
          {category && <label className="flex items-center gap-2 text-[13px]"><Switch checked={f.active} onCheckedChange={(v) => setF({ ...f, active: v })} />Active</label>}
        </div>
        <DialogFooter><Button variant="secondary" onClick={onClose}>Cancel</Button><Button loading={save.isPending} onClick={() => save.mutate(undefined)}>Save</Button></DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ============================================================ SLA policies

export function SlaPage() {
  const [editing, setEditing] = useState<SlaPolicyView | 'new' | null>(null)
  const { data } = useQuery({ queryKey: ['admin', 'sla'], queryFn: () => get<SlaPolicyView[]>('/admin/sla-policies') })
  const remove = useSave((id: number) => del(`/admin/sla-policies/${id}`), [['admin', 'sla']], 'Policy deleted')
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="SLA policies" description="Response and resolution targets per priority. Service-specific policies override the defaults." actions={<Button onClick={() => setEditing('new')}><Plus />New policy</Button>} />
      <Card>
        <Table>
          <THead><tr><TH>Policy</TH><TH>Scope</TH><TH>Priority</TH><TH className="text-right">Response</TH><TH className="text-right">Resolution</TH><TH className="text-right hidden sm:table-cell">Warn at</TH><TH /></tr></THead>
          <tbody>
            {data?.map((p) => (
              <TR key={p.id} className="hover:bg-surface">
                <TD className="text-[13px] font-medium">{p.name}</TD>
                <TD className="text-[13px] text-muted">{p.serviceName}</TD>
                <TD><PriorityBadge priority={p.priority} /></TD>
                <TD className="text-right font-mono text-xs">{minutes(p.responseMinutes)}</TD>
                <TD className="text-right font-mono text-xs">{minutes(p.resolutionMinutes)}</TD>
                <TD className="text-right font-mono text-xs hidden sm:table-cell">{p.warningThresholdPercent}%</TD>
                <TD className="text-right whitespace-nowrap">
                  <Button variant="ghost" size="icon-sm" onClick={() => setEditing(p)} aria-label="Edit"><Pencil /></Button>
                  {p.serviceId && <Button variant="ghost" size="icon-sm" onClick={() => remove.mutate(p.id)} aria-label="Delete"><Trash2 /></Button>}
                </TD>
              </TR>
            ))}
          </tbody>
        </Table>
      </Card>
      {editing && <SlaDialog policy={editing === 'new' ? null : editing} onClose={() => setEditing(null)} />}
    </div>
  )
}

function SlaDialog({ policy, onClose }: { policy: SlaPolicyView | null; onClose: () => void }) {
  const { data: services } = useQuery({ queryKey: ['admin', 'services'], queryFn: () => get<ServiceView[]>('/admin/services') })
  const [f, setF] = useState({ name: policy?.name ?? '', serviceId: policy?.serviceId ? String(policy.serviceId) : '', priority: (policy?.priority ?? 'P2') as Priority, responseMinutes: String(policy?.responseMinutes ?? 15), resolutionMinutes: String(policy?.resolutionMinutes ?? 120), warningThresholdPercent: String(policy?.warningThresholdPercent ?? 75) })
  const body = () => ({ ...f, serviceId: f.serviceId ? Number(f.serviceId) : null, responseMinutes: Number(f.responseMinutes), resolutionMinutes: Number(f.resolutionMinutes), warningThresholdPercent: Number(f.warningThresholdPercent) })
  const save = useSave(() => policy ? put(`/admin/sla-policies/${policy.id}`, body()) : post('/admin/sla-policies', body()), [['admin', 'sla']], 'Policy saved', onClose)
  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent title={policy ? 'Edit SLA policy' : 'New SLA policy'}>
        <div className="grid gap-3 sm:grid-cols-2">
          <Field label="Name" className="sm:col-span-2"><Input value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} /></Field>
          <Field label="Service"><NativeSelect disabled={!!policy} value={f.serviceId} onChange={(e) => setF({ ...f, serviceId: e.target.value })}><option value="">All services (default)</option>{services?.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}</NativeSelect></Field>
          <Field label="Priority"><NativeSelect disabled={!!policy} value={f.priority} onChange={(e) => setF({ ...f, priority: e.target.value as Priority })}>{['P1', 'P2', 'P3', 'P4'].map((p) => <option key={p}>{p}</option>)}</NativeSelect></Field>
          <Field label="Response target (minutes)"><Input type="number" min={1} value={f.responseMinutes} onChange={(e) => setF({ ...f, responseMinutes: e.target.value })} /></Field>
          <Field label="Resolution target (minutes)"><Input type="number" min={1} value={f.resolutionMinutes} onChange={(e) => setF({ ...f, resolutionMinutes: e.target.value })} /></Field>
          <Field label="Warning threshold (%)"><Input type="number" min={10} max={99} value={f.warningThresholdPercent} onChange={(e) => setF({ ...f, warningThresholdPercent: e.target.value })} /></Field>
        </div>
        <DialogFooter><Button variant="secondary" onClick={onClose}>Cancel</Button><Button loading={save.isPending} onClick={() => save.mutate(undefined)}>Save</Button></DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ============================================================ strategies

export function SettingsPage() {
  const { data } = useQuery({ queryKey: ['admin', 'settings'], queryFn: () => get<SettingsView>('/admin/settings') })
  const save = useSave((body: Partial<{ assignmentStrategy: string; priorityStrategy: string; autoAssign: boolean }>) => put('/admin/settings', body), [['admin', 'settings']], 'Settings saved')
  if (!data) return null
  const Option = ({ active, name, description, onClick }: { active: boolean; name: string; description: string; onClick: () => void }) => (
    <button onClick={onClick} className={cn('flex items-start gap-3 rounded-lg border p-4 text-left transition-colors cursor-pointer', active ? 'border-brand bg-brand-soft' : 'border-border hover:border-border-strong')}>
      <span className={cn('mt-0.5 flex size-4 shrink-0 items-center justify-center rounded-full border', active ? 'border-brand bg-brand text-brand-fg' : 'border-border-strong')}>{active && <Check className="size-3" />}</span>
      <span><span className="block text-sm font-medium">{name}</span><span className="mt-0.5 block text-[13px] text-muted">{description}</span></span>
    </button>
  )
  return (
    <div className="flex flex-col gap-8">
      <PageHeader title="Strategies" description="Swap algorithms at runtime (Strategy pattern). Changes apply to the next incident." />
      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <div><h2 className="text-sm font-semibold">Technician assignment</h2><p className="text-[13px] text-muted">How new incidents are routed to engineers.</p></div>
          <label className="flex items-center gap-2 text-[13px]"><Switch checked={data.autoAssign} onCheckedChange={(v) => save.mutate({ autoAssign: v })} />Auto-assign on creation</label>
        </div>
        <div className="grid gap-3 md:grid-cols-2">
          {data.assignmentStrategies.map((s) => <Option key={s.key} active={data.assignmentStrategy === s.key} name={s.name} description={s.description} onClick={() => save.mutate({ assignmentStrategy: s.key })} />)}
        </div>
      </section>
      <section className="flex flex-col gap-3">
        <div><h2 className="text-sm font-semibold">Priority calculation</h2><p className="text-[13px] text-muted">How impact, urgency and business criticality combine into P1–P4.</p></div>
        <div className="grid gap-3 md:grid-cols-2">
          {data.priorityStrategies.map((s) => <Option key={s.key} active={data.priorityStrategy === s.key} name={s.name} description={s.description} onClick={() => save.mutate({ priorityStrategy: s.key })} />)}
        </div>
      </section>
      <p className="text-xs text-subtle">SLA demo clock runs ×{data.slaTimeScale}: policy minutes are divided by this factor so breaches and escalations can be shown live.</p>
    </div>
  )
}

// ============================================================ notification log

export function NotificationLogPage() {
  const [channel, setChannel] = useState('')
  const { data } = useQuery({ queryKey: ['admin', 'notifications', channel], queryFn: () => get<Page<NotificationView>>('/admin/notifications', { channel, size: 100 }), refetchInterval: 10000 })
  const icon = (c: string) => c === 'EMAIL' ? <Mail className="size-3.5" /> : c === 'SMS' ? <MessageSquare className="size-3.5" /> : <Bell className="size-3.5" />
  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Notification log" description="Every delivery across channels. E-mail and SMS are simulated through adapters unless SMTP is configured." />
      <div className="flex gap-1.5">
        {['', 'IN_APP', 'EMAIL', 'SMS'].map((c) => (
          <button key={c} onClick={() => setChannel(c)} className={cn('h-8 rounded-full border px-3 text-[13px] cursor-pointer', channel === c ? 'border-brand bg-brand text-brand-fg' : 'border-border text-muted hover:text-foreground')}>{c ? titleCase(c) : 'All channels'}</button>
        ))}
      </div>
      <Card>
        <Table>
          <THead><tr><TH>Time</TH><TH>Channel</TH><TH>Recipient</TH><TH>Message</TH><TH className="hidden sm:table-cell">Delivery</TH></tr></THead>
          <tbody>
            {data?.content.map((n) => (
              <TR key={n.id}>
                <TD className="whitespace-nowrap font-mono text-xs text-muted">{dateTime(n.createdAt)}</TD>
                <TD><span className="flex items-center gap-1.5 text-[13px]">{icon(n.channel)}{titleCase(n.channel)}</span></TD>
                <TD className="whitespace-nowrap text-[13px]">{n.recipientName} <span className="font-mono text-xs text-subtle">{n.recipientCode}</span></TD>
                <TD className="max-w-md"><p className="truncate text-[13px] font-medium">{n.title}</p><p className="truncate text-xs text-muted">{n.message}</p></TD>
                <TD className="hidden sm:table-cell"><Badge tone={n.deliveryStatus === 'FAILED' ? 'red' : n.deliveryStatus === 'SIMULATED' ? 'outline' : 'green'}>{n.deliveryStatus?.toLowerCase()}</Badge></TD>
              </TR>
            ))}
          </tbody>
        </Table>
      </Card>
    </div>
  )
}
