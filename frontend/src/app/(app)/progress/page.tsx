"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import {
  TrendingUp,
  Flame,
  Clock,
  Target,
  CheckCircle2,
  Calendar,
  Layers,
  ArrowRight,
  RefreshCw,
  AlertCircle,
  Play,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { getProgress } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import { DailyProgressView, RecentSessionView } from "@/lib/types";

export default function ProgressPage() {
  const [range, setRange] = useState<7 | 30 | 90>(30);

  const {
    data: progress,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: queryKeys.me.progress(range),
    queryFn: () => getProgress(range),
  });

  // Error State
  if (isError) {
    return (
      <div className="w-full max-w-[1200px] mx-auto px-6 py-12 flex flex-col items-center justify-center min-h-[50vh]">
        <div className="p-8 rounded-xl border border-[#E5E5E5] bg-white max-w-md w-full flex flex-col items-center text-center gap-4">
          <div className="w-12 h-12 rounded-full bg-[#FEE2E2] flex items-center justify-center text-[#DC2626]">
            <AlertCircle className="w-6 h-6" />
          </div>
          <div className="flex flex-col gap-1">
            <h3 className="text-[18px] font-semibold text-[#111111]">Failed to load progress telemetry</h3>
            <p className="text-[13px] text-[#555555]">
              {error instanceof Error ? error.message : "Unable to retrieve progress data."}
            </p>
          </div>
          <Button onClick={() => refetch()} className="gap-2 mt-2 w-full">
            <RefreshCw className="w-4 h-4" />
            <span>Retry</span>
          </Button>
        </div>
      </div>
    );
  }

  // Loading State
  if (isLoading) {
    return (
      <div className="w-full max-w-[1200px] mx-auto px-6 py-8 flex flex-col gap-8 animate-pulse">
        <div className="flex flex-col gap-2 border-b border-[#E5E5E5] pb-6">
          <div className="h-4 w-48 bg-[#EEEEEE] rounded" />
          <div className="h-8 w-72 bg-[#EEEEEE] rounded mt-2" />
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-32 rounded-xl border border-[#E5E5E5] bg-[#FAFAFA]" />
          ))}
        </div>
        <div className="h-64 rounded-xl border border-[#E5E5E5] bg-[#FAFAFA]" />
      </div>
    );
  }

  const totals = progress?.totals;
  const streak = progress?.streak;
  // Dense array from server: do NOT fill gaps in browser
  const days: DailyProgressView[] = progress?.days ?? [];
  // Oldest first for chronological trend
  const chronologicalSessions: RecentSessionView[] = [
    ...(progress?.recentSessions ?? []),
  ].reverse();

  return (
    <div className="w-full max-w-[1200px] mx-auto px-6 py-8 flex flex-col gap-8">
      {/* 1. Header & Range Switcher */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 border-b border-[#E5E5E5] pb-6">
        <div className="flex flex-col gap-1.5">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-[#111111]" />
            <span className="font-mono text-[11px] uppercase tracking-wider text-[#8A8A8A]">
              Telemetry & Longitudinal Analytics
            </span>
          </div>
          <h1 className="text-[30px] font-semibold text-[#111111] tracking-tight">
            Learning Progress & Activity
          </h1>
          <p className="text-[13px] text-[#555555]">
            Dense study cadence, session score progression, and verified platform telemetry over rolling time horizons.
          </p>
        </div>

        {/* Range Switcher (7 / 30 / 90 days) */}
        <div className="inline-flex items-center p-1 rounded-lg bg-[#EEEEEE] border border-[#E5E5E5] self-start md:self-auto">
          {([7, 30, 90] as const).map((r) => (
            <button
              key={r}
              type="button"
              onClick={() => setRange(r)}
              className={`px-3 py-1.5 rounded text-[12px] font-medium transition-colors ${
                range === r
                  ? "bg-[#111111] text-white shadow-sm"
                  : "text-[#555555] hover:text-[#111111]"
              }`}
            >
              {r}d
            </button>
          ))}
        </div>
      </div>

      {/* 2. Telemetry Totals (4-Column Grid) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Metric 1: Total Questions & Accuracy */}
        <Card className="hover:border-[#111111] transition-colors">
          <CardHeader className="pb-2 flex flex-row items-center justify-between">
            <span className="text-[12px] font-mono text-[#8A8A8A] uppercase tracking-wider">
              Questions Solved
            </span>
            <Target className="w-4 h-4 text-[#111111]" />
          </CardHeader>
          <CardContent className="flex flex-col gap-1">
            <div className="flex items-baseline gap-1.5">
              <span className="text-[28px] font-semibold text-[#111111] leading-none">
                {totals?.questionsAnswered ?? 0}
              </span>
              <span className="text-[12px] text-[#8A8A8A]">
                ({totals?.correctAnswers ?? 0} correct)
              </span>
            </div>
            <span className="text-[12px] text-[#16A34A] font-medium font-mono mt-1">
              {totals?.accuracyPercent ?? 0}% overall accuracy
            </span>
          </CardContent>
        </Card>

        {/* Metric 2: Active Minutes */}
        <Card className="hover:border-[#111111] transition-colors">
          <CardHeader className="pb-2 flex flex-row items-center justify-between">
            <span className="text-[12px] font-mono text-[#8A8A8A] uppercase tracking-wider">
              Active Minutes
            </span>
            <Clock className="w-4 h-4 text-[#111111]" />
          </CardHeader>
          <CardContent className="flex flex-col gap-1">
            <div className="flex items-baseline gap-1.5">
              <span className="text-[28px] font-semibold text-[#111111] leading-none">
                {totals?.activeMinutes ?? 0}
              </span>
              <span className="text-[13px] text-[#8A8A8A]">mins</span>
            </div>
            <span className="text-[12px] text-[#555555] mt-1">
              Across {totals?.sessionsCompleted ?? 0} completed sessions
            </span>
          </CardContent>
        </Card>

        {/* Metric 3: Current Streak */}
        <Card className="hover:border-[#111111] transition-colors">
          <CardHeader className="pb-2 flex flex-row items-center justify-between">
            <span className="text-[12px] font-mono text-[#8A8A8A] uppercase tracking-wider">
              Current Streak
            </span>
            <Flame className="w-4 h-4 text-[#111111]" />
          </CardHeader>
          <CardContent className="flex flex-col gap-1">
            <div className="flex items-baseline gap-1.5">
              <span className="text-[28px] font-semibold text-[#111111] leading-none">
                {streak?.current ?? 0}
              </span>
              <span className="text-[13px] text-[#8A8A8A]">
                day{streak?.current === 1 ? "" : "s"}
              </span>
            </div>
            <span className="text-[12px] text-[#8A8A8A] font-mono mt-1">
              Longest: {streak?.longest ?? 0} days
            </span>
          </CardContent>
        </Card>

        {/* Metric 4: Sessions Completed */}
        <Card className="hover:border-[#111111] transition-colors">
          <CardHeader className="pb-2 flex flex-row items-center justify-between">
            <span className="text-[12px] font-mono text-[#8A8A8A] uppercase tracking-wider">
              Completed Drills
            </span>
            <CheckCircle2 className="w-4 h-4 text-[#111111]" />
          </CardHeader>
          <CardContent className="flex flex-col gap-1">
            <div className="flex items-baseline gap-1.5">
              <span className="text-[28px] font-semibold text-[#111111] leading-none">
                {totals?.sessionsCompleted ?? 0}
              </span>
              <span className="text-[13px] text-[#8A8A8A]">drills</span>
            </div>
            <span className="text-[12px] text-[#555555] mt-1">
              In rolling {range}-day window
            </span>
          </CardContent>
        </Card>
      </div>

      {/* 3. Dense Days Activity Heatmap & Chart (No browser gap filling) */}
      <Card>
        <CardHeader className="pb-3 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <CardTitle className="text-[18px]">Daily Practice Activity Cadence</CardTitle>
            <p className="text-[13px] text-[#8A8A8A] mt-1">
              Dense record of {days.length} consecutive calendar days ending today (server-provided dense sequence)
            </p>
          </div>
          <span className="text-[11px] font-mono text-[#8A8A8A]">
            Window: Last {range} Days
          </span>
        </CardHeader>

        <CardContent className="pt-2 flex flex-col gap-6">
          {/* Dense Heatmap Strip */}
          <div className="overflow-x-auto pb-2">
            <div className="flex flex-wrap gap-1.5 min-w-[320px]">
              {days.map((day) => {
                const count = day.questionsAnswered;
                let bg = "bg-[#F3F4F6]";
                if (count >= 15) bg = "bg-[#111111]";
                else if (count >= 10) bg = "bg-[#4B5563]";
                else if (count >= 5) bg = "bg-[#9CA3AF]";
                else if (count >= 1) bg = "bg-[#D1D5DB]";

                return (
                  <div
                    key={day.date}
                    title={`${day.date}: ${day.questionsAnswered} questions (${day.correctAnswers} correct), ${day.reviewsAnswered} reviews, ${day.activeMinutes} mins active`}
                    className={`w-6 h-6 rounded-md ${bg} flex items-center justify-center text-[10px] font-mono transition-transform hover:scale-110 cursor-pointer`}
                  />
                );
              })}
            </div>

            {/* Heatmap Scale Legend */}
            <div className="flex items-center justify-end gap-2 pt-3 text-[11px] font-mono text-[#8A8A8A]">
              <span>0 questions</span>
              <div className="w-3.5 h-3.5 rounded bg-[#F3F4F6] border border-[#E5E5E5]" />
              <div className="w-3.5 h-3.5 rounded bg-[#D1D5DB]" />
              <div className="w-3.5 h-3.5 rounded bg-[#9CA3AF]" />
              <div className="w-3.5 h-3.5 rounded bg-[#4B5563]" />
              <div className="w-3.5 h-3.5 rounded bg-[#111111]" />
              <span>15+ questions</span>
            </div>
          </div>

          {/* Daily Table Summary for Active Days */}
          <div className="border-t border-[#E5E5E5]/60 pt-4">
            <h4 className="text-[14px] font-semibold text-[#111111] mb-2">
              Daily Breakdown
            </h4>
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-[12px]">
                <thead>
                  <tr className="border-b border-[#E5E5E5] text-[#8A8A8A] font-mono">
                    <th className="pb-2 font-normal">DATE</th>
                    <th className="pb-2 font-normal text-right">QUESTIONS</th>
                    <th className="pb-2 font-normal text-right">CORRECT</th>
                    <th className="pb-2 font-normal text-right">REVIEWS</th>
                    <th className="pb-2 font-normal text-right">ACTIVE MINS</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#E5E5E5]/60">
                  {days
                    .slice()
                    .reverse()
                    .slice(0, 10)
                    .map((d) => (
                      <tr key={d.date} className="hover:bg-[#FAFAFA]">
                        <td className="py-2.5 font-mono text-[#111111]">{d.date}</td>
                        <td className="py-2.5 text-right font-mono text-[#111111]">{d.questionsAnswered}</td>
                        <td className="py-2.5 text-right font-mono text-[#16A34A]">{d.correctAnswers}</td>
                        <td className="py-2.5 text-right font-mono text-[#555555]">{d.reviewsAnswered}</td>
                        <td className="py-2.5 text-right font-mono text-[#8A8A8A]">{d.activeMinutes}m</td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 4. Score Trend from Recent Sessions (Chronological Oldest-First) */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-[18px]">Recent Session Score Progression</CardTitle>
          <p className="text-[13px] text-[#8A8A8A] mt-1">
            Chronological performance trajectory across your last verified practice sessions (oldest first)
          </p>
        </CardHeader>

        <CardContent className="pt-2">
          {chronologicalSessions.length === 0 ? (
            <p className="text-[13px] text-[#8A8A8A] py-6 text-center">
              No recent session scores recorded. Complete a quiz to view your score trend.
            </p>
          ) : (
            <div className="flex flex-col gap-3">
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
                {chronologicalSessions.map((session, idx) => (
                  <div
                    key={session.sessionId}
                    className="p-3.5 rounded-xl border border-[#E5E5E5] bg-[#FAFAFA] flex flex-col justify-between gap-2"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-[11px] font-mono text-[#8A8A8A]">
                        Session #{idx + 1}
                      </span>
                      <span className="font-mono text-[13px] font-semibold text-[#111111]">
                        {session.scorePercent}%
                      </span>
                    </div>

                    <span className="text-[13px] font-semibold text-[#111111] line-clamp-1">
                      {session.trackName}
                    </span>

                    <div className="pt-1.5 border-t border-[#E5E5E5]/60 flex items-center justify-between text-[11px] text-[#8A8A8A]">
                      <span>{new Date(session.completedAt).toLocaleDateString()}</span>
                      <Link
                        href={`/practice/quiz/${session.sessionId}/report`}
                        className="text-[#111111] hover:underline"
                      >
                        Report &rarr;
                      </Link>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
