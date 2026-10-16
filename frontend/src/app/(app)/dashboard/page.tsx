"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import {
  ArrowUpRight,
  Download,
  Flame,
  Clock,
  Play,
  RotateCcw,
  Sparkles,
  ChevronRight,
  TrendingUp,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import {
  getDailyPlan,
  getSkillMastery,
  getReviewQueue,
} from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";

export default function DashboardPage() {
  const [timeRange, setTimeRange] = useState<"7d" | "30d" | "90d">("30d");

  const { data: dailyPlan } = useQuery({
    queryKey: queryKeys.me.planToday,
    queryFn: getDailyPlan,
  });

  const { data: masteryList } = useQuery({
    queryKey: queryKeys.me.skills,
    queryFn: getSkillMastery,
  });

  const { data: reviewItems } = useQuery({
    queryKey: queryKeys.me.reviewDue,
    queryFn: getReviewQueue,
  });

  return (
    <div className="w-full max-w-[1200px] mx-auto px-6 py-8 flex flex-col gap-8">
      {/* 1. Header & Readiness Telemetry Summary */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 border-b border-[#E5E5E5] pb-6">
        <div className="flex flex-col gap-1.5">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-[#111111]" />
            <span className="font-mono text-[11px] uppercase tracking-wider text-[#8A8A8A]">
              Telemetry • Longitudinal Readiness Matrix
            </span>
          </div>

          <div className="flex flex-wrap items-baseline gap-3 mt-1">
            <h2 className="text-[30px] font-semibold text-[#111111] tracking-tight">
              Progress & Mastery Velocity
            </h2>
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#EEEEEE] text-[#111111] text-[12px] font-medium border border-[#E5E5E5]">
              <span>Current Readiness: <strong>78%</strong></span>
              <span className="text-[#8A8A8A] font-normal">(+14% over 30d)</span>
            </div>
          </div>

          <p className="text-[13px] text-[#555555]">
            Spring Placement Season cohort benchmark target:{" "}
            <span className="text-[#111111] font-medium">Tier-1 SDE Cutoff (80%)</span>. You are{" "}
            <strong>2.0%</strong> away from the target threshold.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Button variant="secondary" size="sm" className="gap-2">
            <Download className="w-3.5 h-3.5" strokeWidth={1.5} />
            <span>Export Diagnostic</span>
          </Button>
          <Link href="/practice">
            <Button size="sm" className="gap-2">
              <Play className="w-3.5 h-3.5 fill-current" />
              <span>Start Practice</span>
            </Button>
          </Link>
        </div>
      </div>

      {/* 2. Longitudinal Trajectory Chart Card */}
      <Card>
        <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between pb-3">
          <div>
            <CardTitle className="text-[18px]">Placement Readiness Trajectory</CardTitle>
            <p className="text-[13px] text-[#8A8A8A] mt-1">
              Composite readiness index across mock evaluations, system design rounds, and active quizzes
            </p>
          </div>

          {/* Time range switcher */}
          <div className="inline-flex items-center p-1 rounded-lg bg-[#EEEEEE] border border-[#E5E5E5] mt-2 sm:mt-0">
            {(["7d", "30d", "90d"] as const).map((range) => (
              <button
                key={range}
                onClick={() => setTimeRange(range)}
                className={`px-3 py-1 rounded text-[12px] font-medium transition-colors ${
                  timeRange === range
                    ? "bg-[#111111] text-white shadow-sm"
                    : "text-[#555555] hover:text-[#111111]"
                }`}
              >
                {range}
              </button>
            ))}
          </div>
        </CardHeader>

        <CardContent className="pt-4">
          <div className="w-full relative overflow-x-auto">
            <div className="min-w-[680px] w-full">
              <svg
                className="w-full h-72 overflow-visible"
                fill="none"
                viewBox="0 0 900 280"
                xmlns="http://www.w3.org/2000/svg"
              >
                <defs>
                  <linearGradient id="curveGradient" x1="0" x2="0" y1="0" y2="1">
                    <stop offset="0%" stopColor="#111111" stopOpacity="0.08" />
                    <stop offset="100%" stopColor="#111111" stopOpacity="0.00" />
                  </linearGradient>
                </defs>

                {/* Horizontal Grid lines & Y-Axis Labels */}
                <line stroke="#EFEFEF" strokeDasharray="3 3" strokeWidth="1" x1="50" x2="880" y1="30" y2="30" />
                <text className="font-mono text-[11px]" fill="#747878" textAnchor="end" x="40" y="34">100%</text>

                {/* Tier-1 Cutoff Benchmark line (80%) */}
                <line stroke="#111111" strokeDasharray="5 4" strokeOpacity="0.65" strokeWidth="1.2" x1="50" x2="880" y1="74" y2="74" />
                <text className="font-mono text-[11px] font-medium" fill="#111111" textAnchor="end" x="880" y="66">
                  Tier-1 Placement Cutoff (80%)
                </text>
                <text className="font-mono text-[11px]" fill="#747878" textAnchor="end" x="40" y="78">80%</text>

                {/* 50% line */}
                <line stroke="#EFEFEF" strokeDasharray="3 3" strokeWidth="1" x1="50" x2="880" y1="140" y2="140" />
                <text className="font-mono text-[11px]" fill="#747878" textAnchor="end" x="40" y="144">50%</text>

                {/* 25% line */}
                <line stroke="#EFEFEF" strokeDasharray="3 3" strokeWidth="1" x1="50" x2="880" y1="195" y2="195" />
                <text className="font-mono text-[11px]" fill="#747878" textAnchor="end" x="40" y="199">25%</text>

                {/* Baseline */}
                <line stroke="#E2E2E2" strokeWidth="1" x1="50" x2="880" y1="250" y2="250" />
                <text className="font-mono text-[11px]" fill="#747878" textAnchor="end" x="40" y="254">0%</text>

                {/* Area wash */}
                <path
                  d="M 60 110 C 120 108, 170 102, 220 98 C 280 94, 340 98, 400 90 C 460 82, 510 86, 560 76 C 620 66, 680 82, 730 70 C 780 60, 830 58, 870 52 L 870 250 L 60 250 Z"
                  fill="url(#curveGradient)"
                />

                {/* Trajectory curve */}
                <path
                  d="M 60 110 C 120 108, 170 102, 220 98 C 280 94, 340 98, 400 90 C 460 82, 510 86, 560 76 C 620 66, 680 82, 730 70 C 780 60, 830 58, 870 52"
                  fill="none"
                  stroke="#111111"
                  strokeLinecap="round"
                  strokeWidth="2.2"
                />

                {/* Check points */}
                <circle cx="60" cy="110" fill="#FFFFFF" r="3.5" stroke="#111111" strokeWidth="2" />
                <circle cx="220" cy="98" fill="#FFFFFF" r="3.5" stroke="#111111" strokeWidth="2" />
                <circle cx="400" cy="90" fill="#FFFFFF" r="3.5" stroke="#111111" strokeWidth="2" />
                <circle cx="560" cy="76" fill="#FFFFFF" r="3.5" stroke="#111111" strokeWidth="2" />
                <circle cx="730" cy="70" fill="#111111" r="4.5" stroke="#FFFFFF" strokeWidth="1.5" />
                <circle cx="870" cy="52" fill="#111111" r="4.5" stroke="#FFFFFF" strokeWidth="1.5" />

                {/* Tooltip Pin near Day 24 */}
                <g transform="translate(620, 16)">
                  <rect fill="#111111" height="38" rx="6" width="220" x="0" y="0" />
                  <path d="M 110 38 L 110 44" stroke="#111111" strokeWidth="2" />
                  <text className="font-sans text-[11px] font-medium" fill="#FFFFFF" x="12" y="16">
                    Mock Drill #402: +5% gain
                  </text>
                  <text className="font-mono text-[10px]" fill="#A1A1AA" x="12" y="30">
                    ConcurrentHashMap & Locks
                  </text>
                </g>

                {/* X-axis ticks */}
                <text className="font-mono text-[11px]" fill="#747878" textAnchor="middle" x="60" y="270">Day 1</text>
                <text className="font-mono text-[11px]" fill="#747878" textAnchor="middle" x="220" y="270">Day 6</text>
                <text className="font-mono text-[11px]" fill="#747878" textAnchor="middle" x="400" y="270">Day 12</text>
                <text className="font-mono text-[11px]" fill="#747878" textAnchor="middle" x="560" y="270">Day 18</text>
                <text className="font-mono text-[11px]" fill="#747878" textAnchor="middle" x="730" y="270">Day 24</text>
                <text className="font-mono text-[11px] font-semibold" fill="#111111" textAnchor="middle" x="870" y="270">Today (Day 30)</text>
              </svg>
            </div>
          </div>

          <div className="flex flex-wrap items-center justify-between gap-4 pt-3 border-t border-[#E5E5E5]/60 text-[12px] text-[#8A8A8A]">
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-0.5 bg-[#111111]" />
                <span className="text-[#111111] font-medium">Your Trajectory (Composite 78%)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-0.5 border-t border-dashed border-[#111111]" />
                <span>Tier-1 Cutoff Benchmark (80%)</span>
              </div>
            </div>
            <span>Calculated from 18 verified sessions & mocks</span>
          </div>
        </CardContent>
      </Card>

      {/* 3. Actionable Next Steps & Daily Recommendations */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Recommended Daily Action */}
        <Card className="md:col-span-2 border-[#111111]">
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <div className="flex items-center gap-2">
              <Badge variant="primary" className="text-[11px]">TODAY'S PLAN</Badge>
              <span className="text-[12px] text-[#8A8A8A] font-mono">Day 18 of 60</span>
            </div>
            <div className="flex items-center gap-1.5 text-[12px] font-medium text-[#111111]">
              <Flame className="w-4 h-4 text-[#D97706] fill-[#D97706]" />
              <span>{dailyPlan?.currentStreakDays ?? 18}-day streak</span>
            </div>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <div>
              <h3 className="text-[18px] font-semibold text-[#111111]">
                {dailyPlan?.recommendedAction.title ?? "Distributed Systems & Concurrency (8 Questions • ~15 mins)"}
              </h3>
              <p className="text-[13px] text-[#555555] mt-1">
                Zero-AI code-graded rapid drill focused on high-yield placement concepts: thread safety, CAS primitives, and optimistic locking.
              </p>
            </div>

            <div className="flex items-center justify-between pt-2 border-t border-[#E5E5E5]/60">
              <div className="flex items-center gap-4 text-[12px] text-[#8A8A8A]">
                <span className="flex items-center gap-1">
                  <Clock className="w-3.5 h-3.5" /> 15 mins
                </span>
                <span>8 Questions</span>
                <span className="text-[#16A34A] font-medium">+4% potential gain</span>
              </div>
              <Link href="/practice">
                <Button className="gap-2">
                  <span>Start Today's Drill</span>
                  <ArrowUpRight className="w-4 h-4" />
                </Button>
              </Link>
            </div>
          </CardContent>
        </Card>

        {/* Spaced Repetition Due Queue */}
        <Card>
          <CardHeader className="pb-3 flex flex-row items-center justify-between">
            <CardTitle className="text-[16px]">Review Due Queue</CardTitle>
            <Badge variant="warning">{reviewItems?.length ?? 3} Due</Badge>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <p className="text-[13px] text-[#555555]">
              Past mistakes scheduled for spaced repetition retention today.
            </p>
            <div className="flex flex-col gap-2">
              {reviewItems?.slice(0, 2).map((item) => (
                <div
                  key={item.attemptId}
                  className="p-2.5 rounded-lg bg-[#FAFAFA] border border-[#E5E5E5] text-[12px]"
                >
                  <p className="font-medium text-[#111111] line-clamp-1">
                    {item.questionBody}
                  </p>
                  <div className="flex items-center justify-between text-[#8A8A8A] text-[11px] mt-1 font-mono">
                    <span>{item.topicName}</span>
                    <span className="text-[#DC2626]">Mistakes: {item.mistakeCount}</span>
                  </div>
                </div>
              ))}
            </div>
            <Link href="/review" className="mt-1">
              <Button variant="outline" size="sm" className="w-full gap-2">
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Revisit Queue ({reviewItems?.length ?? 3})</span>
              </Button>
            </Link>
          </CardContent>
        </Card>
      </div>

      {/* 4. Topic Readiness Matrix */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between pb-3">
          <div>
            <CardTitle className="text-[18px]">Core Topic Readiness Matrix</CardTitle>
            <p className="text-[13px] text-[#8A8A8A] mt-1">
              Live mastery levels across Java Backend curriculum topics
            </p>
          </div>
          <Link href="/skills">
            <Button variant="ghost" size="sm" className="gap-1 text-[13px]">
              <span>View Full Skill Map</span>
              <ChevronRight className="w-4 h-4" />
            </Button>
          </Link>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-[#E5E5E5] text-[12px] font-medium text-[#8A8A8A] font-mono">
                  <th className="pb-3 font-normal">TOPIC & DOMAIN</th>
                  <th className="pb-3 font-normal">MASTERY LEVEL</th>
                  <th className="pb-3 font-normal text-center">CONFIDENCE</th>
                  <th className="pb-3 font-normal text-right">QUESTIONS</th>
                  <th className="pb-3 font-normal text-right">ACTION</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E5E5E5]/60 text-[13px]">
                {masteryList?.map((topic) => (
                  <tr key={topic.topicId} className="hover:bg-[#FAFAFA] transition-colors">
                    <td className="py-3.5 pr-4">
                      <div className="font-medium text-[#111111]">{topic.topicName}</div>
                      <div className="text-[11px] font-mono text-[#8A8A8A]">{topic.domain}</div>
                    </td>
                    <td className="py-3.5 px-4 w-44">
                      <div className="flex items-center gap-3">
                        <Progress value={topic.masteryPercentage} className="h-1.5" />
                        <span className="font-mono text-[12px] font-medium text-[#111111] w-8">
                          {topic.masteryPercentage}%
                        </span>
                      </div>
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      {topic.confidenceLevel === "HIGH" ? (
                        <Badge variant="success">Mastered</Badge>
                      ) : topic.confidenceLevel === "MEDIUM" ? (
                        <Badge variant="warning">Progressing</Badge>
                      ) : (
                        <Badge variant="critical">Needs Review</Badge>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono text-[12px] text-[#555555]">
                      {topic.questionsAttempted} solved
                    </td>
                    <td className="py-3.5 pl-4 text-right">
                      <Link href="/practice">
                        <Button variant="ghost" size="sm" className="h-7 px-2 text-[12px]">
                          Practice
                        </Button>
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
