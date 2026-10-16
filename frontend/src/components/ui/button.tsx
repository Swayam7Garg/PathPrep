import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const buttonVariants = cva(
  "inline-flex items-center justify-center whitespace-nowrap rounded-lg font-medium transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-primary disabled:pointer-events-none disabled:opacity-50",
  {
    variants: {
      variant: {
        primary:
          "bg-[#111111] text-white hover:bg-[#262626] active:bg-[#000000]",
        secondary:
          "bg-white border border-[#E5E5E5] text-[#111111] hover:border-[#111111] hover:bg-[#FAFAFA]",
        outline:
          "bg-transparent border border-[#E5E5E5] text-[#111111] hover:border-[#111111]",
        ghost:
          "bg-transparent text-[#555555] hover:bg-[#FAFAFA] hover:text-[#111111]",
        destructive:
          "bg-[#DC2626] text-white hover:bg-[#B91C1C]",
        link: "text-[#111111] underline-offset-4 hover:underline p-0 h-auto",
      },
      size: {
        default: "h-10 px-4 py-2 text-[14px]",
        sm: "h-8 px-3 text-[12px]",
        lg: "h-11 px-6 text-[15px]",
        icon: "h-9 w-9 p-0",
      },
    },
    defaultVariants: {
      variant: "primary",
      size: "default",
    },
  }
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, ...props }, ref) => {
    return (
      <button
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        {...props}
      />
    );
  }
);
Button.displayName = "Button";

export { Button, buttonVariants };
