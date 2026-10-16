"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutGrid,
  Code2,
  Video,
  Brain,
  CheckSquare,
  TrendingUp,
  Settings,
  BookOpen,
  ArrowUpRight,
} from "lucide-react";
import { cn } from "@/lib/utils";

const NAV_ITEMS = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutGrid },
  { href: "/practice", label: "Practice", icon: Code2 },
  { href: "/interviews", label: "Interviews", icon: Video },
  { href: "/skills", label: "Skills", icon: Brain },
  { href: "/review", label: "Review", icon: CheckSquare },
  { href: "/progress", label: "Progress", icon: TrendingUp },
  { href: "/settings", label: "Settings", icon: Settings },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="fixed left-0 top-0 h-screen w-60 bg-white border-r border-[#E5E5E5] z-50 flex flex-col justify-between select-none">
      <div className="flex flex-col">
        {/* Brand header */}
        <div className="h-16 px-6 flex items-center gap-3 border-b border-[#E5E5E5]/60">
          <div className="w-8 h-8 rounded-lg bg-[#111111] text-white flex items-center justify-center font-semibold text-sm tracking-tight">
            P
          </div>
          <div className="flex flex-col">
            <span className="text-[17px] font-semibold text-[#111111] leading-none tracking-tight">
              PrepPath
            </span>
            <span className="text-[11px] text-[#8A8A8A] leading-tight mt-1 font-medium">
              Campus Prep · Tier-1
            </span>
          </div>
        </div>

        {/* Primary nav items */}
        <nav className="p-3 flex flex-col gap-1 mt-2">
          {NAV_ITEMS.map((item) => {
            const Icon = item.icon;
            const isActive =
              item.href === "/dashboard"
                ? pathname === "/" || pathname === "/dashboard"
                : pathname.startsWith(item.href);

            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "flex items-center gap-3 px-3 py-2 rounded-lg text-[14px] font-normal transition-colors",
                  isActive
                    ? "bg-[#F4F3F3] text-[#111111] font-medium"
                    : "text-[#555555] hover:bg-[#F4F3F3]/60 hover:text-[#111111]"
                )}
              >
                <Icon
                  className={cn(
                    "w-4 h-4 shrink-0 transition-colors",
                    isActive ? "text-[#111111]" : "text-[#8A8A8A]"
                  )}
                  strokeWidth={1.5}
                />
                <span>{item.label}</span>
              </Link>
            );
          })}
        </nav>
      </div>

      {/* Footer Resource Card */}
      <div className="p-3 border-t border-[#E5E5E5]/60 mb-2">
        <a
          href="https://github.com"
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center justify-between px-3 py-2 rounded-lg text-[#555555] hover:bg-[#F4F3F3] hover:text-[#111111] transition-colors"
        >
          <div className="flex items-center gap-2.5">
            <BookOpen className="w-4 h-4 text-[#8A8A8A]" strokeWidth={1.5} />
            <span className="text-[12px] font-medium">Placement Guide 2026</span>
          </div>
          <ArrowUpRight className="w-3.5 h-3.5 text-[#8A8A8A]" strokeWidth={1.5} />
        </a>
      </div>
    </aside>
  );
}
