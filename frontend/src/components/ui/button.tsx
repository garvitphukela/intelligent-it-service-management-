import * as React from 'react'
import { Slot } from '@radix-ui/react-slot'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-1.5 whitespace-nowrap rounded-md text-sm font-medium transition-colors disabled:pointer-events-none disabled:opacity-50 [&_svg]:size-4 [&_svg]:shrink-0 cursor-pointer select-none',
  {
    variants: {
      variant: {
        default: 'bg-brand text-brand-fg hover:bg-brand/90 shadow-sm shadow-brand/20',
        secondary: 'border border-border bg-background hover:bg-surface-2 text-foreground',
        ghost: 'hover:bg-surface-2 text-foreground',
        outline: 'border border-border-strong bg-transparent hover:bg-surface-2',
        destructive: 'bg-red text-white hover:bg-red/90',
        accent: 'bg-accent text-white hover:bg-accent/90',
        link: 'text-accent underline-offset-4 hover:underline px-0 h-auto',
      },
      size: {
        default: 'h-9 px-3.5',
        sm: 'h-8 px-2.5 text-[13px]',
        xs: 'h-7 px-2 text-xs',
        lg: 'h-10 px-5',
        icon: 'size-9',
        'icon-sm': 'size-8',
      },
    },
    defaultVariants: { variant: 'default', size: 'default' },
  },
)

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement>, VariantProps<typeof buttonVariants> {
  asChild?: boolean
  loading?: boolean
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, loading, children, disabled, ...props }, ref) => {
    const Comp = asChild ? Slot : 'button'
    return (
      <Comp ref={ref} className={cn(buttonVariants({ variant, size }), className)} disabled={disabled || loading} {...props}>
        {asChild ? children : (<>
          {loading && <span className="size-3.5 animate-spin rounded-full border-2 border-current border-r-transparent" />}
          {children}
        </>)}
      </Comp>
    )
  },
)
Button.displayName = 'Button'
