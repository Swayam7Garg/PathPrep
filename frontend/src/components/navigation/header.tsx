"use client";

import { useState, useRef, useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import Link from "next/link";
import { Bell, User, Settings, LogOut } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { getAiUsage } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import { useAuth } from "@/components/providers/auth-provider";

interface HeaderProps {
  title?: string;
  subtitle?: string;
}

export function Header({ title, subtitle }: HeaderProps) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, logout } = useAuth();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const { data: usage } = useQuery({
    queryKey: queryKeys.me.usage,
    queryFn: getAiUsage,
  });

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

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

  const getInitials = () => {
    if (user?.name) {
      const parts = user.name.trim().split(/\s+/);
      if (parts.length >= 2) {
        return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
      }
      return user.name.slice(0, 2).toUpperCase();
    }
    if (user?.email) {
      return user.email.slice(0, 2).toUpperCase();
    }
    return "PP";
  };

  const handleLogout = async () => {
    await logout();
    router.replace("/login");
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
          {subtitle || `${user?.targetTrackName || "General SDE"} · Placement Season`}
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

        {/* Profile Avatar & Dropdown */}
        <div className="relative pl-2 border-l border-[#E5E5E5]" ref={dropdownRef}>
          <button
            type="button"
            onClick={() => setDropdownOpen((prev) => !prev)}
            className="w-8 h-8 rounded-full bg-[#EEEEEE] border border-[#E5E5E5] flex items-center justify-center text-[12px] font-semibold text-[#111111] hover:ring-2 hover:ring-[#111111]/20 transition-all overflow-hidden"
          >
            {user?.pictureUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={user.pictureUrl}
                alt={user.name || "User"}
                className="w-full h-full object-cover"
              />
            ) : (
              getInitials()
            )}
          </button>

          {dropdownOpen && (
            <div className="absolute right-0 mt-2 w-56 rounded-xl bg-white border border-[#E5E5E5] shadow-lg py-1.5 z-50 flex flex-col text-[13px]">
              <div className="px-3.5 py-2 border-b border-[#E5E5E5]/60 flex flex-col gap-0.5">
                <span className="font-semibold text-[#111111] truncate">
                  {user?.name || "Student"}
                </span>
                <span className="text-[11px] text-[#8A8A8A] font-mono truncate">
                  {user?.email}
                </span>
              </div>

              <Link
                href="/settings"
                onClick={() => setDropdownOpen(false)}
                className="px-3.5 py-2 flex items-center gap-2.5 text-[#333333] hover:bg-[#FAFAFA] transition-colors"
              >
                <Settings className="w-4 h-4 text-[#8A8A8A]" />
                <span>Settings & Profile</span>
              </Link>

              <button
                type="button"
                onClick={() => {
                  setDropdownOpen(false);
                  handleLogout();
                }}
                className="px-3.5 py-2 flex items-center gap-2.5 text-[#DC2626] hover:bg-[#FEF2F2] transition-colors text-left"
              >
                <LogOut className="w-4 h-4 text-[#DC2626]" />
                <span>Sign Out</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
