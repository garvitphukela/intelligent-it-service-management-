import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import * as DropdownMenu from '@radix-ui/react-dropdown-menu'
import * as Popover from '@radix-ui/react-popover'
import {
  Bell, LayoutDashboard, ListTodo, Inbox, Plus, Users, Server, Tags, Timer, Settings2, ScrollText, Send, LogOut, Moon, Sun, Menu, X, UserCircle, Search,
} from 'lucide-react'
import { useAuth, isManagement, isEngineer, isStaff } from '@/lib/auth'
import { useRealtime } from '@/lib/realtime'
import { useTheme } from '@/lib/theme'
import { get, post } from '@/lib/api'
import { relativeTime } from '@/lib/format'
import type { NotificationView, Page } from '@/lib/types'
import { Button } from '@/components/ui/button'
import { LiveDot } from './domain'
import { cn } from '@/lib/utils'
import { CommandMenu } from './CommandMenu'
import { Logo } from './Logo'

interface NavItem { to: string; label: string; icon: React.ComponentType<{ className?: string }>; end?: boolean }

function navFor(role?: string): { section?: string; items: NavItem[] }[] {
  if (role === 'EMPLOYEE') return [{ items: [
    { to: '/app/incidents', label: 'My incidents', icon: Inbox, end: true },
    { to: '/app/incidents/new', label: 'Report incident', icon: Plus },
  ] }]
  const groups: { section?: string; items: NavItem[] }[] = []
  const main: NavItem[] = []
  if (role === 'TECHNICIAN' || role === 'TEAM_LEAD') main.push({ to: '/app/queue', label: 'My queue', icon: ListTodo })
  if (role !== 'TECHNICIAN') main.push({ to: '/app/dashboard', label: 'Dashboard', icon: LayoutDashboard })
  main.push({ to: '/app/incidents', label: 'All incidents', icon: Inbox, end: true })
  main.push({ to: '/app/incidents/new', label: 'Log incident', icon: Plus })
  groups.push({ items: main })
  if (role === 'TECHNICIAN') groups.push({ section: 'Insights', items: [{ to: '/app/dashboard', label: 'Operations', icon: LayoutDashboard }] })
  if (role === 'TEAM_LEAD' || role === 'TECHNICIAN') groups.push({ section: 'Records', items: [{ to: '/app/audit', label: 'Audit log', icon: ScrollText }] })
  if (role === 'ADMIN' || role === 'MANAGER') {
    groups.push({ section: 'Administration', items: [
      { to: '/app/admin/users', label: 'Users & technicians', icon: Users },
      { to: '/app/admin/services', label: 'IT services', icon: Server },
      { to: '/app/admin/categories', label: 'Categories', icon: Tags },
      { to: '/app/admin/sla', label: 'SLA policies', icon: Timer },
      { to: '/app/admin/settings', label: 'Strategies', icon: Settings2 },
    ] })
    groups.push({ section: 'Records', items: [
      { to: '/app/audit', label: 'Audit log', icon: ScrollText },
      { to: '/app/admin/notifications', label: 'Notification log', icon: Send },
    ] })
  }
  return groups
}

