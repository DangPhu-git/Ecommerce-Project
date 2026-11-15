import React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const badgeVariants = cva(
  'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2',
  {
    variants: {
      variant: {
        default:
          'bg-primary-500 text-white',
        secondary:
          'bg-secondary-100 text-secondary-800 dark:bg-secondary-700 dark:text-secondary-100',
        success:
          'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300',
        warning:
          'bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300',
        danger:
          'bg-rose-100 text-rose-800 dark:bg-rose-900/40 dark:text-rose-300',
        outline:
          'border border-border-default text-text-primary',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  }
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {}

export const Badge: React.FC<BadgeProps> = ({
  className,
  variant,
  ...props
}) => {
  return (
    <span className={cn(badgeVariants({ variant }), className)} {...props} />
  );
};
