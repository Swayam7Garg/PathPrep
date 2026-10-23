"use client";

import { useState, useEffect, useCallback, useRef, use } from "react";
import { useRouter } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Clock,
  ArrowRight,
  Flag,
  RotateCcw,
  AlertTriangle,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
  AlertCircle,
  Maximize,
} from "lucide-react";
import { getSession, submitQuizAnswer, completeSession } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import { QuestionView } from "@/lib/types";

// ─── Constants ────────────────────────────────────────────────────────────────
const OPTION_LETTERS = ["A", "B", "C", "D"];
const MAX_VIOLATIONS = 3;
const GRACE_PERIOD_SEC = 30;
const VIOLATION_COOLDOWN_MS = 2000; // debounce: ignore back-to-back violations

// ─── Types ────────────────────────────────────────────────────────────────────
type QuestionStatus = "unanswered" | "answered" | "marked" | "answered-marked";

interface QuestionSlot {
  question: QuestionView | null;
  selectedOptionId: number | null;
  submittedOptionId: number | null;
  status: QuestionStatus;
  timeSpentSec: number;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
function formatTimer(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  if (h > 0)
    return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

// ─── Proctoring Warning Modal ─────────────────────────────────────────────────
function ProctoringWarningModal({
  violationCount,
  countdown,
  onReturnToFullscreen,
}: {
  violationCount: number;
  countdown: number;
  onReturnToFullscreen: () => void;
}) {
  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/40 backdrop-blur-sm">
      <div className="bg-white border border-[#E5E5E5] rounded-2xl p-8 max-w-[440px] w-full mx-4 shadow-2xl">
        {/* Title */}
        <h2 className="text-[#111111] text-xl font-bold mb-3">Fullscreen required</h2>

        {/* Warning count badge */}
        <div className="mb-4">
          <span className="inline-flex items-center gap-1.5 bg-red-50 text-red-600 border border-red-200 text-[13px] font-semibold px-3 py-1 rounded-full">
            <AlertTriangle className="w-3.5 h-3.5" />
            Warning {violationCount} of {MAX_VIOLATIONS}
          </span>
        </div>

        <p className="text-[#555555] text-[14px] leading-relaxed mb-5">
          You left fullscreen mode. Mock exams must stay fullscreen. Return to continue.
        </p>

        {/* Warning box */}
        <div className="bg-red-50 border border-red-200 rounded-xl p-4 mb-5">
          <p className="text-[#111111] text-[14px] font-semibold leading-snug">
            Please take this seriously and stay in fullscreen.
            <br />
            Leaving fullscreen again will raise your warning count.
          </p>
          <p className="text-[#8A8A8A] text-[12px] mt-2">
            After {MAX_VIOLATIONS} warnings, your quiz will be submitted automatically.
          </p>
        </div>

        {/* Countdown */}
        <p className="text-[#555555] text-[14px] mb-5">
          Return within{" "}
          <span className="inline-flex items-center justify-center bg-red-500 text-white text-[12px] font-bold px-2.5 py-0.5 rounded-md min-w-[2.5rem]">
            {countdown}s
          </span>{" "}
          or your test will end automatically.
        </p>

        <button
          onClick={onReturnToFullscreen}
          className="w-full bg-[#111111] hover:bg-[#222222] text-white font-semibold py-2.5 px-5 rounded-xl transition-colors text-[14px] flex items-center justify-center gap-2"
        >
          <Maximize className="w-4 h-4" />
          Return to fullscreen
        </button>
      </div>
    </div>
  );
}

// ─── Submit Confirmation Modal ────────────────────────────────────────────────
function SubmitConfirmModal({
  answered,
  unanswered,
  marked,
  total,
  onConfirm,
  onCancel,
  isSubmitting,
  autoTriggered,
}: {
  answered: number;
  unanswered: number;
  marked: number;
  total: number;
  onConfirm: () => void;
  onCancel: () => void;
  isSubmitting: boolean;
  autoTriggered: boolean;
}) {
  return (
    <div className="fixed inset-0 z-[65] flex items-center justify-center bg-black/30 backdrop-blur-sm">
      <div className="bg-white border border-[#E5E5E5] rounded-2xl p-8 max-w-[440px] w-full mx-4 shadow-2xl">
        <h2 className="text-[#111111] text-xl font-bold mb-2">
          {autoTriggered ? "Auto-submitting Quiz" : "Submit Quiz?"}
        </h2>
        <p className="text-[#555555] text-[13px] mb-6">
          {autoTriggered
            ? "Your quiz is being submitted due to proctoring violations."
            : "Review your answer summary before submitting. This action cannot be undone."}
        </p>

        {/* Stats */}
        <div className="grid grid-cols-3 gap-3 mb-5">
          <div className="bg-[#F6FEF6] border border-[#BBF7D0] rounded-xl p-4 text-center">
            <div className="text-2xl font-bold text-[#16A34A] font-mono">{answered}</div>
            <div className="text-[#555555] text-[11px] mt-1">Answered</div>
          </div>
          <div className="bg-[#FAFAFA] border border-[#E5E5E5] rounded-xl p-4 text-center">
            <div className="text-2xl font-bold text-[#8A8A8A] font-mono">{unanswered}</div>
            <div className="text-[#555555] text-[11px] mt-1">Unanswered</div>
          </div>
          <div className="bg-[#FAF5FF] border border-[#E9D5FF] rounded-xl p-4 text-center">
            <div className="text-2xl font-bold text-[#7C3AED] font-mono">{marked}</div>
            <div className="text-[#555555] text-[11px] mt-1">Marked</div>
          </div>
        </div>

        {unanswered > 0 && (
          <div className="flex items-start gap-2 bg-amber-50 border border-amber-200 rounded-xl p-3 mb-5">
            <AlertTriangle className="w-4 h-4 text-amber-500 mt-0.5 shrink-0" />
            <p className="text-amber-700 text-[12px] leading-relaxed">
              <strong>{unanswered}</strong> unanswered question{unanswered !== 1 ? "s" : ""} will count as skipped.
            </p>
          </div>
        )}

        <div className="flex gap-3">
          {!autoTriggered && (
            <button
              onClick={onCancel}
              disabled={isSubmitting}
              className="flex-1 border border-[#E5E5E5] text-[#555555] hover:text-[#111111] hover:border-[#CCCCCC] py-2.5 px-4 rounded-xl text-[13px] font-medium transition-colors disabled:opacity-40 bg-white"
            >
              Review Answers
            </button>
          )}
          <button
            onClick={onConfirm}
            disabled={isSubmitting}
            className={`${autoTriggered ? "w-full" : "flex-1"} bg-[#111111] hover:bg-[#222222] text-white font-semibold py-2.5 px-4 rounded-xl text-[13px] transition-colors disabled:opacity-60 flex items-center justify-center gap-2`}
          >
            {isSubmitting ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin" />
                Submitting…
              </>
            ) : (
              "Submit Final"
            )}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Main Quiz Page ───────────────────────────────────────────────────────────
export default function ActiveQuizPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const resolvedParams = use(params);
  const sessionId = resolvedParams.id;

  // ── Backend session query ──
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

  // ── Question slots ──
  const [slots, setSlots] = useState<QuestionSlot[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [totalQuestions, setTotalQuestions] = useState(0);
  const [initialized, setInitialized] = useState(false);
  const [backendIndex, setBackendIndex] = useState(0);

  // ── Timers ──
  const [elapsedSec, setElapsedSec] = useState(0);
  const questionStartRef = useRef<number>(Date.now());

  // ── Proctoring ──
  const [violationCount, setViolationCount] = useState(0);
  const [showWarning, setShowWarning] = useState(false);
  const [graceCountdown, setGraceCountdown] = useState(GRACE_PERIOD_SEC);
  const graceTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const proctoringEnabled = useRef(false);
  const lastViolationRef = useRef<number>(0); // timestamp of last violation (debounce)
  const violationCountRef = useRef(0); // mirror of violationCount for use in event handlers

  // ── UI ──
  const [showSubmitModal, setShowSubmitModal] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [autoSubmitTriggered, setAutoSubmitTriggered] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // ─── Initialize from session ───────────────────────────────────────────────
  useEffect(() => {
    if (!sessionData || initialized) return;
    if (sessionData.status === "COMPLETED") {
      router.replace(`/practice/quiz/${sessionId}/report`);
      return;
    }

    const total = sessionData.totalQuestions;
    const startIndex = sessionData.currentIndex;
    setTotalQuestions(total);
    setCurrentIndex(startIndex);
    setBackendIndex(startIndex);

    const initialSlots: QuestionSlot[] = Array.from({ length: total }, (_, i) => ({
      question: i === startIndex ? sessionData.currentQuestion : null,
      selectedOptionId: null,
      submittedOptionId: null,
      status: "unanswered",
      timeSpentSec: 0,
    }));

    setSlots(initialSlots);
    setInitialized(true);
    questionStartRef.current = Date.now();
  }, [sessionData, initialized, sessionId, router]);

  // ── Elapsed timer ──
  useEffect(() => {
    const id = setInterval(() => setElapsedSec((s) => s + 1), 1000);
    return () => clearInterval(id);
  }, []);

  // ─── Fullscreen ────────────────────────────────────────────────────────────
  const enterFullscreen = useCallback(async () => {
    try {
      if (!document.fullscreenElement) {
        await document.documentElement.requestFullscreen();
      }
    } catch {
      // Fullscreen API not available
    }
    proctoringEnabled.current = true;
  }, []);

  // ── Clear grace period ──
  const clearGrace = useCallback(() => {
    if (graceTimerRef.current) {
      clearInterval(graceTimerRef.current);
      graceTimerRef.current = null;
    }
    setGraceCountdown(GRACE_PERIOD_SEC);
  }, []);

  // ── Trigger auto-submit ──
  const doAutoSubmit = useCallback(() => {
    clearGrace();
    setShowWarning(false);
    setAutoSubmitTriggered(true);
    setShowSubmitModal(true);
  }, [clearGrace]);

  // ── Start grace period countdown ──
  const startGrace = useCallback(
    (newCount: number) => {
      clearGrace();
      setGraceCountdown(GRACE_PERIOD_SEC);
      graceTimerRef.current = setInterval(() => {
        setGraceCountdown((prev) => {
          if (prev <= 1) {
            clearInterval(graceTimerRef.current!);
            graceTimerRef.current = null;
            doAutoSubmit();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    },
    [clearGrace, doAutoSubmit]
  );

  // ── Record a proctoring violation (with debounce) ──
  const recordViolation = useCallback(() => {
    if (!proctoringEnabled.current) return;

    // Debounce: ignore if another violation was recorded within cooldown period
    const now = Date.now();
    if (now - lastViolationRef.current < VIOLATION_COOLDOWN_MS) return;
    lastViolationRef.current = now;

    // Use ref to get current count synchronously
    const newCount = violationCountRef.current + 1;
    violationCountRef.current = newCount;
    setViolationCount(newCount);

    if (newCount >= MAX_VIOLATIONS) {
      doAutoSubmit();
    } else {
      setShowWarning(true);
      startGrace(newCount);
    }
  }, [doAutoSubmit, startGrace]);

  // ── Return to fullscreen ──
  const handleReturnToFullscreen = useCallback(async () => {
    clearGrace();
    setShowWarning(false);
    try {
      if (!document.fullscreenElement) {
        await document.documentElement.requestFullscreen();
      }
    } catch { /* ignore */ }
  }, [clearGrace]);

  // ── Fullscreen change event ──
  useEffect(() => {
    const onFSChange = () => {
      if (!document.fullscreenElement && proctoringEnabled.current && !showWarning && !autoSubmitTriggered) {
        recordViolation();
      }
    };
    document.addEventListener("fullscreenchange", onFSChange);
    return () => document.removeEventListener("fullscreenchange", onFSChange);
  }, [showWarning, autoSubmitTriggered, recordViolation]);

  // ── Visibility change event ──
  useEffect(() => {
    const onVisChange = () => {
      if (document.hidden && proctoringEnabled.current && !showWarning && !autoSubmitTriggered) {
        recordViolation();
      }
    };
    document.addEventListener("visibilitychange", onVisChange);
    return () => document.removeEventListener("visibilitychange", onVisChange);
  }, [showWarning, autoSubmitTriggered, recordViolation]);

  // ── Enter fullscreen when initialized ──
  useEffect(() => {
    if (initialized && !proctoringEnabled.current) {
      enterFullscreen();
    }
  }, [initialized, enterFullscreen]);

  // Cleanup grace timer on unmount
  useEffect(
    () => () => {
      clearGrace();
      if (document.fullscreenElement) document.exitFullscreen().catch(() => {});
    },
    [clearGrace]
  );

  // ─── Mutations ─────────────────────────────────────────────────────────────
  const submitAnswerMutation = useMutation({
    mutationFn: ({ optionId, timeSpent }: { optionId: number; timeSpent: number }) =>
      submitQuizAnswer(sessionId, optionId, undefined, timeSpent),
  });

  const completeSessionMutation = useMutation({
    mutationFn: () => completeSession(sessionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.sessions.detail(sessionId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.me.planToday });
      queryClient.invalidateQueries({ queryKey: ["sessions", "history"] });
      queryClient.invalidateQueries({ queryKey: ["me", "skills"] });
      queryClient.invalidateQueries({ queryKey: ["me", "progress"] });
      const nav = () => router.push(`/practice/quiz/${sessionId}/report`);
      if (document.fullscreenElement) {
        document.exitFullscreen().catch(() => {}).finally(nav);
      } else {
        nav();
      }
    },
    onError: () => {
      setIsSubmitting(false);
      setSubmitError("Failed to finalize the session. Please try again.");
    },
  });

  // ─── Slot helpers ──────────────────────────────────────────────────────────
  const updateSlotAt = useCallback((index: number, patch: Partial<QuestionSlot>) => {
    setSlots((prev) => {
      const next = [...prev];
      next[index] = { ...next[index], ...patch };
      return next;
    });
  }, []);

  const currentSlot = slots[currentIndex];

  // ─── Option selection ──────────────────────────────────────────────────────
  const handleSelectOption = useCallback(
    (optionId: number) => {
      updateSlotAt(currentIndex, { selectedOptionId: optionId });
    },
    [currentIndex, updateSlotAt]
  );

  const handleClearResponse = useCallback(() => {
    updateSlotAt(currentIndex, { selectedOptionId: null });
  }, [currentIndex, updateSlotAt]);

  // ─── Mark for Review ──────────────────────────────────────────────────────
  const handleMarkForReview = useCallback(() => {
    if (!currentSlot) return;
    const hasAnswer = currentSlot.selectedOptionId !== null;
    const isMarked =
      currentSlot.status === "marked" || currentSlot.status === "answered-marked";
    const newStatus: QuestionStatus = isMarked
      ? hasAnswer
        ? "answered"
        : "unanswered"
      : hasAnswer
      ? "answered-marked"
      : "marked";
    updateSlotAt(currentIndex, { status: newStatus });
  }, [currentSlot, currentIndex, updateSlotAt]);

  // ─── Navigate to a question ───────────────────────────────────────────────
  const navigateTo = useCallback(
    (index: number) => {
      if (index < 0 || index >= totalQuestions) return;
      if (index === currentIndex) return;
      // Save time spent on current question
      const timeSpent = Math.round((Date.now() - questionStartRef.current) / 1000);
      setSlots((prev) => {
        const next = [...prev];
        next[currentIndex] = {
          ...next[currentIndex],
          timeSpentSec: (next[currentIndex]?.timeSpentSec || 0) + timeSpent,
        };
        return next;
      });
      setCurrentIndex(index);
      questionStartRef.current = Date.now();
    },
    [totalQuestions, currentIndex]
  );

  // ─── Save & Next ──────────────────────────────────────────────────────────
  const handleSaveAndNext = useCallback(async () => {
    if (!currentSlot?.question) return;

    const timeSpent = Math.round((Date.now() - questionStartRef.current) / 1000);
    const hasAnswer = currentSlot.selectedOptionId !== null;
    const isMarked =
      currentSlot.status === "marked" || currentSlot.status === "answered-marked";
    const newStatus: QuestionStatus = hasAnswer
      ? isMarked
        ? "answered-marked"
        : "answered"
      : isMarked
      ? "marked"
      : "unanswered";

    const accumulated = (currentSlot.timeSpentSec || 0) + timeSpent;
    updateSlotAt(currentIndex, { status: newStatus, timeSpentSec: accumulated });

    // Submit to backend if we're on the current backend question and have an answer
    if (currentIndex === backendIndex && hasAnswer && currentSlot.selectedOptionId !== null) {
      try {
        const result = await submitAnswerMutation.mutateAsync({
          optionId: currentSlot.selectedOptionId,
          timeSpent: accumulated,
        });
        updateSlotAt(currentIndex, { submittedOptionId: currentSlot.selectedOptionId });

        if (result.nextQuestion && currentIndex + 1 < totalQuestions) {
          updateSlotAt(currentIndex + 1, { question: result.nextQuestion });
          setBackendIndex(currentIndex + 1);
        }

        if (result.sessionStatus === "COMPLETED" || !result.nextQuestion) {
          completeSessionMutation.mutate();
          return;
        }
      } catch {
        // Non-fatal; will retry on final submit
      }
    }

    // Advance
    if (currentIndex < totalQuestions - 1) {
      setCurrentIndex((prev) => prev + 1);
      questionStartRef.current = Date.now();
    }
  }, [
    currentSlot,
    currentIndex,
    backendIndex,
    totalQuestions,
    updateSlotAt,
    submitAnswerMutation,
    completeSessionMutation,
  ]);

  // ─── Final Submission ──────────────────────────────────────────────────────
  const handleFinalSubmit = useCallback(async () => {
    if (isSubmitting) return;
    setIsSubmitting(true);
    setSubmitError(null);

    try {
      // Submit any remaining un-submitted answers from backendIndex onward
      for (let i = backendIndex; i < totalQuestions; i++) {
        const slot = slots[i];
        if (!slot?.question) break;
        if (slot.selectedOptionId !== null && slot.submittedOptionId === null) {
          try {
            const result = await submitAnswerMutation.mutateAsync({
              optionId: slot.selectedOptionId,
              timeSpent: slot.timeSpentSec || 0,
            });
            updateSlotAt(i, { submittedOptionId: slot.selectedOptionId });
            if (result.nextQuestion && i + 1 < totalQuestions) {
              updateSlotAt(i + 1, { question: result.nextQuestion });
            }
            if (result.sessionStatus === "COMPLETED" || !result.nextQuestion) break;
          } catch {
            break;
          }
        } else {
          break; // unanswered — completeSession handles as skipped
        }
      }
      completeSessionMutation.mutate();
    } catch {
      setIsSubmitting(false);
      setSubmitError("Submission failed. Please try again.");
    }
  }, [isSubmitting, backendIndex, totalQuestions, slots, submitAnswerMutation, updateSlotAt, completeSessionMutation]);

  // ─── Keyboard shortcuts ────────────────────────────────────────────────────
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (isSubmitting) return;

      // Escape key → show fullscreen warning (let browser exit fullscreen naturally,
      // our fullscreenchange listener will detect it and show the warning modal)
      if (e.key === "Escape") {
        // If warning is already showing, ignore
        if (showWarning || showSubmitModal) return;
        // The browser will exit fullscreen; recordViolation fires via fullscreenchange
        return;
      }

      if (showWarning || showSubmitModal) return;
      if (!currentSlot?.question) return;

      const opts = currentSlot.question.options;
      if ((e.key === "a" || e.key === "A" || e.key === "1") && opts[0])
        handleSelectOption(opts[0].id);
      if ((e.key === "b" || e.key === "B" || e.key === "2") && opts[1])
        handleSelectOption(opts[1].id);
      if ((e.key === "c" || e.key === "C" || e.key === "3") && opts[2])
        handleSelectOption(opts[2].id);
      if ((e.key === "d" || e.key === "D" || e.key === "4") && opts[3])
        handleSelectOption(opts[3].id);
      if (e.key === "Enter") handleSaveAndNext();
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [showWarning, showSubmitModal, isSubmitting, currentSlot, handleSelectOption, handleSaveAndNext]);

  // ─── Palette styling ───────────────────────────────────────────────────────
  function getPaletteClass(idx: number): string {
    if (idx === currentIndex) {
      return "bg-[#111111] text-white border-[#111111] ring-2 ring-[#111111] ring-offset-1 ring-offset-[#FAFAFA]";
    }
    const slot = slots[idx];
    if (!slot?.question) {
      return "bg-[#FAFAFA] border-[#E5E5E5] text-[#CCCCCC] cursor-not-allowed";
    }
    switch (slot.status) {
      case "answered":
        return "bg-[#DCFCE7] border-[#86EFAC] text-[#16A34A] hover:bg-[#BBF7D0]";
      case "marked":
        return "bg-[#EDE9FE] border-[#C4B5FD] text-[#7C3AED] hover:bg-[#DDD6FE]";
      case "answered-marked":
        return "bg-[#EDE9FE] border-[#7C3AED] text-[#7C3AED] ring-1 ring-[#16A34A]";
      default:
        return "bg-white border-[#E5E5E5] text-[#555555] hover:border-[#BBBBBB] hover:bg-[#F4F3F3]";
    }
  }

  // ─── Derived counts ────────────────────────────────────────────────────────
  const answeredCount = slots.filter((s) => s.selectedOptionId !== null).length;
  const markedCount = slots.filter(
    (s) => s.status === "marked" || s.status === "answered-marked"
  ).length;
  const unansweredCount = totalQuestions - answeredCount;

  // ─── Loading & Error states ────────────────────────────────────────────────
  if (sessionLoading || !initialized) {
    return (
      <div className="fixed inset-0 bg-[#FAFAFA] flex items-center justify-center z-[60]">
        <div className="flex flex-col items-center gap-4">
          <div className="w-8 h-8 border-2 border-[#111111] border-t-transparent rounded-full animate-spin" />
          <p className="text-[#8A8A8A] text-[13px]">Loading exam session…</p>
        </div>
      </div>
    );
  }

  if (sessionError) {
    return (
      <div className="fixed inset-0 bg-[#FAFAFA] flex items-center justify-center z-[60]">
        <div className="bg-white border border-[#E5E5E5] rounded-xl p-8 max-w-sm w-full text-center flex flex-col items-center gap-4 shadow-sm">
          <AlertCircle className="w-10 h-10 text-red-500" />
          <div>
            <h3 className="text-[#111111] font-semibold text-lg">Failed to load session</h3>
            <p className="text-[#555555] text-[13px] mt-1">
              {sessionErrorObj instanceof Error ? sessionErrorObj.message : "Unable to retrieve session."}
            </p>
          </div>
          <button
            onClick={() => refetchSession()}
            className="bg-[#111111] hover:bg-[#222222] text-white px-5 py-2 rounded-xl text-[13px] font-medium transition-colors flex items-center gap-2"
          >
            <RefreshCw className="w-4 h-4" /> Retry
          </button>
        </div>
      </div>
    );
  }

  if (!currentSlot?.question) {
    return (
      <div className="fixed inset-0 bg-[#FAFAFA] flex items-center justify-center z-[60]">
        <p className="text-[#8A8A8A] text-[13px]">Loading question…</p>
      </div>
    );
  }

  const question = currentSlot.question;

  return (
    <div className="fixed inset-0 bg-[#FAFAFA] text-[#111111] flex flex-col overflow-hidden z-[60]">
      {/* ── Proctoring Warning Modal ── */}
      {showWarning && (
        <ProctoringWarningModal
          violationCount={violationCount}
          countdown={graceCountdown}
          onReturnToFullscreen={handleReturnToFullscreen}
        />
      )}

      {/* ── Submit Confirmation Modal ── */}
      {showSubmitModal && (
        <SubmitConfirmModal
          answered={answeredCount}
          unanswered={unansweredCount}
          marked={markedCount}
          total={totalQuestions}
          onConfirm={() => {
            setShowSubmitModal(false);
            handleFinalSubmit();
          }}
          onCancel={() => {
            if (!autoSubmitTriggered) setShowSubmitModal(false);
          }}
          isSubmitting={isSubmitting}
          autoTriggered={autoSubmitTriggered}
        />
      )}

      {/* ════════════ HEADER ════════════ */}
      <header className="flex items-center justify-between px-6 py-0 border-b border-[#E5E5E5] bg-white shrink-0 h-[56px]">
        {/* Left: Title + violation indicator */}
        <div className="flex items-center gap-3 min-w-0">
          <h1 className="text-[15px] font-semibold text-[#111111] truncate">
            Quiz · {sessionData?.trackName || "Practice"}
          </h1>
          {violationCount > 0 && (
            <span className="shrink-0 flex items-center gap-1 text-[11px] text-red-600 font-medium bg-red-50 border border-red-200 px-2 py-0.5 rounded-full">
              <AlertTriangle className="w-3 h-3" />
              {violationCount}/{MAX_VIOLATIONS} warnings
            </span>
          )}
        </div>

        {/* Center: Elapsed Timer */}
        <div className="flex items-center gap-1.5 font-mono text-[14px] font-semibold text-[#111111] bg-[#FAFAFA] border border-[#E5E5E5] px-4 py-1.5 rounded-xl shrink-0">
          <Clock className="w-4 h-4 text-[#8A8A8A]" />
          <span>{formatTimer(elapsedSec)}</span>
        </div>

        {/* Right: Submit */}
        <button
          onClick={() => setShowSubmitModal(true)}
          disabled={isSubmitting}
          className="flex items-center gap-2 bg-[#111111] hover:bg-[#222222] text-white font-semibold px-4 py-2 rounded-xl text-[13px] transition-colors disabled:opacity-50 shrink-0"
        >
          Submit
          <ChevronRight className="w-4 h-4" />
        </button>
      </header>

      {/* ════════════ BODY ════════════ */}
      <div className="flex flex-1 overflow-hidden">
        {/* ── Question Panel (Left) ── */}
        <main className="flex-1 flex flex-col overflow-y-auto min-w-0">
          {/* Question label */}
          <div className="px-8 pt-6 pb-2">
            <div className="flex items-center gap-2">
              <span className="text-[#8A8A8A] text-[12px] font-mono tracking-wider uppercase">
                Question {currentIndex + 1}
              </span>
              {question.topicName && (
                <>
                  <span className="text-[#CCCCCC]">·</span>
                  <span className="text-[#8A8A8A] text-[12px]">{question.topicName}</span>
                </>
              )}
            </div>
          </div>

          {/* Question body */}
          <div className="px-8 pt-3 pb-6">
            <h2 className="text-[18px] font-semibold text-[#111111] leading-relaxed">
              {question.body}
            </h2>
          </div>

          {/* Options */}
          <div className="px-8 flex flex-col gap-3 pb-6">
            {question.options.map((opt, idx) => {
              const isSelected = currentSlot.selectedOptionId === opt.id;
              return (
                <div
                  key={opt.id}
                  onClick={() => handleSelectOption(opt.id)}
                  className={`flex items-center gap-4 p-4 rounded-xl border cursor-pointer transition-all select-none ${
                    isSelected
                      ? "border-[#111111] bg-[#F4F3F3] ring-1 ring-[#111111]"
                      : "border-[#E5E5E5] bg-white hover:border-[#BBBBBB] hover:bg-[#FAFAFA]"
                  }`}
                >
                  {/* Radio dot */}
                  <div
                    className={`w-5 h-5 rounded-full border-2 flex items-center justify-center shrink-0 transition-all ${
                      isSelected ? "border-[#111111] bg-[#111111]" : "border-[#CCCCCC]"
                    }`}
                  >
                    {isSelected && <div className="w-2 h-2 rounded-full bg-white" />}
                  </div>

                  <span className="text-[14px] text-[#111111] leading-relaxed">
                    <span className="text-[#8A8A8A] font-mono mr-2.5">{OPTION_LETTERS[idx]}.</span>
                    {opt.text}
                  </span>
                </div>
              );
            })}
          </div>

          {submitError && (
            <div className="mx-8 mb-4 flex items-center gap-2 bg-red-50 border border-red-200 rounded-xl px-4 py-3">
              <AlertCircle className="w-4 h-4 text-red-500 shrink-0" />
              <p className="text-red-600 text-[13px]">{submitError}</p>
            </div>
          )}

          <div className="flex-1" />

          {/* ── Bottom Action Bar ── */}
          <div className="border-t border-[#E5E5E5] bg-white px-8 py-4 flex items-center justify-between shrink-0">
            <button
              onClick={handleClearResponse}
              disabled={currentSlot.selectedOptionId === null}
              className="text-[13px] text-[#8A8A8A] hover:text-[#111111] disabled:opacity-30 disabled:cursor-not-allowed transition-colors flex items-center gap-1.5"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              Clear Response
            </button>

            <div className="flex items-center gap-3">
              <button
                onClick={handleMarkForReview}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl border text-[13px] font-medium transition-all ${
                  currentSlot.status === "marked" || currentSlot.status === "answered-marked"
                    ? "bg-[#EDE9FE] border-[#C4B5FD] text-[#7C3AED]"
                    : "border-[#E5E5E5] bg-white text-[#555555] hover:border-[#BBBBBB] hover:text-[#111111]"
                }`}
              >
                <Flag className="w-3.5 h-3.5" />
                Mark for Review
              </button>

              <button
                onClick={handleSaveAndNext}
                disabled={submitAnswerMutation.isPending || completeSessionMutation.isPending}
                className="flex items-center gap-2 bg-[#111111] hover:bg-[#222222] text-white font-semibold px-5 py-2 rounded-xl text-[13px] transition-colors disabled:opacity-60"
              >
                {submitAnswerMutation.isPending ? (
                  <>
                    <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                    Saving…
                  </>
                ) : (
                  <>
                    Save &amp; Next
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Prev / Next nav */}
          <div className="px-8 py-3 flex items-center gap-2 bg-[#FAFAFA] border-t border-[#E5E5E5]/60">
            <button
              onClick={() => navigateTo(currentIndex - 1)}
              disabled={currentIndex === 0}
              className="flex items-center gap-1 text-[12px] text-[#8A8A8A] hover:text-[#111111] disabled:opacity-30 disabled:cursor-not-allowed transition-colors px-3 py-1.5 border border-[#E5E5E5] bg-white rounded-lg hover:border-[#BBBBBB]"
            >
              <ChevronLeft className="w-4 h-4" />
              Previous
            </button>
            <button
              onClick={() => navigateTo(currentIndex + 1)}
              disabled={currentIndex >= backendIndex || !slots[currentIndex + 1]?.question}
              className="flex items-center gap-1 text-[12px] text-[#8A8A8A] hover:text-[#111111] disabled:opacity-30 disabled:cursor-not-allowed transition-colors px-3 py-1.5 border border-[#E5E5E5] bg-white rounded-lg hover:border-[#BBBBBB]"
            >
              Next
              <ChevronRight className="w-4 h-4" />
            </button>
            <span className="text-[11px] text-[#CCCCCC] ml-1.5">
              A–D to select · Enter to save &amp; next
            </span>
          </div>
        </main>

        {/* ── Question Palette (Right) ── */}
        <aside className="w-[210px] shrink-0 border-l border-[#E5E5E5] bg-white flex flex-col">
          {/* Palette header */}
          <div className="px-4 py-3.5 border-b border-[#E5E5E5]">
            <h3 className="text-[12px] font-semibold text-[#8A8A8A] uppercase tracking-wider">
              Question palette
            </h3>
          </div>

          {/* Grid */}
          <div className="p-3 grid grid-cols-4 gap-1.5 content-start">
            {slots.map((slot, idx) => {
              const isLoaded = !!slot?.question;
              const isClickable = isLoaded || idx <= backendIndex;
              return (
                <button
                  key={idx}
                  onClick={() => isClickable && navigateTo(idx)}
                  disabled={!isClickable}
                  className={`w-10 h-10 rounded-lg border text-[13px] font-semibold transition-all ${getPaletteClass(idx)}`}
                  title={!isLoaded && idx > backendIndex ? "Save & Next to unlock" : `Question ${idx + 1}`}
                >
                  {idx + 1}
                </button>
              );
            })}
          </div>

          {/* Legend */}
          <div className="px-4 py-3 border-t border-[#E5E5E5] flex flex-col gap-2">
            {[
              { cls: "bg-[#DCFCE7] border-[#86EFAC]", label: "Answered" },
              { cls: "bg-white border-[#E5E5E5]", label: "Unanswered" },
              { cls: "bg-[#EDE9FE] border-[#C4B5FD]", label: "Marked" },
              { cls: "bg-[#111111] border-[#111111]", label: "Current" },
            ].map(({ cls, label }) => (
              <div key={label} className="flex items-center gap-2">
                <div className={`w-3 h-3 rounded-sm border ${cls} shrink-0`} />
                <span className="text-[11px] text-[#555555]">{label}</span>
              </div>
            ))}
          </div>

          {/* Summary counts */}
          <div className="px-4 py-3 border-t border-[#E5E5E5] mt-auto">
            <div className="flex flex-col gap-1.5">
              <div className="flex items-center justify-between text-[12px]">
                <span className="text-[#8A8A8A]">Answered</span>
                <span className="font-mono font-semibold text-[#16A34A]">{answeredCount}</span>
              </div>
              <div className="flex items-center justify-between text-[12px]">
                <span className="text-[#8A8A8A]">Unanswered</span>
                <span className="font-mono font-semibold text-[#8A8A8A]">{unansweredCount}</span>
              </div>
              <div className="flex items-center justify-between text-[12px]">
                <span className="text-[#8A8A8A]">Marked</span>
                <span className="font-mono font-semibold text-[#7C3AED]">{markedCount}</span>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}
