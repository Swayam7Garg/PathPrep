import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[12px] font-medium transition-colors border",
  {
    variants: {
      variant: {
        default:
          "border-[#E5E5E5] bg-[#EEEEEE] text-[#111111]",
        primary:
          "border-transparent bg-[#111111] text-white",
        outline:
          "border-[#E5E5E5] bg-transparent text-[#555555]",
        success:
          "border-[#BBF7D0] bg-[#DCFCE7] text-[#16A34A]",
        warning:
          "border-[#FDE68A] bg-[#FEF3C7] text-[#D97706]",
        critical:
          "border-[#FECACA] bg-[#FEE2E2] text-[#DC2626]",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return (
    <div className={cn(badgeVariants({ variant }), className)} {...props} />
  );
}

export { Badge, badgeVariants };
