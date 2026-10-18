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
  Clock,
  Target,
  RefreshCw,
  AlertCircle,
  HelpCircle,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { getSessionReport } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import { SessionReportResponse } from "@/lib/types";

export default function PerformanceReportPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const resolvedParams = use(params);
  const sessionId = resolvedParams.id;

  const {
    data: report,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: queryKeys.sessions.report(sessionId),
    queryFn: () => getSessionReport(sessionId),
  });

  if (isLoading) {
    return (
      <div className="w-full max-w-[1000px] mx-auto px-6 py-8 flex flex-col gap-6 animate-pulse">
        <div className="h-6 w-48 bg-[#EEEEEE] rounded" />
        <div className="h-44 rounded-xl border border-[#E5E5E5] bg-[#FAFAFA]" />
        <div className="h-64 rounded-xl border border-[#E5E5E5] bg-[#FAFAFA]" />
      </div>
    );
  }

  if (isError || !report) {
    return (
      <div className="w-full max-w-[1000px] mx-auto px-6 py-12 flex flex-col items-center justify-center min-h-[50vh]">
        <div className="p-8 rounded-xl border border-[#E5E5E5] bg-white max-w-md w-full flex flex-col items-center text-center gap-4">
          <div className="w-12 h-12 rounded-full bg-[#FEE2E2] flex items-center justify-center text-[#DC2626]">
            <AlertCircle className="w-6 h-6" />
          </div>
          <div className="flex flex-col gap-1">
            <h3 className="text-[18px] font-semibold text-[#111111]">Report Not Available</h3>
            <p className="text-[13px] text-[#555555]">
              {error instanceof Error ? error.message : "The diagnostic scorecard for this session could not be loaded."}
            </p>
          </div>
          <div className="flex items-center gap-3 w-full mt-2">
            <Link href="/dashboard" className="flex-1">
              <Button variant="outline" className="w-full">Dashboard</Button>
            </Link>
            <Button onClick={() => refetch()} className="flex-1 gap-2">
              <RefreshCw className="w-4 h-4" />
              <span>Retry</span>
            </Button>
          </div>
        </div>
      </div>
    );
  }

  const summary = report.summary;
  const scoreDelta =
    report.previousScorePercent != null
      ? report.summary.scorePercent - report.previousScorePercent
      : null;

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
                {report.trackName}
              </Badge>
              <Badge variant="success">Completed</Badge>
            </div>
            <h1 className="text-[28px] font-semibold text-[#111111] tracking-tight">
              Session Performance Scorecard
            </h1>
            <p className="text-[13px] text-[#555555] max-w-lg">
              Detailed breakdown of answered questions, accuracy per curriculum topic, and targeted learning recommendations.
            </p>
          </div>

          {/* Score Dial & Delta */}
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
                    strokeDasharray={`${summary.scorePercent}, 100`}
                    strokeLinecap="round"
                    strokeWidth="3.5"
                  />
                </svg>
                <span className="absolute font-mono text-[14px] font-semibold text-[#111111]">
                  {summary.scorePercent}%
                </span>
              </div>
              <span className="text-[11px] font-mono text-[#8A8A8A] mt-1">Total Score</span>
            </div>

            <div className="h-12 w-px bg-[#E5E5E5]" />

            <div className="flex flex-col">
              <span className="font-mono text-[11px] uppercase tracking-wider text-[#8A8A8A]">
                Accuracy of Answered
              </span>
              <span className="text-[18px] font-semibold text-[#111111] mt-0.5">
                {summary.accuracyOfAnsweredPercent}%
              </span>
              {scoreDelta !== null ? (
                <span
                  className={`text-[11px] font-mono ${
                    scoreDelta >= 0 ? "text-[#16A34A]" : "text-[#DC2626]"
                  }`}
                >
                  {scoreDelta >= 0 ? "+" : ""}
                  {scoreDelta.toFixed(1)}% vs previous session
                </span>
              ) : (
                <span className="text-[11px] text-[#8A8A8A]">First session in track</span>
              )}
            </div>
          </div>
        </CardContent>

        {/* Telemetry metrics bar */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 px-6 py-4 border-t border-[#E5E5E5] bg-[#FAFAFA] text-[12px]">
          <div>
            <span className="text-[#8A8A8A] block">Duration</span>
            <span className="font-mono font-medium text-[#111111]">
              {Math.floor(report.elapsedSec / 60)}m {report.elapsedSec % 60}s
            </span>
          </div>
          <div>
            <span className="text-[#8A8A8A] block">Questions Scored</span>
            <span className="font-mono font-medium text-[#111111]">
              {summary.correctCount} of {summary.totalQuestions} ({summary.scorePercent}%)
            </span>
          </div>
          <div>
            <span className="text-[#8A8A8A] block">Unanswered</span>
            <span className="font-mono font-medium text-[#111111]">
              {summary.unansweredCount}
            </span>
          </div>
          <div>
            <span className="text-[#8A8A8A] block">Difficulty</span>
            <span className="font-medium text-[#111111]">{report.difficulty}</span>
          </div>
        </div>
      </Card>

      {/* 3. Topics Breakdown */}
      <div className="flex flex-col gap-4">
        <div>
          <h2 className="text-[18px] font-semibold text-[#111111]">
            Topic Competency Breakdown
          </h2>
          <p className="text-[13px] text-[#8A8A8A]">
            Performance categorized by domain topic accuracy
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {report.topics.map((t) => (
            <Card key={t.topicId} className="p-4 flex flex-col justify-between gap-3">
              <div className="flex items-center justify-between">
                <span className="text-[14px] font-semibold text-[#111111]">
                  {t.topicName}
                </span>
                <Badge
                  variant={
                    t.label === "STRONG"
                      ? "success"
                      : t.label === "COMPETENT"
                      ? "warning"
                      : "critical"
                  }
                  className="text-[10px]"
                >
                  {t.label}
                </Badge>
              </div>

              <div className="flex items-center gap-3">
                <Progress value={t.accuracyPercent} className="h-1.5" />
                <span className="font-mono text-[12px] font-medium text-[#111111] w-12 text-right">
                  {t.accuracyPercent}%
                </span>
              </div>

              <div className="flex items-center justify-between text-[11px] font-mono text-[#8A8A8A] pt-1 border-t border-[#E5E5E5]/60">
                <span>{t.correct} of {t.questionsInTopic} correct</span>
                <span>{t.answered} answered</span>
              </div>
            </Card>
          ))}
        </div>
      </div>

      {/* 4. Recommendations */}
      {report.recommendations.length > 0 && (
        <Card className="border-[#111111] bg-[#FAFAFA]">
          <CardHeader className="pb-3 flex flex-row items-center gap-2">
            <Target className="w-5 h-5 text-[#111111]" />
            <CardTitle className="text-[16px]">Targeted Recommendations</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            {report.recommendations.map((rec, i) => (
              <div
                key={i}
                className="p-3.5 rounded-xl border border-[#E5E5E5] bg-white flex flex-col sm:flex-row sm:items-center justify-between gap-3"
              >
                <div className="flex flex-col gap-0.5">
                  <span className="text-[14px] font-semibold text-[#111111]">
                    {rec.topicName}
                  </span>
                  <span className="text-[12px] text-[#555555]">{rec.reason}</span>
                </div>
                <Link
                  href={`/practice?trackId=${report.trackId}&topicId=${rec.topicId}&count=${rec.suggestedQuestionCount}`}
                >
                  <Button size="sm" variant="outline" className="gap-1.5 shrink-0 text-[12px]">
                    <span>Drill ({rec.suggestedQuestionCount} Qs)</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </Button>
                </Link>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {/* 5. Question-by-Question Review */}
      <div className="flex flex-col gap-4">
        <div>
          <h2 className="text-[18px] font-semibold text-[#111111]">
            Question Review & Solutions
          </h2>
          <p className="text-[13px] text-[#8A8A8A]">
            Review all options and rationales evaluated during this session
          </p>
        </div>

        <div className="flex flex-col gap-4">
          {report.questions.map((q) => (
            <Card key={q.questionId} className="p-5 flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="font-mono text-[12px] font-semibold text-[#111111]">
                    Q{q.position}
                  </span>
                  <Badge variant="default" className="text-[10px] font-mono">
                    {q.topicName}
                  </Badge>
                </div>
                <Badge
                  variant={
                    q.correct === true
                      ? "success"
                      : q.correct === false
                      ? "critical"
                      : "default"
                  }
                  className="text-[10px]"
                >
                  {q.correct === true
                    ? "Correct"
                    : q.correct === false
                    ? "Incorrect"
                    : "Unanswered"}
                </Badge>
              </div>

              <h4 className="text-[15px] font-semibold text-[#111111] leading-snug">
                {q.body}
              </h4>

              {/* Options */}
              <div className="flex flex-col gap-2 pt-1">
                {q.options.map((opt, idx) => {
                  const letter = String.fromCharCode(65 + idx);
                  const isSelected = q.selectedOptionId === opt.id;
                  const isCorrect = q.correctOptionId === opt.id;

                  let borderStyle = "border-[#E5E5E5]";
                  let bgStyle = "bg-white";

                  if (isCorrect) {
                    borderStyle = "border-[#16A34A]";
                    bgStyle = "bg-[#DCFCE7]/20";
                  } else if (isSelected && !isCorrect) {
                    borderStyle = "border-[#DC2626]";
                    bgStyle = "bg-[#FEE2E2]/20";
                  }

                  return (
                    <div
                      key={opt.id}
                      className={`p-3 rounded-lg border text-[13px] flex items-center justify-between ${borderStyle} ${bgStyle}`}
                    >
                      <div className="flex items-center gap-3">
                        <span className="font-mono font-semibold text-[#111111] w-5">
                          {letter}.
                        </span>
                        <span className="text-[#111111]">{opt.text}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        {isSelected && (
                          <Badge variant={isCorrect ? "success" : "critical"} className="text-[10px]">
                            Your Answer
                          </Badge>
                        )}
                        {isCorrect && !isSelected && (
                          <Badge variant="success" className="text-[10px]">
                            Correct Option
                          </Badge>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* Explanation */}
              {q.explanation && (
                <div className="p-3.5 rounded-lg bg-[#FAFAFA] border border-[#E5E5E5] text-[12px] text-[#555555] leading-relaxed mt-1">
                  <span className="font-semibold text-[#111111] block mb-1">
                    Benchmark Explanation:
                  </span>
                  <p>{q.explanation}</p>
                  {q.selectedOptionExplanation && (
                    <p className="mt-1.5 italic text-[#8A8A8A]">
                      {q.selectedOptionExplanation}
                    </p>
                  )}
                </div>
              )}
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
}
