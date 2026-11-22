import { cn } from '@/lib/utils'

/** I-ITSM "beacon" mark: a signal ring around a core, i.e. an incident being watched. */
export function Logo({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 32 32" className={cn('shrink-0', className)} aria-hidden="true">
      <rect width="32" height="32" rx="9" className="fill-brand" />
      <circle cx="16" cy="16" r="8.5" fill="none" className="stroke-brand-fg" strokeWidth="2.6" strokeLinecap="round" strokeDasharray="40 13.4" transform="rotate(-50 16 16)" />
      <circle cx="16" cy="16" r="3.2" className="fill-brand-fg" />
    </svg>
  )
}
