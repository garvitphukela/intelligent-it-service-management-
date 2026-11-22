import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { toast } from 'sonner'
import { ApiError, put } from '@/lib/api'
import { useAuth } from '@/lib/auth'
import type { UserView } from '@/lib/types'
import { Button } from '@/components/ui/button'
import { Card, CardBody, CardHeader } from '@/components/ui/card'
import { Field, Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Switch } from '@/components/ui/misc'
import { PageHeader } from '@/components/domain'

const CHANNELS = [
  { key: 'IN_APP', label: 'In-app', hint: 'Bell and live toasts. Always on.' },
  { key: 'EMAIL', label: 'Email', hint: 'Sent to your work address.' },
  { key: 'SMS', label: 'SMS', hint: 'Only for urgent events (P1, SLA breaches, escalations).' },
]

export function ProfilePage() {
  const { user, refresh } = useAuth()
  const [f, setF] = useState({ fullName: user?.fullName ?? '', phone: user?.phone ?? '', department: user?.department ?? '', channels: new Set(user?.preferredChannels ?? ['IN_APP']) })
  const save = useMutation({
    mutationFn: () => put<UserView>('/auth/me', { fullName: f.fullName, phone: f.phone, department: f.department, preferredChannels: [...f.channels] }),
    onSuccess: () => { refresh(); toast.success('Profile saved') },
    onError: (e: ApiError) => toast.error(e.message),
  })
  if (!user) return null
  return (
    <div className="flex max-w-3xl flex-col gap-6">
      <PageHeader title="Profile & preferences" description={<span className="font-mono">{user.code} · {user.title}</span>} />
      <Card>
        <CardHeader title="Personal details" />
        <CardBody className="grid gap-4 sm:grid-cols-2">
          <Field label="Full name"><Input value={f.fullName} onChange={(e) => setF({ ...f, fullName: e.target.value })} /></Field>
          <Field label="Email"><Input value={user.email} disabled /></Field>
          <Field label="Phone" hint="Used for SMS alerts"><Input value={f.phone} onChange={(e) => setF({ ...f, phone: e.target.value })} /></Field>
          <Field label="Department"><Input value={f.department} onChange={(e) => setF({ ...f, department: e.target.value })} /></Field>
        </CardBody>
      </Card>
      <Card>
        <CardHeader title="Notification channels" description="Choose how you hear about incidents." />
        <div className="divide-y divide-border">
          {CHANNELS.map((c) => (
            <label key={c.key} className="flex items-center justify-between gap-4 px-4 py-3">
              <span><span className="block text-sm font-medium">{c.label}</span><span className="text-xs text-muted">{c.hint}</span></span>
              <Switch checked={f.channels.has(c.key)} disabled={c.key === 'IN_APP'} onCheckedChange={(v) => { const n = new Set(f.channels); v ? n.add(c.key) : n.delete(c.key); setF({ ...f, channels: n }) }} />
            </label>
          ))}
        </div>
      </Card>
      {user.skills && (
        <Card>
          <CardHeader title="Skills & capacity" description="Managed by your administrator" />
          <CardBody className="flex flex-wrap items-center gap-1.5">
            {user.skills.map((s) => <Badge key={s} tone="neutral">{s.toLowerCase()}</Badge>)}
            <span className="ml-2 text-xs text-muted">{user.experienceYears} yrs experience · up to {user.maxWorkload} concurrent incidents</span>
          </CardBody>
        </Card>
      )}
      <div><Button loading={save.isPending} onClick={() => save.mutate()}>Save changes</Button></div>
    </div>
  )
}
