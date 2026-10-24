"use client";

import React, { useState, useEffect, useRef, use, useMemo } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Clock,
  Send,
  Sparkles,
  Bot,
  User,
  Lightbulb,
  CheckCircle2,
  Mic,
  MicOff,
  LogOut,
  Info,
  HelpCircle,
  X,
  ArrowRight,
  ShieldCheck,
  Check,
  BrainCircuit,
  RefreshCw,
  AlertTriangle,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getInterviewSession,
  submitInterviewTurn,
  completeSession,
  retryEvaluations,
} from "@/lib/api";
import { InterviewTurn, InterviewSessionResponse } from "@/lib/types";

// Static generic academic tips only (no per-turn hints or leaked answers)
const STATIC_GENERIC_TIPS = [
  "Structure your response logically: State your core approach first, then explain implementation trade-offs.",
  "Always address edge cases: mention race conditions, memory leaks, null values, and high-concurrency loads.",
  "Quantify complexity: specify Big-O time and space complexity for data structures and algorithmic steps.",
  "Consider failure modes: describe what happens if a downstream service or network partition occurs.",
];

export default function MockInterviewSessionPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const resolvedParams = use(params);
  const sessionId = resolvedParams.id;

  // Polling state management
  const [evalPollingDuration, setEvalPollingDuration] = useState(0);
  const evalStartRef = useRef<number | null>(null);

  // Fetch session with dynamic interval
  const {
    data: session,
    isLoading,
    refetch,
  } = useQuery<InterviewSessionResponse>({
    queryKey: ["interview", sessionId],
    queryFn: () => getInterviewSession(sessionId),
    refetchInterval: (query) => {
      const data = query.state.data;
      if (!data) return 1000;
      if (data.status === "COMPLETED" || data.status === "FAILED") {
        return false;
      }
      if (data.awaiting?.type === "EVALUATION") {
        // Poll every 1s while awaiting EVALUATION; back off to 3s after 30s
        return evalPollingDuration > 30 ? 3000 : 1000;
      }
      return false;
    },
  });

  // Track evaluation duration for polling backoff
  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (session?.awaiting?.type === "EVALUATION") {
      if (!evalStartRef.current) {
        evalStartRef.current = Date.now();
      }
      interval = setInterval(() => {
        if (evalStartRef.current) {
          const seconds = Math.floor((Date.now() - evalStartRef.current) / 1000);
          setEvalPollingDuration(seconds);
        }
      }, 1000);
    } else {
      evalStartRef.current = null;
      setEvalPollingDuration(0);
    }
    return () => clearInterval(interval);
  }, [session?.awaiting?.type]);

  // Navigate when completed
  useEffect(() => {
    if (session?.status === "COMPLETED") {
      router.push(`/practice/quiz/${sessionId}/report`);
    }
  }, [session?.status, sessionId, router]);

  // Local typewriter effect for follow-up turns
  const [animatedTurnIds, setAnimatedTurnIds] = useState<Set<string>>(new Set());
  const [typewriterContent, setTypewriterContent] = useState<{ [turnId: string]: string }>({});

  useEffect(() => {
    if (!session?.turns) return;
    const turns = session.turns;
    const latestTurn = turns[turns.length - 1];
    if (!latestTurn) return;

    const tid = latestTurn.turnId || latestTurn.id || String(turns.length);
    if (Boolean(latestTurn.isFollowUp) && !animatedTurnIds.has(tid)) {
      setAnimatedTurnIds((prev) => new Set(prev).add(tid));
      const fullText = latestTurn.body || latestTurn.content || "";
      let currentIndex = 0;

      const timer = setInterval(() => {
        currentIndex += 3;
        if (currentIndex >= fullText.length) {
          setTypewriterContent((prev) => ({ ...prev, [tid]: fullText }));
          clearInterval(timer);
        } else {
          setTypewriterContent((prev) => ({
            ...prev,
            [tid]: fullText.slice(0, currentIndex),
          }));
        }
      }, 18);

      return () => clearInterval(timer);
    }
  }, [session?.turns, animatedTurnIds]);

  // Response input & controls
  const [inputText, setInputText] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const [showHint, setShowHint] = useState(false);
  const [hintIndex, setHintIndex] = useState(0);
  const [showExitModal, setShowExitModal] = useState(false);
  const [isEnding, setIsEnding] = useState(false);
  const [isRetrying, setIsRetrying] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // Advisory countdown timer initialized from session.timeLimitSec (default 1800s)
  const [elapsedSeconds, setElapsedSeconds] = useState(session?.timeLimitSec ?? 1800);
  const hasInitializedTimer = useRef(false);

  useEffect(() => {
    if (session?.timeLimitSec && !hasInitializedTimer.current) {
      setElapsedSeconds(session.timeLimitSec);
      hasInitializedTimer.current = true;
    }
  }, [session?.timeLimitSec]);

  useEffect(() => {
    const timer = setInterval(() => {
      setElapsedSeconds((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const formatCountdown = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
  };

  // Turn time tracking for timeSpentSec
  const turnStartTimeRef = useRef<number>(Date.now());
  useEffect(() => {
    turnStartTimeRef.current = Date.now();
  }, [session?.turns?.length]);

  // Speech recognition (with text fallback)
  const recognitionRef = useRef<any>(null);
  useEffect(() => {
    if (typeof window !== "undefined") {
      const SpeechRecognition =
        (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

      if (SpeechRecognition) {
        const recognition = new SpeechRecognition();
        recognition.continuous = true;
        recognition.interimResults = true;
        recognition.lang = "en-US";

        recognition.onresult = (event: any) => {
          let transcript = "";
          for (let i = event.resultIndex; i < event.results.length; i++) {
            transcript += event.results[i][0].transcript;
          }
          if (transcript) {
            setInputText((prev) => (prev ? `${prev} ${transcript}` : transcript));
          }
        };

        recognition.onerror = () => setIsRecording(false);
        recognition.onend = () => setIsRecording(false);
        recognitionRef.current = recognition;
      }
    }

    return () => {
      if (recognitionRef.current) {
        try {
          recognitionRef.current.stop();
        } catch {
          // Ignore
        }
      }
    };
  }, []);

  const toggleVoiceRecording = () => {
    if (!recognitionRef.current) {
      alert("Speech recognition is not supported in this browser. Please use the text keyboard input.");
      return;
    }

    if (isRecording) {
      recognitionRef.current.stop();
      setIsRecording(false);
    } else {
      try {
        recognitionRef.current.start();
        setIsRecording(true);
      } catch {
        setIsRecording(false);
      }
    }
  };

  // Scroll to bottom
  const messagesEndRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [session?.turns, typewriterContent, session?.awaiting]);

  // Current question and turn counts
  const currentQuestionNumber = session?.currentQuestionNumber ?? 1;
  const totalQuestions = session?.totalQuestions ?? 4;
  const isAwaitingAnswer = session?.awaiting?.type === "ANSWER";
  const isAwaitingEvaluation = session?.awaiting?.type === "EVALUATION";
  const activeTurnId =
    session?.awaiting?.turnId ||
    session?.turns?.[session.turns.length - 1]?.turnId ||
    "turn-1";

  // Submit Answer Handler with Idempotency Key
  const handleSubmitAnswer = async () => {
    const trimmed = inputText.trim();
    if (!trimmed || isSubmitting || isAwaitingEvaluation) return;

    if (isRecording && recognitionRef.current) {
      recognitionRef.current.stop();
      setIsRecording(false);
    }

    setIsSubmitting(true);
    setSubmitError(null);

    // Retrieve or generate Idempotency-Key from sessionStorage per turn
    const idemStorageKey = `pathprep_idem_${sessionId}_${activeTurnId}`;
    let idempotencyKey: string | null = null;
    if (typeof window !== "undefined") {
      idempotencyKey = sessionStorage.getItem(idemStorageKey);
      if (!idempotencyKey) {
        idempotencyKey = `idem_${sessionId}_${activeTurnId}_${Date.now()}_${Math.random()
          .toString(36)
          .substring(2, 9)}`;
        sessionStorage.setItem(idemStorageKey, idempotencyKey);
      }
    }

    const timeSpentSec = Math.max(
      10,
      Math.floor((Date.now() - turnStartTimeRef.current) / 1000)
    );

    try {
      await submitInterviewTurn(
        sessionId,
        {
          turnId: activeTurnId,
          answerText: trimmed,
          timeSpentSec,
        },
        idempotencyKey || undefined
      );

      setInputText("");
      setShowHint(false);
      // Invalidate and refetch immediately to transition to awaiting EVALUATION
      await queryClient.invalidateQueries({ queryKey: ["interview", sessionId] });
      refetch();
    } catch (err: any) {
      const msg = err?.message || "Failed to submit answer. Please retry.";
      setSubmitError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && (e.metaKey || e.ctrlKey)) {
      e.preventDefault();
      handleSubmitAnswer();
    }
  };

  const handleEndSession = async () => {
    setIsEnding(true);
    try {
      await completeSession(sessionId);
      await queryClient.invalidateQueries({ queryKey: ["interview", sessionId] });
      refetch();
    } catch {
      router.push(`/practice/quiz/${sessionId}/report`);
    } finally {
      setIsEnding(false);
      setShowExitModal(false);
    }
  };

  const handleRetryEvaluations = async () => {
    setIsRetrying(true);
    try {
      await retryEvaluations(sessionId);
      await queryClient.invalidateQueries({ queryKey: ["interview", sessionId] });
      refetch();
    } catch (err: any) {
      alert(err?.message || "Retry failed. Please check connection.");
    } finally {
      setIsRetrying(false);
    }
  };

  return (
    <div className="flex flex-col h-[calc(100vh-65px)] w-full max-w-[900px] mx-auto overflow-hidden bg-white">
      {/* 1. Header Ribbon */}
      <div className="shrink-0 flex items-center justify-between px-4 sm:px-6 h-14 border-b border-[#E5E5E5] bg-white z-20">
        <div className="flex items-center gap-2.5 truncate">
          <span className="font-semibold text-[15px] text-[#111111] tracking-tight">
            PathPrep
          </span>
          <span className="text-[#C4C7C7] text-xs">/</span>
          <span className="text-[13px] text-[#555555] font-medium hidden sm:inline truncate max-w-[320px]">
            Technical Interview Room
          </span>
          <Badge variant="default" className="text-[11px] font-mono py-0">
            Question {currentQuestionNumber} of {totalQuestions}
          </Badge>
          {session?.turns?.some((t) => Boolean(t.isFollowUp)) && (
            <Badge variant="outline" className="text-[10px] font-medium border-[#111111]">
              Follow-up
            </Badge>
          )}
        </div>

        <div className="flex items-center gap-3">
          <div
            className="flex items-center gap-1.5 font-mono text-[12px] bg-[#F4F3F3] text-[#555555] px-2.5 py-1 rounded-md border border-[#E5E5E5]"
            title="Advisory countdown timer"
          >
            <Clock className="w-3.5 h-3.5 text-[#8A8A8A]" />
            <span>{formatCountdown(elapsedSeconds)}</span>
          </div>

          <button
            type="button"
            onClick={() => setShowExitModal(true)}
            className="text-[12px] font-medium text-[#555555] hover:text-[#DC2626] transition-colors cursor-pointer"
          >
            End session
          </button>
        </div>
      </div>

      {/* 2. Progress & Context Ribbon */}
      <div className="shrink-0 w-full bg-white border-b border-[#E5E5E5] py-2.5 px-4 sm:px-6 z-10 flex items-center justify-between gap-4">
        <div className="flex items-center gap-2.5">
          <span className="inline-flex items-center gap-1.5 text-[12px] font-medium px-2.5 py-0.5 rounded-full bg-[#EEEEEE] text-[#111111]">
            <span className="w-1.5 h-1.5 rounded-full bg-[#111111] animate-pulse" />
            Question {currentQuestionNumber} of {totalQuestions}
          </span>
          <span className="text-[#C4C7C7] text-xs hidden sm:inline">•</span>
          <span className="text-[12px] text-[#555555] hidden sm:inline font-medium">
            Live Technical Interview
          </span>
        </div>

        {/* Progress Dots */}
        <div
          className="flex items-center gap-1.5"
          title={`Question ${currentQuestionNumber} of ${totalQuestions}`}
        >
          {Array.from({ length: totalQuestions }).map((_, idx) => (
            <span
              key={idx}
              className={`w-5 h-1.5 rounded-full transition-colors ${
                idx < currentQuestionNumber ? "bg-[#111111]" : "bg-[#E5E5E5]"
              }`}
            />
          ))}
        </div>
      </div>

      {/* 3. Conversational Thread Canvas */}
      <div className="flex-1 overflow-y-auto px-4 sm:px-6 pt-5 pb-52 flex flex-col gap-6 scroll-smooth overscroll-contain">
        {/* Milestone Indicator */}
        <div className="flex justify-center">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#FAFAFA] text-[#555555] text-[12px] border border-[#E5E5E5]">
            <ShieldCheck className="w-3.5 h-3.5 text-[#16A34A]" />
            <span className="font-medium text-[#111111]">
              Technical Interview Room • Session #{sessionId}
            </span>
            <span className="text-[#C4C7C7]">•</span>
            <span className="text-[11px] font-mono">Real-time SDE Grading</span>
          </div>
        </div>

        {/* Turns list */}
        {session?.turns?.map((turn: InterviewTurn, turnIndex: number) => {
          const tid = turn.turnId || turn.id || String(turnIndex);
          const isAi = turn.type === "QUESTION" || turn.type === "FOLLOW_UP";
          const isFollowUp = Boolean(turn.isFollowUp || turn.type === "FOLLOW_UP");
          const fullContent = turn.body || turn.content || turn.answerText || "";
          const displayText =
            isFollowUp && typewriterContent[tid] !== undefined
              ? typewriterContent[tid]
              : fullContent;

          if (isAi) {
            return (
              <div
                key={tid}
                className="flex flex-col items-start max-w-[92%] sm:max-w-[85%] gap-1.5 self-start animate-in fade-in duration-200"
              >
                <div className="flex items-center gap-2 mb-0.5">
                  <div className="w-5 h-5 rounded-full bg-[#111111] flex items-center justify-center text-[10px] text-white font-bold">
                    AI
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="text-[12px] font-semibold text-[#111111]">
                      Interviewer
                    </span>
                    {isFollowUp ? (
                      <Badge
                        variant="outline"
                        className="text-[10px] py-0 px-1.5 border-[#111111] text-[#111111]"
                      >
                        Follow-up
                      </Badge>
                    ) : (
                      <Badge
                        variant="default"
                        className="text-[10px] py-0 px-1.5 bg-[#EEEEEE] text-[#555555]"
                      >
                        Question {turn.questionNumber ?? turn.questionIndex ?? currentQuestionNumber}
                      </Badge>
                    )}
                  </div>
                  {turn.createdAt && (
                    <span className="font-mono text-[11px] text-[#8A8A8A]">
                      {new Date(turn.createdAt).toLocaleTimeString([], {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </span>
                  )}
                </div>

                <div className="bg-[#F4F3F3] text-[#111111] p-4 rounded-2xl rounded-bl-xs border border-[#E5E5E5] w-full">
                  <p className="text-[14px] leading-relaxed whitespace-pre-wrap">
                    {displayText}
                  </p>

                  {/* Show rubric criteria names from each QUESTION turn (Requirement 6) */}
                  {turn.rubricCriteria && turn.rubricCriteria.length > 0 && (
                    <div className="mt-3.5 pt-3 border-t border-[#E5E5E5] flex flex-col gap-1.5 text-[#555555]">
                      <div className="flex items-center gap-1.5 text-[11px] font-semibold text-[#111111]">
                        <BrainCircuit className="w-3.5 h-3.5 text-[#111111]" />
                        <span>Grading Criteria:</span>
                      </div>
                      <div className="flex flex-wrap gap-1.5">
                        {turn.rubricCriteria.map((c, i) => (
                          <span
                            key={i}
                            className="px-2 py-0.5 bg-white border border-[#E5E5E5] rounded text-[11px] text-[#444444] font-medium"
                          >
                            {c.name}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              </div>
            );
          }

          // Candidate Answer Turn (No per-turn scores or feedback during the interview!)
          return (
            <div
              key={tid}
              className="flex flex-col items-end max-w-[92%] sm:max-w-[85%] gap-1.5 self-end animate-in fade-in duration-200"
            >
              <div className="flex items-center gap-2 mb-0.5">
                {turn.createdAt && (
                  <span className="font-mono text-[11px] text-[#8A8A8A]">
                    {new Date(turn.createdAt).toLocaleTimeString([], {
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </span>
                )}
                <span className="text-[12px] font-semibold text-[#555555]">
                  You
                </span>
                <div className="w-5 h-5 rounded-full bg-[#EEEEEE] flex items-center justify-center">
                  <User className="w-3 h-3 text-[#111111]" />
                </div>
              </div>

              <div className="bg-white text-[#111111] p-4 rounded-2xl rounded-br-xs border border-[#E5E5E5] shadow-xs">
                <p className="text-[14px] leading-relaxed whitespace-pre-wrap">
                  {fullContent}
                </p>
              </div>

              <div className="flex items-center gap-1 mr-1 text-[11px] font-mono text-[#555555]">
                <CheckCircle2 className="w-3.5 h-3.5 text-[#16A34A]" />
                <span>Recorded • Rubric grading on completion</span>
              </div>
            </div>
          );
        })}

        {/* Polling / Evaluation Indicator */}
        {isAwaitingEvaluation && (
          <div className="flex items-center gap-2.5 self-start px-3.5 py-1.5 rounded-full bg-[#F4F3F3] border border-[#E5E5E5] text-[12px] text-[#555555] animate-in fade-in duration-150">
            <div className="flex items-center gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-[#555555] animate-bounce" />
              <span
                className="w-1.5 h-1.5 rounded-full bg-[#555555] animate-bounce"
                style={{ animationDelay: "150ms" }}
              />
              <span
                className="w-1.5 h-1.5 rounded-full bg-[#555555] animate-bounce"
                style={{ animationDelay: "300ms" }}
              />
            </div>
            <span>
              AI interviewer evaluating response and calibrating next turn... (
              {evalPollingDuration}s)
            </span>
          </div>
        )}

        {/* FAILED Status Banner with Retry Button (Requirement 7) */}
        {session?.status === "FAILED" && (
          <div className="p-4 rounded-xl bg-[#FEF2F2] border border-[#FEE2E2] flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div className="flex items-center gap-2.5">
              <AlertTriangle className="w-5 h-5 text-[#DC2626] shrink-0" />
              <div className="flex flex-col">
                <span className="text-[14px] font-semibold text-[#991B1B]">
                  Evaluation encountered an issue
                </span>
                <span className="text-[12px] text-[#7F1D1D]">
                  The AI scoring pipeline could not complete. You can retry the evaluation without losing your answers.
                </span>
              </div>
            </div>

            <Button
              onClick={handleRetryEvaluations}
              disabled={isRetrying}
              className="gap-2 bg-[#991B1B] hover:bg-[#7F1D1D] text-white shrink-0 text-xs"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isRetrying ? "animate-spin" : ""}`} />
              <span>{isRetrying ? "Retrying..." : "Retry Evaluation"}</span>
            </Button>
          </div>
        )}

        {/* Hint Drawer: Static Generic Tips Only (Requirement 6) */}
        {showHint && (
          <div className="rounded-xl border border-[#111111] bg-white p-4 shadow-sm animate-in fade-in duration-200">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-2 text-[13px] font-semibold text-[#111111]">
                <Lightbulb className="w-4 h-4 text-[#D97706]" />
                <span>Academic Tip ({hintIndex + 1} of {STATIC_GENERIC_TIPS.length})</span>
              </div>
              <button
                type="button"
                onClick={() => setShowHint(false)}
                className="text-[#8A8A8A] hover:text-[#111111] transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <p className="text-[13px] text-[#555555] leading-relaxed mt-2">
              {STATIC_GENERIC_TIPS[hintIndex]}
            </p>
            <div className="mt-3 pt-2 border-t border-[#E5E5E5] flex justify-end">
              <button
                type="button"
                onClick={() =>
                  setHintIndex((prev) => (prev + 1) % STATIC_GENERIC_TIPS.length)
                }
                className="text-[11px] font-medium text-[#111111] hover:underline"
              >
                Next general tip →
              </button>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} className="h-2 shrink-0" />
      </div>

      {/* 4. Anchored Candidate Response Console */}
      <div className="shrink-0 w-full bg-white border-t border-[#E5E5E5] z-30 px-4 sm:px-6 pt-3 pb-4">
        {session?.status === "COMPLETED" ? (
          <div className="p-4 rounded-xl bg-[#FAFAFA] border border-[#111111] flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-[#DCFCE7] text-[#16A34A] flex items-center justify-center shrink-0">
                <Check className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-[15px] font-semibold text-[#111111]">
                  Interview Session Complete
                </h4>
                <p className="text-[12px] text-[#555555]">
                  All turns processed and graded against placement rubrics.
                </p>
              </div>
            </div>

            <Link href={`/practice/quiz/${sessionId}/report`}>
              <Button className="gap-2 shrink-0">
                <span>View Comprehensive Scorecard</span>
                <ArrowRight className="w-4 h-4" />
              </Button>
            </Link>
          </div>
        ) : (
          <div className="flex flex-col rounded-xl border border-[#E5E5E5] focus-within:border-[#111111] transition-colors bg-white shadow-xs">
            {submitError && (
              <div className="px-3.5 py-2 text-xs text-[#DC2626] bg-[#FEF2F2] border-b border-[#FEE2E2]">
                {submitError}
              </div>
            )}

            {/* Candidate text input */}
            <textarea
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              onKeyDown={handleKeyDown}
              disabled={isSubmitting || isAwaitingEvaluation}
              placeholder={
                isAwaitingEvaluation
                  ? "Awaiting AI evaluation..."
                  : "Type your technical answer here... Be specific about trade-offs, edge cases, and runtime complexity."
              }
              rows={3}
              className="w-full p-3.5 text-[14px] text-[#111111] placeholder:text-[#8A8A8A] bg-transparent border-0 resize-none focus:outline-none disabled:bg-[#FAFAFA] disabled:cursor-not-allowed"
            />

            {/* Sub-bar Controls */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 px-3.5 py-2.5 bg-white border-t border-[#E5E5E5] rounded-b-xl">
              {/* Left metrics & static tips */}
              <div className="flex items-center gap-3">
                <span className="font-mono text-[11px] text-[#8A8A8A]">
                  {inputText.length} / 2,000 characters
                </span>
                <span className="text-[#C4C7C7] text-xs hidden sm:inline">•</span>
                <button
                  type="button"
                  onClick={() => setShowHint(!showHint)}
                  className="inline-flex items-center gap-1 text-[12px] text-[#555555] hover:text-[#111111] transition-colors cursor-pointer"
                >
                  <HelpCircle className="w-3.5 h-3.5 text-[#D97706]" />
                  <span>{showHint ? "Hide tips" : "General advice"}</span>
                </button>
              </div>

              {/* Right: Voice toggle & Submit Button */}
              <div className="flex items-center justify-between sm:justify-end gap-2">
                <button
                  type="button"
                  onClick={toggleVoiceRecording}
                  disabled={isSubmitting || isAwaitingEvaluation}
                  title={isRecording ? "Stop recording" : "Record voice response"}
                  className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border text-[12px] font-medium transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed ${
                    isRecording
                      ? "border-[#FCA5A5] bg-[#FEF2F2] text-[#DC2626] animate-pulse"
                      : "border-[#E5E5E5] bg-white text-[#555555] hover:text-[#111111] hover:border-[#111111]"
                  }`}
                >
                  {isRecording ? <MicOff className="w-3.5 h-3.5" /> : <Mic className="w-3.5 h-3.5" />}
                  <span>{isRecording ? "Listening..." : "Voice input"}</span>
                </button>

                <Button
                  onClick={handleSubmitAnswer}
                  disabled={!inputText.trim() || isSubmitting || isAwaitingEvaluation}
                  size="sm"
                  className="gap-1.5 px-4"
                >
                  <span>{isSubmitting ? "Submitting..." : "Submit answer"}</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* Footer shortcuts */}
        <div className="flex items-center justify-between mt-2 px-1 text-[11px] font-mono text-[#8A8A8A]">
          <span>
            Press{" "}
            <kbd className="px-1 py-0.5 rounded bg-[#EEEEEE] border border-[#E5E5E5] text-[#111111]">
              ⌘ + Enter
            </kbd>{" "}
            to submit
          </span>
          <span>Idempotent turn submission • Session #{sessionId}</span>
        </div>
      </div>

      {/* Exit & Scorecard Confirmation Modal */}
      {showExitModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="w-full max-w-md bg-white rounded-2xl border border-[#111111] shadow-2xl p-6 flex flex-col gap-4">
            <h3 className="text-[17px] font-semibold text-[#111111]">
              End Mock Interview Session?
            </h3>
            <p className="text-[13px] text-[#555555] leading-relaxed">
              Ending the interview session will trigger completion and grade all turns submitted so far against rubric criteria.
            </p>

            <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-[#E5E5E5]">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setShowExitModal(false)}
              >
                Continue Interview
              </Button>
              <Button
                size="sm"
                onClick={handleEndSession}
                disabled={isEnding}
                className="gap-1.5"
              >
                <span>{isEnding ? "Completing..." : "End & Generate Scorecard"}</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
