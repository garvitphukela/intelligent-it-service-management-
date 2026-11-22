import * as React from 'react'
import * as SwitchPrimitive from '@radix-ui/react-switch'
import * as TabsPrimitive from '@radix-ui/react-tabs'
import { cn } from '@/lib/utils'

export function Skeleton({ className }: { className?: string }) {
  return <div className={cn('animate-pulse rounded-md bg-surface-2', className)} />
}

export function Separator({ className }: { className?: string }) {
  return <div className={cn('h-px w-full bg-border', className)} />
}

export function Kbd({ children }: { children: React.ReactNode }) {
  return <kbd className="inline-flex h-5 min-w-5 items-center justify-center rounded border border-border bg-surface px-1 font-mono text-[10px] text-muted">{children}</kbd>
}

export function Switch({ checked, onCheckedChange, disabled, id }: { checked: boolean; onCheckedChange: (v: boolean) => void; disabled?: boolean; id?: string }) {
  return (
    <SwitchPrimitive.Root id={id} checked={checked} onCheckedChange={onCheckedChange} disabled={disabled}
      className="relative h-5 w-9 cursor-pointer rounded-full border border-border-strong bg-surface-2 transition-colors data-[state=checked]:border-brand data-[state=checked]:bg-brand disabled:opacity-50">
      <SwitchPrimitive.Thumb className="block size-4 translate-x-0.5 rounded-full bg-background shadow-sm transition-transform data-[state=checked]:translate-x-[17px]" />
    </SwitchPrimitive.Root>
  )
}

export const Tabs = TabsPrimitive.Root
export function TabsList({ className, ...props }: React.ComponentProps<typeof TabsPrimitive.List>) {
  return <TabsPrimitive.List className={cn('flex gap-4 border-b border-border', className)} {...props} />
}
export function TabsTrigger({ className, ...props }: React.ComponentProps<typeof TabsPrimitive.Trigger>) {
  return <TabsPrimitive.Trigger className={cn('-mb-px cursor-pointer border-b-2 border-transparent pb-2 text-sm text-muted transition-colors hover:text-foreground data-[state=active]:border-brand data-[state=active]:text-foreground', className)} {...props} />
}
export const TabsContent = TabsPrimitive.Content

export function EmptyState({ icon, title, description, action }: { icon?: React.ReactNode; title: string; description?: string; action?: React.ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 px-6 py-14 text-center">
      {icon && <div className="mb-1 rounded-lg border border-border p-2.5 text-muted [&_svg]:size-5">{icon}</div>}
      <p className="text-sm font-medium">{title}</p>
      {description && <p className="max-w-sm text-[13px] text-muted">{description}</p>}
      {action && <div className="mt-2">{action}</div>}
    </div>
  )
}

export function Stat({ label, value, hint, tone }: { label: string; value: React.ReactNode; hint?: React.ReactNode; tone?: 'red' | 'amber' | 'green' | 'blue' }) {
  const color = tone === 'red' ? 'text-red' : tone === 'amber' ? 'text-amber' : tone === 'green' ? 'text-green' : tone === 'blue' ? 'text-accent' : 'text-foreground'
  return (
    <div className="flex flex-col gap-1 p-4">
      <span className="text-[12px] text-muted">{label}</span>
      <span className={cn('font-mono text-2xl font-semibold tracking-tight tabular-nums', color)}>{value}</span>
      {hint && <span className="text-[11px] text-subtle">{hint}</span>}
    </div>
  )
}
