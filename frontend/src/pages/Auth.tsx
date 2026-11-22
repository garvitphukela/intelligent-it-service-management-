import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import { useAuth } from '@/lib/auth'
import { ApiError } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Field, Input } from '@/components/ui/input'
import { Logo } from '@/components/Logo'

const DEMO = [
  { role: 'Employee', email: 'employee@itsm.local', note: 'Ananya · Finance' },
  { role: 'Technician', email: 'tech.apps@itsm.local', note: 'Rohan · Apps' },
  { role: 'Team lead', email: 'lead.infra@itsm.local', note: 'Meera · Infra' },
  { role: 'Administrator', email: 'admin@itsm.local', note: 'Riya · IT Ops' },
  { role: 'IT manager', email: 'manager@itsm.local', note: 'Vikram' },
]

function AuthLayout({ children, title, subtitle }: { children: React.ReactNode; title: string; subtitle: string }) {
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="flex flex-col justify-center px-6 py-12 sm:px-12">
        <div className="mx-auto w-full max-w-sm">
          <div className="mb-10 flex items-center gap-2.5">
            <Logo className="size-8" />
            <span className="font-semibold tracking-tight">I-ITSM</span>
          </div>
          <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
          <p className="mt-1.5 text-sm text-muted">{subtitle}</p>
          <div className="mt-8">{children}</div>
        </div>
      </div>
      <div className="hidden border-l border-border bg-surface lg:flex lg:flex-col lg:justify-between lg:p-12">
        <div className="font-mono text-xs text-muted">Intelligent IT Service Management</div>
        <div className="max-w-md">
          <p className="text-3xl font-semibold leading-tight tracking-tight">Report. Classify. Prioritise. Assign. Resolve.</p>
          <p className="mt-4 text-sm leading-relaxed text-muted">
            Incidents are categorised from their description, prioritised by business impact, routed to the best-fit engineer
            and watched against their SLA, so nothing slips through.
          </p>
          <div className="mt-8 grid grid-cols-3 gap-px overflow-hidden rounded-lg border border-border bg-border text-center">
            {[['Auto', 'categorisation'], ['Smart', 'assignment'], ['Live', 'SLA tracking']].map(([a, b]) => (
              <div key={b} className="bg-background px-3 py-4"><p className="text-sm font-semibold">{a}</p><p className="text-xs text-muted">{b}</p></div>
            ))}
          </div>
        </div>
        <div className="text-xs text-subtle">CS F213 · Object-Oriented Programming</div>
      </div>
    </div>
  )
}

export function LoginPage() {
  const { login, user } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  if (user) return <Navigate to="/app" replace />

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true); setError(null)
    try { await login(email, password); navigate('/app') } catch (err) { setError(err instanceof ApiError ? err.message : 'Sign-in failed') } finally { setBusy(false) }
  }

  return (
    <AuthLayout title="Sign in" subtitle="Use your organisation account.">
      <form onSubmit={submit} className="flex flex-col gap-4">
        <Field label="Email"><Input type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@company.com" /></Field>
        <Field label="Password"><Input type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} /></Field>
        {error && <p className="rounded-md border border-red/30 bg-red-soft px-3 py-2 text-[13px] text-red">{error}</p>}
        <Button type="submit" loading={busy} className="mt-1">Sign in <ArrowRight /></Button>
      </form>
      <p className="mt-6 text-center text-[13px] text-muted">New employee? <Link to="/register" className="text-foreground underline underline-offset-4">Create an account</Link></p>

      <div className="mt-10">
        <p className="mb-2 text-[11px] font-medium uppercase tracking-wider text-subtle">Demo accounts · password Password@123</p>
        <div className="overflow-hidden rounded-lg border border-border">
          {DEMO.map((d) => (
            <button key={d.email} type="button" onClick={() => { setEmail(d.email); setPassword('Password@123') }}
              className="flex w-full items-center justify-between border-b border-border px-3 py-2 text-left last:border-0 hover:bg-surface cursor-pointer">
              <span className="text-[13px] font-medium">{d.role}</span>
              <span className="font-mono text-[11px] text-muted">{d.email}</span>
            </button>
          ))}
        </div>
      </div>
    </AuthLayout>
  )
}

export function RegisterPage() {
  const { register, user } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', email: '', password: '', department: '', phone: '' })
  const [error, setError] = useState<ApiError | null>(null)
  const [busy, setBusy] = useState(false)
  if (user) return <Navigate to="/app" replace />
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [k]: e.target.value })

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true); setError(null)
    try { await register(form); navigate('/app') } catch (err) { setError(err instanceof ApiError ? err : new ApiError(0, 'ERR', 'Registration failed')) } finally { setBusy(false) }
  }

  return (
    <AuthLayout title="Create account" subtitle="Employees can register to report and track incidents.">
      <form onSubmit={submit} className="flex flex-col gap-4">
        <Field label="Full name" error={error?.fieldErrors?.fullName}><Input required value={form.fullName} onChange={set('fullName')} /></Field>
        <Field label="Work email" error={error?.fieldErrors?.email}><Input type="email" required value={form.email} onChange={set('email')} /></Field>
        <Field label="Password" hint="At least 8 characters with letters and digits" error={error?.fieldErrors?.password}><Input type="password" required minLength={8} value={form.password} onChange={set('password')} /></Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Department"><Input value={form.department} onChange={set('department')} /></Field>
          <Field label="Phone"><Input value={form.phone} onChange={set('phone')} /></Field>
        </div>
        {error && !error.fieldErrors && <p className="rounded-md border border-red/30 bg-red-soft px-3 py-2 text-[13px] text-red">{error.message}</p>}
        <Button type="submit" loading={busy}>Create account</Button>
      </form>
      <p className="mt-6 text-center text-[13px] text-muted">Already have an account? <Link to="/login" className="text-foreground underline underline-offset-4">Sign in</Link></p>
    </AuthLayout>
  )
}
