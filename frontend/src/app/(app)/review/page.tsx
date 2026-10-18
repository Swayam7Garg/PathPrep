"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  RotateCcw,
  CheckCircle2,
  XCircle,
  Clock,
  ArrowRight,
  RefreshCw,
  Sparkles,
  AlertCircle,
  Check,
  Calendar,
  Layers,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { getDueReviews, submitReviewAnswer, ApiError } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import { ReviewAnswerResponse, DueReviewItem } from "@/lib/types";

export default function ReviewPage() {
  const queryClient = useQueryClient();

  // Queries
  const {
    data: reviewsData,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: queryKeys.me.reviewDue(),
    queryFn: () => getDueReviews(),
  });

  const items: DueReviewItem[] = reviewsData?.items ?? [];
  const dueCount = reviewsData?.dueCount ?? items.length;

  const [currentIndex, setCurrentIndex] = useState(0);
  const [selectedOptionId, setSelectedOptionId] = useState<number | null>(null);
  const [timeSpentSec, setTimeSpentSec] = useState(0);
  const [answerResult, setAnswerResult] = useState<ReviewAnswerResponse | null>(null);

  // Timer for active question
  useEffect(() => {
    if (answerResult) return;
    const interval = setInterval(() => {
      setTimeSpentSec((prev) => prev + 1);
    }, 1000);
    return () => clearInterval(interval);
  }, [answerResult, currentIndex]);

  // Mutation
  const answerMutation = useMutation({
    mutationFn: async ({
      reviewId,
      selectedId,
      timeSpent,
    }: {
      reviewId: number;
      selectedId: number;
      timeSpent: number;
    }) => {
      return submitReviewAnswer(reviewId, {
        selectedOptionId: selectedId,
        timeSpentSec: timeSpent,
      });
    },
    onSuccess: (result) => {
      setAnswerResult(result);
      // Invalidate queries across the application
      queryClient.invalidateQueries({ queryKey: queryKeys.me.planToday });
      queryClient.invalidateQueries({ queryKey: ["me", "skills"] });
      queryClient.invalidateQueries({ queryKey: ["me", "progress"] });
      queryClient.invalidateQueries({ queryKey: ["me", "review"] });
    },
    onError: async (err: unknown) => {
      if (err instanceof ApiError && (err.status === 409 || err.code === "REVIEW_NOT_DUE")) {
        // 409 REVIEW_NOT_DUE: refetch silently and continue (do not show as error)
        await refetch();
        setSelectedOptionId(null);
        setAnswerResult(null);
        setTimeSpentSec(0);
      }
    },
  });

  const currentItem = items[currentIndex];

  const handleSubmit = () => {
    if (!currentItem || selectedOptionId === null || answerMutation.isPending) return;
    answerMutation.mutate({
      reviewId: currentItem.reviewId,
      selectedId: selectedOptionId,
      timeSpent: timeSpentSec,
    });
  };

  const handleNextQuestion = async () => {
    setSelectedOptionId(null);
    setAnswerResult(null);
    setTimeSpentSec(0);
    if (currentIndex + 1 < items.length) {
      setCurrentIndex((prev) => prev + 1);
    } else {
      // Completed all items in current batch, refetch queue
      await refetch();
      setCurrentIndex(0);
    }
  };

  // Keyboard shortcut for MCQ options
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (answerResult || !currentItem) return;
      const key = e.key.toUpperCase();
      if (key === "A" && currentItem.question.options[0]) {
        setSelectedOptionId(currentItem.question.options[0].id);
      } else if (key === "B" && currentItem.question.options[1]) {
        setSelectedOptionId(currentItem.question.options[1].id);
      } else if (key === "C" && currentItem.question.options[2]) {
        setSelectedOptionId(currentItem.question.options[2].id);
      } else if (key === "D" && currentItem.question.options[3]) {
        setSelectedOptionId(currentItem.question.options[3].id);
      } else if (e.key === "Enter" && selectedOptionId !== null && !answerMutation.isPending) {
        handleSubmit();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [currentItem, selectedOptionId, answerResult, answerMutation.isPending]);

  // Loading State
  if (isLoading) {
    return (
      <div className="w-full max-w-[800px] mx-auto px-6 py-8 flex flex-col gap-6 animate-pulse">
        <div className="h-6 w-48 bg-[#EEEEEE] rounded" />
        <div className="h-10 w-72 bg-[#EEEEEE] rounded" />
        <div className="h-64 rounded-xl border border-[#E5E5E5] bg-[#FAFAFA]" />
      </div>
    );
  }

  // Error State (excluding 409)
  if (isError && !(error instanceof ApiError && error.status === 409)) {
    return (
      <div className="w-full max-w-[800px] mx-auto px-6 py-12 flex flex-col items-center justify-center min-h-[50vh]">
        <div className="p-8 rounded-xl border border-[#E5E5E5] bg-white max-w-md w-full flex flex-col items-center text-center gap-4">
          <div className="w-12 h-12 rounded-full bg-[#FEE2E2] flex items-center justify-center text-[#DC2626]">
            <AlertCircle className="w-6 h-6" />
          </div>
          <div className="flex flex-col gap-1">
            <h3 className="text-[18px] font-semibold text-[#111111]">Failed to load review queue</h3>
            <p className="text-[13px] text-[#555555]">
              {error instanceof Error ? error.message : "Unable to retrieve spaced repetition questions."}
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

  // Empty State
  if (!currentItem || items.length === 0) {
    return (
      <div className="w-full max-w-[800px] mx-auto px-6 py-12 flex flex-col items-center justify-center min-h-[50vh]">
        <div className="p-8 rounded-xl border border-[#E5E5E5] bg-white max-w-md w-full flex flex-col items-center text-center gap-4">
          <div className="w-12 h-12 rounded-full bg-[#DCFCE7] flex items-center justify-center text-[#16A34A]">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <div className="flex flex-col gap-1">
            <h2 className="text-[20px] font-semibold text-[#111111]">Nothing due today</h2>
            <p className="text-[13px] text-[#555555] mt-1 leading-relaxed">
              Your spaced repetition queue is completely clear. Questions missed during quizzes are automatically scheduled based on retention intervals.
            </p>
          </div>
          <div className="flex items-center gap-3 mt-3 w-full">
            <Link href="/dashboard" className="flex-1">
              <Button variant="outline" className="w-full">Dashboard</Button>
            </Link>
            <Link href="/practice" className="flex-1">
              <Button className="w-full">Practice Drill</Button>
            </Link>
          </div>
        </div>
      </div>
    );
  }

  const dueRemaining = answerResult ? answerResult.dueRemaining : items.length - currentIndex;

  return (
    <div className="w-full max-w-[800px] mx-auto px-6 py-8 flex flex-col gap-8">
      {/* 1. Header with Due Telemetry */}
      <div className="flex flex-col gap-1.5 border-b border-[#E5E5E5] pb-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-[#111111]" />
            <span className="font-mono text-[11px] uppercase tracking-wider text-[#8A8A8A]">
              Spaced Repetition Review • Question {currentIndex + 1} of {items.length}
            </span>
          </div>

          <div className="flex items-center gap-2">
            <Badge variant="warning">{dueCount} Due</Badge>
            <Badge variant="default">{dueRemaining} Remaining</Badge>
          </div>
        </div>

        <div className="flex items-baseline justify-between mt-1">
          <h1 className="text-[28px] font-semibold text-[#111111] tracking-tight">
            Active Recall Review
          </h1>
          <div className="flex items-center gap-1.5 font-mono text-[12px] text-[#555555]">
            <Clock className="w-3.5 h-3.5" />
            <span>{timeSpentSec}s</span>
          </div>
        </div>
      </div>

      {/* 2. Single Question Card */}
      <Card className="border-[#111111] overflow-hidden">
        <CardHeader className="bg-[#FAFAFA] border-b border-[#E5E5E5] pb-4">
          <div className="flex items-center justify-between">
            <Badge variant="default" className="text-[11px] font-mono">
              {currentItem.question.topicName}
            </Badge>
            <span className="text-[11px] font-mono text-[#8A8A8A]">
              Step {currentItem.step} • {currentItem.intervalDays}d interval
            </span>
          </div>

          <h3 className="text-[17px] font-semibold text-[#111111] mt-3 leading-snug">
            {currentItem.question.body}
          </h3>
        </CardHeader>

        <CardContent className="p-6 flex flex-col gap-5">
          {/* Options List */}
          <div className="flex flex-col gap-3">
            {currentItem.question.options.map((option, idx) => {
              const letter = String.fromCharCode(65 + idx);
              const isSelected = selectedOptionId === option.id;
              const isAnswered = answerResult !== null;
              const isCorrectOption = answerResult?.correctOptionId === option.id;
              const isUserWrongSelection = isAnswered && isSelected && !answerResult?.correct;

              let cardStyles = "border-[#E5E5E5] bg-white hover:border-[#111111]";
              let badgeStyles = "bg-[#EEEEEE] text-[#111111]";

              if (isAnswered) {
                if (isCorrectOption) {
                  cardStyles = "border-[#16A34A] bg-[#DCFCE7]/20";
                  badgeStyles = "bg-[#16A34A] text-white";
                } else if (isUserWrongSelection) {
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
                  key={option.id}
                  onClick={() => {
                    if (!isAnswered) setSelectedOptionId(option.id);
                  }}
                  className={`p-4 rounded-xl border transition-colors flex items-start gap-3.5 ${
                    isAnswered ? "cursor-default" : "cursor-pointer"
                  } ${cardStyles}`}
                >
                  <div
                    className={`w-7 h-7 rounded-lg flex items-center justify-center font-mono text-[12px] font-semibold shrink-0 transition-colors ${badgeStyles}`}
                  >
                    {letter}
                  </div>
                  <div className="flex flex-col gap-1 flex-1">
                    <span className="text-[14px] text-[#111111] leading-relaxed">
                      {option.text}
                    </span>
                    {isAnswered && isSelected && answerResult?.selectedOptionExplanation && (
                      <span className="text-[12px] text-[#555555] italic mt-1">
                        {answerResult.selectedOptionExplanation}
                      </span>
                    )}
                  </div>
                  {isAnswered && isCorrectOption && (
                    <Check className="w-5 h-5 text-[#16A34A] shrink-0" />
                  )}
                </div>
              );
            })}
          </div>

          {/* Post-Submission Result & Schedule */}
          {answerResult && (
            <div className="flex flex-col gap-4 pt-4 border-t border-[#E5E5E5]/80 animate-in fade-in duration-200">
              {/* Outcome Banner */}
              <div
                className={`p-4 rounded-xl border flex items-start gap-3 ${
                  answerResult.correct
                    ? "bg-[#DCFCE7]/30 border-[#BBF7D0] text-[#16A34A]"
                    : "bg-[#FEE2E2]/30 border-[#FECACA] text-[#DC2626]"
                }`}
              >
                {answerResult.correct ? (
                  <CheckCircle2 className="w-5 h-5 shrink-0 mt-0.5" />
                ) : (
                  <XCircle className="w-5 h-5 shrink-0 mt-0.5" />
                )}
                <div className="flex flex-col gap-1 text-[13px] text-[#111111]">
                  <span className="font-semibold">
                    {answerResult.correct ? "Correct!" : "Incorrect"}
                  </span>
                  <p className="text-[#555555] leading-relaxed">
                    {answerResult.explanation}
                  </p>
                </div>
              </div>

              {/* Schedule Info */}
              <div className="p-3.5 rounded-lg bg-[#FAFAFA] border border-[#E5E5E5] flex items-center justify-between text-[12px]">
                <div className="flex items-center gap-2 text-[#555555]">
                  <Calendar className="w-4 h-4 text-[#111111]" />
                  <span>
                    {answerResult.schedule.graduated
                      ? "Mastered, graduated from spaced repetition"
                      : `Next review in ${answerResult.schedule.intervalDays ?? 1} day${
                          answerResult.schedule.intervalDays === 1 ? "" : "s"
                        }`}
                  </span>
                </div>

                <Badge
                  variant={answerResult.schedule.graduated ? "success" : "default"}
                  className="text-[10px]"
                >
                  {answerResult.schedule.graduated ? "Mastered, graduated" : `Step ${answerResult.schedule.step}`}
                </Badge>
              </div>

              {/* Action Button: Next Question */}
              <div className="flex justify-end pt-2">
                <Button onClick={handleNextQuestion} className="gap-2">
                  <span>Next Question</span>
                  <ArrowRight className="w-4 h-4" />
                </Button>
              </div>
            </div>
          )}

          {/* Pre-Submission Action Controls */}
          {!answerResult && (
            <div className="flex items-center justify-between pt-3 border-t border-[#E5E5E5]/60">
              <span className="font-mono text-[11px] text-[#8A8A8A]">
                Press A, B, C, D to select • Enter to submit
              </span>

              <Button
                onClick={handleSubmit}
                disabled={selectedOptionId === null || answerMutation.isPending}
                className="gap-2"
              >
                <span>{answerMutation.isPending ? "Submitting..." : "Submit Answer"}</span>
                <ArrowRight className="w-4 h-4" />
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
