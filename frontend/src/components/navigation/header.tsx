"use client";

import { usePathname } from "next/navigation";
import { Bell, Sparkles } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { getAiUsage } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";

interface HeaderProps {
  title?: string;
  subtitle?: string;
}

export function Header({ title, subtitle }: HeaderProps) {
  const pathname = usePathname();

  const { data: usage } = useQuery({
    queryKey: queryKeys.me.usage,
    queryFn: getAiUsage,
  });

  const getPageTitle = () => {
    if (title) return title;
    if (pathname.includes("/practice/quiz")) return "Active Drill";
    if (pathname.includes("/practice")) return "Practice Lobby";
    if (pathname.includes("/interviews")) return "Mock Interview Lab";
    if (pathname.includes("/skills")) return "Skill Gap Analysis";
    if (pathname.includes("/review")) return "Spaced Repetition Queue";
    if (pathname.includes("/progress")) return "Readiness Progress";
    if (pathname.includes("/settings")) return "Settings & AI Keys";
    return "Dashboard";
  };

  return (
    <header className="fixed top-0 left-60 right-0 h-16 bg-white border-b border-[#E5E5E5] z-40 flex items-center justify-between px-6">
      {/* Title & Placement Context */}
      <div className="flex items-center gap-3">
        <h1 className="text-[17px] font-semibold text-[#111111] leading-none tracking-tight">
          {getPageTitle()}
        </h1>
        <span className="text-[#C4C7C7] text-xs">•</span>
        <span className="text-[12px] text-[#8A8A8A] font-medium hidden sm:inline">
          {subtitle || "Spring Placement Season · Day 18"}
        </span>
      </div>

      {/* AI Telemetry & User Controls */}
      <div className="flex items-center gap-3.5">
        {/* AI Quota Pill */}
        <div className="flex items-center gap-2 px-3 py-1 rounded-full border border-[#E5E5E5] bg-[#FAFAFA]">
          <span className="w-1.5 h-1.5 rounded-full bg-[#111111]" />
          <span className="font-mono text-[11px] text-[#555555]">
            AI credits today: {usage?.usedToday ?? 1} of {usage?.dailyQuota ?? 2}
          </span>
        </div>

        {/* Notification Bell */}
        <button
          type="button"
          aria-label="Notifications"
          className="w-8 h-8 rounded-lg flex items-center justify-center text-[#555555] hover:bg-[#F4F3F3] hover:text-[#111111] transition-colors"
        >
          <Bell className="w-4 h-4" strokeWidth={1.5} />
        </button>

        {/* Profile Avatar */}
        <div className="flex items-center pl-2 border-l border-[#E5E5E5]">
          <div className="w-8 h-8 rounded-full bg-[#EEEEEE] border border-[#E5E5E5] flex items-center justify-center text-[12px] font-semibold text-[#111111]">
            SP
          </div>
        </div>
      </div>
    </header>
  );
}
