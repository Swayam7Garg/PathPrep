"use client";

import { use } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import {
  ArrowLeft,
  CheckCircle2,
  AlertTriangle,
  BookOpen,
  ArrowRight,
  RotateCcw,
  Download,
  Share2,
  Award,
  Sparkles,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { getSessionReport } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";

export default function PerformanceReportPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const resolvedParams = use(params);
  const sessionId = resolvedParams.id;

  const { data: report, isLoading } = useQuery({
    queryKey: queryKeys.sessions.report(sessionId),
    queryFn: () => getSessionReport(sessionId),
  });

  return (
    <div className="w-full max-w-[1000px] mx-auto px-6 py-8 flex flex-col gap-8">
      {/* 1. Header Navigation & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-[#E5E5E5] pb-6">
        <div className="flex items-center gap-3">
          <Link href="/dashboard">
            <Button variant="outline" size="sm" className="gap-1.5 text-[#555555]">
              <ArrowLeft className="w-4 h-4" />
              <span>Back to Dashboard</span>
            </Button>
          </Link>
          <span className="text-[#C4C7C7]">•</span>
          <span className="font-mono text-[12px] text-[#8A8A8A]">
            SESSION #{sessionId} DIAGNOSTIC REPORT
          </span>
        </div>

        <div className="flex items-center gap-2">
          <Button variant="secondary" size="sm" className="gap-2">
            <Download className="w-3.5 h-3.5" />
            <span>Export Scorecard</span>
          </Button>
          <Link href="/practice">
            <Button size="sm" className="gap-2">
              <RotateCcw className="w-3.5 h-3.5" />
              <span>New Practice Drill</span>
            </Button>
          </Link>
        </div>
      </div>

      {/* 2. Top Summary Scorecard Card */}
      <Card className="border-[#111111]">
        <CardContent className="p-6 sm:p-8 flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-6">
          <div className="flex flex-col gap-2">
            <div className="flex items-center gap-2">
              <Badge variant="primary" className="text-[11px]">
                {report?.trackName ?? "Java Backend Engineer"}
              </Badge>
              <Badge variant="success">Completed</Badge>
            </div>
            <h2 className="text-[28px] font-semibold text-[#111111] tracking-tight">
              Session Performance Breakdown
            </h2>
            <p className="text-[13px] text-[#555555] max-w-lg">
              Evaluated against Tier-1 campus placement rubric benchmarks. Demonstrates solid core principles with targeted gaps in distributed coordination.
            </p>
          </div>

          {/* Readiness Index & Percentile Gauge */}
          <div className="flex items-center gap-6 p-4 rounded-xl bg-[#FAFAFA] border border-[#E5E5E5] shrink-0">
            <div className="flex flex-col items-center">
              <div className="relative w-16 h-16 flex items-center justify-center">
                <svg className="w-16 h-16 -rotate-90" viewBox="0 0 36 36">
                  <path
                    className="text-[#EEEEEE] stroke-current"
                    d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                    fill="none"
                    strokeWidth="3.5"
                  />
                  <path
                    className="text-[#111111] stroke-current"
                    d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                    fill="none"
                    strokeDasharray={`${report?.overallScore ?? 78}, 100`}
                    strokeLinecap="round"
                    strokeWidth="3.5"
                  />
                </svg>
                <span className="absolute font-mono text-[14px] font-semibold text-[#111111]">
                  {report?.overallScore ?? 78}%
                </span>
              </div>
              <span className="text-[11px] font-mono text-[#8A8A8A] mt-1">Accuracy</span>
            </div>

            <div className="h-12 w-px bg-[#E5E5E5]" />

            <div className="flex flex-col">
              <span className="font-mono text-[11px] uppercase tracking-wider text-[#8A8A8A]">
                Cohort Benchmark
              </span>
              <span className="text-[18px] font-semibold text-[#111111] mt-0.5">
                Top 22%
              </span>
              <span className="text-[11px] text-[#555555]">
                Tier-1 Qualified (Cutoff: 80%)
              </span>
            </div>
          </div>
        </CardContent>

        {/* Quick telemetry metrics bar */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 px-6 py-4 border-t border-[#E5E5E5] bg-[#FAFAFA] text-[12px]">
          <div>
            <span className="text-[#8A8A8A] block">Duration</span>
            <span className="font-mono font-medium text-[#111111]">14m 20s</span>
          </div>
          <div>
            <span className="text-[#8A8A8A] block">Questions Scored</span>
            <span className="font-mono font-medium text-[#111111]">8 / 8 (100%)</span>
          </div>
          <div>
            <span className="text-[#8A8A8A] block">Grading Mode</span>
            <span className="font-medium text-[#111111]">Zero-AI Java Code</span>
          </div>
          <div>
            <span className="text-[#8A8A8A] block">Placement Trajectory</span>
            <span className="font-medium text-[#16A34A]">+4.5% Velocity</span>
          </div>
        </div>
      </Card>

      {/* 3. Criterion Score Breakdown */}
      <div className="flex flex-col gap-4">
        <div>
          <h3 className="text-[18px] font-semibold text-[#111111]">
            Scores by Rubric Criterion
          </h3>
          <p className="text-[13px] text-[#8A8A8A]">
            Evaluated against standard college placement interview parameters (1.0 to 5.0 scale)
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {report?.criteriaScores.map((crit) => (
            <Card key={crit.criterionId} className="flex flex-col justify-between">
              <CardContent className="p-5 flex flex-col gap-3">
                <div className="flex items-center justify-between">
                  <span className="text-[14px] font-semibold text-[#111111]">
                    {crit.name}
                  </span>
                  <Badge variant={crit.score >= 4 ? "success" : "warning"}>
                    {crit.score >= 4 ? "Strong" : "Progressing"}
                  </Badge>
                </div>

                <div className="flex items-center gap-3">
                  <Progress value={(crit.score / 5) * 100} className="h-1.5" />
                  <span className="font-mono text-[13px] font-semibold text-[#111111] shrink-0">
                    {crit.score}.0 / 5.0
                  </span>
                </div>

                <blockquote className="text-[12px] text-[#555555] italic border-l-2 border-[#E5E5E5] pl-2.5 mt-1 leading-relaxed">
                  "{crit.evidenceQuote}"
                </blockquote>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>

      {/* 4. Diagnostic Checklists: Strengths & Edge Cases */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Demonstrated Strengths */}
        <Card>
          <CardHeader className="pb-3 flex flex-row items-center gap-2">
            <CheckCircle2 className="w-5 h-5 text-[#16A34A]" />
            <CardTitle className="text-[16px]">Demonstrated Strengths</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-2.5 text-[13px] text-[#555555]">
            <div className="flex items-start gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-[#16A34A] mt-2 shrink-0" />
              <span>Correctly identified CAS primitive application on bucket head insertions.</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-[#16A34A] mt-2 shrink-0" />
              <span>Understood lazy mappingFunction evaluation prevents expensive redundant object construction.</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-[#16A34A] mt-2 shrink-0" />
              <span>Clear distinction between Java 7 Segment locks and Java 8 node synchronization.</span>
            </div>
          </CardContent>
        </Card>

        {/* Missed Key Concepts & Edge Cases */}
        <Card>
          <CardHeader className="pb-3 flex flex-row items-center gap-2">
            <AlertTriangle className="w-5 h-5 text-[#D97706]" />
            <CardTitle className="text-[16px]">Missed Concepts & Edge Cases</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-2.5 text-[13px] text-[#555555]">
            {report?.missedKeyPoints.map((point, i) => (
              <div key={i} className="flex items-start gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-[#D97706] mt-2 shrink-0" />
                <span>{point}</span>
              </div>
            ))}
            <div className="flex items-start gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-[#D97706] mt-2 shrink-0" />
              <span>TreeBin conversion threshold (TREEIFY_THRESHOLD = 8) under heavy collision.</span>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* 5. Exemplary Model Answer Benchmark */}
      <Card>
        <CardHeader className="pb-3 flex flex-row items-center justify-between">
          <div className="flex items-center gap-2">
            <BookOpen className="w-4 h-4 text-[#111111]" />
            <CardTitle className="text-[16px]">Exemplary Model Answer Benchmark</CardTitle>
          </div>
          <Badge variant="default" className="text-[11px]">SDE-2 Reference Standard</Badge>
        </CardHeader>
        <CardContent>
          <p className="text-[13px] text-[#555555] leading-relaxed bg-[#FAFAFA] p-4 rounded-lg border border-[#E5E5E5] font-mono text-[12px]">
            {report?.modelAnswer}
          </p>
        </CardContent>
      </Card>

      {/* 6. Recommended Next Practice Actions */}
      <Card className="border-[#111111]">
        <CardHeader className="pb-3">
          <CardTitle className="text-[16px]">Targeted Recommended Actions</CardTitle>
          <p className="text-[12px] text-[#8A8A8A]">
            Closing these two gaps will elevate your composite score above the 80% Tier-1 placement cutoff.
          </p>
        </CardHeader>
        <CardContent className="flex flex-col sm:flex-row gap-4 pt-1">
          {report?.recommendedTopics.map((item) => (
            <div
              key={item.topicId}
              className="flex-1 p-4 rounded-lg bg-[#FAFAFA] border border-[#E5E5E5] flex flex-col justify-between gap-3"
            >
              <div>
                <Badge variant={item.action === "PRACTICE" ? "primary" : "warning"} className="text-[10px]">
                  {item.action}
                </Badge>
                <h4 className="font-semibold text-[14px] text-[#111111] mt-2">
                  {item.topicName}
                </h4>
              </div>
              <Link href="/practice">
                <Button size="sm" variant="outline" className="w-full gap-1.5">
                  <span>Start Focused Drill</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </Button>
              </Link>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
