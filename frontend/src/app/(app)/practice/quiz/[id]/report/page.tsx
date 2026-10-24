"use client";

import { use, useState, useMemo } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import {
  ArrowLeft,
  ArrowRight,
  RotateCcw,
  Clock,
  Target,
  RefreshCw,
  AlertCircle,
  CheckCircle2,
  XCircle,
  HelpCircle,
  ChevronLeft,
  ChevronRight,
  BookOpen,
  LayoutList,
  Columns,
  Sparkles,
  ThumbsUp,
  ThumbsDown,
  BrainCircuit,
  ShieldAlert,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { getSessionReport, submitEvaluationFeedback } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import { SessionReportResponse, ReportQuestionDetail } from "@/lib/types";

type FilterType = "all" | "incorrect" | "correct" | "unanswered";

export default function PerformanceReportPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const resolvedParams = use(params);
  const sessionId = resolvedParams.id;

  const [activeQuestionIndex, setActiveQuestionIndex] = useState(0);
  const [filter, setFilter] = useState<FilterType>("all");
  const [viewMode, setViewMode] = useState<"step" | "all">("step");

  // Feedback state per evaluation
  const [feedbackGiven, setFeedbackGiven] = useState<{ [evalId: number]: "UP" | "DOWN" }>({});
  const [submittingFeedback, setSubmittingFeedback] = useState<{ [evalId: number]: boolean }>({});

  const handleThumbFeedback = async (evalId: number, thumb: "UP" | "DOWN") => {
    setSubmittingFeedback((prev) => ({ ...prev, [evalId]: true }));
    try {
      await submitEvaluationFeedback(evalId, {
        thumb,
        positive: thumb === "UP",
        rating: thumb === "UP" ? "THUMBS_UP" : "THUMBS_DOWN",
      });
      setFeedbackGiven((prev) => ({ ...prev, [evalId]: thumb }));
    } catch (err) {
      console.error("Failed to submit feedback", err);
    } finally {
      setSubmittingFeedback((prev) => ({ ...prev, [evalId]: false }));
    }
  };

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

  // Filtered questions based on the selected tab
  const filteredQuestions = useMemo(() => {
    if (!report?.questions) return [];
    switch (filter) {
      case "incorrect":
        return report.questions.filter((q) => q.correct === false);
      case "correct":
        return report.questions.filter((q) => q.correct === true);
      case "unanswered":
        return report.questions.filter((q) => !q.answered || q.correct === null);
      case "all":
      default:
        return report.questions;
    }
  }, [report?.questions, filter]);

  // Keep active index in bounds when filter changes
  const safeActiveIndex = Math.min(
    Math.max(0, activeQuestionIndex),
    Math.max(0, (filteredQuestions.length || 1) - 1)
  );

  const currentQuestion: ReportQuestionDetail | undefined =
    filteredQuestions[safeActiveIndex];

  if (isLoading) {
    return (
      <div className="w-full max-w-[1060px] mx-auto px-6 py-8 flex flex-col gap-6 animate-pulse">
        <div className="h-6 w-48 bg-[#EEEEEE] rounded" />
        <div className="h-44 rounded-xl border border-[#E5E5E5] bg-[#FAFAFA]" />
        <div className="h-32 rounded-xl border border-[#E5E5E5] bg-[#FAFAFA]" />
        <div className="h-80 rounded-xl border border-[#E5E5E5] bg-[#FAFAFA]" />
      </div>
    );
  }

  if (isError || !report) {
    return (
      <div className="w-full max-w-[1060px] mx-auto px-6 py-12 flex flex-col items-center justify-center min-h-[50vh]">
        <div className="p-8 rounded-xl border border-[#E5E5E5] bg-white max-w-md w-full flex flex-col items-center text-center gap-4 shadow-sm">
          <div className="w-12 h-12 rounded-full bg-[#FEE2E2] flex items-center justify-center text-[#DC2626]">
            <AlertCircle className="w-6 h-6" />
          </div>
          <div className="flex flex-col gap-1">
            <h3 className="text-[18px] font-semibold text-[#111111]">Report Not Available</h3>
            <p className="text-[13px] text-[#555555]">
              {error instanceof Error
                ? error.message
                : "The diagnostic scorecard for this session could not be loaded."}
            </p>
          </div>
          <div className="flex items-center gap-3 w-full mt-2">
            <Link href="/dashboard" className="flex-1">
              <Button variant="secondary" className="w-full">Dashboard</Button>
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
  const incorrectCount = Math.max(
    0,
    summary.totalQuestions - summary.correctCount - summary.unansweredCount
  );
  const scoreDelta =
    report.previousScorePercent != null
      ? report.summary.scorePercent - report.previousScorePercent
      : null;

  return (
    <div className="w-full max-w-[1060px] mx-auto px-4 sm:px-6 py-8 flex flex-col gap-8 text-[#111111]">
      {/* ─── 1. Header Navigation & Actions ───────────────────────── */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-[#E5E5E5] pb-5">
        <div className="flex items-center gap-3">
          <Link href="/dashboard">
            <Button variant="secondary" size="sm" className="gap-1.5 text-[#555555]">
              <ArrowLeft className="w-4 h-4" />
              <span>Dashboard</span>
            </Button>
          </Link>
          <span className="text-[#C4C7C7]">•</span>
          <span className="font-mono text-[12px] text-[#8A8A8A]">
            EXAM #{sessionId} DIAGNOSTIC REPORT
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

      {/* ─── 2. Top Summary Scorecard Card ────────────────────────── */}
      <Card className="border-[#E5E5E5] shadow-sm bg-white overflow-hidden">
        <CardContent className="p-6 sm:p-8 flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-6">
          <div className="flex flex-col gap-2">
            <div className="flex items-center gap-2">
              <Badge variant="primary" className="text-[11px]">
                {report.trackName}
              </Badge>
              {report.mode === "INTERVIEW" && (
                <Badge variant="outline" className="text-[11px] font-mono">
                  INTERVIEW MODE
                </Badge>
              )}
              <Badge variant="outline" className="text-[11px] font-mono">
                {report.difficulty}
              </Badge>
              <Badge variant="success">
                {report.status === "COMPLETED" ? "Completed" : report.status}
              </Badge>
            </div>
            <h1 className="text-[26px] sm:text-[28px] font-bold text-[#111111] tracking-tight">
              {report.mode === "INTERVIEW"
                ? "Mock Interview Performance & Evaluation Report"
                : "Performance Summary & Diagnostics"}
            </h1>
            <p className="text-[13px] text-[#555555] max-w-lg leading-relaxed">
              {report.mode === "INTERVIEW"
                ? "Comprehensive SDE rubric scorecard, qualitative feedback, and multi-criteria evaluation of your interview turns."
                : "Diagnostic breakdown of your answers, curriculum topic mastery, and verified explanations for every question."}
            </p>
          </div>

          {/* Score Dial & Accuracy Card */}
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
                <span className="absolute font-mono text-[14px] font-bold text-[#111111]">
                  {summary.scorePercent}%
                </span>
              </div>
              <span className="text-[11px] font-mono text-[#8A8A8A] mt-1">Total Score</span>
            </div>

            <div className="h-12 w-px bg-[#E5E5E5]" />

            <div className="flex flex-col justify-center">
              <span className="font-mono text-[11px] uppercase tracking-wider text-[#8A8A8A]">
                Accuracy of Answered
              </span>
              <span className="text-[20px] font-bold text-[#111111] mt-0.5">
                {summary.accuracyOfAnsweredPercent}%
              </span>
              {scoreDelta !== null ? (
                <span
                  className={`text-[11px] font-mono mt-0.5 ${
                    scoreDelta >= 0 ? "text-[#16A34A]" : "text-[#DC2626]"
                  }`}
                >
                  {scoreDelta >= 0 ? "+" : ""}
                  {scoreDelta.toFixed(1)}% vs previous
                </span>
              ) : (
                <span className="text-[11px] text-[#8A8A8A] mt-0.5">Baseline attempt</span>
              )}
            </div>
          </div>
        </CardContent>

        {/* Telemetry Metrics Strip */}
        <div className="grid grid-cols-2 sm:grid-cols-5 gap-4 px-6 py-4 border-t border-[#E5E5E5] bg-[#FAFAFA] text-[12px]">
          <div>
            <span className="text-[#8A8A8A] block">Duration</span>
            <span className="font-mono font-medium text-[#111111]">
              {Math.floor(report.elapsedSec / 60)}m {report.elapsedSec % 60}s
            </span>
          </div>
          <div>
            <span className="text-[#8A8A8A] block">Total Questions</span>
            <span className="font-mono font-medium text-[#111111]">
              {summary.totalQuestions}
            </span>
          </div>
          <div>
            <span className="text-[#8A8A8A] block">Correct Answers</span>
            <span className="font-mono font-medium text-[#16A34A] flex items-center gap-1">
              <CheckCircle2 className="w-3.5 h-3.5" />
              {summary.correctCount} ({summary.scorePercent}%)
            </span>
          </div>
          <div>
            <span className="text-[#8A8A8A] block">Incorrect Answers</span>
            <span className="font-mono font-medium text-[#DC2626] flex items-center gap-1">
              <XCircle className="w-3.5 h-3.5" />
              {incorrectCount}
            </span>
          </div>
          <div>
            <span className="text-[#8A8A8A] block">Unanswered / Skipped</span>
            <span className="font-mono font-medium text-[#D97706] flex items-center gap-1">
              <HelpCircle className="w-3.5 h-3.5" />
              {summary.unansweredCount}
            </span>
          </div>
        </div>
      </Card>

      {/* AI Evaluation Notice Banner (Requirement 7) */}
      <div className="p-3.5 rounded-xl bg-[#FAFAFA] border border-[#E5E5E5] flex items-center gap-2.5 text-[12px] text-[#555555]">
        <ShieldAlert className="w-4 h-4 text-[#8A8A8A] shrink-0" />
        <span>
          <strong className="text-[#111111]">AI Disclaimer:</strong> AI-generated feedback can contain mistakes. Verify technical explanations, edge cases, and rubric criteria against official engineering documentation.
        </span>
      </div>

      {/* ─── 3. Topics Breakdown ─────────────────────────────────── */}
      {report.topics.length > 0 && (
        <div className="flex flex-col gap-3">
          <div>
            <h2 className="text-[17px] font-bold text-[#111111]">
              Topic Competency Breakdown
            </h2>
            <p className="text-[12px] text-[#8A8A8A]">
              Performance categorized by domain topic accuracy
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
            {report.topics.map((t) => (
              <Card key={t.topicId} className="p-4 flex flex-col justify-between gap-3 bg-white border-[#E5E5E5]">
                <div className="flex items-center justify-between gap-2">
                  <span className="text-[13px] font-semibold text-[#111111] truncate">
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
                    className="text-[10px] shrink-0"
                  >
                    {t.label}
                  </Badge>
                </div>

                <div className="flex items-center gap-3">
                  <Progress value={t.accuracyPercent} className="h-1.5 flex-1" />
                  <span className="font-mono text-[12px] font-bold text-[#111111] w-10 text-right">
                    {t.accuracyPercent}%
                  </span>
                </div>

                <div className="flex items-center justify-between text-[11px] font-mono text-[#8A8A8A] pt-1.5 border-t border-[#E5E5E5]">
                  <span>{t.correct} of {t.questionsInTopic} correct</span>
                  <span>{t.answered} answered</span>
                </div>
              </Card>
            ))}
          </div>
        </div>
      )}


      {/* ─── 5. Question-by-Question Review Section ────────────────── */}
      <div className="flex flex-col gap-4 pt-2 border-t border-[#E5E5E5]">
        {/* Section Header with Controls */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <h2 className="text-[18px] font-bold text-[#111111] flex items-center gap-2">
              <BookOpen className="w-5 h-5 text-[#111111]" />
              <span>Question Review & Solutions</span>
            </h2>
            <p className="text-[13px] text-[#8A8A8A]">
              Step through question-by-question to analyze what you got wrong and study benchmark solutions.
            </p>
          </div>

          {/* View Mode Toggle */}
          <div className="flex items-center gap-1.5 bg-[#EEEEEE] p-1 rounded-lg self-start sm:self-auto">
            <button
              onClick={() => setViewMode("step")}
              className={`flex items-center gap-1.5 px-3 py-1 rounded-md text-[12px] font-medium transition-colors ${
                viewMode === "step"
                  ? "bg-white text-[#111111] shadow-sm"
                  : "text-[#555555] hover:text-[#111111]"
              }`}
            >
              <Columns className="w-3.5 h-3.5" />
              <span>Step-by-Step</span>
            </button>
            <button
              onClick={() => setViewMode("all")}
              className={`flex items-center gap-1.5 px-3 py-1 rounded-md text-[12px] font-medium transition-colors ${
                viewMode === "all"
                  ? "bg-white text-[#111111] shadow-sm"
                  : "text-[#555555] hover:text-[#111111]"
              }`}
            >
              <LayoutList className="w-3.5 h-3.5" />
              <span>View All</span>
            </button>
          </div>
        </div>

        {/* Filter Pills */}
        <div className="flex flex-wrap items-center gap-2 pt-1">
          <button
            onClick={() => {
              setFilter("all");
              setActiveQuestionIndex(0);
            }}
            className={`px-3 py-1.5 rounded-lg text-[12px] font-medium border transition-colors ${
              filter === "all"
                ? "bg-[#111111] text-white border-[#111111]"
                : "bg-white text-[#555555] border-[#E5E5E5] hover:border-[#111111]"
            }`}
          >
            All Questions ({report.questions.length})
          </button>
          <button
            onClick={() => {
              setFilter("incorrect");
              setActiveQuestionIndex(0);
            }}
            className={`px-3 py-1.5 rounded-lg text-[12px] font-medium border transition-colors flex items-center gap-1.5 ${
              filter === "incorrect"
                ? "bg-[#DC2626] text-white border-[#DC2626]"
                : "bg-white text-[#DC2626] border-[#FECACA] hover:bg-[#FEE2E2]/30"
            }`}
          >
            <XCircle className="w-3.5 h-3.5" />
            <span>Incorrect ({incorrectCount})</span>
          </button>
          <button
            onClick={() => {
              setFilter("correct");
              setActiveQuestionIndex(0);
            }}
            className={`px-3 py-1.5 rounded-lg text-[12px] font-medium border transition-colors flex items-center gap-1.5 ${
              filter === "correct"
                ? "bg-[#16A34A] text-white border-[#16A34A]"
                : "bg-white text-[#16A34A] border-[#BBF7D0] hover:bg-[#DCFCE7]/30"
            }`}
          >
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>Correct ({summary.correctCount})</span>
          </button>
          {summary.unansweredCount > 0 && (
            <button
              onClick={() => {
                setFilter("unanswered");
                setActiveQuestionIndex(0);
              }}
              className={`px-3 py-1.5 rounded-lg text-[12px] font-medium border transition-colors flex items-center gap-1.5 ${
                filter === "unanswered"
                  ? "bg-[#D97706] text-white border-[#D97706]"
                  : "bg-white text-[#D97706] border-[#FDE68A] hover:bg-[#FEF3C7]/30"
              }`}
            >
              <HelpCircle className="w-3.5 h-3.5" />
              <span>Unanswered ({summary.unansweredCount})</span>
            </button>
          )}
        </div>

        {/* Question Selector Palette (In Step Mode) */}
        {viewMode === "step" && (
          <div className="bg-white border border-[#E5E5E5] rounded-xl p-4 shadow-sm">
            <div className="flex items-center justify-between mb-3 text-[12px]">
              <span className="font-semibold text-[#111111]">Question Navigator</span>
              <span className="text-[#8A8A8A] font-mono">
                Click any question to view
              </span>
            </div>

            <div className="flex flex-wrap gap-2">
              {filteredQuestions.map((q, idx) => {
                const isCurrent = idx === safeActiveIndex;
                const isCorrect = q.correct === true;
                const isIncorrect = q.correct === false;

                let stateClasses = "bg-white border-[#E5E5E5] text-[#555555]";
                if (isCorrect) {
                  stateClasses = "bg-[#DCFCE7]/40 border-[#86EFAC] text-[#15803D]";
                } else if (isIncorrect) {
                  stateClasses = "bg-[#FEE2E2]/40 border-[#FCA5A5] text-[#B91C1C]";
                } else {
                  stateClasses = "bg-[#FEF3C7]/40 border-[#FDE68A] text-[#B45309]";
                }

                if (isCurrent) {
                  stateClasses += " ring-2 ring-[#111111] ring-offset-1 font-bold shadow-sm";
                }

                return (
                  <button
                    key={q.questionId}
                    onClick={() => setActiveQuestionIndex(idx)}
                    className={`h-9 px-3 rounded-lg border text-[13px] font-mono flex items-center gap-1.5 transition-all ${stateClasses}`}
                    title={`Question ${q.position}: ${q.topicName}`}
                  >
                    <span>Q{q.position}</span>
                    {isCorrect && <CheckCircle2 className="w-3 h-3" />}
                    {isIncorrect && <XCircle className="w-3 h-3" />}
                    {!q.answered && <HelpCircle className="w-3 h-3" />}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* ─── Question Content (Step-by-Step Mode) ─── */}
        {viewMode === "step" && currentQuestion && (
          <Card className="border-[#E5E5E5] shadow-sm bg-white overflow-hidden">
            {/* Top Bar of the Question */}
            <div className="px-6 py-4 bg-[#FAFAFA] border-b border-[#E5E5E5] flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2.5">
                <span className="font-mono text-[14px] font-bold text-[#111111]">
                  Question {currentQuestion.position} of {report.questions.length}
                </span>
                <Badge variant="outline" className="text-[11px] font-mono">
                  {currentQuestion.topicName}
                </Badge>
                {currentQuestion.timeSpentSec != null && (
                  <span className="text-[11px] font-mono text-[#8A8A8A] flex items-center gap-1">
                    <Clock className="w-3 h-3" />
                    {currentQuestion.timeSpentSec}s
                  </span>
                )}
              </div>

              {/* Status Badge */}
              <div>
                {currentQuestion.correct === true ? (
                  <Badge variant="success" className="text-[12px] px-3 py-1 gap-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    <span>Correct Answer</span>
                  </Badge>
                ) : currentQuestion.correct === false ? (
                  <Badge variant="critical" className="text-[12px] px-3 py-1 gap-1.5">
                    <XCircle className="w-3.5 h-3.5" />
                    <span>Incorrect Answer</span>
                  </Badge>
                ) : (
                  <Badge variant="warning" className="text-[12px] px-3 py-1 gap-1.5">
                    <HelpCircle className="w-3.5 h-3.5" />
                    <span>Unanswered</span>
                  </Badge>
                )}
              </div>
            </div>

            <CardContent className="p-6 sm:p-8 flex flex-col gap-6">
              {/* Question Text */}
              <div className="text-[16px] sm:text-[17px] font-medium text-[#111111] leading-relaxed whitespace-pre-line">
                {currentQuestion.body}
              </div>

              {/* Branch on Mode: Interview vs Quiz */}
              {report.mode === "INTERVIEW" ? (
                <div className="flex flex-col gap-5">
                  {/* Candidate Answer */}
                  <div className="p-4 rounded-xl bg-white border border-[#E5E5E5] flex flex-col gap-1.5 shadow-xs">
                    <span className="text-[12px] font-semibold uppercase tracking-wider text-[#8A8A8A]">
                      Your Submitted Answer
                    </span>
                    <p className="text-[14px] text-[#111111] leading-relaxed whitespace-pre-wrap">
                      {report.evaluations?.find((e) => e.questionId === currentQuestion.questionId)?.candidateAnswer ||
                        currentQuestion.selectedOptionExplanation ||
                        "Candidate technical response recorded during interview turn."}
                    </p>
                  </div>

                  {/* AI Evaluator Feedback */}
                  <div className="p-5 rounded-xl bg-[#FAFAFA] border border-[#E5E5E5] flex flex-col gap-3">
                    <div className="flex items-center justify-between flex-wrap gap-2">
                      <div className="flex items-center gap-2 text-[#111111] font-semibold text-[14px]">
                        <BrainCircuit className="w-4 h-4 text-[#111111]" />
                        <span>Rubric Evaluation &amp; SDE Critique:</span>
                      </div>
                      {(() => {
                        const qEval = report.evaluations?.find(
                          (e) => e.questionId === currentQuestion.questionId
                        );
                        return qEval ? (
                          <Badge variant="outline" className="font-mono text-xs">
                            Criterion: {qEval.rubricCriterionName} • Score: {qEval.score}%
                          </Badge>
                        ) : null;
                      })()}
                    </div>

                    <div className="text-[13px] text-[#444444] leading-relaxed whitespace-pre-line pl-6">
                      {report.evaluations?.find((e) => e.questionId === currentQuestion.questionId)?.feedback ||
                        currentQuestion.explanation ||
                        "Detailed rubric critique."}
                    </div>

                    {(() => {
                      const qEval = report.evaluations?.find(
                        (e) => e.questionId === currentQuestion.questionId
                      );
                      return qEval?.evidenceQuote ? (
                        <div className="ml-6 p-3 rounded-lg bg-white border border-[#E5E5E5] text-[12px] text-[#555555] italic">
                          &ldquo;{qEval.evidenceQuote}&rdquo;
                        </div>
                      ) : null;
                    })()}

                    {/* Thumbs Up / Down per question (Requirement 7) */}
                    {(() => {
                      const qEval = report.evaluations?.find(
                        (e) => e.questionId === currentQuestion.questionId
                      );
                      const evalId = qEval?.id ?? currentQuestion.questionId;
                      return (
                        <div className="ml-6 pt-3 border-t border-[#E5E5E5] flex items-center justify-between flex-wrap gap-3">
                          <span className="text-[12px] text-[#8A8A8A]">
                            Was this AI evaluation helpful?
                          </span>
                          <div className="flex items-center gap-2">
                            <button
                              type="button"
                              disabled={submittingFeedback[evalId]}
                              onClick={() => handleThumbFeedback(evalId, "UP")}
                              className={`px-2.5 py-1.5 rounded-lg border text-xs flex items-center gap-1.5 transition-colors cursor-pointer ${
                                feedbackGiven[evalId] === "UP"
                                  ? "bg-[#DCFCE7] border-[#86EFAC] text-[#16A34A] font-semibold"
                                  : "bg-white border-[#E5E5E5] text-[#555555] hover:text-[#111111]"
                              }`}
                            >
                              <ThumbsUp className="w-3.5 h-3.5" />
                              <span>Helpful</span>
                            </button>
                            <button
                              type="button"
                              disabled={submittingFeedback[evalId]}
                              onClick={() => handleThumbFeedback(evalId, "DOWN")}
                              className={`px-2.5 py-1.5 rounded-lg border text-xs flex items-center gap-1.5 transition-colors cursor-pointer ${
                                feedbackGiven[evalId] === "DOWN"
                                  ? "bg-[#FEE2E2] border-[#FCA5A5] text-[#DC2626] font-semibold"
                                  : "bg-white border-[#E5E5E5] text-[#555555] hover:text-[#111111]"
                              }`}
                            >
                              <ThumbsDown className="w-3.5 h-3.5" />
                              <span>Needs Improvement</span>
                            </button>
                            {feedbackGiven[evalId] && (
                              <span className="text-[11px] text-[#16A34A] font-medium ml-1">
                                Feedback saved!
                              </span>
                            )}
                          </div>
                        </div>
                      );
                    })()}
                  </div>
                </div>
              ) : (
                <>
                  {/* MCQ Options with Clear Diagnostics */}
                  <div className="flex flex-col gap-3">
                    <span className="text-[12px] font-semibold uppercase tracking-wider text-[#8A8A8A]">
                      Options & Your Selection
                    </span>

                    <div className="flex flex-col gap-2.5">
                      {currentQuestion.options.map((opt, idx) => {
                        const letter = String.fromCharCode(65 + idx);
                        const isSelected = currentQuestion.selectedOptionId === opt.id;
                        const isCorrect = currentQuestion.correctOptionId === opt.id;

                        let containerStyle = "border-[#E5E5E5] bg-white text-[#333333]";
                        let letterBadgeStyle = "bg-[#EEEEEE] text-[#555555]";

                        if (isCorrect && isSelected) {
                          // Correctly selected
                          containerStyle =
                            "border-[#16A34A] bg-[#DCFCE7]/30 text-[#111111] ring-1 ring-[#16A34A]";
                          letterBadgeStyle = "bg-[#16A34A] text-white";
                        } else if (isCorrect && !isSelected) {
                          // Correct option but user didn't pick it
                          containerStyle =
                            "border-[#16A34A] bg-[#DCFCE7]/20 text-[#111111]";
                          letterBadgeStyle = "bg-[#16A34A] text-white";
                        } else if (isSelected && !isCorrect) {
                          // Wrong option picked by user
                          containerStyle =
                            "border-[#DC2626] bg-[#FEE2E2]/30 text-[#111111] ring-1 ring-[#DC2626]";
                          letterBadgeStyle = "bg-[#DC2626] text-white";
                        }

                        return (
                          <div
                            key={opt.id}
                            className={`p-4 rounded-xl border text-[14px] flex flex-col sm:flex-row sm:items-center justify-between gap-3 transition-all ${containerStyle}`}
                          >
                            <div className="flex items-start sm:items-center gap-3">
                              <span
                                className={`w-7 h-7 rounded-lg font-mono text-[12px] font-bold flex items-center justify-center shrink-0 ${letterBadgeStyle}`}
                              >
                                {letter}
                              </span>
                              <span className="font-normal text-[#111111] leading-relaxed">
                                {opt.text}
                              </span>
                            </div>

                            {/* Badges on the right */}
                            <div className="flex items-center gap-2 pl-10 sm:pl-0 shrink-0">
                              {isSelected && isCorrect && (
                                <Badge variant="success" className="text-[11px] gap-1">
                                  <CheckCircle2 className="w-3 h-3" />
                                  <span>Your Answer (Correct)</span>
                                </Badge>
                              )}
                              {isSelected && !isCorrect && (
                                <Badge variant="critical" className="text-[11px] gap-1">
                                  <XCircle className="w-3 h-3" />
                                  <span>Your Answer (Incorrect)</span>
                                </Badge>
                              )}
                              {isCorrect && !isSelected && (
                                <Badge variant="success" className="text-[11px] gap-1">
                                  <CheckCircle2 className="w-3 h-3" />
                                  <span>Correct Option</span>
                                </Badge>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>

                  {/* ─── Detailed Solution & Benchmark Explanation Box ─── */}
                  <div className="mt-2 flex flex-col gap-3">
                    {currentQuestion.explanation && (
                      <div className="p-5 rounded-xl bg-[#FAFAFA] border border-[#E5E5E5] flex flex-col gap-2.5">
                        <div className="flex items-center gap-2 text-[#111111] font-semibold text-[14px]">
                          <BookOpen className="w-4 h-4 text-[#16A34A]" />
                          <span>Benchmark Solution & Explanation:</span>
                        </div>
                        <div className="text-[13px] text-[#444444] leading-relaxed whitespace-pre-line pl-6">
                          {currentQuestion.explanation}
                        </div>
                      </div>
                    )}

                    {/* Specific Misconception Breakdown for user's chosen option */}
                    {currentQuestion.selectedOptionExplanation && (
                      <div className="p-4 rounded-xl bg-[#FEE2E2]/20 border border-[#FECACA] flex flex-col gap-2">
                        <div className="flex items-center gap-2 text-[#DC2626] font-semibold text-[13px]">
                          <AlertCircle className="w-4 h-4" />
                          <span>Why your chosen option was incorrect:</span>
                        </div>
                        <div className="text-[13px] text-[#555555] leading-relaxed pl-6">
                          {currentQuestion.selectedOptionExplanation}
                        </div>
                      </div>
                    )}

                    {/* Unanswered Callout */}
                    {!currentQuestion.answered && (
                      <div className="p-4 rounded-xl bg-[#FEF3C7]/20 border border-[#FDE68A] flex items-center gap-2.5 text-[13px] text-[#92400E]">
                        <HelpCircle className="w-4 h-4 shrink-0" />
                        <span>
                          You skipped this question during the exam. The correct option is highlighted above with its benchmark explanation.
                        </span>
                      </div>
                    )}
                  </div>
                </>
              )}
            </CardContent>

            {/* Bottom Next / Previous Controls */}
            <div className="px-6 py-4 bg-[#FAFAFA] border-t border-[#E5E5E5] flex items-center justify-between gap-4">
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setActiveQuestionIndex((prev) => Math.max(0, prev - 1))}
                disabled={safeActiveIndex === 0}
                className="gap-1.5"
              >
                <ChevronLeft className="w-4 h-4" />
                <span>Previous Question</span>
              </Button>

              <div className="text-[13px] font-mono text-[#8A8A8A]">
                {safeActiveIndex + 1} of {filteredQuestions.length}
              </div>

              <Button
                variant="primary"
                size="sm"
                onClick={() =>
                  setActiveQuestionIndex((prev) =>
                    Math.min(filteredQuestions.length - 1, prev + 1)
                  )
                }
                disabled={safeActiveIndex >= filteredQuestions.length - 1}
                className="gap-1.5"
              >
                <span>Next Question</span>
                <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          </Card>
        )}

        {/* ─── Empty state if filter has no results ─── */}
        {viewMode === "step" && filteredQuestions.length === 0 && (
          <div className="p-12 rounded-xl border border-[#E5E5E5] bg-white text-center flex flex-col items-center justify-center gap-3">
            <CheckCircle2 className="w-10 h-10 text-[#16A34A]" />
            <h3 className="text-[16px] font-semibold text-[#111111]">
              No questions found for &quot;{filter}&quot;
            </h3>
            <p className="text-[13px] text-[#555555]">
              {filter === "incorrect"
                ? "Outstanding! You got 100% of your answered questions correct."
                : "No questions match this filter."}
            </p>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setFilter("all")}
              className="mt-2"
            >
              Show All Questions
            </Button>
          </div>
        )}

        {/* ─── View All Mode (Full Stacked List) ─── */}
        {viewMode === "all" && (
          <div className="flex flex-col gap-5">
            {filteredQuestions.map((q) => (
              <Card key={q.questionId} className="p-6 flex flex-col gap-4 border-[#E5E5E5] bg-white">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-[13px] font-bold text-[#111111]">
                      Q{q.position}
                    </span>
                    <Badge variant="outline" className="text-[10px] font-mono">
                      {q.topicName}
                    </Badge>
                  </div>
                  <Badge
                    variant={
                      q.correct === true
                        ? "success"
                        : q.correct === false
                        ? "critical"
                        : "warning"
                    }
                    className="text-[11px]"
                  >
                    {q.correct === true
                      ? "Correct"
                      : q.correct === false
                      ? "Incorrect"
                      : "Unanswered"}
                  </Badge>
                </div>

                <div className="text-[15px] font-medium text-[#111111] leading-snug">
                  {q.body}
                </div>

                {report.mode === "INTERVIEW" ? (
                  <div className="flex flex-col gap-3 pt-1">
                    <div className="p-3.5 rounded-lg bg-white border border-[#E5E5E5] text-[13px] flex flex-col gap-1">
                      <span className="text-[11px] font-semibold uppercase text-[#8A8A8A]">
                        Candidate Response:
                      </span>
                      <p className="text-[#111111] whitespace-pre-wrap">
                        {report.evaluations?.find((e) => e.questionId === q.questionId)?.candidateAnswer ||
                          q.selectedOptionExplanation ||
                          "Candidate technical response recorded during interview turn."}
                      </p>
                    </div>

                    <div className="p-4 rounded-lg bg-[#FAFAFA] border border-[#E5E5E5] flex flex-col gap-2.5">
                      <div className="flex items-center justify-between flex-wrap gap-2">
                        <span className="text-[13px] font-semibold text-[#111111] flex items-center gap-1.5">
                          <BrainCircuit className="w-3.5 h-3.5" />
                          <span>Evaluation &amp; Critique</span>
                        </span>
                        {(() => {
                          const qEval = report.evaluations?.find((e) => e.questionId === q.questionId);
                          return qEval ? (
                            <Badge variant="outline" className="font-mono text-[10px]">
                              {qEval.rubricCriterionName} • {qEval.score}%
                            </Badge>
                          ) : null;
                        })()}
                      </div>
                      <p className="text-[12px] text-[#444444] leading-relaxed">
                        {report.evaluations?.find((e) => e.questionId === q.questionId)?.feedback ||
                          q.explanation}
                      </p>

                      {/* Thumbs Up / Down in View All mode */}
                      {(() => {
                        const qEval = report.evaluations?.find((e) => e.questionId === q.questionId);
                        const evalId = qEval?.id ?? q.questionId;
                        return (
                          <div className="pt-2 border-t border-[#E5E5E5] flex items-center justify-between">
                            <span className="text-[11px] text-[#8A8A8A]">Was this helpful?</span>
                            <div className="flex items-center gap-2">
                              <button
                                type="button"
                                disabled={submittingFeedback[evalId]}
                                onClick={() => handleThumbFeedback(evalId, "UP")}
                                className={`p-1.5 rounded-md border text-xs flex items-center gap-1 ${
                                  feedbackGiven[evalId] === "UP"
                                    ? "bg-[#DCFCE7] border-[#86EFAC] text-[#16A34A]"
                                    : "bg-white border-[#E5E5E5] text-[#555555]"
                                }`}
                              >
                                <ThumbsUp className="w-3 h-3" />
                              </button>
                              <button
                                type="button"
                                disabled={submittingFeedback[evalId]}
                                onClick={() => handleThumbFeedback(evalId, "DOWN")}
                                className={`p-1.5 rounded-md border text-xs flex items-center gap-1 ${
                                  feedbackGiven[evalId] === "DOWN"
                                    ? "bg-[#FEE2E2] border-[#FCA5A5] text-[#DC2626]"
                                    : "bg-white border-[#E5E5E5] text-[#555555]"
                                }`}
                              >
                                <ThumbsDown className="w-3 h-3" />
                              </button>
                            </div>
                          </div>
                        );
                      })()}
                    </div>
                  </div>
                ) : (
                  <>
                    {/* Options */}
                    <div className="flex flex-col gap-2 pt-1">
                      {q.options.map((opt, idx) => {
                        const letter = String.fromCharCode(65 + idx);
                        const isSelected = q.selectedOptionId === opt.id;
                        const isCorrect = q.correctOptionId === opt.id;

                        let borderStyle = "border-[#E5E5E5] bg-white";
                        if (isCorrect) {
                          borderStyle = "border-[#16A34A] bg-[#DCFCE7]/20";
                        } else if (isSelected && !isCorrect) {
                          borderStyle = "border-[#DC2626] bg-[#FEE2E2]/20";
                        }

                        return (
                          <div
                            key={opt.id}
                            className={`p-3 rounded-lg border text-[13px] flex items-center justify-between gap-3 ${borderStyle}`}
                          >
                            <div className="flex items-center gap-3">
                              <span className="font-mono font-bold text-[#111111] w-5">
                                {letter}.
                              </span>
                              <span className="text-[#111111]">{opt.text}</span>
                            </div>
                            <div className="flex items-center gap-2 shrink-0">
                              {isSelected && (
                                <Badge
                                  variant={isCorrect ? "success" : "critical"}
                                  className="text-[10px]"
                                >
                                  {isCorrect ? "Your Answer" : "Your Answer"}
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
                      <div className="p-4 rounded-lg bg-[#FAFAFA] border border-[#E5E5E5] text-[12px] text-[#555555] leading-relaxed">
                        <span className="font-semibold text-[#111111] block mb-1">
                          Benchmark Explanation:
                        </span>
                        <p>{q.explanation}</p>
                        {q.selectedOptionExplanation && (
                          <p className="mt-2 text-[#DC2626]">
                            {q.selectedOptionExplanation}
                          </p>
                        )}
                      </div>
                    )}
                  </>
                )}
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* ─── Recommended Focus Areas (Placed at the end after questions & solutions) ─── */}
      {report.recommendations.length > 0 && (
        <Card className="border-[#E5E5E5] bg-[#FAFAFA]">
          <CardHeader className="pb-3 flex flex-row items-center gap-2">
            <Target className="w-4 h-4 text-[#111111]" />
            <CardTitle className="text-[15px] font-semibold">Recommended Focus Areas</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-2.5">
            {report.recommendations.map((rec, i) => (
              <div
                key={i}
                className="p-3.5 rounded-xl border border-[#E5E5E5] bg-white flex flex-col sm:flex-row sm:items-center justify-between gap-3"
              >
                <div className="flex flex-col gap-0.5">
                  <span className="text-[13px] font-semibold text-[#111111]">
                    {rec.topicName}
                  </span>
                  <span className="text-[12px] text-[#555555]">{rec.reason}</span>
                </div>
                <Link
                  href={`/practice?trackId=${report.trackId}&topicId=${rec.topicId}&count=${rec.suggestedQuestionCount}`}
                >
                  <Button size="sm" variant="secondary" className="gap-1.5 shrink-0 text-[12px]">
                    <span>Practice Drill ({rec.suggestedQuestionCount} Qs)</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </Button>
                </Link>
              </div>
            ))}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
