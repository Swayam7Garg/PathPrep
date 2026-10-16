"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import {
  Brain,
  AlertTriangle,
  ArrowRight,
  GitBranch,
  Layers,
  Sparkles,
  Zap,
  CheckCircle2,
  TrendingUp,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { getSkillMastery, getSkillGaps } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";

export default function SkillGapAnalysisPage() {
  const { data: masteryList } = useQuery({
    queryKey: queryKeys.me.skills,
    queryFn: getSkillMastery,
  });

  const { data: gapItems } = useQuery({
    queryKey: queryKeys.me.gaps,
    queryFn: getSkillGaps,
  });

  return (
    <div className="w-full max-w-[1200px] mx-auto px-6 py-8 flex flex-col gap-8">
      {/* Page Header */}
      <div className="flex flex-col gap-1.5 border-b border-[#E5E5E5] pb-6">
        <div className="flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-[#111111]" />
          <span className="font-mono text-[11px] uppercase tracking-wider text-[#8A8A8A]">
            Curriculum Matrix • Prerequisite Directed Graph
          </span>
        </div>
        <h2 className="text-[30px] font-semibold text-[#111111] tracking-tight">
          Skill Gap Analysis & Mastery Graph
        </h2>
        <p className="text-[13px] text-[#555555]">
          Identify blocking prerequisite concepts and targeted weaknesses that yield the largest jump in placement readiness.
        </p>
      </div>

      {/* 1. Radar Chart / Multivariate Domain Matrix Card */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        <Card className="lg:col-span-7">
          <CardHeader className="pb-2">
            <CardTitle className="text-[18px]">Technical Domain Radar</CardTitle>
            <p className="text-[13px] text-[#8A8A8A]">
              Multivariate proficiency across 6 core technical domains evaluated in mock sessions & quizzes
            </p>
          </CardHeader>
          <CardContent className="flex flex-col items-center">
            {/* Radar Spider SVG */}
            <div className="w-full flex justify-center py-2 relative">
              <svg
                aria-label="Technical Proficiency Spider Chart"
                className="w-full max-w-[420px] h-auto overflow-visible select-none"
                viewBox="0 0 460 440"
              >
                {/* 100% Ring */}
                <polygon
                  fill="none"
                  points="359.9,290 230,365 100.1,290 100.1,140 230,65 359.9,140"
                  stroke="#E5E5E5"
                  strokeWidth="1"
                />
                {/* 75% Ring */}
                <polygon
                  fill="none"
                  points="327.4,271.25 230,327.5 132.6,271.25 132.6,158.75 230,102.5 327.4,158.75"
                  stroke="#EBEBEB"
                  strokeDasharray="2 3"
                  strokeWidth="1"
                />
                {/* 50% Ring */}
                <polygon
                  fill="none"
                  points="295,252.5 230,290 165,252.5 165,177.5 230,140 295,177.5"
                  stroke="#E5E5E5"
                  strokeWidth="1"
                />

                {/* Axis Rays */}
                <line stroke="#E5E5E5" strokeWidth="1" x1="230" x2="230" y1="215" y2="65" />
                <line stroke="#E5E5E5" strokeWidth="1" x1="230" x2="359.9" y1="215" y2="140" />
                <line stroke="#E5E5E5" strokeWidth="1" x1="230" x2="359.9" y1="215" y2="290" />
                <line stroke="#E5E5E5" strokeWidth="1" x1="230" x2="230" y1="215" y2="365" />
                <line stroke="#E5E5E5" strokeWidth="1" x1="230" x2="100.1" y1="215" y2="290" />
                <line stroke="#E5E5E5" strokeWidth="1" x1="230" x2="100.1" y1="215" y2="140" />

                {/* Benchmark Polygon (80% Cutoff) */}
                <polygon
                  fill="none"
                  points="230,95 327.4,158.8 340.4,278.8 230,342.5 126.1,275 132.6,158.8"
                  stroke="#A3A3A3"
                  strokeDasharray="4 4"
                  strokeWidth="1.5"
                />

                {/* User Profile Polygon */}
                <polygon
                  fill="#111111"
                  fillOpacity="0.08"
                  points="230,122 292.4,179 331.3,273.5 230,341 115.7,281 154.7,171.5"
                  stroke="#111111"
                  strokeWidth="2"
                />

                {/* Data Points */}
                <circle cx="230" cy="122" fill="#111111" r="4" stroke="#FFFFFF" strokeWidth="1.5" />
                <circle cx="292.4" cy="179" fill="#111111" r="4" stroke="#FFFFFF" strokeWidth="1.5" />
                <circle cx="331.3" cy="273.5" fill="#111111" r="4" stroke="#FFFFFF" strokeWidth="1.5" />
                <circle cx="230" cy="341" fill="#111111" r="4" stroke="#FFFFFF" strokeWidth="1.5" />
                <circle cx="115.7" cy="281" fill="#111111" r="4" stroke="#FFFFFF" strokeWidth="1.5" />
                <circle cx="154.7" cy="171.5" fill="#111111" r="4" stroke="#FFFFFF" strokeWidth="1.5" />

                {/* Labels */}
                <text className="font-sans text-[12px] font-semibold" fill="#111111" textAnchor="middle" x="230" y="44">
                  Concurrency (62%)
                </text>
                <text className="font-sans text-[12px] font-semibold" fill="#DC2626" textAnchor="start" x="375" y="132">
                  Database & Indexing (48%)
                </text>
                <text className="font-sans text-[12px] font-semibold" fill="#111111" textAnchor="start" x="375" y="295">
                  JVM Internals (78%)
                </text>
                <text className="font-sans text-[12px] font-semibold" fill="#111111" textAnchor="middle" x="230" y="385">
                  Spring Framework (84%)
                </text>
                <text className="font-sans text-[12px] font-semibold" fill="#111111" textAnchor="end" x="85" y="295">
                  Data Structures (88%)
                </text>
                <text className="font-sans text-[12px] font-semibold" fill="#111111" textAnchor="end" x="85" y="132">
                  System Design (58%)
                </text>
              </svg>
            </div>

            <div className="flex items-center justify-center gap-6 pt-2 text-[12px] text-[#8A8A8A]">
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-0.5 bg-[#111111]" />
                <span className="text-[#111111] font-medium">Your Profile</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-0.5 border-t border-dashed border-[#A3A3A3]" />
                <span>Tier-1 Placement Cutoff (80%)</span>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Primary Diagnostic Focus Callout */}
        <div className="lg:col-span-5 flex flex-col gap-4">
          <Card className="border-[#111111]">
            <CardHeader className="pb-3 flex flex-row items-center gap-2">
              <AlertTriangle className="w-5 h-5 text-[#DC2626]" />
              <CardTitle className="text-[16px]">Primary Blocking Gap</CardTitle>
            </CardHeader>
            <CardContent className="flex flex-col gap-3">
              <h3 className="text-[17px] font-semibold text-[#111111]">
                Database Indexing & Distributed Locks
              </h3>
              <p className="text-[13px] text-[#555555] leading-relaxed">
                Your performance in B-Tree vs Hash index lookup and Redis distributed lock contention is currently at <strong>48%</strong>. This topic carries a <strong>1.45x weight</strong> in Java Backend campus interviews.
              </p>
              <div className="p-3 rounded-lg bg-[#FAFAFA] border border-[#E5E5E5] text-[12px]">
                <span className="font-semibold text-[#111111] block mb-1">Impact Analysis:</span>
                <span>Resolving this deficit will add approximately <strong>+6.2%</strong> to your composite placement readiness index.</span>
              </div>
              <Link href="/practice" className="mt-1">
                <Button className="w-full gap-2">
                  <span>Start Remediation Drill (6 Qs)</span>
                  <ArrowRight className="w-4 h-4" />
                </Button>
              </Link>
            </CardContent>
          </Card>

          {/* Prerequisite Hierarchy Card */}
          <Card>
            <CardHeader className="pb-3 flex flex-row items-center justify-between">
              <div className="flex items-center gap-2">
                <GitBranch className="w-4 h-4 text-[#111111]" />
                <CardTitle className="text-[16px]">Prerequisite Dependency Tree</CardTitle>
              </div>
              <Badge variant="default" className="text-[10px]">Java Track</Badge>
            </CardHeader>
            <CardContent className="flex flex-col gap-3 text-[12px]">
              <div className="flex items-center justify-between p-2.5 rounded-lg bg-[#FAFAFA] border border-[#E5E5E5]">
                <div className="flex flex-col">
                  <span className="font-medium text-[#111111]">Distributed Locking (Redis)</span>
                  <span className="text-[#8A8A8A] text-[11px]">Requires: Thread Pools & ExecutorService</span>
                </div>
                <Badge variant="warning">Unlocked</Badge>
              </div>

              <div className="flex items-center justify-between p-2.5 rounded-lg bg-[#FAFAFA] border border-[#E5E5E5]">
                <div className="flex flex-col">
                  <span className="font-medium text-[#111111]">JVM Memory Model & GC Tuning</span>
                  <span className="text-[#8A8A8A] text-[11px]">Requires: Collections & ConcurrentHashMap</span>
                </div>
                <Badge variant="success">Mastered</Badge>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* 2. Priority Skill Gaps Ranked Table */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-[18px]">Ranked Skill Deficits by Placement Impact</CardTitle>
          <p className="text-[13px] text-[#8A8A8A]">
            Weighted formula: Deficit (100 - Score) × Track Topic Importance Weight
          </p>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-[#E5E5E5] text-[12px] font-medium text-[#8A8A8A] font-mono">
                  <th className="pb-3 font-normal">UNMASTERED TOPIC</th>
                  <th className="pb-3 font-normal">TRACK IMPACT WEIGHT</th>
                  <th className="pb-3 font-normal">BLOCKING PREREQUISITES</th>
                  <th className="pb-3 font-normal text-right">RECOMMENDED DRILL</th>
                  <th className="pb-3 font-normal text-right">ACTION</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E5E5E5]/60 text-[13px]">
                {gapItems?.map((gap) => (
                  <tr key={gap.topicId} className="hover:bg-[#FAFAFA] transition-colors">
                    <td className="py-3.5 pr-4">
                      <span className="font-semibold text-[#111111]">{gap.topicName}</span>
                    </td>
                    <td className="py-3.5 px-4 font-mono font-medium text-[#111111]">
                      <Badge variant="primary" className="text-[11px] font-mono">
                        {gap.gapWeight}x Weight
                      </Badge>
                    </td>
                    <td className="py-3.5 px-4 text-[#555555]">
                      {gap.missingPrerequisites.length > 0 ? (
                        <span className="text-[#D97706] font-medium">
                          {gap.missingPrerequisites.join(", ")}
                        </span>
                      ) : (
                        <span className="text-[#8A8A8A]">None (Direct entry)</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-right font-mono text-[#555555]">
                      {gap.recommendedDrillCount} Questions
                    </td>
                    <td className="py-3.5 pl-4 text-right">
                      <Link href="/practice">
                        <Button size="sm" variant="outline" className="h-7 px-2.5 text-[12px]">
                          Target Drill
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