export function AppShell() {
  const { user } = useAuth()
  const { connected } = useRealtime()
  const [open, setOpen] = useState(false)
  const groups = navFor(user?.role)

  const sidebar = (
    <nav className="flex flex-1 flex-col gap-5 overflow-y-auto px-3 py-4">
      {groups.map((g, gi) => (
        <div key={gi} className="flex flex-col gap-0.5">
          {g.section && <p className="px-2 pb-1 text-[11px] font-medium uppercase tracking-wider text-subtle">{g.section}</p>}
          {g.items.map((it) => (
            <NavLink key={it.to + it.label} to={it.to} end={it.end} onClick={() => setOpen(false)}
              className={({ isActive }) => cn('flex h-8 items-center gap-2.5 rounded-md px-2 text-[13px] transition-colors',
                isActive ? 'bg-brand-soft font-medium text-brand' : 'text-muted hover:bg-surface-2 hover:text-foreground')}>
              <it.icon className="size-4" />{it.label}
            </NavLink>
          ))}
        </div>
      ))}
    </nav>
  )

  return (
    <div className="flex min-h-screen">
      <aside className="sticky top-0 hidden h-screen w-60 shrink-0 flex-col border-r border-border bg-surface md:flex">
        <Brand />
        {sidebar}
        <div className="border-t border-border px-4 py-3"><LiveDot connected={connected} /></div>
      </aside>

      {open && (
        <div className="fixed inset-0 z-40 md:hidden">
          <div className="absolute inset-0 bg-black/40" onClick={() => setOpen(false)} />
          <aside className="absolute inset-y-0 left-0 flex w-64 flex-col border-r border-border bg-background animate-in">
            <div className="flex items-center justify-between pr-2"><Brand /><Button variant="ghost" size="icon-sm" onClick={() => setOpen(false)} aria-label="Close menu"><X /></Button></div>
            {sidebar}
          </aside>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex h-14 items-center gap-2 border-b border-border bg-background/80 px-4 backdrop-blur md:px-6">
          <Button variant="ghost" size="icon-sm" className="md:hidden" onClick={() => setOpen(true)} aria-label="Open menu"><Menu /></Button>
          <CommandMenu trigger={
            <button className="flex h-8 w-full max-w-xs items-center gap-2 rounded-md border border-border bg-surface px-2.5 text-[13px] text-subtle hover:border-border-strong cursor-pointer">
              <Search className="size-3.5" /> <span className="flex-1 text-left">Search incidents…</span>
              <kbd className="hidden rounded border border-border px-1 font-mono text-[10px] sm:inline">⌘K</kbd>
            </button>
          } />
          <div className="ml-auto flex items-center gap-1">
            <ThemeButton />
            <NotificationBell />
            <UserMenu />
          </div>
        </header>
        <main className="mx-auto w-full max-w-[1400px] flex-1 px-4 py-6 md:px-8 md:py-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

function Brand() {
  const { user } = useAuth()
  return (
    <Link to="/app" className="flex h-14 items-center gap-2.5 border-b border-border px-4">
      <Logo className="size-7" />
      <div className="leading-tight">
        <p className="text-sm font-semibold tracking-tight">I-ITSM</p>
        <p className="text-[11px] text-muted">{user?.title}</p>
      </div>
    </Link>
  )
}

function ThemeButton() {
  const [theme, setTheme] = useTheme()
  return (
    <Button variant="ghost" size="icon-sm" onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')} aria-label="Toggle theme">
      {theme === 'dark' ? <Sun /> : <Moon />}
    </Button>
  )
}

function NotificationBell() {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const { data: unread } = useQuery({ queryKey: ['notifications', 'unread'], queryFn: () => get<{ unread: number }>('/notifications/unread-count'), refetchInterval: 30000 })
  const { data } = useQuery({ queryKey: ['notifications', 'list'], queryFn: () => get<Page<NotificationView>>('/notifications', { size: 15 }) })
  const readAll = useMutation({ mutationFn: () => post('/notifications/read-all'), onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }) })
  const readOne = useMutation({ mutationFn: (id: number) => post(`/notifications/${id}/read`), onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }) })
  const count = unread?.unread ?? 0
  return (
    <Popover.Root>
      <Popover.Trigger asChild>
        <Button variant="ghost" size="icon-sm" className="relative" aria-label="Notifications">
          <Bell />
          {count > 0 && <span className="absolute right-1 top-1 flex h-3.5 min-w-3.5 items-center justify-center rounded-full bg-red px-0.5 font-mono text-[9px] font-semibold text-white">{count > 9 ? '9+' : count}</span>}
        </Button>
      </Popover.Trigger>
      <Popover.Portal>
        <Popover.Content align="end" sideOffset={8} className="z-50 w-[360px] max-w-[calc(100vw-24px)] rounded-lg border border-border bg-background shadow-xl shadow-black/10 animate-in">
          <div className="flex items-center justify-between border-b border-border px-4 py-2.5">
            <p className="text-sm font-semibold">Notifications</p>
            <Button variant="link" size="xs" onClick={() => readAll.mutate()} disabled={count === 0}>Mark all read</Button>
          </div>
          <div className="max-h-[420px] overflow-y-auto">
            {data?.content.length === 0 && <p className="px-4 py-10 text-center text-[13px] text-muted">You're all caught up.</p>}
            {data?.content.map((n) => (
              <Popover.Close asChild key={n.id}>
                <button onClick={() => { if (!n.read) readOne.mutate(n.id); if (n.incidentId) navigate(`/app/incidents/${n.incidentId}`) }}
                  className="flex w-full gap-3 border-b border-border px-4 py-3 text-left last:border-0 hover:bg-surface cursor-pointer">
                  <span className={cn('mt-1.5 size-1.5 shrink-0 rounded-full', n.read ? 'bg-transparent' : 'bg-accent')} />
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-[13px] font-medium">{n.title}</span>
                    <span className="mt-0.5 line-clamp-2 block text-xs text-muted">{n.message}</span>
                    <span className="mt-1 block text-[11px] text-subtle">{relativeTime(n.createdAt)}</span>
                  </span>
                </button>
              </Popover.Close>
            ))}
          </div>
        </Popover.Content>
      </Popover.Portal>
    </Popover.Root>
  )
}

function UserMenu() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  if (!user) return null
  const initials = user.fullName.split(' ').map((p) => p[0]).slice(0, 2).join('')
  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger asChild>
        <button className="ml-1 flex size-8 items-center justify-center rounded-full border border-border bg-surface-2 text-[11px] font-semibold cursor-pointer hover:border-border-strong" aria-label="Account menu">{initials}</button>
      </DropdownMenu.Trigger>
      <DropdownMenu.Portal>
        <DropdownMenu.Content align="end" sideOffset={8} className="z-50 w-60 rounded-lg border border-border bg-background p-1 shadow-xl shadow-black/10 animate-in">
          <div className="px-2.5 py-2">
            <p className="text-sm font-medium">{user.fullName}</p>
            <p className="text-xs text-muted">{user.email}</p>
            <p className="mt-1 font-mono text-[11px] text-subtle">{user.code} · {user.title}</p>
          </div>
          <DropdownMenu.Separator className="my-1 h-px bg-border" />
          <DropdownMenu.Item onSelect={() => navigate('/app/profile')} className="flex h-8 cursor-pointer items-center gap-2 rounded-md px-2.5 text-[13px] outline-none data-[highlighted]:bg-surface-2"><UserCircle className="size-4" />Profile & preferences</DropdownMenu.Item>
          <DropdownMenu.Item onSelect={() => { logout(); navigate('/login') }} className="flex h-8 cursor-pointer items-center gap-2 rounded-md px-2.5 text-[13px] text-red outline-none data-[highlighted]:bg-surface-2"><LogOut className="size-4" />Sign out</DropdownMenu.Item>
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  )
}

export { isManagement, isEngineer, isStaff }
