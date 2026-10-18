"use client";

import { useState, useEffect, use } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Clock,
  ArrowRight,
  Check,
  X,
  Lightbulb,
  Flag,
  RotateCcw,
  LogOut,
  ShieldCheck,
  AlertCircle,
  RefreshCw,
} from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { getSession, submitQuizAnswer, completeSession } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import { QuestionView, AnswerResponse } from "@/lib/types";

const OPTION_LETTERS = ["A", "B", "C", "D"];

export default function ActiveQuizPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const resolvedParams = use(params);
  const sessionId = resolvedParams.id;

  // Session Query
  const {
    data: sessionData,
    isLoading: sessionLoading,
    isError: sessionError,
    error: sessionErrorObj,
    refetch: refetchSession,
  } = useQuery({
    queryKey: queryKeys.sessions.detail(sessionId),
    queryFn: () => getSession(sessionId),
  });

  // State
  const [currentIndex, setCurrentIndex] = useState(0);
  const [totalQuestions, setTotalQuestions] = useState(8);
  const [timeRemaining, setTimeRemaining] = useState(120);
  const [selectedOptionId, setSelectedOptionId] = useState<number | null>(null);
  const [isAnswerSubmitted, setIsAnswerSubmitted] = useState(false);
  const [answerResult, setAnswerResult] = useState<AnswerResponse | null>(null);
  const [currentQuestion, setCurrentQuestion] = useState<QuestionView | null>(null);
  const [timeSpentSec, setTimeSpentSec] = useState(0);

  // Initialize from sessionData
  useEffect(() => {
    if (!sessionData) return;
    if (sessionData.status === "COMPLETED") {
      router.replace(`/practice/quiz/${sessionId}/report`);
      return;
    }
    setCurrentIndex(sessionData.currentIndex);
    setTotalQuestions(sessionData.totalQuestions);
    if (sessionData.currentQuestion) {
      setCurrentQuestion(sessionData.currentQuestion);
    }
  }, [sessionData, sessionId, router]);

  // Timer countdown
  useEffect(() => {
    if (isAnswerSubmitted) return;
    const interval = setInterval(() => {
      setTimeRemaining((prev) => (prev > 0 ? prev - 1 : 0));
      setTimeSpentSec((prev) => prev + 1);
    }, 1000);
    return () => clearInterval(interval);
  }, [isAnswerSubmitted]);

  // Helper to invalidate all learning loop queries
  const invalidateAllLearningQueries = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.me.planToday });
    queryClient.invalidateQueries({ queryKey: ["me", "skills"] });
    queryClient.invalidateQueries({ queryKey: ["me", "progress"] });
    queryClient.invalidateQueries({ queryKey: ["me", "review"] });
    queryClient.invalidateQueries({ queryKey: queryKeys.sessions.detail(sessionId) });
    queryClient.invalidateQueries({ queryKey: ["sessions", "history"] });
  };

  // Submit Answer Mutation
  const submitMutation = useMutation({
    mutationFn: () =>
      submitQuizAnswer(sessionId, selectedOptionId!, undefined, timeSpentSec),
    onSuccess: (data) => {
      setAnswerResult(data);
      setIsAnswerSubmitted(true);
      invalidateAllLearningQueries();
    },
  });

  // Complete Session Mutation
  const completeMutation = useMutation({
    mutationFn: () => completeSession(sessionId),
    onSuccess: () => {
      invalidateAllLearningQueries();
      router.push(`/practice/quiz/${sessionId}/report`);
    },
  });

  const handleSubmitAnswer = () => {
    if (!selectedOptionId || isAnswerSubmitted || submitMutation.isPending) return;
    submitMutation.mutate();
  };

  const handleNextQuestion = () => {
    if (
      currentIndex + 1 >= totalQuestions ||
      answerResult?.sessionStatus === "COMPLETED" ||
      !answerResult?.nextQuestion
    ) {
      completeMutation.mutate();
      return;
    }

    setCurrentQuestion(answerResult.nextQuestion);
    setCurrentIndex((prev) => prev + 1);
    setSelectedOptionId(null);
    setIsAnswerSubmitted(false);
    setAnswerResult(null);
    setTimeRemaining(120);
    setTimeSpentSec(0);
  };

  // Keyboard shortcut listener (A, B, C, D and Enter)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!currentQuestion) return;
      if (isAnswerSubmitted) {
        if (e.key === "Enter") {
          handleNextQuestion();
        }
        return;
      }

      const key = e.key.toUpperCase();
      if (key === "A" && currentQuestion.options[0]) setSelectedOptionId(currentQuestion.options[0].id);
      if (key === "B" && currentQuestion.options[1]) setSelectedOptionId(currentQuestion.options[1].id);
      if (key === "C" && currentQuestion.options[2]) setSelectedOptionId(currentQuestion.options[2].id);
      if (key === "D" && currentQuestion.options[3]) setSelectedOptionId(currentQuestion.options[3].id);

      if (e.key === "Enter" && selectedOptionId) {
        handleSubmitAnswer();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [selectedOptionId, isAnswerSubmitted, currentQuestion]);

  const formatTimer = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
  };

  if (sessionLoading) {
    return (
      <div className="w-full max-w-[1000px] mx-auto px-6 py-8 flex flex-col gap-6 animate-pulse">
        <div className="h-6 w-48 bg-[#EEEEEE] rounded" />
        <div className="h-10 w-full bg-[#EEEEEE] rounded" />
        <div className="h-64 rounded-xl border border-[#E5E5E5] bg-[#FAFAFA]" />
      </div>
    );
  }

  if (sessionError || (!currentQuestion && !sessionData)) {
    return (
      <div className="w-full max-w-[1000px] mx-auto px-6 py-12 flex flex-col items-center justify-center min-h-[50vh]">
        <div className="p-8 rounded-xl border border-[#E5E5E5] bg-white max-w-md w-full flex flex-col items-center text-center gap-4">
          <div className="w-12 h-12 rounded-full bg-[#FEE2E2] flex items-center justify-center text-[#DC2626]">
            <AlertCircle className="w-6 h-6" />
          </div>
          <div className="flex flex-col gap-1">
            <h3 className="text-[18px] font-semibold text-[#111111]">Failed to load quiz session</h3>
            <p className="text-[13px] text-[#555555]">
              {sessionErrorObj instanceof Error ? sessionErrorObj.message : "Unable to retrieve session."}
            </p>
          </div>
          <Button onClick={() => refetchSession()} className="gap-2 mt-2 w-full">
            <RefreshCw className="w-4 h-4" />
            <span>Retry</span>
          </Button>
        </div>
      </div>
    );
  }

  if (!currentQuestion) return null;

  return (
    <div className="w-full max-w-[1000px] mx-auto px-6 py-8 flex flex-col gap-6">
      {/* 1. Top Status Ribbon */}
      <div className="flex flex-col gap-3 pb-4 border-b border-[#E5E5E5]">
        <div className="flex items-center justify-between text-[13px]">
          <div className="flex items-center gap-2">
            <Badge variant="primary" className="text-[11px] font-mono">
              QUESTION {currentIndex + 1} OF {totalQuestions}
            </Badge>
            <span className="text-[#8A8A8A] font-mono">•</span>
            <span className="font-mono text-[11px] uppercase tracking-wider text-[#555555]">
              {currentQuestion.topicName}
            </span>
          </div>

          <div className="flex items-center gap-4">
            <div className="flex items-center gap-1.5 font-mono text-[12px] font-medium text-[#111111]">
              <Clock className="w-3.5 h-3.5 text-[#555555]" />
              <span>{formatTimer(timeRemaining)} remaining</span>
            </div>
            <Link
              href="/dashboard"
              onClick={invalidateAllLearningQueries}
              className="text-[#8A8A8A] hover:text-[#111111] flex items-center gap-1 text-[12px] transition-colors"
            >
              <LogOut className="w-3.5 h-3.5" />
              <span>Save & Exit</span>
            </Link>
          </div>
        </div>

        {/* Micro-rail progress */}
        <Progress value={((currentIndex + 1) / totalQuestions) * 100} className="h-1.5" />
      </div>

      {/* 2. Question Prompt Area */}
      <div className="flex flex-col gap-3 pt-2">
        <div className="flex items-center gap-2">
          <Badge variant="default" className="text-[11px]">
            Difficulty: Level {currentQuestion.difficulty}
          </Badge>
          <span className="text-[12px] text-[#8A8A8A]">Single Choice MCQ</span>
        </div>

        <h2 className="text-[22px] font-semibold text-[#111111] leading-snug tracking-tight">
          {currentQuestion.body}
        </h2>
      </div>

      {/* 3. Interactive MCQ Option Cards */}
      <div className="grid grid-cols-1 gap-3 pt-2">
        {currentQuestion.options.map((opt, idx) => {
          const letter = OPTION_LETTERS[idx];
          const isSelected = selectedOptionId === opt.id;
          const isCorrect = isAnswerSubmitted && Number(answerResult?.correctOptionId) === Number(opt.id);
          const isIncorrectSelection = isAnswerSubmitted && isSelected && !answerResult?.correct;

          let cardStyles = "border-[#E5E5E5] bg-white hover:border-[#111111]";
          let badgeStyles = "bg-[#EEEEEE] text-[#111111]";

          if (isAnswerSubmitted) {
            if (isCorrect) {
              cardStyles = "border-[#16A34A] bg-[#DCFCE7]/20";
              badgeStyles = "bg-[#16A34A] text-white";
            } else if (isIncorrectSelection) {
              cardStyles = "border-[#DC2626] bg-[#FEE2E2]/20";
              badgeStyles = "bg-[#DC2626] text-white";
            } else {
              cardStyles = "border-[#E5E5E5] bg-white opacity-60";
            }
          } else if (isSelected) {
            cardStyles = "border-[#111111] ring-1 ring-[#111111] bg-white";
            badgeStyles = "bg-[#111111] text-white";
          }

          return (
            <div
              key={opt.id}
              onClick={() => {
                if (!isAnswerSubmitted) setSelectedOptionId(opt.id);
              }}
              className={`p-4 rounded-xl border transition-colors flex items-start gap-4 ${
                isAnswerSubmitted ? "cursor-default" : "cursor-pointer"
              } ${cardStyles}`}
            >
              <div
                className={`w-7 h-7 rounded-lg flex items-center justify-center font-mono text-[13px] font-semibold shrink-0 transition-colors ${badgeStyles}`}
              >
                {letter}
              </div>

              <div className="flex items-center justify-between flex-1 gap-4">
                <span className="text-[14px] text-[#111111] leading-relaxed">
                  {opt.text}
                </span>

                {isAnswerSubmitted && isCorrect && (
                  <Badge variant="success" className="gap-1 shrink-0">
                    <Check className="w-3 h-3" />
                    <span>Correct Answer</span>
                  </Badge>
                )}

                {isAnswerSubmitted && isIncorrectSelection && (
                  <Badge variant="critical" className="gap-1 shrink-0">
                    <X className="w-3 h-3" />
                    <span>Your Selection</span>
                  </Badge>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Pre-Submission Submit Bar */}
      {!isAnswerSubmitted && (
        <div className="flex items-center justify-between pt-4 border-t border-[#E5E5E5]/60 mt-2">
          <span className="text-[12px] text-[#8A8A8A] font-mono">
            Press A, B, C, D to pick • Enter to submit
          </span>

          <Button
            onClick={handleSubmitAnswer}
            disabled={!selectedOptionId || submitMutation.isPending}
            className="gap-2"
          >
            <span>{submitMutation.isPending ? "Evaluating Answer..." : "Submit Answer"}</span>
            <ArrowRight className="w-4 h-4" />
          </Button>
        </div>
      )}

      {/* Post-Submission Answer Explanation Console */}
      {isAnswerSubmitted && (
        <div className="flex flex-col gap-4 mt-2 animate-in fade-in duration-200">
          <Card className="border-[#111111] bg-white">
            <div className="p-4 bg-[#FAFAFA] border-b border-[#E5E5E5] flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Lightbulb className="w-4 h-4 text-[#111111]" />
                <span className="font-semibold text-[14px] text-[#111111]">
                  Option Analysis & Benchmark Rationale
                </span>
              </div>
              <Badge variant={answerResult?.correct ? "success" : "critical"}>
                {answerResult?.correct ? "Scored +1.0" : "Scored 0.0"}
              </Badge>
            </div>

            <CardContent className="p-5 flex flex-col gap-4">
              <div className="flex items-start gap-3">
                <p className="text-[13px] text-[#555555] leading-relaxed">
                  {answerResult?.explanation}
                </p>
              </div>
            </CardContent>
          </Card>

          {/* Action Row */}
          <div className="flex items-center justify-between pt-2">
            <div className="flex items-center gap-2 text-[12px] text-[#8A8A8A]">
              <ShieldCheck className="w-4 h-4 text-[#16A34A]" />
              <span>Zero AI tokens consumed · Instant Java verification</span>
            </div>
            <Button
              onClick={handleNextQuestion}
              disabled={completeMutation.isPending}
              className="gap-2"
            >
              <span>
                {completeMutation.isPending
                  ? "Finalizing Report..."
                  : currentIndex + 1 >= totalQuestions
                  ? "View Performance Report"
                  : "Next Question"}
              </span>
              <ArrowRight className="w-4 h-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
