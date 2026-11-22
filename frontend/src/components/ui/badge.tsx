import * as React from 'react'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

const badgeVariants = cva(
  'inline-flex items-center gap-1 rounded-full border px-2 h-5 text-[11px] font-medium whitespace-nowrap leading-none',
  {
    variants: {
      tone: {
        neutral: 'border-border bg-surface-2 text-foreground',
        outline: 'border-border text-muted',
        blue: 'border-transparent bg-accent-soft text-accent',
        red: 'border-transparent bg-red-soft text-red',
        amber: 'border-transparent bg-amber-soft text-amber',
        green: 'border-transparent bg-green-soft text-green',
        solid: 'border-transparent bg-foreground text-background',
      },
    },
    defaultVariants: { tone: 'neutral' },
  },
)

export function Badge({ className, tone, dot, children, ...props }: React.HTMLAttributes<HTMLSpanElement> & VariantProps<typeof badgeVariants> & { dot?: boolean }) {
  return (
    <span className={cn(badgeVariants({ tone }), className)} {...props}>
      {dot && <span className="size-1.5 rounded-full bg-current" />}
      {children}
    </span>
  )
}
