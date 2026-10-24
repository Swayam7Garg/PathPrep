"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import {
  Flame,
  CheckCircle2,
  AlertTriangle,
  Play,
  RotateCcw,
  ArrowRight,
  TrendingUp,
  Clock,
  Target,
  BookOpen,
  History,
  AlertCircle,
  RefreshCw,
  Compass,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { getTodayPlan, getSessionHistory } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import { PlanItem } from "@/lib/types";
import { DailyChecklist } from "@/components/plan/daily-checklist";

export default function DashboardPage() {
  const {
    data: plan,
    isLoading: planLoading,
    isError: planError,
    error: planErrorObj,
    refetch: refetchPlan,
  } = useQuery({
    queryKey: queryKeys.me.planToday,
    queryFn: getTodayPlan,
  });

  const {
    data: sessionsData,
    isLoading: sessionsLoading,
    isError: sessionsError,
    refetch: refetchSessions,
  } = useQuery({
    queryKey: queryKeys.sessions.history({ size: 5 }),
    queryFn: () => getSessionHistory({ size: 5 }),
  });

  // Error State
  if (planError) {
    return (
      <div className="w-full max-w-[1200px] mx-auto px-6 py-12 flex flex-col items-center justify-center min-h-[50vh]">
        <div className="p-8 rounded-xl border border-[#E5E5E5] bg-white max-w-md w-full flex flex-col items-center text-center gap-4">
          <div className="w-12 h-12 rounded-full bg-[#FEE2E2] flex items-center justify-center text-[#DC2626]">
            <AlertCircle className="w-6 h-6" />
          </div>
          <div className="flex flex-col gap-1">
            <h3 className="text-[18px] font-semibold text-[#111111]">Failed to load today's plan</h3>
            <p className="text-[13px] text-[#555555]">
              {planErrorObj instanceof Error ? planErrorObj.message : "Could not connect to the learning server."}
            </p>
          </div>
          <Button onClick={() => refetchPlan()} className="gap-2 mt-2 w-full">
            <RefreshCw className="w-4 h-4" />
            <span>Retry</span>
          </Button>
        </div>
      </div>
    );
  }

  // Loading Skeleton State
  if (planLoading) {
    return (
      <div className="w-full max-w-[1200px] mx-auto px-6 py-8 flex flex-col gap-8 animate-pulse">
        <div className="flex flex-col gap-2 border-b border-[#E5E5E5] pb-6">
          <div className="h-4 w-48 bg-[#EEEEEE] rounded" />
          <div className="h-8 w-72 bg-[#EEEEEE] rounded mt-2" />
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-36 rounded-xl border border-[#E5E5E5] bg-[#FAFAFA]" />
          ))}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 h-72 rounded-xl border border-[#E5E5E5] bg-[#FAFAFA]" />
          <div className="h-72 rounded-xl border border-[#E5E5E5] bg-[#FAFAFA]" />
        </div>
      </div>
    );
  }

  const streak = plan?.streak;
  const readiness = plan?.readiness;
  const recommendedFocus = plan?.recommendedFocus;
  const items: PlanItem[] = plan?.items ?? [];
  const recentSessions = sessionsData?.items ?? [];

  return (
    <div className="w-full max-w-[1200px] mx-auto px-6 py-8 flex flex-col gap-8">
      {/* 1. Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 border-b border-[#E5E5E5] pb-6">
        <div className="flex flex-col gap-1.5">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-[#111111]" />
            <span className="font-mono text-[11px] uppercase tracking-wider text-[#8A8A8A]">
              Longitudinal Learning Plan • {plan?.date ?? "Today"}
            </span>
          </div>
          <h1 className="text-[30px] font-semibold text-[#111111] tracking-tight">
            Dashboard & Learning Momentum
          </h1>
          <p className="text-[13px] text-[#555555]">
            Daily prioritized drills and spaced review queue calibrated from your quiz performance.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Link href="/practice">
            <Button size="sm" className="gap-2">
              <Play className="w-3.5 h-3.5 fill-current" />
              <span>Practice Lobby</span>
            </Button>
          </Link>
          <Link href="/review">
            <Button variant="outline" size="sm" className="gap-2">
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Review ({plan?.reviewDueCount ?? 0})</span>
            </Button>
          </Link>
        </div>
      </div>

      {/* 2. Key Telemetry Metrics (4-Column Grid) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Metric 1: Streak */}
        <Card
          className={`flex flex-col justify-between transition-colors ${
            streak?.atRisk
              ? "border-[#D97706] bg-[#FEF3C7]/20"
              : "border-[#E5E5E5] hover:border-[#111111]"
          }`}
        >
          <CardHeader className="pb-2 flex flex-row items-center justify-between">
            <span className="text-[12px] font-mono text-[#8A8A8A] uppercase tracking-wider">
              Practice Streak
            </span>
            <Flame
              className={`w-4 h-4 ${
                streak?.atRisk ? "text-[#D97706] fill-[#D97706]" : "text-[#111111]"
              }`}
            />
          </CardHeader>
          <CardContent className="flex flex-col gap-2">
            <div className="flex items-baseline gap-1.5">
              <span className="text-[28px] font-semibold text-[#111111] leading-none">
                {streak?.current ?? 0}
              </span>
              <span className="text-[13px] text-[#8A8A8A]">
                day{streak?.current === 1 ? "" : "s"}
              </span>
            </div>
            {streak?.atRisk ? (
              <div className="flex items-center gap-1.5 text-[11px] font-medium text-[#D97706]">
                <AlertTriangle className="w-3.5 h-3.5" />
                <span>Practise today to keep your streak</span>
              </div>
            ) : (
              <div className="text-[11px] text-[#8A8A8A] font-mono">
                Longest: {streak?.longest ?? 0} days
              </div>
            )}
          </CardContent>
        </Card>

        {/* Metric 2: Daily Goal Progress */}
        <Card className="flex flex-col justify-between hover:border-[#111111] transition-colors">
          <CardHeader className="pb-2 flex flex-row items-center justify-between">
            <span className="text-[12px] font-mono text-[#8A8A8A] uppercase tracking-wider">
              Daily Goal
            </span>
            <Target className="w-4 h-4 text-[#111111]" />
          </CardHeader>
          <CardContent className="flex flex-col gap-2">
            <div className="flex items-baseline gap-1.5">
              <span className="text-[28px] font-semibold text-[#111111] leading-none">
                {plan?.todayQuestionsAnswered ?? 0}
              </span>
              <span className="text-[13px] text-[#8A8A8A]">
                / {plan?.dailyGoal ?? 10} questions
              </span>
            </div>
            <Progress
              value={
                plan && plan.dailyGoal > 0
                  ? Math.min(100, (plan.todayQuestionsAnswered / plan.dailyGoal) * 100)
                  : 0
              }
              className="h-1.5"
            />
            <span className="text-[11px] text-[#8A8A8A]">
              {(plan?.todayQuestionsAnswered ?? 0) >= (plan?.dailyGoal ?? 10)
                ? "Daily goal reached!"
                : `${(plan?.dailyGoal ?? 10) - (plan?.todayQuestionsAnswered ?? 0)} questions remaining`}
            </span>
          </CardContent>
        </Card>

        {/* Metric 3: Readiness Dial / Honest Empty State */}
        <Card className="flex flex-col justify-between hover:border-[#111111] transition-colors">
          <CardHeader className="pb-2 flex flex-row items-center justify-between">
            <span className="text-[12px] font-mono text-[#8A8A8A] uppercase tracking-wider">
              Placement Readiness
            </span>
            <TrendingUp className="w-4 h-4 text-[#111111]" />
          </CardHeader>
          <CardContent className="flex flex-col gap-2">
            {readiness?.percent == null ? (
              <div className="flex flex-col gap-1.5">
                <span className="text-[13px] font-medium text-[#111111]">
                  Answer {readiness?.minAnswersRequired ?? 10} questions to see your readiness
                </span>
                <Progress
                  value={
                    readiness && readiness.minAnswersRequired > 0
                      ? Math.min(
                          100,
                          (readiness.basedOnQuestions / readiness.minAnswersRequired) * 100
                        )
                      : 0
                  }
                  className="h-1.5"
                />
                <span className="text-[11px] text-[#8A8A8A] font-mono">
                  {readiness?.basedOnQuestions ?? 0} / {readiness?.minAnswersRequired ?? 10} answered
                </span>
              </div>
            ) : (
              <div className="flex flex-col gap-1">
                <div className="flex items-baseline gap-1.5">
                  <span className="text-[28px] font-semibold text-[#111111] leading-none">
                    {readiness.percent}%
                  </span>
                </div>
                <p className="text-[11px] text-[#555555] line-clamp-2">
                  {readiness.reason}
                </p>
                <span className="text-[10px] text-[#8A8A8A] font-mono mt-0.5">
                  Based on {readiness.basedOnQuestions} questions
                </span>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Metric 4: Review Due Queue */}
        <Card className="flex flex-col justify-between hover:border-[#111111] transition-colors">
          <CardHeader className="pb-2 flex flex-row items-center justify-between">
            <span className="text-[12px] font-mono text-[#8A8A8A] uppercase tracking-wider">
              Due For Review
            </span>
            <RotateCcw className="w-4 h-4 text-[#111111]" />
          </CardHeader>
          <CardContent className="flex flex-col gap-2">
            <div className="flex items-baseline gap-1.5">
              <span className="text-[28px] font-semibold text-[#111111] leading-none">
                {plan?.reviewDueCount ?? 0}
              </span>
              <span className="text-[13px] text-[#8A8A8A]">
                item{plan?.reviewDueCount === 1 ? "" : "s"}
              </span>
            </div>
            <p className="text-[11px] text-[#555555]">
              Spaced repetition questions due for active recall today.
            </p>
            <Link href="/review" className="text-[12px] font-medium text-[#111111] underline">
              Open review queue &rarr;
            </Link>
          </CardContent>
        </Card>
      </div>

      {/* 3. Recommended Focus Callout */}
      {recommendedFocus && (
        <Card className="border-[#111111] bg-[#FAFAFA]">
          <CardContent className="p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex flex-col gap-1">
              <div className="flex items-center gap-2">
                <Compass className="w-4 h-4 text-[#111111]" />
                <span className="font-mono text-[11px] uppercase tracking-wider text-[#8A8A8A]">
                  Recommended Diagnostic Focus
                </span>
                <Badge
                  variant={
                    recommendedFocus.kind === "NOT_STARTED"
                      ? "default"
                      : recommendedFocus.kind === "WEAK"
                      ? "warning"
                      : "critical"
                  }
                  className="text-[10px]"
                >
                  {recommendedFocus.kind === "NOT_STARTED"
                    ? "Start here"
                    : recommendedFocus.kind === "WEAK"
                    ? "Needs work"
                    : "Prerequisite blocked"}
                </Badge>
              </div>
              <h3 className="text-[17px] font-semibold text-[#111111]">
                {recommendedFocus.topicName}
              </h3>
              <p className="text-[13px] text-[#555555] max-w-3xl">
                {recommendedFocus.reason}
              </p>
            </div>

            <Link
              href={`/practice?topicId=${recommendedFocus.topicId}&count=8`}
              className="shrink-0"
            >
              <Button size="sm" className="gap-2">
                <span>Start Practice Drill</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Button>
            </Link>
          </CardContent>
        </Card>
      )}

      {/* 4. Today's Plan & Recent Sessions Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left Column: Interactive Daily Checklist & System Actions */}
        <div className="lg:col-span-7 flex flex-col gap-6">
          {/* User-Defined Daily Plan & Interactive Checklist */}
          <DailyChecklist />

          <div className="flex items-center justify-between pt-2">
            <div className="flex items-center gap-2">
              <BookOpen className="w-4 h-4 text-[#111111]" />
              <h2 className="text-[17px] font-semibold text-[#111111]">Adaptive Study Recommendations</h2>
            </div>
            <span className="text-[12px] font-mono text-[#8A8A8A]">
              {items.length} algorithmic recommendation{items.length === 1 ? "" : "s"}
            </span>
          </div>

          {items.length === 0 ? (
            <Card className="p-8 text-center flex flex-col items-center justify-center gap-3">
              <CheckCircle2 className="w-8 h-8 text-[#16A34A]" />
              <div className="flex flex-col gap-1">
                <h4 className="text-[15px] font-medium text-[#111111]">
                  All caught up for today!
                </h4>
                <p className="text-[13px] text-[#555555]">
                  No pending sessions or reviews. Start a new drill from the practice lobby whenever you're ready.
                </p>
              </div>
              <Link href="/practice" className="mt-2">
                <Button variant="outline" size="sm">
                  Go to Practice Lobby
                </Button>
              </Link>
            </Card>
          ) : (
            <div className="flex flex-col gap-3">
              {items.map((item, index) => {
                let targetUrl = "/practice";
                let actionLabel = "Start Drill";

                if (item.type === "RESUME" && item.sessionId) {
                  targetUrl = `/practice/quiz/${item.sessionId}`;
                  actionLabel = "Resume Session";
                } else if (item.type === "REVIEW") {
                  targetUrl = "/review";
                  actionLabel = "Start Review";
                } else if (item.type === "DRILL") {
                  const params = new URLSearchParams();
                  if (item.topicId) params.set("topicId", item.topicId.toString());
                  if (item.questionCount) params.set("count", item.questionCount.toString());
                  targetUrl = `/practice?${params.toString()}`;
                  actionLabel = `Practice (${item.questionCount ?? 8} Qs)`;
                }

                return (
                  <Card
                    key={`${item.type}-${item.sessionId ?? item.topicId ?? index}`}
                    className="p-4 hover:border-[#111111] transition-colors"
                  >
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                      <div className="flex flex-col gap-1">
                        <div className="flex items-center gap-2">
                          <Badge
                            variant={
                              item.type === "RESUME"
                                ? "primary"
                                : item.type === "REVIEW"
                                ? "warning"
                                : "default"
                            }
                            className="text-[10px]"
                          >
                            {item.type}
                          </Badge>
                          <span className="text-[11px] font-mono text-[#8A8A8A] flex items-center gap-1">
                            <Clock className="w-3 h-3" />
                            ~{item.estimatedMinutes} mins
                          </span>
                        </div>
                        <h4 className="text-[15px] font-semibold text-[#111111] mt-0.5">
                          {item.title}
                        </h4>
                        <p className="text-[12px] text-[#555555]">
                          {item.detail}
                        </p>
                      </div>

                      <Link href={targetUrl} className="shrink-0 self-start sm:self-auto">
                        <Button size="sm" variant={item.type === "RESUME" ? "primary" : "outline"} className="gap-1.5">
                          <span>{actionLabel}</span>
                          <ArrowRight className="w-3.5 h-3.5" />
                        </Button>
                      </Link>
                    </div>
                  </Card>
                );
              })}
            </div>
          )}
        </div>

        {/* Right Column: Recent Sessions History (GET /sessions?size=5) */}
        <div className="lg:col-span-5 flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <History className="w-4 h-4 text-[#111111]" />
              <h2 className="text-[18px] font-semibold text-[#111111]">Recent Sessions</h2>
            </div>
            <Link href="/practice" className="text-[12px] text-[#555555] hover:text-[#111111] underline">
              New session
            </Link>
          </div>

          {sessionsLoading ? (
            <div className="flex flex-col gap-2 animate-pulse">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-16 rounded-xl border border-[#E5E5E5] bg-[#FAFAFA]" />
              ))}
            </div>
          ) : recentSessions.length === 0 ? (
            <Card className="p-6 text-center text-[13px] text-[#8A8A8A]">
              No practice sessions recorded yet. Start a session to build your scorecard.
            </Card>
          ) : (
            <div className="flex flex-col gap-2.5">
              {recentSessions.map((session) => (
                <div
                  key={session.id}
                  className="p-3.5 rounded-xl border border-[#E5E5E5] bg-white hover:border-[#111111] transition-colors flex items-center justify-between gap-3"
                >
                  <div className="flex flex-col gap-0.5">
                    <div className="flex items-center gap-2">
                      <span className="text-[13px] font-semibold text-[#111111]">
                        {session.trackName}
                      </span>
                      <Badge
                        variant={session.status === "COMPLETED" ? "success" : "default"}
                        className="text-[10px]"
                      >
                        {session.status}
                      </Badge>
                    </div>
                    <span className="text-[11px] font-mono text-[#8A8A8A]">
                      {session.answeredCount} of {session.totalQuestions} questions •{" "}
                      {session.scorePercent != null ? `${session.scorePercent}% score` : "In progress"}
                    </span>
                  </div>

                  {session.status === "COMPLETED" ? (
                    <Link href={`/practice/quiz/${session.id}/report`}>
                      <Button variant="ghost" size="sm" className="h-7 text-[12px] px-2">
                        View Report
                      </Button>
                    </Link>
                  ) : (
                    <Link href={`/practice/quiz/${session.id}`}>
                      <Button variant="outline" size="sm" className="h-7 text-[12px] px-2">
                        Resume
                      </Button>
                    </Link>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
