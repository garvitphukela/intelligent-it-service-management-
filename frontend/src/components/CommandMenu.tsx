import { useEffect, useState, type ReactNode } from 'react'
import * as D from '@radix-ui/react-dialog'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Search } from 'lucide-react'
import { get } from '@/lib/api'
import type { IncidentSummary, Page } from '@/lib/types'
import { PriorityBadge, StatusBadge } from './domain'

/** ⌘K quick search by incident number or keyword. */
export function CommandMenu({ trigger }: { trigger: ReactNode }) {
  const [open, setOpen] = useState(false)
  const [q, setQ] = useState('')
  const [debounced, setDebounced] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); setOpen((o) => !o) }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])
  useEffect(() => { const t = setTimeout(() => setDebounced(q.trim()), 200); return () => clearTimeout(t) }, [q])

  const isNumber = /^(inc)?-?\d+$/i.test(debounced)
  const { data, isFetching } = useQuery({
    queryKey: ['incidents', 'cmdk', debounced],
    queryFn: () => get<Page<IncidentSummary>>('/incidents', isNumber ? { number: debounced, size: 8 } : { q: debounced, size: 8 }),
    enabled: open && debounced.length > 1,
  })

  return (
    <D.Root open={open} onOpenChange={(o) => { setOpen(o); if (!o) setQ('') }}>
      <D.Trigger asChild>{trigger}</D.Trigger>
      <D.Portal>
        <D.Overlay className="fixed inset-0 z-50 bg-black/40" />
        <D.Content className="fixed left-1/2 top-[14vh] z-50 w-[calc(100vw-32px)] max-w-xl -translate-x-1/2 overflow-hidden rounded-xl border border-border bg-background shadow-2xl animate-in">
          <D.Title className="sr-only">Search incidents</D.Title>
          <D.Description className="sr-only">Type an incident number or keyword</D.Description>
          <div className="flex items-center gap-2 border-b border-border px-4">
            <Search className="size-4 text-muted" />
            <input autoFocus value={q} onChange={(e) => setQ(e.target.value)} placeholder="INC1024, payment, VPN…" className="h-12 flex-1 bg-transparent text-sm outline-none placeholder:text-subtle" />
            {isFetching && <span className="size-3.5 animate-spin rounded-full border-2 border-muted border-r-transparent" />}
          </div>
          <div className="max-h-80 overflow-y-auto p-1">
            {debounced.length <= 1 && <p className="px-3 py-6 text-center text-[13px] text-muted">Search by incident number or words in the title or description.</p>}
            {debounced.length > 1 && data?.content.length === 0 && <p className="px-3 py-6 text-center text-[13px] text-muted">No incidents match “{debounced}”.</p>}
            {data?.content.map((i) => (
              <button key={i.id} onClick={() => { setOpen(false); navigate(`/app/incidents/${i.id}`) }}
                className="flex w-full items-center gap-3 rounded-md px-3 py-2 text-left hover:bg-surface-2 cursor-pointer">
                <span className="w-16 font-mono text-xs text-muted">{i.number}</span>
                <span className="flex-1 truncate text-[13px]">{i.title}</span>
                <PriorityBadge priority={i.priority} />
                <StatusBadge status={i.status} />
              </button>
            ))}
          </div>
        </D.Content>
      </D.Portal>
    </D.Root>
  )
}
